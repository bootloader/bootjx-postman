package com.boot.jx.postman.store;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.boot.jx.AppContextUtil;
import com.boot.jx.api.ApiFieldError;
import com.boot.jx.api.ApiResponseUtil;
import com.boot.jx.dict.ContactType;
import com.boot.jx.logger.AuditDetailProvider;
import com.boot.jx.logger.LoggerService;
import com.boot.jx.model.ModelPatch;
import com.boot.jx.model.ModelPatch.ModelPatchCommand;
import com.boot.jx.model.ModelPatch.ModelPatches;
import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex;
import com.boot.jx.mongo.CommonMongoQB.MongoQueryBuilder;
import com.boot.jx.mongo.CommonMongoQueryBuilder;
import com.boot.jx.mongo.CommonMongoQueryBuilder.SimpleDocQuery;
import com.boot.jx.mongo.CommonMongoQueryBuilder.SimpleDocQueryBuilder;
import com.boot.jx.mongo.CommonMongoTemplate;
import com.boot.jx.mongo.MongoStore;
import com.boot.jx.postman.PMEnvironment;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.CustomerProfileDoc;
import com.boot.jx.postman.doc.PBContactDoc;
import com.boot.jx.postman.doc.config.CustomerFieldMasterDoc;
import com.boot.jx.postman.dto.VCallMeta;
import com.boot.jx.postman.model.ContactMeta;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.postman.model.MessageDefinitions.IMessage;
import com.boot.jx.postman.pbook.PBAddress;
import com.boot.jx.postman.pbook.PBDate;
import com.boot.jx.postman.pbook.PBDocument;
import com.boot.jx.postman.pbook.PBEmail;
import com.boot.jx.postman.pbook.PBName;
import com.boot.jx.postman.pbook.PBPhone;
import com.boot.jx.postman.pbook.PBWebsite;
import com.boot.jx.postman.query.ChatContactQuery;
import com.boot.jx.scope.tnt.Tenants;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.UtilityModels.UniqueIndex;
import com.boot.utils.ArgUtil;
import com.boot.utils.Constants;
import com.boot.utils.DateUtil;
import com.boot.utils.JsonUtil;
import com.boot.utils.PatternUtil;
import com.boot.utils.PhoneUtil;
import com.boot.utils.PhoneUtil.PhoneNumberResult;
import com.boot.utils.UniqueID;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

@Component
public class ContactStore extends MongoStore {

	private static final Logger LOGGER = LoggerFactory.getLogger(ContactStore.class);

	public static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

	@Autowired
	protected PMEnvironment environment;

	@Autowired
	CommonMongoTemplate cMongoTemplate;

	@Lazy
	@Autowired(required = false)
	protected AuditDetailProvider auditDetailProvider;

	@Autowired
	private MessageContextStore messageContextStore;

	/**
	 * This will return ChatContactDoc from contextStore, only if it matches the
	 * contactId, this will not fallback to DB lookup
	 * 
	 * @param contactId
	 * @return
	 */
	public ChatContactDoc getContactById(String contactId) {
		if (messageContextStore == null) {
			return null;
		}
		ChatContactDoc contact = messageContextStore.getChatContactDoc();

		if (contact == null) {
			return null;
		}

		if (ArgUtil.is(contact.getContactId(), contactId)) {
			if (LoggerService.isLocalDebug())
				LOGGER.info("HIT [CHAT_CONTACT] {}", contactId);
			return messageContextStore.getChatContactDoc();
		}
		return null;
	}

	public ChatContactDoc findContactById(String contactId) {
		ChatContactDoc x = getContactById(contactId);
		if (x != null) {
			return x;
		}
		return findById(contactId, ChatContactDoc.class);
	}

	public ChatContactDoc store(ChatContactDoc chatContact) {
		messageContextStore.setChatContactDoc(chatContact);
		return chatContact;
	}

	/**
	 * 
	 * @deprecated
	 * 
	 *             This will find doc by id inside Contactable or create contactId
	 *             by creating new object from Contactable first and the find the
	 *             doc
	 * 
	 * @param contactMeta
	 * @return
	 */
	public ChatContactDoc findContact(Contactable contactable) {
		Contactable contact = PostManUtil.getContactMeta(contactable);
		if (ArgUtil.isEmpty(contact.getContactId())) {
			return null;
		}
		return findContactById(contact.getContactId());
	}

	/**
	 * 
	 * This will find doc by id inside Contactable or create contactId from
	 * Contactable first and the find the doc
	 * 
	 * @param contactable
	 * @return
	 */
	public ChatContactDoc getContact(Contactable contactable) {
		String contactId = PostManUtil.createContactId(contactable);
		ChatContactDoc chatContactDoc = super.findById(contactId, ChatContactDoc.class);
		return chatContactDoc;
	}

	public ChatContactDoc getContact(IMessage inboxMessage) {
		String contactId = PostManUtil.createContactId(inboxMessage);
		ChatContactDoc chatContactDoc = super.findById(contactId, ChatContactDoc.class);
		return chatContactDoc;
	}

	public PBPhone parsePhone(PBPhone pbPhone) {
		String defaultRegion = environment.config().prefsEntry("postman.phonebook.region").asString("IN");
		if (ArgUtil.not(pbPhone.phone)) {
			pbPhone.phone = String.format("+%s%s", pbPhone.countryCallingCode, pbPhone.nationalNumber);
		}
		pbPhone.phone = pbPhone.phone.replace(" ", "").replaceAll("^[\\+0\\s]+(?!$)", "").trim();
		PhoneNumberResult wrappedNumber = PhoneUtil.parse("+" + pbPhone.phone, defaultRegion);

		if (wrappedNumber.isMock()) {
			pbPhone.phone = "+" + pbPhone.phone;

			if (pbPhone.phone.startsWith("+155501")) {
				pbPhone.countryCallingCode = "1";
				pbPhone.nationalNumber = pbPhone.phone.substring(2);
			} else {
				pbPhone.countryCallingCode = "99";
				pbPhone.nationalNumber = pbPhone.phone.substring(3);
			}
		} else {
			PhoneNumber phoneNumber = wrappedNumber.getPhone();
			pbPhone.nationalNumber = com.boot.utils.ArgUtil.parseAsString(phoneNumber.getNationalNumber());
			pbPhone.countryCallingCode = com.boot.utils.ArgUtil.parseAsString(phoneNumber.getCountryCode());
			pbPhone.phone = String.format("+%s%s", phoneNumber.getCountryCode(), phoneNumber.getNationalNumber());
			pbPhone.country = PHONE_NUMBER_UTIL.getRegionCodeForCountryCode(phoneNumber.getCountryCode());
		}

		return pbPhone;
	}

