package com.boot.jx.postman.doc.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.api.ApiFieldError;
import com.boot.jx.api.ApiResponseUtil;
import com.boot.jx.exception.AmxApiError;
import com.boot.jx.exception.ApiHttpExceptions.ApiHttpException;
import com.boot.jx.exception.ApiHttpExceptions.ApiHttpServerException;
import com.boot.jx.mongo.CommonDocInterfaces.TimeStampIndex.TimeStampDoc;
import com.boot.jx.postman.PMEnvironment;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.fasterxml.jackson.annotation.JsonView;

@Document(collection = "TEMP_CONFIG_CHANNEL")
@TypeAlias("ChannelConfigTemp")
public class ChannelConfigLogger extends TimeStampDoc implements Serializable {

	private static final long serialVersionUID = -6368905475787041196L;

	public class ChannelConfigTempLog {
		private String api;
		private String message;
		private Object statusCode;
		private Map<String, Object> resp;
		private List<Object> trace;

		public String getApi() {
			return api;
		}

		public void setApi(String api) {
			this.api = api;
		}

		public Map<String, Object> getResp() {
			return resp;
		}

		public void setResp(Map<String, Object> resp) {
			this.resp = resp;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public List<Object> getTrace() {
			return trace;
		}

		public void setTrace(List<Object> trace) {
			this.trace = trace;
		}

		public Object getStatusCode() {
			return statusCode;
		}

		public void setStatusCode(Object statusCode) {
			this.statusCode = statusCode;
		}
	}

	@Id
	private String id;

	@Indexed
	private String domain;
	private String lane;
	@Indexed
	private String channelType;
	private String channelId;

	@JsonView(PMEnvironment.ProtectedProperty.class)
	protected String channelKey;

	private String channelConfigId;

	private Map<String, Object> resp;

	private List<ChannelConfigTempLog> logs;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getLane() {
		return lane;
	}

	public void setLane(String lane) {
		this.lane = lane;
	}

	public String getChannelType() {
		return channelType;
	}

	public void setChannelType(String channelType) {
		this.channelType = channelType;
	}

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public String getChannelKey() {
		return channelKey;
	}

	public void setChannelKey(String channelKey) {
		this.channelKey = channelKey;
	}

	public String getChannelConfigId() {
		return channelConfigId;
	}

	public void setChannelConfigId(String channelConfigId) {
		this.channelConfigId = channelConfigId;
	}

	public Map<String, Object> getResp() {
		return resp;
	}

	public void setResp(Map<String, Object> resp) {
		this.resp = resp;
	}

	public List<ChannelConfigTempLog> getLogs() {
		return logs;
	}

	public void setLogs(List<ChannelConfigTempLog> logs) {
		this.logs = logs;
	}

	public List<ChannelConfigTempLog> logs() {
		if (this.logs == null) {
			this.logs = new ArrayList<ChannelConfigTempLog>();
		}
		return this.logs;
	}

	public ChannelConfigLogger log(String api, Map<String, Object> resp) {
		ChannelConfigTempLog log = new ChannelConfigTempLog();
		log.setApi(api);
		log.setResp(resp);
		this.logs().add(log);
		return this;
	}

	public ChannelConfigLogger log(String api, Throwable e) {
		if (e == null) {
			return this;
		}
		ChannelConfigTempLog log = new ChannelConfigTempLog();
		log.setApi(api);
		log.setTrace(new ArrayList<Object>());

		log.getTrace().add(e.getMessage());

		StackTraceElement[] traces = e.getStackTrace();

		if (traces.length > 0 && traces[0].toString().length() > 0) {
			for (StackTraceElement trace : traces) {
				log.getTrace().add(trace.toString());
			}
		}

		if (e instanceof ApiHttpServerException || e instanceof ApiHttpException) {
			AmxApiError r = ((ApiHttpException) e).getResponse();
			log.setResp(MapModel.from(r.getBody()).toMap());
			log.setStatusCode(r.getRawStatusCode());
		}

		List<ApiFieldError> errors = ApiResponseUtil.getErrors();
		if (ArgUtil.is(errors)) {
			log.getTrace().add(errors);
		}

		this.logs().add(log);
		return this;
	}

}
