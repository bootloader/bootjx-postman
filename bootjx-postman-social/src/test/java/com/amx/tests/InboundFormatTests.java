package com.amx.tests;

import java.net.URL;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.boot.jx.postman.model.FormReply;
import com.boot.jx.postman.model.ext.InBoundMsg;
import com.boot.jx.postman.model.ext.InBoundWrapper;
import com.boot.utils.FileUtil;
import com.boot.utils.JsonUtil;

@SpringBootTest
public class InboundFormatTests {

//	public static void main(String[] arg) {
//		new JsonConversionTests().curl();
//		new JsonConversionTests().chatSessionDTO();
//	}

	@Test
	public void testFormReply() {
		// assertEquals("t1", StringUtils.trim("/abc/def/ghij", '/'), "abc/def/ghij");
		URL url = FileUtil.getResource("sample/inbound-out.json");
		String json = FileUtil.read(url);

		InBoundWrapper inbound = JsonUtil.parse(json, InBoundWrapper.class);

		InBoundMsg msg = inbound.messages.get(0);

		JsonUtil.print(msg.input);

		msg.form = JsonUtil.toObject(msg.input, FormReply.class);

		JsonUtil.print(msg.form);

	}

}
