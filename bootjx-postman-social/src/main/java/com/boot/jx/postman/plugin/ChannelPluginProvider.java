package com.boot.jx.postman.plugin;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.type.filter.AssignableTypeFilter;

import com.boot.common.ScopedBeanFactory;
import com.boot.jx.AppContextUtil;
import com.boot.jx.common.impl.ConfigMeta;
import com.boot.jx.common.impl.ConfigMeta.CONVERT_TYPE;
import com.boot.jx.common.impl.ConfigMeta.ConfigMetaProperty;
import com.boot.jx.common.impl.ConfigMeta.DATA_TYPE;
import com.boot.jx.common.impl.ConfigMeta.INPUT_TYPE;
import com.boot.jx.dict.ContactType;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PMEnvironment.AChannelDetails;
import com.boot.jx.postman.PMEnvironment.ChannelTypeSpecificProps;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel;
import com.boot.model.UtilityModels.Stringable;
import com.boot.utils.ArgUtil;
import com.boot.utils.ClazzUtil;
import com.boot.utils.Constants;

@SuppressWarnings("unchecked")
public class ChannelPluginProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChannelPluginProvider.class);

	@Retention(RetentionPolicy.RUNTIME)
	@Lazy
	public @interface ConnectorMapping {
		ContactType[] contactType();

		String[] channel() default "DEFAULT";
	}

	public static abstract class ChannelBasedFactory<T> extends ScopedBeanFactory<String, T> {
		private static final long serialVersionUID = 1L;

		public ChannelBasedFactory(List<T> beans) {
			super(beans);
		}

		@Override
		public String[] getKeys(T lib) {
			ConnectorMapping annotation = ClazzUtil.getAnnotationFromBean(lib, ConnectorMapping.class);
			// lib.getClass().getAnnotation();
			List<String> zoom = new ArrayList<String>();
			if (annotation != null) {
				// LOGGER.debug("get(ContactType {}, String {})", annotation.contactType(),
				// annotation.channel());
				for (ContactType contactType : annotation.contactType()) {
					for (String channel : annotation.channel()) {
						zoom.add(String.format("%s_%s", contactType, channel));
					}
				}
				return zoom.toArray(new String[0]);
			}
			return null;
		}

		public T get(ContactType contactType, String channel) {
			LOGGER.debug("get(ContactType {}, String {})", contactType, channel);
			String precisedKey = String.format("%s_%s", contactType, channel);
			T x = this.get(precisedKey);
			if (ArgUtil.is(x)) {
				return x;
			}
			precisedKey = String.format("%s_DEFAULT", contactType);
			return this.get(precisedKey);
		}

		abstract public T getDefault();

		public T get(ChannelConfig channelConfig) {
			if (ArgUtil.is(channelConfig)) {
				T connector = get(channelConfig.getContactType(), channelConfig.getChannelType());
				if (ArgUtil.is(connector)) {
					return connector;
				}
			}
			return getDefault();
		}
	}

	public static interface ChannelPlugin<C extends AChannelDetails> extends ChannelTypeSpecificProps {
		/**
		 * usually return new AChannelDetails();
		 * 
		 * @return new instance of {@link AChannelDetails}
		 */
		public C newChannelDetails();

		@SuppressWarnings("unchecked")
		default public C getChannelDetails(AChannelDetails channelDetails) {
			return (C) channelDetails;
		}

		/**
		 * For new Configs
		 * 
		 * @param config
		 * @param details
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public default ChannelConfig updateChannelConfig(ChannelConfig config, AChannelDetails details) {
			updatePluginSpecs(config);
			// Channel Specific Properties
			String lane = laneTrimmed(ArgUtil.nonEmpty(details.getLane(), config.getLane()));
			config.setLane(lane.replaceAll("[^a-zA-Z0-9\\_]+", ""));
			String sublaneRaw = ArgUtil.nonEmpty(details != null ? details.getSublane() : null,
					config != null ? config.getSublane() : null);

			String sublane = laneTrimmed(sublaneRaw != null ? sublaneRaw : "");

			if (ArgUtil.is(sublane)) {
				config.setSublane(sublane.replaceAll("[^a-zA-Z0-9\\_]+", ""));
			}

			setDetails(config, (C) details);
			return config;
		}

		default public String laneTrimmed(String lane) {
			return lane;
		}

		public default void updatePluginSpecs(ChannelConfig config) {
			// Plugin Specific Properties
			config.setContactType(this.getContactType());
			config.setChannelType(this.getChannelType());

			config.setInboundAllowed(this.isInboundAllowed());
			config.setOutboundAllowed(this.isOutboundAllowed());
			config.setPushAllowed(this.isPushAllowed());
			config.setPushOnlyApproved(this.isPushOnlyApproved());
			config.setPushFreeTextAllowed(this.isPushFreeTextAllowed());
			config.setPushToNewContactAllowed(this.isPushToNewContactAllowed());
			config.setWebhookManual(this.isWebhookManual());
			config.setPushLocalStore(this.isPushLocalStore());
		}

		/**
		 * For new Configs
		 * 
		 * @param config
		 * @param details
		 */
		public void setDetails(ChannelConfig config, C details);

		public C getDetails(ChannelConfig config);

		public void addConfigMeta(List<ConfigMeta> configMetaList);

		default public List<ConfigMeta> listConfigMeta(PMEnvironment pmEnvironment) {
			List<ConfigMeta> list = ConfigMeta.createList();
			list.add(new ConfigMeta().key("name").title("Desc"));
			list.add(new ConfigMeta().key("channelCode").title("Channel Code").max(4));
			list.add(new ConfigMeta().key("channelKey").title("Channel Key").readonly().hidden()
					.defaultValue(PostManUtil.UNIQUE_API_KEY()));
			list.add(new ConfigMeta().key("inboundQueue").title("Default Queue").optional()
					.optionsSource("getx:/api/options/inbound_queue").optionsKey("code").order(100));

			String serviceDomain = pmEnvironment.config().prefsEntry("mry.prop.service.domain").asString();
			String clientDomain = AppContextUtil.getTenant();
			list.add(new ConfigMeta().key("webhookUrl").title("Webhook URL").hidden()
					.defaultValue(String.format("https://%s.%s/postman", clientDomain, serviceDomain)));

			this.addConfigMeta(list);
			return list;
		}

		void importChannelDetailsFromMap(C channelDetails, MapModel map);

		@SuppressWarnings("unchecked")
		default void importChannelDetailsFromMap(AChannelDetails channelDetails, MapModel map, String channelType) {
			importChannelDetailsFromMap((C) channelDetails, map);
		}

		default public void importChannelConfigFromMap(ChannelConfig config, MapModel map, String channelType) {
			AChannelDetails channelDetails = getDetails(config);

			if (channelDetails == null) {
				channelDetails = newChannelDetails();
			}

			boolean isSandbox = config.isSandbox();
			boolean isMaster = config.isMaster();
			boolean isMockEnabled = config.isMockEnabled();
			importChannelDetailsFromMap(channelDetails, map, channelType);

			config.setName(map.getString("name", ArgUtil.nonEmpty(config.getName(), getDefaultName(config))));
			config.setChannelKey(map.getString("channelKey",
					ArgUtil.nonEmpty(config.getChannelKey(), PostManUtil.UNIQUE_API_KEY())));
			config.setChannelCode(map.getString("channelCode", config.getChannelCode()));

			config.setInboundQueue(map.getString("inboundQueue"));

			config.setWebhookUrl(map.getString("webhookUrl", config.getWebhookUrl()));

			config.setSandbox(isSandbox);
			config.setMaster(isMaster);
			config.setMockChannel(isMockEnabled);
			Object webObj = map.get("web");
			if (webObj instanceof Map) {
				MapModel webMap = MapModel.from((Map<String, Object>) webObj);
				if (ArgUtil.is(webMap)) {
					WebPlugin.WebConfigDetails webConfig = config.getWeb();
					if (webMap.containsKey("iceBreaker")) {
						webConfig.setIceBreaker(webMap.entry("iceBreaker").asString(null));
					}
				}
			}

			updateChannelConfig(config, channelDetails);
		}

		default public String getDefaultName(ChannelConfig config) {
			if (!ArgUtil.is(config.getName())) {
				return String.format("%s %s", this.getContactType(), config.getLane());
			}
			return config.getName();
		}

	}

	public interface DefaultChannelPlugin<T extends AChannelDetails> extends ChannelPlugin<T> {

		@Override
		default public void addConfigMeta(List<ConfigMeta> configMetaList) {
			Class<?> clazz = AopProxyUtils.ultimateTargetClass(newChannelDetails());
			String pathContext = Constants.BLANK;
			if (clazz.isAnnotationPresent(ConfigMetaProperty.class)) {
				ConfigMetaProperty annotation = clazz.getAnnotation(ConfigMetaProperty.class);
				if (ArgUtil.is(annotation.context())) {
					pathContext = annotation.context() + ".";
				}
			}
			Field[] fields = ClazzUtil.getAllFields(clazz);

			for (Field field : fields) {
				if (field.isAnnotationPresent(ConfigMetaProperty.class)) {
					ConfigMetaProperty annotation = field.getAnnotation(ConfigMetaProperty.class);
					String path = pathContext + annotation.path();
					ConfigMeta cm = new ConfigMeta().path(path).pathRaw(annotation.pathRaw()).title(annotation.title())
							.desc(annotation.desc()).createonly(annotation.createonly())
							.writeonly(annotation.writeonly()).optional(annotation.optional())
							.inputType(annotation.inputType());
					if (annotation.inputType() == INPUT_TYPE.OPTIONS && annotation.dataType() == DATA_TYPE.SWITCH
							&& annotation.converterType() == CONVERT_TYPE.BOOLEAN) {
						cm.optionsOnOff();
					} else if (annotation.inputType() == INPUT_TYPE.OPTIONS && ArgUtil.is(annotation.optionsSource())) {
						cm.optionsSource(annotation.optionsSource()).optionsKey(annotation.optionsKey())
								.optionsLabel(annotation.optionsLabel());
					}
					if (ArgUtil.is(annotation.defaultValue())) {
						cm.defaultValue(annotation.defaultValue());
					}

					if (ArgUtil.is(annotation.optionValues())) {
						for (String optionValue : annotation.optionValues()) {
							cm.optionValues(optionValue);
						}
					}

					if (annotation.hidden()) {
						cm.hidden();
					}

					configMetaList.add(cm);
				}
			}
		}

		@Override
		default public void importChannelDetailsFromMap(T channelDetails, MapModel map) {
			Class<?> clazz = AopProxyUtils.ultimateTargetClass(channelDetails);

			String pathContext = Constants.BLANK;
			if (clazz.isAnnotationPresent(ConfigMetaProperty.class)) {
				ConfigMetaProperty annotation = clazz.getAnnotation(ConfigMetaProperty.class);
				if (ArgUtil.is(annotation.context())) {
					pathContext = annotation.context() + ".";
				}
			}

			Field[] fields = ClazzUtil.getAllFields(clazz);

			for (Field field : fields) {
				if (field.isAnnotationPresent(ConfigMetaProperty.class)) {
					ConfigMetaProperty annotation = field.getAnnotation(ConfigMetaProperty.class);
					// pd = new PropertyDescriptor(field.getName(), clazz);
					PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(clazz, field.getName());
					if (ArgUtil.is(pd)) {
						Method setter = pd.getWriteMethod();
						Method getter = pd.getReadMethod();
						Type type = field.getGenericType();
						String typeName = type.getTypeName();
						String path = pathContext + annotation.path();
						// Class<?> componentType = ((Class<?>) type).getComponentType();
						try {
							Object currentValue = getter.invoke(channelDetails);
							if ("java.lang.String".equals(typeName)) {
								setter.invoke(channelDetails,
										map.pathEntry(path).asString(ArgUtil.parseAsString(currentValue)));
							} else if ("int".equals(typeName) || "java.lang.Integer".equals(typeName)) {
								setter.invoke(channelDetails,
										map.pathEntry(path).asInteger(ArgUtil.parseAsInteger(currentValue)));
							} else if ("boolean".equals(typeName) || "java.lang.Boolean".equals(typeName)) {
								setter.invoke(channelDetails,
										map.pathEntry(path).asBoolean(ArgUtil.parseAsBoolean(currentValue)));
							} else if ("java.lang.String[]".equals(typeName)) {
								setter.invoke(channelDetails, map.pathEntry(path).value());
							} else if (type instanceof Class && ((Class<?>) type).isEnum()) {
								setter.invoke(channelDetails,
										map.pathEntry(path).asEnum(ArgUtil.parseAsEnum(currentValue, type), type));
							} else if (typeName.startsWith("java.util.Map<java.lang.String")) {
								setter.invoke(channelDetails, map.pathEntry(path).asMap());
							} else if (Stringable.class.isAssignableFrom((Class<?>) type)
									|| ((Class<?>) type).isAssignableFrom(Stringable.class)) {
								Class<?> cl = Class.forName(typeName);
								Constructor<?> cons = cl.getConstructor();
								Stringable o = (Stringable) cons.newInstance();
								o.fromString(map.pathEntry(ArgUtil.nonEmpty(annotation.pathRaw(), path)).asString());
								setter.invoke(channelDetails, o);
							} else {
								setter.invoke(channelDetails, map.pathEntry(path).defaultValue(currentValue));
							}
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
								| ClassNotFoundException | NoSuchMethodException | SecurityException
								| InstantiationException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	public static final Map<String, ChannelPlugin<? extends AChannelDetails>> PLUGIN_MAPPING = new HashMap<String, ChannelPlugin<? extends AChannelDetails>>();
	public static final Map<String, AChannelDetails> DETAILS_MAPPING = new HashMap<String, AChannelDetails>();

	public static <C extends AChannelDetails> void register(ChannelPlugin<C> channelPlugin) {
		C details = channelPlugin.newChannelDetails();
		DETAILS_MAPPING.put(channelPlugin.getChannelType(), channelPlugin.newChannelDetails());
		PLUGIN_MAPPING.put(channelPlugin.getChannelType(), channelPlugin);
	}

	public static ChannelPlugin<? extends AChannelDetails> getOrDefault(String channelType) {
		return PLUGIN_MAPPING.getOrDefault(channelType, WEB);
	}

	public static ChannelPlugin<? extends AChannelDetails> get(String channelType) {
		return PLUGIN_MAPPING.get(channelType);
	}

	private static final WebPlugin WEB = new WebPlugin();
	private static final FacebookPlugin FACEBOOK = new FacebookPlugin();
	private static final TwitterPlugin TWITTER = new TwitterPlugin();
	private static final TelegramPlugin TELEGRAM = new TelegramPlugin();
	private static final WAGupShupPlugin WA_GUPSHUP = new WAGupShupPlugin();
	private static final WA360Plugin WA_360D = new WA360Plugin();
	private static final InstagramPlugin INSTAGRAM = new InstagramPlugin();
	private static final EmailPlugin EMAIL = new EmailPlugin();
	private static final OAPlugin OA = new OAPlugin();
	/** WABA CLOUD plugin **/
	public static final WA360CloudPlugin WA_360DC = new WA360CloudPlugin();

	static {
//		register(WEB);
//		register(FACEBOOK);
//		register(TWITTER);
//		register(TELEGRAM);
//		register(WA_GUPSHUP);
//		register(WA_360D);
//		register(INSTAGRAM);
//		register(EMAIL);
//		register(new SMSPlugin());
//		register(new TwilioSMSPlugin());
//		register(OA);
//		register(WA_360DC);
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new AssignableTypeFilter(ChannelPlugin.class));

		Set<BeanDefinition> components = provider.findCandidateComponents("com/boot/jx");

		for (BeanDefinition component : components) {
			try {
				Class cls = Class.forName(component.getBeanClassName());
				@SuppressWarnings("unchecked")
				Constructor<?> ctor = cls.getConstructor();
				if (ctor != null) {
					Object object = ctor.newInstance();
					if (object != null) {
						register((ChannelPlugin<AChannelDetails>) object);
					}
				}

			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
					| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				LOGGER.error("No Default Constructor {}(AmxApiError apiError)", component.getBeanClassName(), e);
			}
		}

	}

}