	public void applyCallPermissionReply(String contactId, String rawResponse, Long grantedAt) {
		String raw = ArgUtil.parseAsString(rawResponse);
		if (!ArgUtil.is(raw)) {
			return;
		}
		String v = raw.trim();
		boolean allowed;
		if (v.equalsIgnoreCase("accept")) {
			allowed = true;
		} else if (v.equalsIgnoreCase("reject")) {
			allowed = false;
		} else {
			return;
		}

		VCallMeta meta = new VCallMeta();
		meta.setPermission(allowed);
		meta.setGrantedAt(grantedAt != null ? grantedAt.longValue() : System.currentTimeMillis());
		ChatContactQuery q = new ChatContactQuery(contactId);
		q.setVCallMeta(meta);
		super.updateFirst(q);
	}

	public List<ChatContactDoc> searchContacts(String search, String lane) {
		// TODO:-- Optimize Search
		// Query query =
		// TextQuery.queryText(TextCriteria.forDefaultLanguage().matching(search)).sortByScore()

		Criteria c = Criteria.where("lane").is(lane); // Lane should be fixed

		if (ArgUtil.is(search)) {
			c = c.orOperator(
					// Check all fields
					Criteria.where("name").regex(PatternUtil.toPattern("" + search + "", "i")),
					Criteria.where("phone").regex(PatternUtil.toPattern("" + search + "", "i")),
					Criteria.where("email").regex(PatternUtil.toPattern("" + search + "", "i")));
		}
		Query query = new Query()
				// New Criteria
				.addCriteria(c);
		return find(query, ChatContactDoc.class);

	}

	public CustomerProfileDoc findProfileByPhone(String phone) {
		PBPhone ph = parsePhone(new PBPhone().phone(phone));
		MongoQueryBuilder<CustomerProfileDoc> qb = CommonMongoQueryBuilder.collection(CustomerProfileDoc.class)
				.where(Criteria.where("phones").elemMatch(Criteria.where("nationalNumber").is(ph.nationalNumber)
						.and("countryCallingCode").is(ph.countryCallingCode)));
		return findOne(qb);
	}

	public CustomerProfileDoc findProfileByEmail(String email) {
		MongoQueryBuilder<CustomerProfileDoc> qb = CommonMongoQueryBuilder.collection(CustomerProfileDoc.class)
				.where(Criteria.where("emails").elemMatch(Criteria.where("email").is(email)));
		return findOne(qb);
	}

	public List<CustomerProfileDoc> findProfileByContactId(String contactId) {
		ChatContactDoc contact = findById(contactId, ChatContactDoc.class);

		List<Criteria> orOperator = new LinkedList<Criteria>();

		if (ArgUtil.is(contact.getEmail())) {
			orOperator.add(Criteria.where("emails").elemMatch(Criteria.where("email").is(contact.getEmail())));
		}
		if (ArgUtil.is(contact.phone())) {
			PBPhone ph = parsePhone(new PBPhone().phone(contact.phone()));
			orOperator.add(Criteria.where("phones").elemMatch(Criteria.where("nationalNumber").is(ph.nationalNumber)
					.and("countryCallingCode").is(ph.countryCallingCode)));
		}

		if (ArgUtil.is(contact.user().getCode())) {
			orOperator.add(Criteria.where("code").is(contact.user().getCode()));
		}

		if (ArgUtil.is(contact.user().getEmail())) {
			orOperator.add(Criteria.where("emails").elemMatch(Criteria.where("email").is(contact.user().getEmail())));
		}

		if (ArgUtil.is(contact.user().getMobile())) {
			PBPhone ph = parsePhone(new PBPhone().phone(contact.user().getMobile()));
			orOperator.add(Criteria.where("phones").elemMatch(Criteria.where("nationalNumber").is(ph.nationalNumber)
					.and("countryCallingCode").is(ph.countryCallingCode)));
		}

		if (ArgUtil.is(orOperator.size())) {
			MongoQueryBuilder<CustomerProfileDoc> qb = CommonMongoQueryBuilder.collection(CustomerProfileDoc.class)
					.where(new Criteria().orOperator(orOperator.toArray(new Criteria[orOperator.size()])));
			return find(qb);
		} else {
			return new ArrayList<CustomerProfileDoc>();
		}
	}

	public void linkProfile(ChatContactQuery contactQuery, SimpleDocQuery<CustomerProfileDoc> profileQuery) {
		ChatContactDoc contact = contactQuery.getDoc();
		CustomerProfileDoc profile = profileQuery.getDoc();

		// Update Contact
		contact.profile().setId(profile.getId());
		contact.profile().setCode(profile.code);
		contact.profile().setName(profile.name.getFormattedName());
		contactQuery.set("profile", contact.profile());

		// Update Profile
		ContactMeta connected = new ContactMeta();
		connected.copyFrom(contact);
		profileQuery.setunset("linked", patch(ModelPatchCommand.ADD, profile.linked(), connected));

	}

	public void linkProfile(ChatContactQuery contactQuery, CustomerProfileDoc profile) {
		SimpleDocQuery<CustomerProfileDoc> profileQuery = SimpleDocQuery.doc(profile);
		linkProfile(contactQuery, profileQuery);
		update(profileQuery);
	}

	public ChatContactDoc linkProfile(String contactId, String profileId) {
		ChatContactDoc contact = findById(contactId, ChatContactDoc.class);
		ChatContactQuery contactQuery = new ChatContactQuery(contact);
		CustomerProfileDoc profile = findById(profileId, CustomerProfileDoc.class);
		linkProfile(contactQuery, profile);
		update(contactQuery);
		return contact;
	}

