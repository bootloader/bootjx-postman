package com.boot.jx.postman.client;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.boot.jx.AppConfig;
import com.boot.jx.postman.PostManService;
import com.boot.jx.postman.model.Notipy;
import com.boot.jx.postman.model.Notipy.ChannelType;
import com.boot.utils.ArgUtil;
import com.boot.utils.JsonUtil;
import com.boot.utils.NetworkAdapter;
import com.boot.utils.NetworkAdapter.NetAddress;

public class PostManContextListener implements ServletContextListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(PostManContextListener.class);
	private static NetAddress NET_ADDRESS = null;

	public PostManService getService(ServletContextEvent sce) {
		return WebApplicationContextUtils.getRequiredWebApplicationContext(sce.getServletContext())
				.getBean(PostManService.class);
	}

	public static NetAddress getNetAddress() {
		if (NET_ADDRESS == null) {
			NET_ADDRESS = NetworkAdapter.getAddress();
		}
		return NET_ADDRESS;
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		try {
			AppConfig appConfig = WebApplicationContextUtils.getRequiredWebApplicationContext(sce.getServletContext())
					.getBean(AppConfig.class);

			if (!appConfig.isDebug()) {
				NetAddress netAddress = getNetAddress();
				PostManService postManService = getService(sce);
				Notipy msg = new Notipy();
				msg.setIChannel(ChannelType.DEPLOYER);
				msg.setColor("#36a64f");
				msg.setMessage("App : " + appConfig.getAppName());
				msg.addLine("Status : server is started...");
				if (!ArgUtil.isEmpty(netAddress)) {
					msg.addLine("SERVER:  " + JsonUtil.toStringMap(netAddress).toString());
				}
				LOGGER.info("{} : Status : server is started...", appConfig.getAppName());

				postManService.notifySlack(msg);
			}

		} catch (Exception e) {
			LOGGER.error("Exception while Sending Server Up Status", e);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		try {
			AppConfig appConfig = WebApplicationContextUtils.getRequiredWebApplicationContext(sce.getServletContext())
					.getBean(AppConfig.class);
			if (!appConfig.isDebug()) {
				PostManService postManService = getService(sce);
				Notipy msg = new Notipy();
				msg.setIChannel(ChannelType.DEPLOYER);
				msg.setColor("danger");
				msg.setMessage("App : " + appConfig.getAppName());
				msg.addLine("Status : server is shutdown...");
				LOGGER.info("{} : Status : server is shutdown...", appConfig.getAppName());
				postManService.notifySlack(msg);
			}
		} catch (Exception e) {
			LOGGER.error("Exception while Sending Server Down Status", e);
		}
	}

}
