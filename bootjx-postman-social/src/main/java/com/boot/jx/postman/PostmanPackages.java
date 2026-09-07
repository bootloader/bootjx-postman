package com.boot.jx.postman;

import java.io.IOException;

import com.boot.jx.model.CommonFile;
import com.boot.jx.model.CommonFileStream;
import com.boot.jx.model.CommonTemplateMeta;
import com.boot.jx.postman.model.ITemplates.BasicTemplate;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.model.MapModel;

public class PostmanPackages {

	public static interface ICommonTmplPackage {
		// public CommonFile process(CommonFile file, ContactType contactType);

		public CommonFile process(CommonFile file, Contactable contactMeta);

		public String process(String templateContent, Object contact);

		public CommonFile process(CommonFile file, BasicTemplate basicTemplate);
	}

	public static interface TemplateResolver {
		public BasicTemplate get(String templateId);

		public BasicTemplate get(CommonTemplateMeta template);

		public BasicTemplate get(CommonTemplateMeta template, Contactable contactable, MapModel model);

		// public BasicTemplate get(CommonTemplateMeta template, ContactType
		// contactType);

	}

	public static interface MessageClient {
		public OutboxMessage send(ChannelConfig channelConfig, OutboxMessage outboxMessage);
	}

	public interface MockMessenger {

		public void onRead(MapModel req);

		public void onDeleiver(MapModel req);

		boolean isMockNumber(String number);

		MapModel send(MapModel req, ChannelConfig channelConfig);

		void onSend(MapModel req);

	}
	
	/**
	 * Optional send-time hooks for skipping Meta or recording API error codes on the outbox.
	 */
	public interface MessageFailureGuard {

		/**
		 * Decides whether this outbound send should hit Meta or replay a cached failure.
		 * For an armed marketing outbox with a registered error code, builds and returns a
		 * Meta-shaped error response body so the normal send error handling runs without
		 * an HTTP call. Returns {@code null} when no replay applies and Meta should be
		 * contacted.
		 */
		MapModel enforce(OutboxMessage outboxMessage);

		/**
		 * Attaches a Meta error code to the outbox when the send response is a failure.
		 * Stores only the transient {@code replayErrorCode} field so a later
		 * {@link com.boot.jx.postman.guard.MessageFailureGuard#capture} can remember the
		 * failure on the contact. Intentionally does not set {@code meta.replayErrorCode};
		 * that meta flag is reserved for an armed replay from {@code resolve}, not for a
		 * fresh API error.
		 */
		void escalate(OutboxMessage outboxMessage, String errorCode);

	}

	public static interface Text2Media {

		public String toImage(String text) throws IOException;

		public String toImage(String text, String style, String textId) throws IOException;

		public CommonFileStream toImageFile(String text, String style) throws IOException;

		public String toImageUrl(String text, String style, String textId) throws IOException;
	}

}
