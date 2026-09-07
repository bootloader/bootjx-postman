package com.boot.jx.postman.wacfb;

import com.boot.jx.swagger.ApiMockModelProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WacfbInboundMedia {

    @Deprecated
    @ApiMockModelProperty(example = "/file/of/vdo", value = "absolute-filepath-on-coreapp", hidden = true)
    public String file;

    @ApiMockModelProperty(example = "2121212", value = "ID of the media",
	    notes = "Can be used to delete the media if stored locally on the client.")
    public String id;

    @ApiMockModelProperty(example = "http(s)://link-to-media-file-url", value = "link-to-audio-file")
    public String link;

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
    
    @ApiMockModelProperty(example = "928821678902042", value = "mediaId")
    public String mediaId;

    public String getMediaId() {
		return mediaId;
	}

	public void setMediaId(String mediaId) {
		this.mediaId = mediaId;
	}

	public String getFile() {
	return file;
    }

    public void setFile(String file) {
	this.file = file;
    }

    public String getId() {
	return id;
    }

    public void setId(String id) {
	this.id = id;
    }

    public String getLink() {
	return link;
    }

    public void setLink(String link) {
	this.link = link;
    }

    public String getMimeType() {
	return mimeType;
    }

    public void setMimeType(String mimeType) {
	this.mimeType = mimeType;
    }

    public String getSha256() {
	return sha256;
    }

    public void setSha256(String sha256) {
	this.sha256 = sha256;
    }

    public String getCaption() {
	return caption;
    }

    public void setCaption(String caption) {
	this.caption = caption;
    }

    public String getFilename() {
	return filename;
    }

    public void setFilename(String filename) {
	this.filename = filename;
    }

    public Object getMetadata() {
	return metadata;
    }

    public void setMetadata(Object metadata) {
	this.metadata = metadata;
    }

}