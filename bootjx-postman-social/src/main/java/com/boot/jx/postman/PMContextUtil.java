package com.boot.jx.postman;

import com.boot.jx.AppContextUtil;

public class PMContextUtil {

	public static ClientApp clientApp() {
		return AppContextUtil.get("XmsVendorConfigurer:ClientApp");
	}

	public static ClientApp clientApp(ClientApp clientApp) {
		AppContextUtil.set("XmsVendorConfigurer:ClientApp", clientApp);
		return clientApp;
	}

	public static String publicUrl(String public_url) {
		AppContextUtil.set("----public_url---", public_url);
		return public_url;
	}

	public static String publicUrl() {
		return AppContextUtil.get("----public_url---");
	}

}
