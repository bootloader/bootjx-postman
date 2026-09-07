package com.boot.jx.postman.events;

import com.boot.jx.postman.model.Message;
import com.boot.jx.tunnel.ITunnelDefs.ITunnelEvent;

public class UserMessageEvent extends Message<UserMessageEvent> implements ITunnelEvent {

	private static final long serialVersionUID = -1354844357577261297L;

	String image = null;
	String link = null;

	public UserMessageEvent() {
		super();
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

}
