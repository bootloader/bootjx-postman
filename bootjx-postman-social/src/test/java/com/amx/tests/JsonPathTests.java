package com.amx.tests;

import java.net.URL;
import java.util.List;
import java.util.Map;

import com.boot.model.MapModel;
import com.boot.utils.BashUtil.CurlCommand;
import com.boot.utils.FileUtil;
import com.boot.utils.JsonUtil;

public class JsonPathTests {

	public static void main(String[] arg) {
		new JsonPathTests().curl();
	}

	public void curl() {
		URL url = FileUtil.getResource("sample/curl_test.txt", JsonPathTests.class);
		String text = FileUtil.read(url);
		System.out.println(JsonUtil.toJson(CurlCommand.parse(text)));
	}

	// @Test
	public void wa360TemplatesMap() {
		// assertEquals("t1", StringUtils.trim("/abc/def/ghij", '/'), "abc/def/ghij");
		URL url = FileUtil.getResource("sample/wa360d_templates.json");
		String json = FileUtil.read(url);
		MapModel map = MapModel.from(JsonUtil.fromJsonToMap(json));

		List<Map<String, Object>> wabaTemplates = map.keyEntry("waba_templates").asListOfMap();

		System.out.println(JsonUtil.toJson(wabaTemplates));
		for (Map<String, Object> wa360Template : wabaTemplates) {
			System.out.println(wa360Template.get("name"));
		}

	}

}