	public ChatContactDoc delinkProfile(String contactId) {
		ChatContactDoc contact = findById(contactId, ChatContactDoc.class);

		// Update Contact
		ChatContactQuery query = new ChatContactQuery(contact);
		String profileId = contact.profile().getId();
		query.unset("profile");
		contact.setProfile(null);
		update(query);

		if (ArgUtil.is(profileId)) {
			CustomerProfileDoc profile = findById(profileId, CustomerProfileDoc.class);
			SimpleDocQuery<CustomerProfileDoc> profileQuery = SimpleDocQuery.doc(profile);
			ContactMeta connected = new ContactMeta();
			connected.copyFrom(contact);
			profileQuery.setunset("linked", patch(ModelPatchCommand.REMOVE, profile.linked(), connected));
			update(profileQuery);

		}

		return contact;
	}

	public static <T extends UniqueIndex<T>> Set<T> patch(ModelPatchCommand command, Set<T> items, T item) {
		switch (command) {
		case ADD:
		case UPDATE:
			Optional<T> found = Optional.empty();
			if (ArgUtil.is(item.uuid())) {
				found = items.stream().filter(itm -> ArgUtil.is(item.uuid(), itm.uuid())).findFirst();
			}
			if (found.isPresent()) {
				found.get().update(item);
			} else {
				item.uuid(UniqueID.generateString());
			}
			items.add(item);
			break;
		case REMOVE:
			items.remove(item);
			break;
		case DELETE:
			return null;
		default:
			break;
		}
		return items;
	}

	public CustomerProfileDoc patchCustomerProfile(ModelPatches req) {
		CustomerProfileDoc doc = findById(req.getId(), CustomerProfileDoc.class);
		SimpleDocQueryBuilder qb = SimpleDocQueryBuilder.doc(doc);
		for (ModelPatch patch : req.getPatches()) {
			switch (patch.getField()) {
			case "email":
			case "emails":
				PBEmail email = patch.value().as(PBEmail.class);
				qb.setunset("emails", patch(patch.getCommand(), doc.emails(), email));
				break;
			case "phone":
			case "phones":
			case "mobile":
			case "mobiles":
				PBPhone phone = parsePhone(patch.value().as(PBPhone.class));
				qb.setunset("phones", patch(patch.getCommand(), doc.phones(), phone));
				break;
			case "address":
			case "addresses":
				PBAddress address = patch.value().as(PBAddress.class);
				qb.setunset("addresses", patch(patch.getCommand(), doc.addresses(), address));
				break;
			case "url":
			case "urls":
				PBWebsite url = patch.value().as(PBWebsite.class);
				qb.setunset("urls", patch(patch.getCommand(), doc.urls(), url));
				break;
			case "name":
				PBName name = patch.value().as(PBName.class);
				qb.setunset("name", name.fix());
				break;
			case "code":
				qb.setunset("code", patch.value().asString());
				break;
			case "rmCode":
				qb.setunset("rmCode", patch.value().asString());
				break;

			case "additionalInfo.alt_phones":
			case "alt_phones":
				PBPhone alt_phone = parsePhone(patch.value().as(PBPhone.class));
				qb.setunset("additionalInfo.alt_phones", patch(patch.getCommand(), doc.phones(), alt_phone));
				break;
			case "additionalInfo.alt_emails":
			case "alt_emails":
				PBEmail alt_email = patch.value().as(PBEmail.class);
				qb.setunset("additionalInfo.alt_emails", patch(patch.getCommand(), doc.emails, alt_email));
				break;

			default:
				// TODO:- Check additonal validty in Masters and its type in master,
				// then based on type of field do conversion below and update instead
				// using.asString() for all
				Object objType = checkFieldType(getFileName(patch.getField()), patch.value());
				if (ArgUtil.is(objType)) {
					if (patch.getCommand().equals(ModelPatchCommand.REMOVE)) {
						qb.setunset(patch.getField(), ArgUtil.parseAsT(Constants.BLANK, objType, false));
					} else {
						qb.setunset(patch.getField(), ArgUtil.parseAsT(patch.getValue(), objType, false));
					}
				}
				break;
			}
			update(qb);
			doc = findById(req.getId(), CustomerProfileDoc.class);
		}

		return doc;
	}

	public CustomerProfileDoc findProfileByCode(String code) {
		MongoQueryBuilder<CustomerProfileDoc> qb = CommonMongoQueryBuilder.collection(CustomerProfileDoc.class)
				.where(Criteria.where("code").is(code));
		return findOne(qb);
	}

	public void checkDuplicate(ModelPatches req) {
		CustomerProfileDoc cusPfDoc = null;

		for (ModelPatch patch : req.getPatches()) {
			if (patch.getCommand().equals(ModelPatchCommand.ADD)) {
				switch (patch.getField()) {
				case "email":
				case "emails":
					PBEmail email = patch.value().as(PBEmail.class);
					cusPfDoc = findProfileByEmail(email.getEmail());
					if (ArgUtil.is(cusPfDoc)) {
						ApiResponseUtil.throwInputException(new ApiFieldError().field(patch.getField())
								.codeKey("ValidEmailDuplicate").description(patch.getField() + " already exists"));
					}
					break;
				case "phone":
				case "phones":
					PBPhone phone = parsePhone(patch.value().as(PBPhone.class));
					cusPfDoc = findProfileByPhone(phone.getPhone());
					if (ArgUtil.is(cusPfDoc)) {
						ApiResponseUtil.throwInputException(new ApiFieldError().field(patch.getField())
								.codeKey("ValidPhoneDuplicate").description(patch.getField() + " already exists"));
					}
					break;
				case "code":
					cusPfDoc = findProfileByCode(patch.value().asString());
					if (ArgUtil.is(cusPfDoc)) {
						ApiResponseUtil.throwInputException(new ApiFieldError().field(patch.getField())
								.codeKey("ValidCodeDuplicate").description(patch.getField() + " already exists"));
					}
					break;
				}
			}

		}
	}

