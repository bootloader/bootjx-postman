package com.boot.jx.postman.model.outbound;

import java.util.List;

import com.boot.jx.postman.pbook.PBAddress;
import com.boot.jx.postman.pbook.PBEmail;
import com.boot.jx.postman.pbook.PBPhone;
import com.boot.jx.swagger.ApiMockModelProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 
 * https://developers.facebook.com/docs/whatsapp/api/messages/contacts-location-messages
 * https://developers.facebook.com/docs/whatsapp/api/messages#contact-messages
 * 
 * @author lalittanwar
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonMsgContactCard {

	public static class OutBoundMsgContactAddress {
		@ApiMockModelProperty(example = "Menlo Park")
		public String city;
		@ApiMockModelProperty(example = "United States")
		public String country;
		@ApiMockModelProperty(example = "us")
		public String country_code;
		@ApiMockModelProperty(example = "CA")
		public String state;
		@ApiMockModelProperty(example = "1 Hacker Way")
		public String street;
		@ApiMockModelProperty(example = "HOME")
		public String type;
		@ApiMockModelProperty(example = "94025")
		public String zip;

		public PBAddress toAddress() {
			PBAddress dto = new PBAddress();
			dto.setType(type);
			dto.setCity(city);
			dto.setCountry(country);
			dto.setCountryCode(country_code);
			dto.setState(state);
			dto.setStreet(street);
			dto.setZip(zip);
			return dto;
		}
	}

	public List<OutBoundMsgContactAddress> addresses;
	public String birthday;

	public static class OutBoundMsgContactEmail {
		@ApiMockModelProperty(example = "test@whatsapp.com")
		public String email;
		@ApiMockModelProperty(example = "WORK")
		public String type;

		public PBEmail toEmail() {
			return new PBEmail().email(email).type(type);
		}

	}

	public List<OutBoundMsgContactEmail> emails;

	public static class OutBoundMsgContactPhone {
		@ApiMockModelProperty(example = "+1 (940) 555-1234")
		public String phone;
		@ApiMockModelProperty(example = "HOME")
		public String type;

		@ApiMockModelProperty(example = "16505551234")
		public String wa_id;

		public PBPhone toPhone() {
			PBPhone nephone = new PBPhone();
			nephone.setType(type);
			nephone.setWhatsAppId(wa_id);
			nephone.setPhone(phone);
			return nephone;
		}
	}

	public List<OutBoundMsgContactPhone> phones;

	public static class OutBoundMsgContactName {
		@ApiMockModelProperty(example = "John")
		public String first_name;
		@ApiMockModelProperty(example = "John Smith")
		public String formatted_name;
		@ApiMockModelProperty(example = "Smith")
		public String last_name;
	}

	public OutBoundMsgContactName name;

	public static class OutBoundMsgContactOrg {
		@ApiMockModelProperty(example = "WhatsApp")
		public String company;
		@ApiMockModelProperty(example = "Design")
		public String department;
		@ApiMockModelProperty(example = "Manager")
		public String title;
	}

	public OutBoundMsgContactOrg org;

	public static class OutBoundMsgContactUrl {
		@ApiMockModelProperty(example = "https://www.facebook.com")
		public String url;

		@ApiMockModelProperty(example = "WORK")
		public String type;
	}

	public static class OutBoundMsgContactSocial {
		@ApiMockModelProperty(example = "https://www.facebook.com")
		public String userid;

		@ApiMockModelProperty(example = "FACEBOOK")
		public String service;

	}

	public List<OutBoundMsgContactUrl> urls;
	public List<OutBoundMsgContactSocial> ims;

}
