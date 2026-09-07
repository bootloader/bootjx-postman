package com.boot.jx.postman.model.ext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.boot.jx.postman.model.FormReply;
import com.boot.jx.postman.model.MessageReferral;
import com.boot.jx.postman.model.MessageReplyTo;
import com.boot.jx.postman.model.TagDocument;
import com.boot.jx.postman.model.ext.CommonMsgText.InBoundMsgText;
import com.boot.jx.swagger.ApiMockModelProperty;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InBoundMsg {

	@JsonProperty("from")
	@ApiMockModelProperty(example = "919988776655", value = "Contact of user")
	public String contactFrom;

	@ApiMockModelProperty(example = "wa919988776655_918828218374", value = "Unique Contact Id of user")
	public String contactId;

	@JsonProperty("id")
	@ApiMockModelProperty(example = "1234567", value = "Unique Message Id assigined by Service")
	public String messageId;

	@ApiMockModelProperty(example = "1234567", value = "Unique Message Id assigined by Channel if any")
	public String messageIdExt;

	@ApiMockModelProperty(example = "156753339076", value = "message-timestamp")
	public Long timestamp;

	@ApiMockModelProperty(example = "text", value = "message-type",
			allowableValues = "audio,document,image,location,system,text,video,voice")
	public String type;

	public InBoundMsgText text;

	@ApiMockModelProperty(hidden = true)
	public InBoundMsgText system;

	public InBoundMsgMedia video;
	public InBoundMsgMedia voice;
	public InBoundMsgMedia audio;
	public InBoundMsgMedia document;
	public InBoundMsgMedia image;
	@ApiMockModelProperty(hidden = true)
	public InBoundMsgMedia sticker;

	public List<InBoundMsgMedia> attachments;

	public CommonMsgLocation location;

	@ApiMockModelProperty(value = "Form (Button/List/Flows) selections by user")
	public FormReply form = new FormReply();

	@ApiMockModelProperty(hidden = true)
	@Deprecated
	public MessageReplyTo replyTo = new MessageReplyTo();

	@ApiMockModelProperty(value = "Reference source of Inbound Message")
	public MessageReferral referral = new MessageReferral();

	@ApiMockModelProperty(hidden = true)
	public Map<String, Object> input = new HashMap<String, Object>();

	@ApiMockModelProperty(value = "Several Tags/Categories Assigned by our ML/NLP program")
	public TagDocument tags;

	public MsgSession session;

	@ApiMockModelProperty(example = "{}",
			value = "Original Message sent by Channel :  only if modified/error by service")
	public Object originalMessage;

}