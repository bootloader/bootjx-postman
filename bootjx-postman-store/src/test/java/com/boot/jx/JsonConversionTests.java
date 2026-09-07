package com.boot.jx;

import java.net.URL;

import org.junit.Test;

import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.dto.ChatSessionDTO;
import com.boot.utils.EntityDtoUtil;
import com.boot.utils.FileUtil;
import com.boot.utils.JsonUtil;

public class JsonConversionTests {

	public static void main(String[] arg) {
		// new JsonConversionTests().curl();
		new JsonConversionTests().chatSessionDoc2DTO();
	}

	@Test
	public void chatSessionDoc2DTO() {

		ChatSessionDoc chatSessionDoc = new ChatSessionDoc();
		ChatSessionDTO chatSessionDto = EntityDtoUtil.entityToDto(chatSessionDoc, new ChatSessionDTO());

		// assertEquals("t1", StringUtils.trim("/abc/def/ghij", '/'), "abc/def/ghij");
		URL url = FileUtil.getResource("sample/chat_session_dto.json", JsonConversionTests.class);
		String json = FileUtil.read(url);

		ChatSessionDTO dto = JsonUtil.parse(json, ChatSessionDTO.class);
		System.out.println(JsonUtil.toJson(dto));
	}

}
