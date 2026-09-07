package com.amx.tests;

import java.lang.reflect.Field;
import java.net.InetAddress;
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

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.pool.PoolStats;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import com.boot.jx.AppConfig;
import com.boot.jx.JasyptEncryptorConfig;
import com.boot.jx.filter.AppClientErrorHanlder;
import com.boot.jx.filter.AppClientInterceptor;
import com.boot.jx.postman.rest.RestPooledService;
import com.boot.jx.rest.RestService;
import com.boot.jx.scope.tnt.TenantProperties;

/**
 * Same "unpooled vs pooled" comparison as {@link TestRestService}, but
 * exercising the actual production beans - {@link RestService} (the default,
 * unpooled {@code AjaxRestService}) and {@link RestPooledService} (pooled)
 * via their real {@code ajax(url)} entry point, wired up by a real Spring
 * context (interceptors, meta filters, error handler, etc. all included).
 *
 * {@link TestRestService} deliberately rebuilt bare {@code RestTemplate}s to
 * isolate the pooling mechanism from Spring plumbing; this test instead runs
 * exactly the call path production code (e.g. {@code WacfbClient}) would use,
 * so any wiring bugs (missing bean, interceptor throwing, etc.) show up here
 * too, not just the connection-reuse question.
 *
 * Requires a Spring context (unlike {@code TestRestService}'s plain
 * {@code main}), so this is a real {@code @Test}, run via
 * {@code mvn -Dtest=TestRestServiceReal test}. Still not asserting anything -
 * it's a printout, not a pass/fail check - since we're reading timing/manual
 * output, not verifying a fixed contract.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestRestServiceReal.MinimalTestConfig.class)
public class TestRestServiceReal {

	/**
	 * lib-postman is a library module with no {@code @SpringBootApplication} of
	 * its own (those only exist in the server- and ms- modules that depend on
	 * this one), so {@code @SpringBootTest} can't auto-discover a config.
	 * Rather than component-scanning all of {@code com.boot.jx} (which would
	 * also pull in DB/Mongo/AWS beans this module doesn't actually need and
	 * can't satisfy in a test), explicitly wire up just the beans
	 * {@link RestService}/{@link RestPooledService} need: {@link AppConfig}
	 * (provides the default {@code RestTemplate} bean + app properties),
	 * {@link AppClientInterceptor}, {@link AppClientErrorHanlder},
	 * {@link TenantProperties}.
	 */
	@Configuration
	@Import({ AppConfig.class, JasyptEncryptorConfig.class, AppClientInterceptor.class, AppClientErrorHanlder.class,
			TenantProperties.class, RestService.class, RestPooledService.class })
	@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
			MongoAutoConfiguration.class, MongoDataAutoConfiguration.class })
	static class MinimalTestConfig {
	}

	private static final String TARGET_URL = "https://jsonplaceholder.typicode.com/todos/1";

	private static final int WARMUP_CALLS = 5;
	private static final int SEQUENTIAL_CALLS = 20;
	private static final int CONCURRENT_THREADS = 20;
	private static final int CALLS_PER_THREAD = 10; // 200 total concurrent calls

	private static final AtomicInteger UNPOOLED_CONNECTIONS_OPENED = new AtomicInteger();

	@Autowired
	private RestService restService;

	@Autowired
	private RestPooledService restPooledService;

	@Test
	public void compareRestServiceVsRestPooledService() throws Exception {
		// Count every *new* physical TLS socket opened JVM-wide, so the
		// unpooled RestService's real HttpsURLConnection-based calls can be
		// measured the same way as the pooled side below - independent of
		// third-party response-time jitter.
		HttpsURLConnection.setDefaultSSLSocketFactory(new CountingSSLSocketFactory(
				(SSLSocketFactory) SSLSocketFactory.getDefault(), UNPOOLED_CONNECTIONS_OPENED));

		// Force RestPooledService to build its RestTemplate/HttpClient now,
		// so we can reach into its real PoolingHttpClientConnectionManager.
		PoolingHttpClientConnectionManager pooledConnectionManager = getConnectionManager(
				restPooledService.restTemplate());

		System.out.println("Target: " + TARGET_URL);
		System.out.println(
				"Warming up (JIT + first connection), " + WARMUP_CALLS + " calls per client...");
		runSequential(this::callViaRestService, WARMUP_CALLS);
		runSequential(this::callViaRestPooledService, WARMUP_CALLS);
		UNPOOLED_CONNECTIONS_OPENED.set(0);
		pooledConnectionManager.closeIdleConnections(0, TimeUnit.MILLISECONDS);

		System.out.println();
		System.out.println("=== Sequential: 1 thread, " + SEQUENTIAL_CALLS + " calls ===");
		Stats seqUnpooled = runSequential(this::callViaRestService, SEQUENTIAL_CALLS);
		int seqUnpooledConns = UNPOOLED_CONNECTIONS_OPENED.getAndSet(0);
		Stats seqPooled = runSequential(this::callViaRestPooledService, SEQUENTIAL_CALLS);
		PoolStats seqPoolStats = pooledConnectionManager.getTotalStats();
		pooledConnectionManager.closeIdleConnections(0, TimeUnit.MILLISECONDS);
		seqUnpooled.print("RestService        (unpooled)");
		seqPooled.print("RestPooledService   (pooled)");
		printDelta(seqUnpooled, seqPooled);
		printConnections(SEQUENTIAL_CALLS, seqUnpooledConns, seqPoolStats);

		System.out.println();
		int totalConcurrent = CONCURRENT_THREADS * CALLS_PER_THREAD;
		System.out.println("=== Concurrent: " + CONCURRENT_THREADS + " threads x " + CALLS_PER_THREAD
				+ " calls = " + totalConcurrent + " total ===");
		Stats concUnpooled = runConcurrent(this::callViaRestService, CONCURRENT_THREADS, CALLS_PER_THREAD);
		int concUnpooledConns = UNPOOLED_CONNECTIONS_OPENED.getAndSet(0);
		Stats concPooled = runConcurrent(this::callViaRestPooledService, CONCURRENT_THREADS, CALLS_PER_THREAD);
		PoolStats concPoolStats = pooledConnectionManager.getTotalStats();
		pooledConnectionManager.closeIdleConnections(0, TimeUnit.MILLISECONDS);
		concUnpooled.print("RestService        (unpooled)");
		concPooled.print("RestPooledService   (pooled)");
		printDelta(concUnpooled, concPooled);
		printConnections(totalConcurrent, concUnpooledConns, concPoolStats);
	}

	/** Exactly what production code calling the default bean would write. */
	private long callViaRestService() {
		long start = System.nanoTime();
		try {
			restService.ajax(TARGET_URL).acceptJson().get().asString();
		} catch (Exception e) {
			System.err.println("RestService call failed: " + e.getMessage());
		}
		return (System.nanoTime() - start) / 1_000_000;
	}

	/** Exactly what production code calling the pooled bean would write. */
	private long callViaRestPooledService() {
		long start = System.nanoTime();
		try {
			restPooledService.ajax(TARGET_URL).acceptJson().get().asString();
		} catch (Exception e) {
			System.err.println("RestPooledService call failed: " + e.getMessage());
		}
		return (System.nanoTime() - start) / 1_000_000;
	}

	/**
	 * Reaches past {@code HttpComponentsClientHttpRequestFactory} /
	 * {@code CloseableHttpClient} (neither exposes the pooling manager
	 * directly) to the real {@link PoolingHttpClientConnectionManager}
	 * {@code RestPooledService} built, purely for read-only stats/reset - no
	 * production code needs to change for this.
	 */
	private static PoolingHttpClientConnectionManager getConnectionManager(RestTemplate pooledRestTemplate) {
		try {
			ClientHttpRequestFactory factory = pooledRestTemplate.getRequestFactory();
			// RestService.getLocalRestTemplate() sets interceptors, so the RestTemplate
			// actually hands out an InterceptingClientHttpRequestFactory wrapping the
			// real HttpComponentsClientHttpRequestFactory - unwrap it first.
			while (!(factory instanceof HttpComponentsClientHttpRequestFactory)) {
				Field delegateField = findField(factory.getClass(), "requestFactory");
				delegateField.setAccessible(true);
				factory = (ClientHttpRequestFactory) delegateField.get(factory);
			}
			CloseableHttpClient httpClient = (CloseableHttpClient) ((HttpComponentsClientHttpRequestFactory) factory)
					.getHttpClient();
			Field connManagerField = findField(httpClient.getClass(), "connManager");
			connManagerField.setAccessible(true);
			return (PoolingHttpClientConnectionManager) connManagerField.get(httpClient);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(
					"Couldn't reach into HttpClient's connection manager via reflection (httpclient/spring internals may have changed)",
					e);
		}
	}

	private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
		for (Class<?> c = type; c != null; c = c.getSuperclass()) {
			try {
				return c.getDeclaredField(name);
			} catch (NoSuchFieldException ignored) {
				// try superclass
			}
		}
		throw new NoSuchFieldException(name + " not found on " + type + " or its superclasses");
	}

	private static Stats runSequential(Callable<Long> call, int calls) throws Exception {
		long[] latenciesMs = new long[calls];
		long start = System.nanoTime();
		for (int i = 0; i < calls; i++) {
			latenciesMs[i] = call.call();
		}
		long totalMs = (System.nanoTime() - start) / 1_000_000;
		return Stats.from(calls, totalMs, latenciesMs);
	}

	private static Stats runConcurrent(Callable<Long> call, int threads, int callsPerThread) throws Exception {
		int totalCalls = threads * callsPerThread;
		List<Callable<Long>> tasks = new ArrayList<>(totalCalls);
		for (int i = 0; i < totalCalls; i++) {
			tasks.add(call);
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

	private static void printDelta(Stats before, Stats after) {
		double avgDeltaPct = 100.0 * (before.avgMs - after.avgMs) / before.avgMs;
		double throughputDeltaPct = 100.0 * (after.throughputPerSec - before.throughputPerSec)
				/ before.throughputPerSec;
		System.out.printf("  -> pooled avg latency %.1f%% %s, throughput %.1f%% %s%n", Math.abs(avgDeltaPct),
				avgDeltaPct >= 0 ? "lower" : "higher", Math.abs(throughputDeltaPct),
				throughputDeltaPct >= 0 ? "higher" : "lower");
	}

	private static void printConnections(int totalCalls, int unpooledConns, PoolStats pooledStatsAfterPhase) {
		System.out.printf(
				"  connections (of %d calls):  unpooled new-sockets=%-4d   pooled pool-state=%s (leased+available = distinct sockets pool is holding)%n",
				totalCalls, unpooledConns, pooledStatsAfterPhase);
	}

	/**
	 * Delegates every call to a real {@link SSLSocketFactory}, but counts
	 * calls to any {@code createSocket} overload - i.e. every time a
	 * brand-new physical socket is created. Reused HttpsURLConnection
	 * keep-alive connections never go through {@code createSocket} again, so
	 * this counter is exactly "how many TCP+TLS handshakes actually
	 * happened", untouched by response-time jitter.
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
		public java.net.Socket createSocket(java.net.Socket s, String host, int port, boolean autoClose)
				throws java.io.IOException {
			counter.incrementAndGet();
			return delegate.createSocket(s, host, port, autoClose);
		}

		@Override
		public java.net.Socket createSocket(String host, int port) throws java.io.IOException {
			counter.incrementAndGet();
			return delegate.createSocket(host, port);
		}

		@Override
		public java.net.Socket createSocket(String host, int port, InetAddress localHost, int localPort)
				throws java.io.IOException {
			counter.incrementAndGet();
			return delegate.createSocket(host, port, localHost, localPort);
		}

		@Override
		public java.net.Socket createSocket(InetAddress host, int port) throws java.io.IOException {
			counter.incrementAndGet();
			return delegate.createSocket(host, port);
		}

		@Override
		public java.net.Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
				throws java.io.IOException {
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
