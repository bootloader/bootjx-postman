package com.boot.jx.postman.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.boot.jx.dict.ContactType;
import com.boot.jx.logger.LoggerService;
import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex;
import com.boot.jx.mongo.CommonMongoTemplate;
import com.boot.jx.mongo.MongoUtils;
import com.boot.jx.postman.PMConstants;
import com.boot.jx.postman.PMConstants.APP_TYPE;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.PMEnvironment.PMConfigurationObject;
import com.boot.jx.postman.client.CommonServiceClient;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.MessageDoc;
import com.boot.jx.postman.doc.QuickMedia;
import com.boot.jx.postman.doc.QuickReply;
import com.boot.jx.postman.doc.config.ClientAppConfigDoc;
import com.boot.jx.postman.doc.config.CustomerFieldMasterDoc;
import com.boot.jx.postman.store.QuickStore;
import com.boot.jx.utils.PostManUtil;
import com.boot.utils.ArgUtil;

@Component
public class StarterDocKit {

	public static final Logger LOGGER = LoggerService.getLogger(StarterDocKit.class);

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private CommonMongoTemplate commonMongoTemplate;

	@Autowired
	private PMEnvironment pmEnvironment;

	@Autowired(required = false)
	private ConfigManager configManager;

	@Autowired(required = false)
	private QuickStore quickStore;

	@Autowired(required = false)
	private CommonServiceClient commonServiceClient;

	private QuickMedia createTemplateReply(String name, String title, String category, String content, String url) {
		QuickMedia temp5 = commonMongoTemplate.findById(name, QuickMedia.class);
		if (ArgUtil.isEmpty(temp5)) {
			temp5 = new QuickMedia();
		}
		temp5.setId(name);
		temp5.setCode(name);
		temp5.setTitle(title);
		temp5.setType("IMAGE");
		temp5.setCategory(category);
		temp5.setUrl(url);
		temp5.setContent(content);
		return temp5;
	}

	private QuickReply createQuickReply(String id, String title, String category) {
		QuickReply temp5 = commonMongoTemplate.findById(id, QuickReply.class);
		if (ArgUtil.isEmpty(temp5)) {
			temp5 = new QuickReply();
		}
		temp5.setId(id);
		temp5.setTitle(title);
		temp5.setCategory(category);
		return temp5;
	}

	private void createClientApp(ClientAppConfigDoc clientAppConfig) {
		ClientAppConfigDoc app = commonMongoTemplate.findById(clientAppConfig.getId(), ClientAppConfigDoc.class);
		if (ArgUtil.isEmpty(app) || !ArgUtil.areEqual(app.getKeyVersion(), clientAppConfig.getKeyVersion())) {
			try {
				commonMongoTemplate.save(clientAppConfig);
			} catch (Exception e) {
				LOGGER.error("createClientAppErrror:" + clientAppConfig.getKeyName(), e);
			}
		}
	}

	private void createPredefinedMstField(String code, String titleAndDesc, String type) {

		Query qryQuery = new Query();
		Criteria criteria = Criteria.where("code").is(code);
		qryQuery.addCriteria(criteria);
		CustomerFieldMasterDoc fiedMaster = commonMongoTemplate.findOne(qryQuery, CustomerFieldMasterDoc.class);
		if (ArgUtil.is(fiedMaster)) {
			if (!type.equalsIgnoreCase(fiedMaster.getType())) {
				commonMongoTemplate.remove(fiedMaster);
				fiedMaster = null;
			}
		}

		if (!ArgUtil.is(fiedMaster)) {
			fiedMaster = new CustomerFieldMasterDoc();
			fiedMaster.setCode(code);
			fiedMaster.setLabel(titleAndDesc);
			fiedMaster.setType(type);
			fiedMaster.setDesc(titleAndDesc);
			fiedMaster.setActive(true);
			fiedMaster.setPredefined(true);
			fiedMaster.setRequired(false);
			fiedMaster.setCreated(TimeStampIndex.now());
			if (ArgUtil.is(code) && code.equalsIgnoreCase("gender")) {
				List<Object> defaultOptions = new ArrayList<>();
				// Option 1: Male
				Map<String, String> maleOption = new HashMap<>();
				maleOption.put("label", "Male");
				maleOption.put("value", "male");
				defaultOptions.add(maleOption);
				// Option 2: Female
				Map<String, String> femaleOption = new HashMap<>();
				femaleOption.put("label", "Female");
				femaleOption.put("value", "female");
				defaultOptions.add(femaleOption);
				// Set the default options in the fieldMaster
				fiedMaster.setPossibleOptions(defaultOptions);
			} else if (ArgUtil.is(code) && code.equalsIgnoreCase("title")) {
				List<Object> defaultOptions = new ArrayList<>();
				// Option 1: Mr.
				Map<String, String> mrOpt = new HashMap<>();
				mrOpt.put("label", "Mr.");
				mrOpt.put("value", "mr.");
				defaultOptions.add(mrOpt);
				// Option 2: Mrs.
				Map<String, String> mrsOpt = new HashMap<>();
				mrsOpt.put("label", "Mrs.");
				mrsOpt.put("value", "mrs.");
				defaultOptions.add(mrsOpt);

				Map<String, String> msOpt = new HashMap<>();
				msOpt.put("label", "Ms.");
				msOpt.put("value", "ms.");
				defaultOptions.add(msOpt);

				Map<String, String> drOpt = new HashMap<>();
				drOpt.put("label", "Dr.");
				drOpt.put("value", "dr.");
				defaultOptions.add(drOpt);
				// Set the default options in the fieldMaster
				fiedMaster.setPossibleOptions(defaultOptions);
			}
		}

		if (ArgUtil.is(fiedMaster)) {
			try {
				commonMongoTemplate.save(fiedMaster);
			} catch (Exception e) {
				LOGGER.error("createCustomerMasterFieldErrror:" + code, e);
			}
		}

	}

