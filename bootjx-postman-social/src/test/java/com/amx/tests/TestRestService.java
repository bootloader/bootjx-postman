package com.amx.tests;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Standalone micro-benchmark (plain {@code main}, no Spring context / DI
 * needed) comparing:
 *
 * <ul>
 * <li><b>Unpooled</b> - the same {@link SimpleClientHttpRequestFactory}
 * config the default {@code RestService} bean currently uses
 * ({@code AppConfig#restTemplate} in bootjx-common-lib).</li>
 * <li><b>Pooled</b> - the same Apache HttpClient
 * {@code PoolingHttpClientConnectionManager} config
 * {@code com.boot.jx.postman.rest.RestPooledService#restTemplate()} builds.</li>
 * </ul>
 *
 * Deliberately rebuilds the two configs here rather than instantiating
 * {@code RestService}/{@code RestPooledService} directly, since those are
 * Spring beans wired for header/interceptor/meta-info plumbing that's
 * unrelated to what we're actually measuring (connection reuse), and would
 * need a full application context to exercise safely.
 *
 * Hits a small, free, unauthenticated public JSON endpoint so this is safe
 * to run without WhatsApp/Meta credentials - same request/response shape
 * (small JSON over HTTPS) as a WABA Graph API call, just without auth.
 *
 * Run directly as a Java application. Not a JUnit test / not part of the
 * build - this is a one-off "is pooling actually worth it here" sanity
 * check.
 *
 * Wall-clock latency/throughput against a third-party host you don't
 * control is noisy - re-run a few times and you'll see the delta shrink,
 * vanish, or flip sign, especially at only ~100 calls. That noise doesn't
 * mean pooling "doesn't work"; it means end-to-end timing isn't a reliable
 * way to observe it at this scale. So alongside the timing, this also
 * counts the number of *physical* TCP/TLS connections each client actually
 * opens (via a counting {@link SSLSocketFactory} for the unpooled/JDK path,
 * and a counting {@link ConnectionSocketFactory} for the pooled/Apache
 * path). That count is deterministic-ish and directly reflects the
 * mechanism pooling changes, independent of how fast the far end happened
 * to respond on any given run: unpooled should open roughly one connection
 * per concurrently-in-flight request (bounded above by concurrency, not by
 * total calls, since the JDK still reuses within a single thread's own
 * sequential calls), while pooled should open roughly one connection per
 * concurrency level and then reuse it across every subsequent call from any
 * thread, however many total calls are made.
 */
public class TestRestService {

	private static final String TARGET_URL = "https://jsonplaceholder.typicode.com/todos/1";

	private static final int WARMUP_CALLS = 5;
	private static final int SEQUENTIAL_CALLS = 20;
	private static final int CONCURRENT_THREADS = 20;
	private static final int CALLS_PER_THREAD = 10; // 200 total concurrent calls

	private static final AtomicInteger UNPOOLED_CONNECTIONS_OPENED = new AtomicInteger();
	private static final AtomicInteger POOLED_CONNECTIONS_OPENED = new AtomicInteger();

	public static void main(String[] args) throws Exception {

		RestTemplate unpooled = buildUnpooledRestTemplate();
		RestTemplate pooled = buildPooledRestTemplate();

		System.out.println("Target: " + TARGET_URL);
		System.out.println("Warming up (JIT + first connection), " + WARMUP_CALLS + " calls per client...");
		runSequential(unpooled, WARMUP_CALLS);
		runSequential(pooled, WARMUP_CALLS);
		UNPOOLED_CONNECTIONS_OPENED.set(0);
		POOLED_CONNECTIONS_OPENED.set(0);

		System.out.println();
		System.out.println("=== Sequential: 1 thread, " + SEQUENTIAL_CALLS + " calls ===");
		Stats seqUnpooled = runSequential(unpooled, SEQUENTIAL_CALLS);
		int seqUnpooledConns = UNPOOLED_CONNECTIONS_OPENED.getAndSet(0);
		Stats seqPooled = runSequential(pooled, SEQUENTIAL_CALLS);
		int seqPooledConns = POOLED_CONNECTIONS_OPENED.getAndSet(0);
		seqUnpooled.print("RestService-style   (unpooled)");
		seqPooled.print("RestPooledService-style (pooled)");
		printDelta(seqUnpooled, seqPooled);
		printConnections(SEQUENTIAL_CALLS, seqUnpooledConns, seqPooledConns);

		System.out.println();
		int totalConcurrent = CONCURRENT_THREADS * CALLS_PER_THREAD;
		System.out.println("=== Concurrent: " + CONCURRENT_THREADS + " threads x " + CALLS_PER_THREAD + " calls = "
				+ totalConcurrent + " total ===");
		Stats concUnpooled = runConcurrent(unpooled, CONCURRENT_THREADS, CALLS_PER_THREAD);
		int concUnpooledConns = UNPOOLED_CONNECTIONS_OPENED.getAndSet(0);
		Stats concPooled = runConcurrent(pooled, CONCURRENT_THREADS, CALLS_PER_THREAD);
		int concPooledConns = POOLED_CONNECTIONS_OPENED.getAndSet(0);
		concUnpooled.print("RestService-style   (unpooled)");
		concPooled.print("RestPooledService-style (pooled)");
		printDelta(concUnpooled, concPooled);
		printConnections(totalConcurrent, concUnpooledConns, concPooledConns);
	}

	/**
	 * Mirrors the current default {@code RestService} bean: no pool, no
	 * timeouts. Installs a counting {@link SSLSocketFactory} JVM-wide so we can
	 * see how many *new* TLS sockets {@link HttpsURLConnection} actually opens
	 * versus how many it served out of its own internal keep-alive cache.
	 */
	private static RestTemplate buildUnpooledRestTemplate() {
		HttpsURLConnection.setDefaultSSLSocketFactory(
				new CountingSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault(),
						UNPOOLED_CONNECTIONS_OPENED));
		return new RestTemplate(new SimpleClientHttpRequestFactory());
	}

	/**
	 * Mirrors {@code com.boot.jx.postman.rest.RestPooledService#restTemplate()}'s
	 * defaults, with a counting {@link ConnectionSocketFactory} swapped in for
	 * https so we can see how many *new* sockets the pool actually creates
	 * versus how many calls are served by handing out an already-open pooled
	 * connection.
	 */
	private static RestTemplate buildPooledRestTemplate() {
		ConnectionSocketFactory countingSslFactory = new ConnectionSocketFactory() {
			private final SSLConnectionSocketFactory delegate = SSLConnectionSocketFactory.getSocketFactory();

			@Override
			public Socket createSocket(HttpContext context) throws IOException {
				POOLED_CONNECTIONS_OPENED.incrementAndGet();
				return delegate.createSocket(context);
			}

			@Override
			public Socket connectSocket(int connectTimeout, Socket sock, HttpHost host,
					InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context)
					throws IOException {
				return delegate.connectSocket(connectTimeout, sock, host, remoteAddress, localAddress, context);
			}
		};

		Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("https", countingSslFactory).register("http", PlainConnectionSocketFactory.getSocketFactory())
				.build();

		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
		connectionManager.setMaxTotal(200);
		connectionManager.setDefaultMaxPerRoute(100);

		CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connectionManager)
				.setDefaultRequestConfig(
						RequestConfig.custom().setConnectTimeout(5000).setSocketTimeout(15000).build())
				.evictIdleConnections(30, TimeUnit.SECONDS).evictExpiredConnections().build();

		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
				httpClient);
		requestFactory.setConnectTimeout(5000);
		requestFactory.setReadTimeout(15000);
		return new RestTemplate(requestFactory);
	}

	private static Stats runSequential(RestTemplate restTemplate, int calls) {
		long[] latenciesMs = new long[calls];
		long start = System.nanoTime();
		for (int i = 0; i < calls; i++) {
			latenciesMs[i] = call(restTemplate);
		}
		long totalMs = (System.nanoTime() - start) / 1_000_000;
		return Stats.from(calls, totalMs, latenciesMs);
	}

	private static Stats runConcurrent(RestTemplate restTemplate, int threads, int callsPerThread)
			throws InterruptedException {
		int totalCalls = threads * callsPerThread;
		List<Callable<Long>> tasks = new ArrayList<>(totalCalls);
		for (int i = 0; i < totalCalls; i++) {
			tasks.add(() -> call(restTemplate));
		}

		ExecutorService executor = Executors.newFixedThreadPool(threads);
		List<Future<Long>> futures;
		long start = System.nanoTime();
		try {
			futures = executor.invokeAll(tasks);
		} finally {
			executor.shutdown();
			executor.awaitTermination(1, TimeUnit.MINUTES);
		}
		long totalMs = (System.nanoTime() - start) / 1_000_000;

		long[] latenciesMs = new long[futures.size()];
		for (int i = 0; i < futures.size(); i++) {
			try {
				latenciesMs[i] = futures.get(i).get();
			} catch (Exception e) {
				latenciesMs[i] = -1;
			}
		}
		return Stats.from(totalCalls, totalMs, latenciesMs);
	}

	private static long call(RestTemplate restTemplate) {
		long callStart = System.nanoTime();
		try {
			restTemplate.getForObject(TARGET_URL, String.class);
		} catch (Exception e) {
			System.err.println("Call failed: " + e.getMessage());
		}
		return (System.nanoTime() - callStart) / 1_000_000;
	}

	private static void printDelta(Stats before, Stats after) {
		double avgDeltaPct = 100.0 * (before.avgMs - after.avgMs) / before.avgMs;
		double throughputDeltaPct = 100.0 * (after.throughputPerSec - before.throughputPerSec)
				/ before.throughputPerSec;
		System.out.printf("  -> pooled avg latency %.1f%% %s, throughput %.1f%% %s%n", Math.abs(avgDeltaPct),
				avgDeltaPct >= 0 ? "lower" : "higher", Math.abs(throughputDeltaPct),
				throughputDeltaPct >= 0 ? "higher" : "lower");
	}

	private static void printConnections(int totalCalls, int unpooledConns, int pooledConns) {
		System.out.printf(
				"  connections opened (of %d calls):   unpooled=%-4d pooled=%-4d  (lower = more reuse; this is the noise-free signal)%n",
				totalCalls, unpooledConns, pooledConns);
	}

	/**
	 * Delegates every call to a real {@link SSLSocketFactory}, but counts calls
	 * to any {@code createSocket} overload - i.e. every time a *brand-new*
	 * physical socket is created. Reused keep-alive connections never go
	 * through {@code createSocket} again, so this counter is exactly "how many
	 * TCP+TLS handshakes actually happened", untouched by response-time jitter.
	 */
	private static final class CountingSSLSocketFactory extends SSLSocketFactory {
		private final SSLSocketFactory delegate;
		private final AtomicInteger counter;

		CountingSSLSocketFactory(SSLSocketFactory delegate, AtomicInteger counter) {
			this.delegate = delegate;
			this.counter = counter;
		}

		@Override
		public String[] getDefaultCipherSuites() {
			return delegate.getDefaultCipherSuites();
		}

		@Override
		public String[] getSupportedCipherSuites() {
			return delegate.getSupportedCipherSuites();
		}

		@Override
		public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
			counter.incrementAndGet();
			return delegate.createSocket(s, host, port, autoClose);
		}

		@Override
		public Socket createSocket(String host, int port) throws IOException {
			counter.incrementAndGet();
			return delegate.createSocket(host, port);
		}

		@Override
		public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
			counter.incrementAndGet();
			return delegate.createSocket(host, port, localHost, localPort);
		}

		@Override
		public Socket createSocket(InetAddress host, int port) throws IOException {
			counter.incrementAndGet();
			return delegate.createSocket(host, port);
		}

		@Override
		public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
				throws IOException {
			counter.incrementAndGet();
			return delegate.createSocket(address, port, localAddress, localPort);
		}
	}

	private static final class Stats {
		final long totalMs;
		final double avgMs;
		final long minMs;
		final long maxMs;
		final double throughputPerSec;

		private Stats(long totalMs, double avgMs, long minMs, long maxMs, double throughputPerSec) {
			this.totalMs = totalMs;
			this.avgMs = avgMs;
			this.minMs = minMs;
			this.maxMs = maxMs;
			this.throughputPerSec = throughputPerSec;
		}

		static Stats from(int count, long totalMs, long[] latenciesMs) {
			long sum = 0;
			long min = Long.MAX_VALUE;
			long max = Long.MIN_VALUE;
			int ok = 0;
			for (long l : latenciesMs) {
				if (l < 0) {
					continue;
				}
				sum += l;
				min = Math.min(min, l);
				max = Math.max(max, l);
				ok++;
			}
			double avg = ok == 0 ? 0 : (double) sum / ok;
			double throughput = totalMs == 0 ? 0 : count / (totalMs / 1000.0);
			return new Stats(totalMs, avg, ok == 0 ? 0 : min, ok == 0 ? 0 : max, throughput);
		}

		void print(String label) {
			System.out.printf("  %-32s total=%5dms  avg=%6.1fms  min=%4dms  max=%5dms  throughput=%6.1f req/s%n",
					label, totalMs, avgMs, minMs, maxMs, throughputPerSec);
		}
	}

}