	@SuppressWarnings("unchecked")
	public CustomerProfileDoc createprofile(CustomerProfileDoc req) {
		ObjectMapper objectMapper = new ObjectMapper();
		CustomerProfileDoc doc = findById(req.getId(), CustomerProfileDoc.class);
		if (ArgUtil.is(doc)) {
			updateProfile(req, doc);
		} else {
			doc = new CustomerProfileDoc();

			if (ArgUtil.is(req.getName())) {
				PBName pbName = new PBName();
				pbName.setFirstName(req.getName().getFirstName());
				pbName.setLastName(req.getName().getLastName());
				pbName.setMiddleName(req.getName().getMiddleName());
				pbName.setFormattedName(req.getName().getFormattedName());
				pbName.fix();
				doc.setName(pbName);
			}

			if (ArgUtil.is(findProfileByCode(req.getCode()))) {
				ApiResponseUtil.throwInputException(new ApiFieldError().obzect("code").field("code")
						.codeKey("ValidCodeDuplicate").description(req.getCode() + " already exists"));
			}

			doc.setCode(req.getCode());
			doc.setRmCode(req.getRmCode());
			doc.setSource(req.getSource());
			Set<PBEmail> emails = new HashSet<>();
			if (req.getEmails() != null && !req.getEmails().isEmpty()) {
				Set<PBEmail> reqEmails = req.getEmails();

				for (PBEmail pbEmail : reqEmails) {
					PBEmail pb = new PBEmail();
					if (ArgUtil.is(findProfileByEmail(pbEmail.getEmail()))) {
						ApiResponseUtil.throwInputException(new ApiFieldError().obzect("email").field("email")
								.codeKey("ValidEmailDuplicate").description(pbEmail.getEmail() + " already exists"));
					}
					pb.setUuid(ArgUtil.parseAsString(pb.getUuid(), UniqueID.generateString()));
					emails.add(pb.update(pbEmail));
				}
				doc.setEmails(emails);
			}
			Set<PBPhone> phones = new HashSet<>();
			if (req.getPhones() != null && !req.getPhones().isEmpty()) {
				Set<PBPhone> reqPhones = req.getPhones();
				for (PBPhone phone : reqPhones) {
					if (ArgUtil.is(findProfileByPhone(phone.getPhone()))) {
						ApiResponseUtil.throwInputException(new ApiFieldError().obzect("phone").field("phone")
								.codeKey("ValidPhoneDuplicate").description(phone.getPhone() + " already exists"));
					}
					PBPhone pb = parsePhone(phone);
					pb.setUuid(ArgUtil.parseAsString(pb.getUuid(), UniqueID.generateString()));
					phones.add(pb);
				}
				doc.setPhones(phones);
			}
			if (req.getAddresses() != null && !req.getAddresses().isEmpty()) {
				Set<PBAddress> pAddresses = new HashSet<>();
				for (PBAddress adre : req.getAddresses()) {
					PBAddress pa = new PBAddress();
					pa.setUuid(ArgUtil.parseAsString(adre.getUuid(), UniqueID.generateString()));
					pa.update(adre);
					pAddresses.add(pa);

				}
				doc.setAddresses(pAddresses);

			}

			if (req.getWorks() != null && !req.getWorks().isEmpty()) {
				doc.setWorks(req.getWorks());
			}

			if (req.getUrls() != null && !req.getUrls().isEmpty()) {
				Set<PBWebsite> pws = new HashSet<>();
				for (PBWebsite ws : req.getUrls()) {
					PBWebsite pWebsite = new PBWebsite();
					pWebsite.setUuid(ArgUtil.parseAsString(ws.getUuid(), UniqueID.generateString()));
					pws.add(pWebsite.update(ws));

				}
				doc.setUrls(pws);
			}
			Map<String, Object> addInfoMap = new HashMap<String, Object>();
			if (req.getAdditionalInfo() != null && !req.getAdditionalInfo().isEmpty()) {
				Map<String, Object> addInfo = req.getAdditionalInfo();
				/** Retrieve all the key-value pairs from the map **/
				Set<Map.Entry<String, Object>> entries = addInfo.entrySet();
				for (Map.Entry<String, Object> entry : entries) {
					switch (entry.getKey()) {
					case "emails":
					case "alt_emails":
						List<Object> emailtList = (List<Object>) entry.getValue();
						Set<PBEmail> pbEmails = new TreeSet<PBEmail>();
						for (Object obj : emailtList) {
							Map<String, Object> pMap = JsonUtil.toJsonMap(obj);
							PBEmail pbEmail = new PBEmail();
							for (Map.Entry<String, Object> eMapmail : pMap.entrySet()) {
								if (eMapmail.getKey().contains("email")) {
									pbEmail.setEmail(ArgUtil.parseAsString(eMapmail.getValue(), Constants.BLANK));
								} else if (eMapmail.getKey().contains("label")) {
									pbEmail.setLabel(ArgUtil.parseAsString(eMapmail.getValue(), Constants.BLANK));
								} else if (eMapmail.getKey().contains("type")) {
									pbEmail.setType(ArgUtil.parseAsString(eMapmail.getValue(), Constants.BLANK));
								}
							}
							pbEmail.setUuid(UniqueID.generateString());
							pbEmails.add(pbEmail);

						}
						addInfoMap.put(entry.getKey(), pbEmails);
						break;
					case "phones":
					case "alt_phones":
						List<Object> phoneLstList = (List<Object>) entry.getValue();
						Set<PBPhone> pbPhones = new TreeSet<PBPhone>();
						for (Object obj : phoneLstList) {
							Map<String, Object> pMap = JsonUtil.toJsonMap(obj);
							PBPhone ph = parsePhone(new PBPhone().phone(pMap.get("phone").toString()));
							ph.setUuid(ArgUtil.parseAsString(ph.getUuid(), UniqueID.generateString()));
							if (ArgUtil.is(ph))
								pbPhones.add(ph);
						}
						addInfoMap.put(entry.getKey(), pbPhones);
						break;
					case "title":
					case "Title":
						addInfoMap.put(entry.getKey(), entry.getValue());
						break;
					case "gender":
					case "Gender":
						addInfoMap.put(entry.getKey(), entry.getValue());
						break;
					case "dob":
					case "DOB":
						Set<PBDate> setPbDate = setDateFld(entry.getValue());
						addInfoMap.put(entry.getKey(), setPbDate);
						break;
					case "document":
					case "DOCUMENT":
						Set<PBDocument> setPbDoc = setDocument(entry.getValue());
						addInfoMap.put(entry.getKey(), setPbDoc);
						break;
					default:
						Object object = checkFieldType(entry.getKey(), entry.getValue());
						if (ArgUtil.isEmpty(object)) {
							LOGGER.debug("Json Util else  :" + JsonUtil.toJson(object) + "\t key-value :"
									+ entry.getKey() + "-" + JsonUtil.toJson(entry.getValue()));
						} else {
							if (object != null && object.toString().equalsIgnoreCase("date")) {
								addInfoMap.put(entry.getKey(), setDateFld(entry.getValue()));
							} else {
								addInfoMap.put(entry.getKey(), entry.getValue());
							}
						}
					}
				}

			}
			doc.setAdditionalInfo(addInfoMap);

			doc.setCreated(TimeStampIndex.now().by(auditDetailProvider.getAuditUser()));
			save(doc);

		}
		return doc;
	}

