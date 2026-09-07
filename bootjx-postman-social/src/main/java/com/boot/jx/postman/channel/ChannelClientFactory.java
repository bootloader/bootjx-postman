package com.boot.jx.postman.channel;

import java.util.List;

import org.springframework.stereotype.Component;

import com.boot.jx.postman.channel.ChannelClientFactory.ChannelClient;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ChannelBasedFactory;
import com.boot.model.MapModel;

@Component
public class ChannelClientFactory extends ChannelBasedFactory<ChannelClient> {

	private static final long serialVersionUID = 6389348869023880588L;

	public ChannelClientFactory(List<ChannelClient> beans) {
		super(beans);
	}

	public interface ChannelClient {
		MapModel updateTemplates(ChannelConfig channelConfig, MapModel from);

		MapModel createTemplates(ChannelConfig channelConfig, MapModel from);

		MapModel fetchTemplates(ChannelConfig channelConfig);
		
		MapModel listOfFlows(ChannelConfig channelConfig);
		MapModel flowsAssets(String flowId, ChannelConfig channelConfig);
	}

	@Override
	public ChannelClient getDefault() {
		return null;
	}
}
