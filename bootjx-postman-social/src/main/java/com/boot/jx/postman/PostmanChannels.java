package com.boot.jx.postman;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;

public class PostmanChannels {

	public static interface PublicProperty {
	}

	public static interface ProtectedProperty extends PublicProperty {
	}

	public static interface OneTimeVisibleProperty extends ProtectedProperty {
	}

	public static interface ChannelDetails extends Serializable {
		@JsonView(PublicProperty.class)
		public String getLane();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static abstract class AChannelDetails implements ChannelDetails {
		private static final long serialVersionUID = -5531902306230415784L;
	}

	public static interface ChannelPlugin<C extends AChannelDetails> {

	}
}