	public Object checkFieldType(String code, Object value) {
		Object objType = null;
		if (ArgUtil.is(code) && ArgUtil.is(value)) {
			Query qryQuery = new Query();
			qryQuery.addCriteria(Criteria.where("code").is(code).and("active").is(true));
			List<CustomerFieldMasterDoc> cmFieldDoc = cMongoTemplate.find(qryQuery, CustomerFieldMasterDoc.class);
			if (cmFieldDoc != null && !cmFieldDoc.isEmpty()) {
				Object fldType = cmFieldDoc.get(0).getType();
				if (ArgUtil.is(fldType)) {
					objType = ArgUtil.parseAsT(fldType, new String(), false);
				}
			}
			return objType;
		}
		return objType;
	}

	private String getFileName(String additionalInfo) {
		String fldCode = Arrays.stream(additionalInfo.split("\\.")).skip(1).findFirst().orElse(additionalInfo);
		return fldCode;
	}

	@SuppressWarnings("unchecked")
	public void updateProfile(CustomerProfileDoc req, CustomerProfileDoc doc) {
		ObjectMapper objectMapper = new ObjectMapper();

		if (ArgUtil.is(req.getCode())) {
			CustomerProfileDoc docCode = findProfileByCode(req.getCode());
			if (ArgUtil.is(docCode) && !docCode.getId().equalsIgnoreCase(doc.getId())) {
				ApiResponseUtil.throwInputException(new ApiFieldError().obzect("code").field("code")
						.codeKey("ValidCodeDuplicate").description(req.getCode() + " already exists"));
			} else {
				doc.setCode(ArgUtil.parseAsString(req.getCode(), doc.getCode()));
			}
		}

		doc.setRmCode(ArgUtil.parseAsString(req.getRmCode(), doc.getRmCode()));
		if (ArgUtil.is(req.getName())) {
			PBName pbName = new PBName();
			pbName.setFirstName(ArgUtil.parseAsString(req.getName().getFirstName(), doc.getName().getFirstName()));
			pbName.setLastName(ArgUtil.parseAsString(req.getName().getMiddleName(), doc.getName().getMiddleName()));
			pbName.setMiddleName(ArgUtil.parseAsString(req.getName().getLastName(), doc.getName().getLastName()));
			pbName.setFormattedName(
					ArgUtil.parseAsString(req.getName().getFormattedName(), doc.getName().getFormattedName()));
			pbName.fix();
			doc.setName(pbName);
		}

		if (req.getPhones() != null && !req.getPhones().isEmpty()) {
			Set<PBPhone> reqPhones = req.getPhones();
			// Convert the request objects to a Set of UUIDs (to identify which are new or
			// missing)
			Set<String> reqPhoneUuids = reqPhones.stream().map(PBPhone::getUuid).collect(Collectors.toSet());

			// Find phones in DB that are not in the request (to be deleted)
			if (doc.getPhones() != null) {
				List<PBPhone> phonesToDelete = doc.getPhones().stream()
						.filter(phone -> !reqPhoneUuids.contains(phone.getUuid())
								&& (phone.getPhone() == null || !phone.getPhone().contains("*")))
						.collect(Collectors.toList());

				doc.getPhones().removeAll(phonesToDelete);

			}

			for (PBPhone reqph : reqPhones) {
				Optional<PBPhone> found = Optional.empty();
				String uuid = reqph.getUuid();
				String ph = reqph.getPhone();
				if (ph != null && !ph.contains("*")) {
					CustomerProfileDoc dupPh = findProfileByPhone(ph);
					if (ArgUtil.is(dupPh) && !dupPh.getId().equalsIgnoreCase(doc.getId())) {
						ApiResponseUtil.throwInputException(new ApiFieldError().obzect("phone").field("phone")
								.codeKey("ValidPhoneDuplicate").description(ph + " already exists"));
					}
					if (doc.getPhones() != null) {
						found = doc.getPhones().stream().filter(phone -> phone.getUuid().equals(uuid)).findFirst();
					}
					if (found.isPresent() && !ph.contains("*")) {
						String upPh = reqph.getPhone();
						PBPhone phu = parsePhone(new PBPhone().phone(upPh));
						phu.setUuid(reqph.getUuid());
						found.get().update(phu);
					} else {
						PBPhone pb = parsePhone(reqph);
						pb.setUuid(ArgUtil.parseAsString(pb.getUuid(), UniqueID.generateString()));
						if (doc.getPhones() != null) {
							doc.getPhones().add(pb);
						} else {
							Set<PBPhone> phs = new HashSet<>();
							phs.add(pb);
							doc.setPhones(phs);
						}
					}
				}
			}
		}
		if (req.getEmails() != null && !req.getEmails().isEmpty()) {
			Set<PBEmail> reqPbEmails = req.getEmails();
			// Convert the request objects to a Set of UUIDs (to identify which are new or
			// missing)
			Set<String> reqEmailUuids = reqPbEmails.stream().map(PBEmail::getUuid).collect(Collectors.toSet());

			List<PBEmail> eMailToDelete = new ArrayList<>();
			if (doc.getEmails() != null) {
				eMailToDelete = doc.getEmails().stream().filter(email -> !reqEmailUuids.contains(email.getUuid()))
						.collect(Collectors.toList());
				doc.getEmails().removeAll(eMailToDelete);
			}
			for (PBEmail reqEm : reqPbEmails) {
				Optional<PBEmail> found = Optional.empty();
				String uuid = reqEm.getUuid();
				String em = reqEm.getEmail();
				if (em != null && !em.contains("*")) {
					CustomerProfileDoc dupEm = findProfileByEmail(em);
					if (ArgUtil.is(dupEm) && !dupEm.getId().equalsIgnoreCase(doc.getId())) {
						ApiResponseUtil.throwInputException(new ApiFieldError().obzect("email").field("Email")
								.codeKey("ValidEmailDuplicate").description(em + " already exists"));
					}
					if (doc.getEmails() != null) {
						found = doc.getEmails().stream().filter(email -> email.getUuid().equals(uuid)).findFirst();
					}
					if (found.isPresent()) {
						found.get().update(reqEm);
					} else {
						PBEmail pbEm = new PBEmail();
						pbEm.setUuid(ArgUtil.parseAsString(pbEm.getUuid(), UniqueID.generateString()));
						pbEm.update(reqEm);
						if (doc.getEmails() != null) {
							doc.getEmails().add(pbEm);
						} else {
							Set<PBEmail> emails = new HashSet<>();
							emails.add(pbEm);
							doc.setEmails(emails);
						}

					}
				}
			}

		}

		Map<String, Object> addInfoMap = req.getAdditionalInfo();
		if (ArgUtil.is(addInfoMap)) {
			for (Map.Entry<String, Object> entry : addInfoMap.entrySet()) {
				switch (entry.getKey()) {
				case "title":
				case "Title":
					addInfoMap.put(entry.getKey(), entry.getValue());
					break;
				case "gender":
				case "Gender":
					addInfoMap.put(entry.getKey(), entry.getValue());
					break;
				case "dob":
				case "DOB":
					List<PBDate> pbDate = objectMapper.convertValue(entry.getValue(),
							new TypeReference<List<PBDate>>() {
							});
					Set<PBDate> spbDate = addUpdateDate(pbDate, doc, entry.getKey().toString());
					addInfoMap.put(entry.getKey(), spbDate);

					break;
				case "emails":
				case "alt_emails":
					List<PBEmail> pbEmails = objectMapper.convertValue(entry.getValue(),
							new TypeReference<List<PBEmail>>() {
							});
					Set<PBEmail> spbmails = addUpdateEmail(pbEmails, doc, entry.getKey().toString());
					addInfoMap.put(entry.getKey(), spbmails);
					break;
				case "phone":
				case "alt_phones":
					List<PBPhone> pbPhones = objectMapper.convertValue(entry.getValue(),
							new TypeReference<List<PBPhone>>() {
							});

					Set<PBPhone> spbPhone = addUpdatePhone(pbPhones, doc, entry.getKey().toString());
					LOGGER.info("spbPhone...., " + JsonUtil.toJson(spbPhone));
					addInfoMap.put(entry.getKey(), spbPhone);
					break;

				case "document":
				case "DOCUMENT":
					List<PBDocument> pbDocument = objectMapper.convertValue(entry.getValue(),
							new TypeReference<List<PBDocument>>() {
							});
					Set<PBDocument> spbDocument = addUpdateDocument(pbDocument, doc, entry.getKey().toString());
					addInfoMap.put(entry.getKey(), spbDocument);
					break;

				default:
					Object object = checkFieldType(entry.getKey(), entry.getValue());
					if (ArgUtil.isEmpty(object)) {
						LOGGER.info("Json Util else  :" + JsonUtil.toJson(object) + "\t key-value :" + entry.getKey()
								+ "-" + JsonUtil.toJson(entry.getValue()));
					} else {
						if (ArgUtil.is(entry.getValue())) {
							if (ArgUtil.isNotEmpty(object) && object.toString().equalsIgnoreCase("date")) {
								ObjectMapper objectMapperDt = new ObjectMapper();
								List<PBDate> pbDateU = objectMapperDt.convertValue(entry.getValue(),
										new TypeReference<List<PBDate>>() {
										});
								Set<PBDate> spbDateU = addUpdateDate(pbDateU, doc, entry.getKey().toString());
								if (ArgUtil.is(spbDateU)) {
									addInfoMap.put(entry.getKey(), spbDateU);
								}
							} else {
								addInfoMap.put(entry.getKey(), entry.getValue());
							}
						}

					}
				}
			}

		}
		doc.setAdditionalInfo(addInfoMap);

		doc.setUpdated(TimeStampIndex.now().by(auditDetailProvider.getAuditUser()));
		save(doc);
	}

