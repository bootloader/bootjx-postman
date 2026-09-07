package com.boot.jx.postman.rest;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.boot.jx.rest.AjaxRestService;
import com.boot.jx.rest.RestService;
import com.boot.jx.rest.RestService.Ajax;

@Component
public class RestNoSSLService implements AjaxRestService {

	RestTemplate restTemplate;

	public RestTemplate restTemplate() {

		if (restTemplate == null) {
			TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
			SSLContext sslContext;
			try {
				sslContext = org.apache.http.ssl.SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy)
						.build();

				SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext,
						NoopHostnameVerifier.INSTANCE);

				CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();

				HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();

				requestFactory.setHttpClient(httpClient);
				restTemplate = new RestTemplate(requestFactory);
				restService.getLocalRestTemplate(restTemplate);

			} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
				e.printStackTrace();
			}
		}

		return restTemplate;
	}

	@Autowired
	RestService restService;

	public Ajax ajax(String url) {
		return restService.ajax(restTemplate(), url);
	}

}
