package com.boot.jx.postman.wacfb;

import java.io.Serializable;

import com.boot.jx.swagger.ApiMockModelProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WacfbOutBoundMedia implements Serializable {

    private static final long serialVersionUID = -654441367316876851L;

    @ApiMockModelProperty(example = "5do9756xbso34578", required = false, value = "Media Id",
	    notes = "Required when type is audio, document, image, sticker, or video and you are not using a link."
		    + "\n The media object ID. This is returned when the media is successfully uploaded to the "
		    + "WhatsApp Business API client via the media endpoint.\n"
		    + "\n Do not use this field when message type is set to text.")
    public String id;

    @ApiMockModelProperty(example = "http(s)://the-url", required = false, value = "Public URL of Media file",
	    notes = "Required when type is audio, document, image, sticker, or video and you are not using an uploaded media ID."
		    + "\n The protocol and URL of the media to be sent. Use only with HTTP/HTTPS URLs.\n" + "\n"
		    + "\n Do not use this field when message type is set to text.")
    public String link;

    @ApiMockModelProperty(example = "your-video-caption", required = false, value = "your-media-caption",
	    notes = "Describes the specified document, image, or video media.\n"
		    + "\n Do not use with audio or sticker media.")
    public String caption;

    @ApiMockModelProperty(example = "your-video-caption", required = false, value = "your-document-filename",
	    notes = "Describes the specified document, image, or video media.\n"
		    + "Describes the filename for the specific document. Use only with document media.")
    public String filename;
    
    @ApiMockModelProperty(example = "media-id", required =false, value = "your-media-Id",
    	    notes = "media Id is used to send media for Wacfb ")
        public String mediaId;
    

    public String getMediaId() {
		return mediaId;
	}

	public void setMediaId(String mediaId) {
		this.mediaId = mediaId;
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
}