	private Set<PBPhone> addUpdatePhone(List<PBPhone> reqPhones, CustomerProfileDoc doc, String entryKey) {
		Set<PBPhone> setPbPhone = new TreeSet<>();
		ObjectMapper objectMapper = new ObjectMapper();
		// Convert the request objects to a Set of UUIDs (to identify which are new or
		// missing)
		Set<String> reqPhoneUuids = reqPhones.stream().map(PBPhone::getUuid).collect(Collectors.toSet());

		// Find phones in DB that are not in the request (to be deleted)
		List<PBPhone> pbPhoneDbList = objectMapper.convertValue(doc.getAdditionalInfo().get(entryKey),
				new TypeReference<List<PBPhone>>() {
				});

		if (ArgUtil.isNotEmpty(pbPhoneDbList)) {
			List<PBPhone> phonesToDelete = pbPhoneDbList.stream()
					.filter(phone -> !reqPhoneUuids.contains(phone.getUuid())
							&& (phone.getPhone() == null || !phone.getPhone().contains("*")))
					.collect(Collectors.toList());

			// Remove only non-masked phones
			pbPhoneDbList.removeAll(phonesToDelete);
		}

		for (PBPhone reqph : reqPhones) {
			Optional<PBPhone> found = Optional.empty();
			String uuid = reqph.getUuid();
			String phoneN = reqph.getPhone();
			if (ArgUtil.isNotEmpty(pbPhoneDbList)) {
				found = pbPhoneDbList.stream().filter(phone -> phone.getUuid().equals(uuid)).findFirst();
			}

			if (found.isPresent() && !phoneN.contains("*")) {
				String upPh = reqph.getPhone();
				PBPhone phu = parsePhone(new PBPhone().phone(upPh));
				phu.setUuid(reqph.getUuid());
				found.get().update(phu);
				setPbPhone.add(found.get()); // Add the updated phone
			} else if (found.isPresent()) {
				setPbPhone.add(found.get());// add existing phone
			} else {
				PBPhone pb = parsePhone(reqph);
				pb.setUuid(ArgUtil.parseAsString(pb.getUuid(), UniqueID.generateString()));
				setPbPhone.add(pb); // Add the new phone
			}
		}
		return setPbPhone;
	}

