package com.amx.tests;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.boot.jx.postman.dto.ChatSessionDTO;
import com.boot.jx.postman.wa360.WA360Constants.InBoundWrapperPaths;
import com.boot.jx.postman.wa360.WA360Template;
import com.boot.model.MapModel;
import com.boot.utils.BashUtil.CurlCommand;
import com.boot.utils.FileUtil;
import com.boot.utils.JsonUtil;

@SpringBootTest
public class JsonConversionTests {

//	public static void main(String[] arg) {
//		new JsonConversionTests().curl();
//		new JsonConversionTests().chatSessionDTO();
//	}

	@Test
	public void curl() {
		URL url = FileUtil.getResource("sample/curl_test.txt", JsonConversionTests.class);
		String text = FileUtil.read(url);
		System.out.println(JsonUtil.toJsonPrettyPrint(CurlCommand.parse(text)));
	}

	@Test
	public void chatSessionDTO() {
		// assertEquals("t1", StringUtils.trim("/abc/def/ghij", '/'), "abc/def/ghij");
		URL url = FileUtil.getResource("sample/chat_session_dto.json", JsonConversionTests.class);
		String json = FileUtil.read(url);

		ChatSessionDTO dto = JsonUtil.parse(json, ChatSessionDTO.class);
		System.out.println(JsonUtil.toJson(dto));
	}

	// @Test
	public void wa360d() {
		// assertEquals("t1", StringUtils.trim("/abc/def/ghij", '/'), "abc/def/ghij");
		URL url = FileUtil.getResource("sample/wa360d_inbound.json");
		String json = FileUtil.read(url);
		MapModel map = MapModel.from(JsonUtil.fromJsonToMap(json));
		String number = map.entry(InBoundWrapperPaths.CONTACT_NUMBER).asString();

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

	// @Test
	public void wa360Templates() {
		// assertEquals("t1", StringUtils.trim("/abc/def/ghij", '/'), "abc/def/ghij");
		URL url = FileUtil.getResource("sample/wa360d_templates.json");
		String json = FileUtil.read(url);
		MapModel map = MapModel.from(JsonUtil.fromJsonToMap(json));

		List<WA360Template> wabaTemplates = map.keyEntry("waba_templates").asList(WA360Template.class);

		System.out.println(JsonUtil.toJson(wabaTemplates));
		for (WA360Template wa360Template : wabaTemplates) {
			System.out.println(wa360Template.getName());
		}

	}

}
