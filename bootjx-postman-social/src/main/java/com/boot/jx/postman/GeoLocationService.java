package com.boot.jx.postman;

import com.boot.jx.postman.model.GeoLocation;

public interface GeoLocationService {

	public GeoLocation getLocation(String ip) throws PostManException;

}