	private Set<PBEmail> addUpdateEmail(List<PBEmail> reqEmail, CustomerProfileDoc doc, String entryKey) {
		Set<PBEmail> setPbEmail = new TreeSet<>();
		ObjectMapper objectMapper = new ObjectMapper();
		// Convert the request objects to a Set of UUIDs (to identify which are new or
		// missing)
		Set<String> reqEmailUuids = reqEmail.stream().map(PBEmail::getUuid).collect(Collectors.toSet());

		// Find phones in DB that are not in the request (to be deleted)
		List<PBEmail> pbEmailDbList = objectMapper.convertValue(doc.getAdditionalInfo().get(entryKey),
				new TypeReference<List<PBEmail>>() {
				});

		if (ArgUtil.isNotEmpty(pbEmailDbList)) {
			List<PBEmail> eMailToDelete = pbEmailDbList.stream()
					.filter(email -> !reqEmailUuids.contains(email.getUuid())
							&& (email.getEmail() == null || !email.getEmail().contains("*")))
					.collect(Collectors.toList());

			// Remove only non-masked emails
			pbEmailDbList.removeAll(eMailToDelete);
		}
		for (PBEmail reqEm : reqEmail) {
			Optional<PBEmail> found = Optional.empty();
			String uuid = reqEm.getUuid();
			String emai = reqEm.getEmail();

			if (ArgUtil.isNotEmpty(pbEmailDbList)) {
				found = pbEmailDbList.stream().filter(email -> email.getUuid().equals(uuid)).findFirst();
			}

			if (found.isPresent() && !emai.contains("*")) {
				found.get().update(reqEm);
				setPbEmail.add(found.get()); // Add the updated phone
			} else if (found.isPresent()) {
				setPbEmail.add(found.get()); // existing record
			} else {
				PBEmail pbEm = new PBEmail();
				pbEm.setUuid(ArgUtil.parseAsString(pbEm.getUuid(), UniqueID.generateString()));
				pbEm.update(reqEm);
				setPbEmail.add(pbEm);
			}

		}
		return setPbEmail;
	}

	private Set<PBDocument> addUpdateDocument(List<PBDocument> reqDoc, CustomerProfileDoc doc, String entryKey) {
		Set<PBDocument> setPbDoc = new TreeSet<>();
		ObjectMapper objectMapper = new ObjectMapper();
		// Convert the request objects to a Set of UUIDs (to identify which are new or
		// missing)
		Set<String> reqDocUuids = reqDoc.stream().map(PBDocument::getUuid).collect(Collectors.toSet());

		// Find phones in DB that are not in the request (to be deleted)
		List<PBDocument> pbDocDbList = objectMapper.convertValue(doc.getAdditionalInfo().get(entryKey),
				new TypeReference<List<PBDocument>>() {
				});

		if (ArgUtil.isNotEmpty(pbDocDbList)) {
			List<PBDocument> eDocToDelete = pbDocDbList.stream()
					.filter(document -> !reqDocUuids.contains(document.getUuid())).collect(Collectors.toList());
			// Remove phones that are not in the request from the document
			pbDocDbList.removeAll(eDocToDelete);
		}

		for (PBDocument reqDoct : reqDoc) {
			Optional<PBDocument> found = Optional.empty();
			String uuid = reqDoct.getUuid();
			if (ArgUtil.isNotEmpty(pbDocDbList)) {
				found = pbDocDbList.stream().filter(doct -> doct.getUuid().equals(uuid)).findFirst();
			}

			if (found.isPresent()) {
				found.get().update(reqDoct);
				setPbDoc.add(found.get()); // Add the updated phone
			} else {
				PBDocument pbDoc = new PBDocument();
				pbDoc.setUuid(ArgUtil.parseAsString(pbDoc.getUuid(), UniqueID.generateString()));
				pbDoc.update(reqDoct);
				setPbDoc.add(pbDoc);
			}

		}
		return setPbDoc;
	}

	// Generic method to convert a string into a list of a specific type (String or
	// Integer)
	public static <T> List<T> convertStringToList(String input, String delimiter, Function<String, T> converter) {
		return Arrays.stream(input.split(delimiter)).map(converter).collect(Collectors.toList());
	}

	public long getDateWithTSM(String dateString) {
		long timestamp = 0;

		try {
			// Define the date format
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

			// Parse the input date string into a LocalDate
			LocalDate localDate = LocalDate.parse(dateString, formatter);

			// Get the timezone string from your setup
			String tz = environment.domainConfig().getTimeZoneFromSetup();

			// Parse the timezone and handle invalid formats
			ZoneId zoneId = parseTimeZone(tz);

			// Combine LocalDate with ZoneId to get ZonedDateTime
			ZonedDateTime zonedDateTime = localDate.atStartOfDay(zoneId);

			// Convert ZonedDateTime to epoch milliseconds
			timestamp = zonedDateTime.toInstant().toEpochMilli();

			return timestamp;
		} catch (Exception e) {
			if (ArgUtil.is(dateString)) {
				timestamp = Long.parseLong(dateString);
			}
		}
		return timestamp;
	}

	private ZoneId parseTimeZone(String tz) {
		// Extract the region part before "::"
		if (tz.contains("::")) {
			tz = tz.split("::")[0];
		}
		try {
			return ZoneId.of(tz); // Valid region-based ZoneId
		} catch (DateTimeException e) {
			e.printStackTrace();
			return ZoneId.of("Asia/Kolkata"); // Fallback to default
		}
	}