	private void createPredefinedMstField() {
		PMConfigurationObject version = pmEnvironment.local().prefsEntry("version.customer.field.master");
		String predefiend_customer_filed_version = "v1.7";
		if (!version.is(predefiend_customer_filed_version)) {
			createPredefinedMstField("title", "Title", "dropdown");
			createPredefinedMstField("dob", "Date of Birth", "date");
			createPredefinedMstField("gender", "Gender", "dropdown");
			createPredefinedMstField("alt_phones", "Alternate phone", "phone");
			createPredefinedMstField("alt_emails", "Alternate email", "email");
			version.setValue(predefiend_customer_filed_version);
			configManager.save(version);
		}
	}

	/**
	 * Required to create index
	 * 
	 * @param contactType
	 */
	private void createMessageIndex() {
		PMConfigurationObject version = pmEnvironment.local().prefsEntry("version.message.index");
		String predefiend_customer_filed_version = "v1";
		if (!version.is(predefiend_customer_filed_version)) {

			for (ContactType contactType : ContactType.values()) {
				MessageDoc wa = MessageDoc.instance(contactType);
				mongoTemplate.save(wa);
				mongoTemplate.remove(wa);
			}

			version.setValue(predefiend_customer_filed_version);
			configManager.save(version);
		}
	}

	private void createDefaultTemplats() {
		commonMongoTemplate.save(createTemplateReply("GIRL_AND_BIKE", "Girl and bike", "Gallery1", "See this Nice Pic",
				"https://res.cloudinary.com/www-mehery-com/image/upload/v1611688334/samples/bike.jpg"));

		commonMongoTemplate.save(createTemplateReply("OFFICE_N_WORK", "Office & Work", "Gallery1", "Work environment",
				"https://res.cloudinary.com/www-mehery-com/image/upload/v1611688339/samples/imagecon-group.jpg"));

		commonMongoTemplate.save(createTemplateReply("KITTEN_PLAYING", "Kitten Playing", "Animals", "Happy Kitten",
				"https://res.cloudinary.com/www-mehery-com/image/upload/v1611688341/samples/animals/kitten-playing.gif"));

		commonMongoTemplate.save(createTemplateReply("THREE_DOGS", "Three Dogs", "Animals", "Gang of Dogs",
				"https://res.cloudinary.com/www-mehery-com/image/upload/v1611688335/samples/animals/three-dogs.jpg"));

		commonMongoTemplate.save(createTemplateReply("REINDEER", "Reindeer", "Animals", "In Snow",
				"https://res.cloudinary.com/www-mehery-com/image/upload/v1611688331/samples/animals/reindeer.jpg"));

		commonMongoTemplate.save(createTemplateReply("CAT", "Cat", "Animals", "Bad Cat",
				"https://res.cloudinary.com/www-mehery-com/image/upload/v1611688330/samples/animals/cat.jpg"));

		commonMongoTemplate.save(createTemplateReply("ACCESSORIES BAG", "Accessories Bag", "Ecommerce", "Cool bag",
				"https://res.cloudinary.com/www-mehery-com/image/upload/v1611688338/samples/ecommerce/accessories-bag.jpg"));

		commonMongoTemplate.save(createTemplateReply("LEATHER BAG GRAY", "Leather Bag Gray", "Ecommerce", "Formal bag",
				"https://res.cloudinary.com/www-mehery-com/image/upload/v1611688338/samples/ecommerce/leather-bag-gray.jpg"));

		commonMongoTemplate.save(createTemplateReply("SHOES", "Shoes", "Ecommerce", "Purple Shoes",
				"https://res.cloudinary.com/www-mehery-com/image/upload/v1611688333/samples/ecommerce/shoes.png"));

		// Quick Replies
		commonMongoTemplate.save(createQuickReply("0", "Hello", "greeting"));
		commonMongoTemplate.save(createQuickReply("1", "Very Good Morning", "greeting-morning"));
		commonMongoTemplate.save(createQuickReply("2", "Very Good After Noon", "greeting-afternoon"));
		commonMongoTemplate.save(createQuickReply("3", "Very Good Evening", "greeting-evening"));
		commonMongoTemplate.save(createQuickReply("4", "Nice talking too.", "conversation-complete"));
		commonMongoTemplate.save(createQuickReply("5", "You're welcome.", "conversation-complete"));
	}

