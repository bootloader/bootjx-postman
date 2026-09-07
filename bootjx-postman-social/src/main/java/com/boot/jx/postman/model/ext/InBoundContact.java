package com.boot.jx.postman.model.ext;

import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.swagger.ApiMockModelProperty;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.UtilityModels.JsonIgnoreNull;
import com.boot.model.UtilityModels.JsonIgnoreUnknown;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InBoundContact implements JsonIgnoreUnknown, JsonIgnoreNull {
	private static final long serialVersionUID = 7064422892950497366L;

	@ApiMockModelProperty(example = "WHATSAPP", value = "Contact Type")
	public ContactType contactType;

	@ApiMockModelProperty(example = "wa360:919988776655", value = "Channel for message")
	public String channelId;

	@ApiMockModelProperty(example = "919999998888",
			value = "Contact Used by User while sending message" + "eg your business number or email address")
	public String lane;

	@ApiMockModelProperty(example = "1234567", value = "Unique Id assigined to user by Service")
	public String contactId;

	@JsonProperty("csid")
	@ApiMockModelProperty(example = "91YOURNUMBER", value = "Channel Specific ID", required = false)
	public String csid;

	@ApiMockModelProperty(example = "John Doe", value = "Name collected from Channel", required = false, hidden = true)
	public String name;

	@ApiMockModelProperty(example = "919988776655", value = "Mobile Number collected from Channel", required = false,
			hidden = true)
	public String phone;

	@ApiMockModelProperty(example = "abc@xyz.com", value = "Email Id collected from Channel", required = false,
			hidden = true)
	public String email;

	public InBoundContactProfile profile;

	public ContactBusinessProfile businessProfile;

	public static InBoundContact from(Contactable contactable) {
		InBoundContact contact = new InBoundContact();
		contact.contactType = contactable.type();
		contact.lane = contactable.getLane();
		contact.contactId = PostManUtil.CONTACT_ID(contactable);
		contact.csid = contactable.getCsid();
		contact.channelId = PostManUtil.CHANNEL_ID(contactable);

		contact.profile = new InBoundContactProfile();
		contact.profile.name = contactable.getName();
		contact.profile.email = contactable.getEmail();
		contact.profile.phone = contactable.phone();

		return contact;
	}

}