	private Set<PBDate> addUpdateDate(List<PBDate> reqDate, CustomerProfileDoc doc, String entryKey) {
		try {
			long ts = 0l;

			Set<PBDate> setPbDate = new TreeSet<>();
			ObjectMapper objectMapper = new ObjectMapper();
			// Convert the request objects to a Set of UUIDs (to identify which are new or
			// missing)
			Set<String> reqDateUuids = reqDate.stream().map(PBDate::getUuid).collect(Collectors.toSet());

			// Find phones in DB that are not in the request (to be deleted)
			List<PBDate> pbDateDbList = objectMapper.convertValue(doc.getAdditionalInfo().get(entryKey),
					new TypeReference<List<PBDate>>() {
					});
			if (ArgUtil.isNotEmpty(pbDateDbList)) {
				List<PBDate> dateToDelete = pbDateDbList.stream().filter(date -> !reqDateUuids.contains(date.getUuid()))
						.collect(Collectors.toList());
				// Remove phones that are not in the request from the document
				pbDateDbList.removeAll(dateToDelete);
			}
			for (PBDate reqDt : reqDate) {
				Optional<PBDate> found = Optional.empty();
				String uuid = reqDt.getUuid();
				if (ArgUtil.isNotEmpty(pbDateDbList)) {
					found = pbDateDbList.stream().filter(date -> date.getUuid().equals(uuid)).findFirst();
				}

				if (found.isPresent()) {
					PBDate pbDate = found.get().update(reqDt);
					ts = getDateWithTSM(pbDate.getDate());
					pbDate.setStamp(ts);
					pbDate.setStampLocal(DateUtil.parseDate(pbDate.getDate()).getTime());
					pbDate.setTimeZone(environment.domainConfig().getTimeZoneFromSetup());
					setPbDate.add(pbDate);
				} else {
					PBDate pbDate = new PBDate();
					pbDate.setUuid(ArgUtil.parseAsString(pbDate.getUuid(), UniqueID.generateString()));
					pbDate.setDate(reqDt.getDate());
					ts = getDateWithTSM(pbDate.getDate());
					pbDate.setStamp(ts);
					pbDate.setStampLocal(DateUtil.parseDate(pbDate.getDate()).getTime());
					pbDate.setTimeZone(environment.domainConfig().getTimeZoneFromSetup());
					setPbDate.add(pbDate);
				}

			}
			return setPbDate;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private Set<PBDate> setDateFld(Object value) {
		Set<PBDate> setPbDate = new TreeSet<PBDate>();
		List<Object> pbDate = (List<Object>) value;
		long ts = 0l;

		for (Object obj : pbDate) {
			Map<String, Object> pMap = JsonUtil.toJsonMap(obj);
			PBDate pbD = new PBDate();
			for (Map.Entry<String, Object> eMapdate : pMap.entrySet()) {
				if (eMapdate.getKey().contains("dob") || eMapdate.getKey().contains("date")) {
					pbD.setDate((ArgUtil.parseAsString(eMapdate.getValue(), Constants.BLANK)));
				}
			}
			pbD.setUuid(UniqueID.generateString());
			ts = getDateWithTSM(pbD.getDate());
			pbD.setStamp(ts);
			pbD.setStampLocal(DateUtil.parseDate(pbD.getDate()).getTime());
			pbD.setTimeZone(environment.domainConfig().getTimeZoneFromSetup());
			setPbDate.add(pbD);

		}
		return setPbDate;
	}

	private Set<PBDocument> setDocument(Object value) {
		Set<PBDocument> setPbDoc = new TreeSet<PBDocument>();
		List<Object> pbDoc = (List<Object>) value;
		for (Object obj : pbDoc) {
			PBDocument pbD = new PBDocument();
			String json = JsonUtil.toJson(obj);
			ObjectMapper mapper = new ObjectMapper();
			try {
				pbD = mapper.readValue(json, PBDocument.class);
				pbD.setUuid(UniqueID.generateString());

			} catch (JsonParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JsonMappingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// PBDocument p=(PBDocument)obj;
			Map<String, Object> pMap = JsonUtil.toJsonMap(obj);

			setPbDoc.add(pbD);
		}
		return setPbDoc;
	}

	public List<String> deDuplicateCheck(CustomerProfileDoc req) {
		List<String> duplicateLst = new ArrayList();
		if (ArgUtil.is(req.getCode())) {
			CustomerProfileDoc docCode = findProfileByCode(req.getCode());
			if (ArgUtil.is(docCode) && !docCode.getId().equalsIgnoreCase(req.getId())) {
				duplicateLst.add("Code " + req.getCode() + " already exists");
			}
		}
		if (req.getEmails() != null && !req.getEmails().isEmpty()) {
			Set<PBEmail> reqEmails = req.getEmails();

			for (PBEmail pbEmail : reqEmails) {
				if (pbEmail.getEmail() != null && !pbEmail.getEmail().contains("*")) {
					CustomerProfileDoc dupEm = findProfileByEmail(pbEmail.getEmail());
					if (ArgUtil.is(dupEm) && !dupEm.getId().equalsIgnoreCase(req.getId())) {
						duplicateLst.add("Email " + pbEmail.getEmail() + " already exists");
					}
				}
			}
		}
		if (req.getPhones() != null && !req.getPhones().isEmpty()) {
			Set<PBPhone> reqPhones = req.getPhones();
			for (PBPhone phone : reqPhones) {
				if (phone.getPhone() != null && !phone.getPhone().contains("*")) {
					CustomerProfileDoc ph = findProfileByPhone(phone.getPhone());
					if (ArgUtil.is(ph) && !ph.getId().equalsIgnoreCase(req.getId())) {
						duplicateLst.add("Phone " + phone.getPhone() + " already exists");
					}
				}
			}
		}

		return duplicateLst;
	}

	@Async
	public void saveMaster(ChatContactDoc contactDoc) {

		if (!ArgUtil.is(contactDoc)) {
			return;
		}

		PBContactDoc pbContactDoc = new PBContactDoc();

		ContactType contactType = contactDoc.type();;

		pbContactDoc.setType(contactType);
		String name = ArgUtil.anyOf(contactDoc.getName(), contactDoc.info().getName());
		pbContactDoc.setName(PBName.parse(name));
		if (ArgUtil.is(contactType, ContactType.WHATSAPP, ContactType.SMS, ContactType.RCS, ContactType.TELEGRAM)) {

			PBPhone pbPhone = parsePhone(new PBPhone().phone(contactDoc.getPhone()));
			pbPhone.setPhone(contactDoc.getPhone());
			pbPhone.setVerified(ArgUtil.parseAsBoolean(contactDoc.getPhoneVerified()));

			pbContactDoc.setPhone(pbPhone);
			pbContactDoc.setCsid(contactDoc.getPhone());
		} else if (ArgUtil.is(contactType, ContactType.EMAIL)) {

			PBEmail pbEmail = new PBEmail().email(contactDoc.getEmail());
			pbEmail.setVerified(ArgUtil.parseAsBoolean(contactDoc.getEmailVerified()));

			pbContactDoc.setEmail(pbEmail);
			pbContactDoc.setCsid(contactDoc.getEmail());
		} else {
			return;
		}

		pbContactDoc.setContactId(pbContactDoc.toString());
		pbContactDoc.setChangestamp(System.currentTimeMillis());
		AppContextUtil.clear();
		AppContextUtil.setTenant(Tenants.getDefault());
		AppContextUtil.init();

		tenantDefault().save(pbContactDoc);
	}

}
