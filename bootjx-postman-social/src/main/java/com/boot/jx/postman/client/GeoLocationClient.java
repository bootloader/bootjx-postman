package com.boot.jx.postman.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.AppConfig;
import com.boot.jx.postman.GeoLocationService;
import com.boot.jx.postman.PMConstants.PostManUrls;
import com.boot.jx.postman.PostManException;
import com.boot.jx.postman.model.GeoLocation;
import com.boot.jx.rest.RestService;

@Component
public class GeoLocationClient implements GeoLocationService {

	@Autowired
	RestService restService;

	@Autowired
	AppConfig appConfig;

	public GeoLocation getLocation(String ip) throws PostManException {
		try {
			return restService.ajax(appConfig.getPostmapURL()).path(PostManUrls.GEO_LOC).queryParam("ip", ip).get()
					.as(GeoLocation.class);
		} catch (Exception e) {
			throw new PostManException(e);
		}
	};

}
