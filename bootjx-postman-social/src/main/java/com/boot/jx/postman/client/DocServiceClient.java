package com.boot.jx.postman.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.boot.jx.AppConfig;
import com.boot.jx.AppContextUtil;
import com.boot.jx.api.ApiResponse;
import com.boot.jx.postman.PMConstants.PostManUrls;
import com.boot.jx.postman.PostManException;
import com.boot.jx.postman.model.DocResult;
import com.boot.jx.rest.RestService;
import com.boot.jx.rest.RestService.Ajax;
import com.boot.utils.ArgUtil;
import com.boot.utils.CryptoUtil.HashBuilder;
import com.boot.utils.JsonUtil;
import com.boot.utils.UniqueID;
import com.boot.utils.Urly;

@Component
public class DocServiceClient {

	@Value("${jax.doc.url}")
	private String docUrl;

	@Value("${jax.doc.local}")
	private String docLocal;

	@Value("${jax.doc.key}")
	private String docKey;

	@Autowired
	private RestService restService;

	@Autowired
	private AppConfig appConfig;

	@Value("${jax.postman.url}")
	private String postManUrl;

	@Deprecated
	private String generateUrl(String url, String uniqueKey, String dir, String type, String docNum)
			throws MalformedURLException, URISyntaxException {
		long timestamp = System.currentTimeMillis();
//		return Urly.parse(String.format("%s/success", url)).queryParam("dir", dir)
//				.queryParam("docid", docid)
//				.queryParam("type", type)
//				.queryParam("timestamp", timestamp).getURL();
		return Urly.parse(String.format("%s/upload/%d/%s/%s", url, timestamp, uniqueKey,
				new HashBuilder().secret(docKey).message(uniqueKey)
						.currentTime(timestamp)
						.interval(600)
						.toHMAC().output()))
				.queryParam("dir", dir)
				.queryParam("docid", docNum)
				.queryParam("docNum", docNum)
				.queryParam("tnt", AppContextUtil.getTenant())
				.queryParam("type", type).getURL();
	}

	@Deprecated
	public String generateUrl(String uniqueKey, String dir, String type, String docNum)
			throws MalformedURLException, URISyntaxException {
		return generateUrl(docUrl, uniqueKey, dir, type, docNum);
	}

	@Deprecated
	public String generateUrl(String dir, String type, String docNum)
			throws MalformedURLException, URISyntaxException {
		return generateUrl(docUrl, UniqueID.generateString62(), dir, type, docNum);
	}

	@Deprecated
	public String generateBucketUrl(String url, String uniqueKey, String dir, String type, String docid)
			throws MalformedURLException, URISyntaxException {
		return Urly.parse(String.format("%s/bucket", url)).queryParam("dir", dir)
				.queryParam("docid", docid)
				.queryParam("type", type).getURL();
	}

	public ApiResponse<DocResult, Object> validate(String docid) {
		try {
			return restService.ajax(appConfig.getPostmapURL()).path(PostManUrls.DOC_VALIDATE_ID).field("docid", docid)
					.postForm()
					.as(new ParameterizedTypeReference<ApiResponse<DocResult, Object>>() {
					});
		} catch (Exception e) {
			throw new PostManException(e);
		}
	}

	public String imageUrl(String imageId, String ext)
			throws MalformedURLException, URISyntaxException {
		long timestamp = System.currentTimeMillis();
		String publicKey = UniqueID.generateString62();
		return Urly.parse(String.format("%s/hmac/%d/%s/%s/%s.%s", docUrl, timestamp, publicKey,
				new HashBuilder().secret(docKey).message(publicKey)
						.currentTime(timestamp)
						.interval(600)
						.toHMAC().output(),
				imageId, ext))
				.getURL();
	}

	public <T> T imageJson(String imageId, Class<T> toValueType) {
		try {
			return JsonUtil.toObject(restService.ajax(docLocal).path(PostManUrls.DOC_IMAGE_BY_ID)
					.pathParam("image_id", imageId)
					.pathParam("ext", "json")
					.get()
					.as(new ParameterizedTypeReference<Map<String, Object>>() {
					}), toValueType);
		} catch (Exception e) {
			throw new PostManException(e);
		}
	}

	public DocResult imageJson(String imageId) {
		return this.imageJson(imageId, DocResult.class);
	}

	public String imageBase64(String imageId) {
		try {
			return restService.ajax(docLocal).path(PostManUrls.DOC_IMAGE_BY_ID)
					.pathParam("image_id", imageId)
					.pathParam("ext", "base64")
					.get()
					.asString();
		} catch (Exception e) {
			throw new PostManException(e);
		}
	}

	public DocResult scan(String uniqueKey, String dir, String type, String docid,
			MultipartFile file,
			MultipartFile fileback) throws MalformedURLException, IOException, URISyntaxException {
		String url = generateUrl(docLocal, uniqueKey, dir, type, docid);
		// System.out.println(url);
		Ajax x = restService.ajax(
				url).field("file", file);
		if (ArgUtil.is(fileback)) {
			x.field("fileback", fileback);
		}
		return x.postForm()
				.as(new ParameterizedTypeReference<DocResult>() {
				});
	}

	public String getDocUrl() {
		return docUrl;
	}

	public String generateSecureUrl(String url, String dir, String type, String docNum)
			throws MalformedURLException, URISyntaxException {
		long timestamp = System.currentTimeMillis();
		String publicKey = UniqueID.generateString62();
		String hmac = new HashBuilder().secret(docKey).message(publicKey)
				.currentTime(timestamp)
				.interval(600)
				.toHMAC().output();
		return (Urly.parse(String.format("%s/pub/v2/file/upload", url))
				.queryParam("timestamp", timestamp)
				.queryParam("key", publicKey)
				.queryParam("hmac", hmac)
				.queryParam("dir", dir)
				.queryParam("docid", docNum)
				.queryParam("docNum", docNum)
				.queryParam("tnt", AppContextUtil.getTenant())
				.queryParam("type", type).getURL());
	}

	public String generateSecureUrl(String dir, String type, String docNum)
			throws MalformedURLException, URISyntaxException {
		return generateSecureUrl(docUrl, dir, type, docNum);
	}

	public DocResult uploadSecureUrl(String dir, String type, String docNum, MultipartFile... page)
			throws URISyntaxException, IOException {
		String url = generateSecureUrl(docUrl, dir, type, docNum);
		Ajax x = restService.ajax(url);
		for (int i = 0; i < page.length; i++) {
			if (ArgUtil.is(page[i])) {
				x.field("pages" + i, page[i]);
			}
		}
		return x.postForm()
				.as(new ParameterizedTypeReference<DocResult>() {
				});
	}

	public String generateSecureUrlView(String url, String id, String ext)
			throws MalformedURLException, URISyntaxException {
		long timestamp = System.currentTimeMillis();
		String publicKey = UniqueID.generateString62();
		String hmac = new HashBuilder().secret(docKey).message(publicKey)
				.currentTime(timestamp)
				.interval(600)
				.toHMAC().output();
		return (Urly.parse(String.format("%s/pub/v2/view/%s/%s/%s/%s.%s", url,
				timestamp, publicKey, hmac, id, ext))
				// .queryParam("timestamp", timestamp)
				// .queryParam("key", publicKey)
				// .queryParam("hmac", hmac)
				// .queryParam("tnt", AppContextUtil.getTenant())
				.getURL());
	}

	public String generateSecureUrlView(String id, String ext)
			throws MalformedURLException, URISyntaxException {
		return generateSecureUrlView(docUrl, id, ext);
	}
}
