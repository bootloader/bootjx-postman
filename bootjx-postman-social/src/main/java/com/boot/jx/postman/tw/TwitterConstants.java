package com.boot.jx.postman.tw;

import com.boot.model.MapModel;
import com.boot.utils.JsonPath;

public class TwitterConstants {

    public static class InBoundWrapperPaths {

    }

    public static class OutBoundPaths {
	public static final JsonPath EVENT_TYPE = new JsonPath("event/type");
	public static final JsonPath DM_TEXT = new JsonPath("event/message_create/message_data/text");
	public static final JsonPath DM_RECIPIENT = new JsonPath("event/message_create/target/recipient_id");

	public static final JsonPath QUICK_REPLY_TYPE = new JsonPath(
		"event/message_create/message_data/quick_reply/type");
	public static final JsonPath QUICK_REPLY_OPTIONS = new JsonPath(
		"event/message_create/message_data/quick_reply/options");
	public static final JsonPath MESSAGE_CTA = new JsonPath("event/message_create/message_data/ctas");

	public static final JsonPath ATTACHMENT_TYPE = new JsonPath(
		"event/message_create/message_data/attachment/type");
	public static final JsonPath ATTACHMENT_MEDIA = new JsonPath(
		"event/message_create/message_data/attachment/media/id");

	public static final JsonPath DM_MESSAGE_ID = new JsonPath("event/id");

    }

    public static final String[] componentTypes = { "header", "body", "button" };
    public static final String[] componentButtonSubTypes = { "quick_reply", "url" };
    public static final String[] componentParameterTypes = { // parameters types
	    "text", // for component:body and component:button
	    "currency", "date_time", // for component:body
	    "payload" // only for component:button Used for sub_type = "quick_reply
    };

    public static final String BASE_URL = "https://api.twitter.com/1.1";

    public static String API_V1(String path) {
	return BASE_URL + path;
    }

    public static class TmplComponent extends MapModel {
	MapModel parameters;

	public TmplComponent create(String type) {
	    this.put("type", type);
	    this.parameters = MapModel.createInstance();
	    return this;
	}

	public TmplComponent body() {
	    return this.create("body");
	}

	public TmplComponent header() {
	    return this.create("header");
	}

	public TmplComponent button(String subType, int index) {
	    this.put("type", "button");
	    this.parameters = MapModel.createInstance();
	    return this;
	}

	public TmplComponent parameter(String type, Object value) {
	    this.parameters.add(MapModel.createInstance().put("type", type).put(type, value));
	    return this;
	}

	public static TmplComponent createInstance() {
	    return new TmplComponent();
	}

	public TmplComponent build() {
	    this.put("parameters", parameters.list());
	    return this;
	}

    }
}