	public void onlyOncePerDomain() {
		createMessageIndex();
		createPredefinedMstField();
	}

	public boolean isFlagUpdated(String flagKey, String latestVersion) {
		PMConfigurationObject currentVersion = pmEnvironment.local().prefsEntry(flagKey);
		if (!currentVersion.is(latestVersion) && currentVersion.lessThan(latestVersion)) {
			currentVersion.setValue(latestVersion);
			configManager.save(currentVersion);
			return true;
		}
		return false;
	}

	public void domain() {
		if (isFlagUpdated("domain.created.version", "v3")) {
			onlyOncePerDomain();
			commonServiceClient.publishDomainCreatedEvent("v3");
			if (ArgUtil.is(configManager)) {
				configManager.refresh();
			}
		}
		if (isFlagUpdated("domain.indexes.session", "v1")) {
			MongoUtils.cleanupIndexes(commonMongoTemplate, "CHAT_SESSION", ChatSessionDoc.class);
		}

		if (isFlagUpdated("version.ticket.meta", "v1")) {
			quickStore.createTicketMeta();
		}
	}

	@PostConstruct
	public void init() {

		try {
			createDefaultTemplats();

		} catch (Exception e) {
			e.printStackTrace();
		}

		ClientAppConfigDoc agentApp = new ClientAppConfigDoc();
		agentApp.setId(PMConstants.DEFAULT.AGENT_QUEUE_CODE);
		agentApp.setKeyName("Agent Desk");
		agentApp.setQueue(PMConstants.DEFAULT.AGENT_QUEUE_CODE);
		agentApp.setAppType(APP_TYPE.AGENT.name());
		agentApp.setKey(PostManUtil.UNIQUE_API_KEY());
		agentApp.setKeyVersion("v3");
		agentApp.setShared(true);
		createClientApp(agentApp);

		ClientAppConfigDoc botApp = new ClientAppConfigDoc();
		botApp.setId(PMConstants.DEFAULT.BOT_QUEUE_CODE);
		botApp.setKeyName("Basic Bot");
		botApp.setQueue(PMConstants.DEFAULT.BOT_QUEUE_CODE);
		botApp.setAppType(APP_TYPE.BOT.name());
		botApp.setKey(PostManUtil.UNIQUE_API_KEY());
		botApp.setKeyVersion("v3");
		botApp.setShared(true);
		createClientApp(botApp);

		ClientAppConfigDoc adminApp = new ClientAppConfigDoc();
		adminApp.setId(PMConstants.DEFAULT.ADMIN_QUEUE_CODE);
		adminApp.setKeyName("Admin App");
		adminApp.setQueue(PMConstants.DEFAULT.ADMIN_QUEUE_CODE);
		adminApp.setAppType(APP_TYPE.DEFAULT.name());
		adminApp.setKey(PostManUtil.UNIQUE_API_KEY());
		adminApp.setKeyVersion("v4");
		adminApp.setShared(true);
		createClientApp(adminApp);

		ClientAppConfigDoc feedbackApp = new ClientAppConfigDoc();
		feedbackApp.setId(PMConstants.DEFAULT.FEEDBACK_QUEUE_CODE);
		feedbackApp.setKeyName("Feedback Collector");
		feedbackApp.setQueue(PMConstants.DEFAULT.FEEDBACK_QUEUE_CODE);
		feedbackApp.setAppType(APP_TYPE.FEEDBACK.name());
		feedbackApp.setKey(PostManUtil.UNIQUE_API_KEY());
		feedbackApp.setKeyVersion("v4");
		feedbackApp.setShared(true);
		createClientApp(feedbackApp);

		ClientAppConfigDoc bootflowApp = new ClientAppConfigDoc();
		bootflowApp.setId(PMConstants.DEFAULT.APP_FLOW_QUEUE_CODE);
		bootflowApp.setKeyName("AppFlow");
		bootflowApp.setQueue(PMConstants.DEFAULT.APP_FLOW_QUEUE_CODE);
		bootflowApp.setAppType(APP_TYPE.APPFLOW.name());
		bootflowApp.setKey(PostManUtil.UNIQUE_API_KEY());
		bootflowApp.setKeyVersion("v4");
		bootflowApp.setShared(true);
		createClientApp(bootflowApp);
		

	}
}
