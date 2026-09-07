package com.boot.jx.postman.model.ext;

import com.boot.jx.dict.FileFormat;
import com.boot.jx.postman.model.Attachment;
import com.boot.jx.swagger.ApiMockModelProperty;
import com.boot.utils.ArgUtil;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InBoundMsgMedia extends CommonMsg {

	@Deprecated
	@ApiMockModelProperty(example = "/file/of/vdo", value = "absolute-filepath-on-coreapp", hidden = true)
	public String file;

	@ApiMockModelProperty(example = "2121212", value = "ID of the media",
			notes = "Can be used to delete the media if stored locally on the client.")
	public String id;

	@ApiMockModelProperty(example = "http(s)://link-to-media-file-url", value = "link-to-file")
	public String link;

	@ApiMockModelProperty(example = "https://link-to-media-file-url", value = "secure-link-to-file")
	public String linkSecure;

	@JsonProperty("mime_type")
	@ApiMockModelProperty(example = "video/mp4", value = "Mime type of the media.")
	public String mimeType;

	@ApiMockModelProperty(example = "m3232kaoe4belrr", value = "checksum")
	public String sha256;

	@ApiMockModelProperty(example = "document-caption", value = "The provided caption for the media.", required = false)
	public String caption;

	@ApiMockModelProperty(example = "document-filename", value = "Filename on the sender's device.", required = false,
			notes = "This will only be present in document media messages.")
	public String filename;

	@ApiMockModelProperty(example = "m3232kaoe4belrr", value = "Metadata pertaining to sticker media.", hidden = true)
	public Object metadata;

	public static InBoundMsgMedia from(Attachment attachment) {
		InBoundMsgMedia media = new InBoundMsgMedia();
		media.caption = attachment.getMediaCaption();
		media.filename = attachment.getMediaName();
		media.link = attachment.getMediaURL();
		if (ArgUtil.is(attachment.getMediaURL())) {
			media.linkSecure = attachment.getMediaURL().replaceFirst("http://", "https://");
		}
		media.mimeType = attachment.getMediaMimeType();
		return media;
	}

	public Attachment toAttachment() {
		Attachment attachment = new Attachment();
		attachment.setMediaCaption(caption);
		attachment.setMediaName(filename);
		attachment.setMediaURL(link);
		if (ArgUtil.is(linkSecure)) {
			attachment.setMediaURL(linkSecure);
		}
		attachment.setMediaMimeType(mimeType);
		if (ArgUtil.is(mimeType)) {
			attachment.setMediaType(FileFormat.from(mimeType).getFileType().toString());
		}
		return attachment;
	}

}