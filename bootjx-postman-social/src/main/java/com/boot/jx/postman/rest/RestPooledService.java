package com.boot.jx.postman.rest;

import java.util.concurrent.TimeUnit;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.boot.jx.rest.AjaxRestService;
import com.boot.jx.rest.RestService;
import com.boot.jx.rest.RestService.Ajax;

/**
 * {@link AjaxRestService} backed by a pooled, timeout-bound
 * {@link RestTemplate}, for clients that make frequent/concurrent calls to a
 * small set of stable external hosts (e.g. the WhatsApp/Meta Graph API via
 * {@code WacfbClient}).
 *
 * Unlike the default {@code RestService} bean (JDK {@code HttpURLConnection},
 * no explicit pool), this reuses TCP/TLS connections across calls via Apache
 * {@code HttpClient}'s {@code PoolingHttpClientConnectionManager}, which cuts
 * repeated handshake cost under concurrent/bursty load (bulk sends) and avoids
 * unbounded connect/read waits.
 *
 * Sibling of {@link RestNoSSLService} (SSL-relaxed internal calls) and
 * {@code RestHookService} (customer webhook calls) — same "dedicated
 * RestTemplate behind a small wrapper" pattern, tuned for pooling instead.
 */
@Component
public class RestPooledService implements AjaxRestService {

	@Value("${app.resttemplate.pooled.max-total:200}")
	private int maxTotal;

	@Value("${app.resttemplate.pooled.max-per-route:100}")
	private int maxPerRoute;

	@Value("${app.resttemplate.pooled.connect-timeout:5000}")
	private int connectTimeoutMs;

	@Value("${app.resttemplate.pooled.read-timeout:15000}")
	private int readTimeoutMs;

	@Value("${app.resttemplate.pooled.idle-eviction-seconds:30}")
	private int idleEvictionSeconds;

	@Autowired
	private RestService restService;

	private volatile RestTemplate restTemplate;

	public RestTemplate restTemplate() {
		if (restTemplate == null) {
			synchronized (this) {
				if (restTemplate == null) {
					CloseableHttpClient httpClient = HttpClients.custom().setMaxConnTotal(maxTotal)
							.setMaxConnPerRoute(maxPerRoute)
							.setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(connectTimeoutMs)
									.setSocketTimeout(readTimeoutMs).build())
							// Evict connections the remote host may have silently closed, so a
							// stale pooled connection doesn't surface as a "connection reset"
							// under bursty send traffic.
							.evictIdleConnections(idleEvictionSeconds, TimeUnit.SECONDS).evictExpiredConnections()
							.build();

					HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
							httpClient);
					requestFactory.setConnectTimeout(connectTimeoutMs);
					requestFactory.setReadTimeout(readTimeoutMs);

					RestTemplate localRestTemplate = new RestTemplate(requestFactory);
					restService.getLocalRestTemplate(localRestTemplate);
					restTemplate = localRestTemplate;
				}
			}
		}
		return restTemplate;
	}

	@Override
	public Ajax ajax(String url) {
		return restService.ajax(restTemplate(), url);
	}

}
