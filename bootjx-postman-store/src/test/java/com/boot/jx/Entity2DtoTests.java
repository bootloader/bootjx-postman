package com.boot.jx;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.dto.ChatSessionDTO;
import com.boot.utils.EntityDtoUtil;
import com.boot.utils.JsonUtil;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class Entity2DtoTests {

	public static void main(String[] arg) {
		// new JsonConversionTests().curl();
		// new JsonConversionTests().chatSessionDoc2DTO();
	}

	@Test
	public void chatSessionDoc2DTO() {
		ChatSessionDoc chatSessionDoc = new ChatSessionDoc();
		ChatSessionDTO chatSessionDto = EntityDtoUtil.entityToDto(chatSessionDoc, new ChatSessionDTO());
		System.out.println(JsonUtil.toJson(chatSessionDto));
	}

}
