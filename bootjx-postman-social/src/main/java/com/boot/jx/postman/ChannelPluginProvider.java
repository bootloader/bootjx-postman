package com.boot.jx.postman;

import com.boot.jx.postman.PostmanChannels.AChannelDetails;
import com.boot.jx.postman.PostmanChannels.ChannelPlugin;

public class ChannelPluginProvider {

	public interface DefaultChannelPlugin<T extends AChannelDetails> extends ChannelPlugin<T> {

	}

}
