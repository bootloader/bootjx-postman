package com.boot.jx.postman.wa360;

import java.io.Serializable;

import com.boot.jx.swagger.ApiMockModelProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@Deprecated
public class WA360CloudOutBoundAudio implements Serializable {

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

   
   
}
