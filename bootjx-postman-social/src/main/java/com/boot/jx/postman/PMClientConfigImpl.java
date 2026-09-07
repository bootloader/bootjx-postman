package com.boot.jx.postman;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.retry.annotation.EnableRetry;

import com.boot.jx.AppConfig;
import com.boot.jx.AppContextUtil;
import com.boot.jx.http.CommonHttpRequest;
import com.boot.jx.postman.PMConfiguration.PMConfigurationModel;
import com.boot.jx.postman.PMEnvironment.PMClientConfig;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.utils.PostManUtil;
import com.boot.utils.ArgUtil;
import com.boot.utils.TimeUtils.TimePeriod;
import com.boot.utils.URLBuilder;
import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;

@Configuration
@EnableEncryptableProperties
@PropertySource("classpath:application-postman.properties")
@EnableRetry
public class PMClientConfigImpl implements PMClientConfig {

	public static class PATH {
		public static final String ASSIGN_TO_AGENT = "/int/assign/agent";
	}

	@Value("${postman.contact.details.url}")
	private String contactDetailsUrl;

	@Value("${app.local.dummy.bot.enabled}")
	boolean localDummyBotEnabled;

	@Value("${" + PMConstants.PROPERTIES.POSTMAN_CHAT_SESSION_TIMEOUT + "}")
	private String chatSessionTimeout;

	@Value("${postman.agent.session.timeout}")
	private String agentSessionTimeout;

	@Value("${postman.default.sender}")
	private String defaultSender;

	@Autowired
	private PMEnvironment environment;

	@Autowired
	private CommonHttpRequest commonHttpRequest;

	@Autowired
	private AppConfig appConfig;

	@Override
	public boolean isLocalDummyBotEnabled() {
		return localDummyBotEnabled;
	}

	@Override
	public String getDefaultSender() {
		return environment.config().prefsEntry("postman.bot.name")
				.asString(ArgUtil.parseAsString(environment.local().agent().getDefaultBotName(), defaultSender));
	}

	@Override
	public String getContactDetailsUrl() {
		return environment.local().prefsEntry("postman.contact.details.url").asString(contactDetailsUrl);
	}

	@Override
	public String getChatSessionTimeout() {
		return environment.local().prefsEntry(PMConstants.PROPERTIES.POSTMAN_CHAT_SESSION_TIMEOUT)
				.asString(chatSessionTimeout);
	}

	@Override
	public TimePeriod getAgentSessionTimeout() {
		return TimePeriod.from(agentSessionTimeout);
	}

	@Override
	public String getWebhookBase(ChannelConfig channelConfig, String appPrefix) {
		String webhookUrl = channelConfig.getWebhookUrl();
		if (!ArgUtil.is(webhookUrl)) {
			String publicUrl = PMContextUtil.publicUrl();
			if (ArgUtil.is(publicUrl)) {
				webhookUrl = publicUrl;
			} else if (isLocalDummyBotEnabled()) {
				webhookUrl = String.format("%s%s", commonHttpRequest.getServerHost(), appConfig.getAppPrefix(),
						environment.config().prefsEntry("mry.prop.service.server").asString());
			} else {
				webhookUrl = String.format("https://%s.%s/%s", AppContextUtil.getTenant(),
						environment.config().prefsEntry("mry.prop.service.server").asString(),
						ArgUtil.nonEmpty(appPrefix, "postman"));
			}
		}
		return webhookUrl;
	}

	@Override
	public String getWebhookUrl(ChannelConfig channelConfig, String appPrefix, Map<String, Object> query) {
		PMConfigurationModel config = environment.local();
		String webhookEndPoint = getWebhookBase(channelConfig, appPrefix);
		String webhookPath = PostManUtil.CHANNEL_CALLBACK_PATH(config.getAccountKey(), channelConfig);
		try {
			URLBuilder url = URLBuilder.parse(webhookEndPoint).path(webhookPath);
			url.queryParam("tnt", AppContextUtil.getTenant());
			if (ArgUtil.is(query)) {
				for (Entry<String, Object> entry : query.entrySet()) {
					url.queryParam(entry.getKey(), entry.getValue());
				}
			}
			return url.getURL();
		} catch (MalformedURLException | URISyntaxException e) {
			return String.format("%s/%s", webhookEndPoint, webhookPath);
		}
	}

}
