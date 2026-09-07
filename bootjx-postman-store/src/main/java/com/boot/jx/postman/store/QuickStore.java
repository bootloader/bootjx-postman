package com.boot.jx.postman.store;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import com.boot.jx.mongo.CommonDocInterfaces.IDocument;
import com.boot.jx.mongo.CommonDocInterfaces.SimpleDocument;
import com.boot.jx.mongo.CommonMongoQueryBuilder;
import com.boot.jx.mongo.CommonMongoStore;
import com.boot.jx.postman.doc.QuickLocation;
import com.boot.jx.postman.doc.QuickMedia;
import com.boot.jx.postman.doc.ticket.CustomerTicketStatus;
import com.boot.model.UtilityModels.JsonIgnoreUnknown;
import com.boot.utils.ArgUtil;
import com.boot.utils.EntityDtoUtil;
import com.boot.utils.PatternUtil;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

@Component
public class QuickStore extends CommonMongoStore<QuickStore> {

	public static interface QuickGalleryItem extends JsonIgnoreUnknown, SimpleDocument {
		String getId();

		void setId(String id);

		String getCategory();

		String getCode();

		String getTitle();

		default boolean isReadonly() {
			return false;
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(QuickStore.class);

	@SuppressWarnings("unchecked")
	public <T extends QuickGalleryItem> List<T> groupByCategory(Class<T> clazz) {
		List<Document> list = new ArrayList<Document>();
		// Equivalent to $project
		DBObject projectFields = new BasicDBObject();
		projectFields.put("_id", 0);
		projectFields.put("category", "$_id");
		Document project = new Document("$project", projectFields);
		List<QuickMedia> other = new ArrayList<QuickMedia>();
		list.add(Aggregation.group("category").count().as("count").toDocument(Aggregation.DEFAULT_CONTEXT));
		list.add(project);
		try {
			MongoCollection<Document> col = mongoTemplate.getCollection(mongoTemplate.getCollectionName(clazz));
			MongoCursor<Document> cursor = col.aggregate(list).iterator();

			while (cursor.hasNext()) {
				Document doc = cursor.next();
				other.add(new QuickMedia().from(doc));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return (List<T>) other;
	}

	public <T extends QuickGalleryItem> List<T> findByCategory(String category, Class<T> clazz) {
		return find(CommonMongoQueryBuilder.collection(clazz)
				.where(Criteria.where("category").regex(PatternUtil.equalsIgnoreCase(category))));
	}

	public <T extends QuickGalleryItem> List<T> findGalleryItems(String codeIdOrTitle, Class<T> clazz) {

		List<Criteria> orList = new ArrayList<Criteria>();
		orList.add(Criteria.where("code").is(codeIdOrTitle));
		orList.add(Criteria.where("_id").is(codeIdOrTitle));
		if (codeIdOrTitle != null && ObjectId.isValid(codeIdOrTitle)) {
			orList.add(Criteria.where("id").is(new ObjectId(codeIdOrTitle)));
		}
		orList.add(Criteria.where("title").regex(PatternUtil.equalsIgnoreCase(codeIdOrTitle)));

		return find(CommonMongoQueryBuilder.collection(clazz)
				.where(new Criteria().orOperator(orList.toArray(new Criteria[orList.size()]))));
	}

	public <T extends QuickGalleryItem> T findByCode(String code, Class<T> clazz) {

		List<Criteria> orList = new ArrayList<Criteria>();
		orList.add(Criteria.where("code").is(code));
		orList.add(Criteria.where("_id").is(code));
		if (code != null && ObjectId.isValid(code)) {
			orList.add(Criteria.where("id").is(new ObjectId(code)));
		}

		return findOne(
				CommonMongoQueryBuilder.collection(clazz)
						.where(new Criteria().orOperator(orList.toArray(new Criteria[orList.size()]))).getQuery(),
				clazz);
	}

	public <T extends QuickGalleryItem> T createGalleryItem(T src, T dest) {
		QuickGalleryItem quickTag = findByIdOrDefault(src.getId(), new QuickLocation());
		String id = quickTag.getId();
		EntityDtoUtil.copyProperties(quickTag, src);
		quickTag.setId(id);
		saveAndAudit(quickTag, ArgUtil.is(quickTag.getId()));
		return dest;
	}

	public <T extends IDocument> T saveOnSubmit(T quickGalleryItem, Class<T> entityClass) {
		if (quickGalleryItem instanceof QuickGalleryItem) {
			try {
				// Create a new instance of entityClass safely
				T newInstance = entityClass.getDeclaredConstructor().newInstance();
				createGalleryItem((QuickGalleryItem) quickGalleryItem, (QuickGalleryItem) newInstance);
				return newInstance;
			} catch (InstantiationException | IllegalAccessException | NoSuchMethodException
					| InvocationTargetException e) {
				throw new RuntimeException("Failed to create a new instance of " + entityClass.getName(), e);
			}
		} else {
			save(quickGalleryItem);
		}
		return quickGalleryItem;
	}

	public void createTicketMeta() {
		save(CustomerTicketStatus.OPEN);
		save(CustomerTicketStatus.IN_PROGRESS);
		save(CustomerTicketStatus.RESOLVED);
		save(CustomerTicketStatus.CLOSED);
	}

}
