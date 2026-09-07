package com.boot.jx.postman.rest;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.boot.jx.logger.LoggerService;

public class RestBenchMarking {

	public static final Logger LOGGER = LoggerService.getLogger(RestBenchMarking.class);

	public static class RestBenchStat {
		public int requests;
		public int errors;
		public long totalMs;
		public double avgMs;
		public long minMs;
		public long maxMs;
		public double throughputPerSec;

		public static RestBenchStat from(int calls, long totalMs, long[] latenciesMs, int errors) {
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
			RestBenchStat stat = new RestBenchStat();
			stat.requests = calls;
			stat.errors = errors;
			stat.totalMs = totalMs;
			stat.avgMs = ok == 0 ? 0 : (double) sum / ok;
			stat.minMs = ok == 0 ? 0 : min;
			stat.maxMs = ok == 0 ? 0 : max;
			stat.throughputPerSec = totalMs == 0 ? 0 : calls / (totalMs / 1000.0);
			return stat;
		}
	}

	public static int callCount(int warmup, int actual) {
		return Math.min(warmup, actual);
	}

	public static Field findField(Class<?> type, String name) throws NoSuchFieldException {
		for (Class<?> c = type; c != null; c = c.getSuperclass()) {
			try {
				return c.getDeclaredField(name);
			} catch (NoSuchFieldException ignored) {
				// try superclass
			}
		}
		throw new NoSuchFieldException(name + " not found on " + type + " or its superclasses");
	}

	/**
	 * Reaches past {@code HttpComponentsClientHttpRequestFactory} /
	 * {@code CloseableHttpClient} (neither exposes the pooling manager directly) to
	 * the real {@link PoolingHttpClientConnectionManager} {@link RestPooledService}
	 * built, purely for read-only stats - no production code needs to change for
	 * this.
	 */
	public static PoolingHttpClientConnectionManager getPooledConnectionManager(RestTemplate pooledRestTemplate) {
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

	public static Map<String, Object> summarize(RestBenchStat unpooled, RestBenchStat pooled) {
		double avgDeltaPct = unpooled.avgMs == 0 ? 0 : 100.0 * (unpooled.avgMs - pooled.avgMs) / unpooled.avgMs;
		double throughputDeltaPct = unpooled.throughputPerSec == 0 ? 0
				: 100.0 * (pooled.throughputPerSec - unpooled.throughputPerSec) / unpooled.throughputPerSec;
		Map<String, Object> summary = new LinkedHashMap<>();
		summary.put("pooledAvgLatencyVsUnpooled",
				String.format("%.1f%% %s", Math.abs(avgDeltaPct), avgDeltaPct >= 0 ? "lower" : "higher"));
		summary.put("pooledThroughputVsUnpooled",
				String.format("%.1f%% %s", Math.abs(throughputDeltaPct), throughputDeltaPct >= 0 ? "higher" : "lower"));
		summary.put("note",
				"Wall-clock is noisy against a public endpoint - treat pooledConnectionPoolState as the more reliable signal of connection reuse.");
		return summary;
	}

	public static long timeCall(Callable<String> call, AtomicInteger errors) {
		long callStart = System.nanoTime();
		try {
			call.call();
		} catch (Exception e) {
			errors.incrementAndGet();
			LOGGER.warn("bench-rest call failed: {}", e.getMessage());
		}
		return (System.nanoTime() - callStart) / 1_000_000;
	}

	public static RestBenchStat runBench(int calls, int threads, Callable<String> call) {
		long[] latenciesMs = new long[calls];
		AtomicInteger errors = new AtomicInteger();
		long start = System.nanoTime();
		if (threads <= 1) {
			for (int i = 0; i < calls; i++) {
				latenciesMs[i] = timeCall(call, errors);
			}
		} else {
			ExecutorService executor = Executors.newFixedThreadPool(threads);
			try {
				List<Future<Long>> futures = new java.util.ArrayList<>(calls);
				for (int i = 0; i < calls; i++) {
					futures.add(executor.submit(() -> timeCall(call, errors)));
				}
				for (int i = 0; i < futures.size(); i++) {
					try {
						latenciesMs[i] = futures.get(i).get();
					} catch (Exception e) {
						latenciesMs[i] = -1;
					}
				}
			} finally {
				executor.shutdown();
			}
		}
		long totalMs = (System.nanoTime() - start) / 1_000_000;
		return RestBenchStat.from(calls, totalMs, latenciesMs, errors.get());
	}

}
