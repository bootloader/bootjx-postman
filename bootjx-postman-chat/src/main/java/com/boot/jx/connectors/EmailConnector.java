package com.boot.jx.connectors;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.URLDataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.mail.util.MimeMessageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boot.jx.dict.ContactType;
import com.boot.jx.email.EmailReplyParser;
import com.boot.jx.exception.AmxApiException;
import com.boot.jx.model.CommonFile;
import com.boot.jx.model.CommonFileStream;
import com.boot.jx.postman.PMConstants.CHANNEL_TYPE;
import com.boot.jx.postman.PMEnvironment.PMClientConfig;
import com.boot.jx.postman.client.PMFileStoreClient;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.CustomerProfileDoc;
import com.boot.jx.postman.doc.config.ChannelConfigLogger;
import com.boot.jx.postman.dto.ChatMessageDTO;
import com.boot.jx.postman.model.Attachment;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.Message;
import com.boot.jx.postman.model.Message.Status;
import com.boot.jx.postman.model.MessageBoxEvent;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.outbound.OutBoundMsgBasic.ApiOutBoundMsg;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ConnectorMapping;
import com.boot.jx.postman.plugin.EmailPlugin;
import com.boot.jx.postman.plugin.EmailPlugin.EmailConfigDetails;
import com.boot.jx.postman.query.ChatContactQuery;
import com.boot.jx.postman.wa360.WA360Constants;
import com.boot.jx.rest.RestService;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.CollectionUtil;
import com.boot.utils.URLBuilder;
import com.boot.utils.Urly;

@Component
@ConnectorMapping(contactType = ContactType.EMAIL, channel = CHANNEL_TYPE.EMAIL)
public class EmailConnector extends AbstractConnector<EmailConfigDetails, EmailPlugin> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EmailConnector.class);

	@Autowired
	private RestService restService;

	@Autowired
	private PMClientConfig pmClientConfig;

	@Autowired
	private PMFileStoreClient pmFileStoreClient;

	@Override
	public void onChannelUpdate(ChannelConfig channelConfig, ChannelConfigLogger channelConfigLogger) {
		String webhookUrl = pmClientConfig.getWebhookUrl(channelConfig, null, null);
		restService.ajax(WA360Constants.BASE_URL).path("v1/configs/webhook")
				.header(WA360Constants.D360_API_KEY, channelConfig.getWa360d().getApiKey())
				.post(MapModel.createInstance().put("url", webhookUrl).toMap()).asMap();
	}

	public Session getMailSession(ChannelConfig channel) {

		EmailConfigDetails emailConfig = channel.getEmail();

		if (!ArgUtil.is(emailConfig)) {
			LOGGER.warn("No Email Config found {}", channel.getChannelId());
			return null;
		}

		Properties properties = new Properties();
		properties.put("mail.store.protocol", ArgUtil.nonEmpty(emailConfig.getProtocol(), "pop3"));
		properties.put("mail.pop3s.host", emailConfig.getPop3Host());
		properties.put("mail.pop3s.port", ArgUtil.nonEmpty(emailConfig.getPop3Port(), "995"));
		properties.put("mail.pop3.starttls.enable", emailConfig.isPop3StartTls());
		properties.put("mail.smtp.auth", emailConfig.isSmtpAuth());
		properties.put("mail.smtp.starttls.enable", emailConfig.isSmtpStartTls());
		properties.put("mail.smtp.host", emailConfig.getSmtpHost());
		properties.put("mail.smtp.port", emailConfig.getSmtpPort());
		return Session.getDefaultInstance(properties);
	}

	@Override
	public CustomerProfileDoc findProfile(ChatContactDoc chatContactDoc) {
		return contactStore.findProfileByEmail(chatContactDoc.getEmail());
	}

	public OutboxMessage initSession(ChatSessionDoc session, InboxMessage inboxMessage) {
		ChatContactQuery contactQuery = messageContext.contact();
		ChatContactDoc chatContactDoc = messageContext.contact().getDoc();
		if (chatContactDoc.getEmail() == null) {
			contactQuery.setEmail(inboxMessage.contact().getEmail());
		}
		if (!chatContactDoc.getEmailVerified()) {
			contactQuery.setEmailVerified(true);
		}
		return null;
	}

	public InboxMessage toInboxMessage(ChannelConfig channelConfig, MimeMessageParser email) throws Exception {

		// Create Default Message from Channel
		InboxMessage inboxMessage = this.createInboxMessage(channelConfig);
		// Set Contact info
		InternetAddress from = null;
		try {
			from = CollectionUtil
					.first(InternetAddress.parse(InternetAddress.toString(email.getMimeMessage().getFrom())));
		} catch (MessagingException e) {
			e.printStackTrace();
		}

		if (!ArgUtil.is(from)) {
			return null;
		}

		inboxMessage.contact().setCsid(from.getAddress());
		inboxMessage.contact().setName(from.getPersonal());
		inboxMessage.contact().setEmail(from.getAddress());

		// Set Additional info
		inboxMessage.setFrom(from.getAddress());
		inboxMessage.setFromName(from.getPersonal());
		inboxMessage.to().add(channelConfig.getLane());

		// Extract Message Details
		inboxMessage.setMessageIdExt(CollectionUtil.first(email.getMimeMessage().getHeader("Message-ID")));
		inboxMessage.setReplyIdExt(CollectionUtil.first(email.getMimeMessage().getHeader("In-Reply-To")));
		inboxMessage.setSubject(email.getSubject());
		inboxMessage.setMessage(EmailReplyParser.parseReply(email.getPlainContent()));

		if (ArgUtil.is(inboxMessage.getSubject())) {
			inboxMessage.session().setTicketHash(PostManUtil.createTicketHash(inboxMessage));
		}

		List<DataSource> attc = email.getAttachmentList();
		for (DataSource dataSource : attc) {
			CommonFileStream srcFile = new CommonFileStream().from(dataSource);
			CommonFile dstFile = pmFileStoreClient.uploadSessionFileAsync(srcFile,
					PostManUtil.createContactId(inboxMessage), inboxMessage.getMessageIdExt());
			inboxMessage.attachment(new Attachment().mediaURL(dstFile.getUrl()).mediaType(dstFile.getFileType())
					.mediaSrc(srcFile.getUrl()).mediaName(srcFile.getName()).mediaMimeType(srcFile.getContentType()));
		}

		return inboxMessage;
	}

	@Override
	public Status onSend(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
			ApiOutBoundMsg outBoundMsg) {
		try {
			Session session = getMailSession(channelConfig);

			if (!ArgUtil.is(session)) {
				outboxMessage.logs().add(String.format("No Session Created for %s", chatContactDoc));
				outboxMessage.updateStatus(OutboxMessage.Status.NSENT);
				return Message.Status.NSENT;
			}

			ChatSessionDoc chatSession = ArgUtil.is(context().session()) ? context().session().getDoc() : null;

			if (!ArgUtil.is(outboxMessage.getReplyIdExt())) {
				if (ArgUtil.is(chatSession)) {
					ChatMessageDTO lastMsg = chatSession.lastMsg();
					if (ArgUtil.is(lastMsg)) {
						outboxMessage.setReplyIdExt(lastMsg.getMessageIdExt());
					}
				}
			}

			if (!ArgUtil.is(outboxMessage.getSubject())) {
				if (ArgUtil.is(chatSession)) {
					outboxMessage.setSubject(chatSession.getSubject());
				}
			}

			MimeMessage replyMessage = new MimeMessage(session);
			replyMessage.setFrom(new InternetAddress(channelConfig.getEmail().getSmtpUser(), channelConfig.getName()));
			replyMessage.addRecipient(RecipientType.TO, new InternetAddress(outboxMessage.contact().getCsid()));
			replyMessage.setSubject(ArgUtil.nonEmpty(outboxMessage.getSubject(), channelConfig.getName()));
			replyMessage.addHeader("In-Reply-To", outboxMessage.getReplyIdExt());

			// Create a multipar message
			Multipart multipart = new MimeMultipart();

			BodyPart messageBodyPart;
			// Create the message part
			if (ArgUtil.is(outboxMessage.getMessage())) {
				messageBodyPart = new MimeBodyPart();
				// messageBodyPart.setText(outboxMessage.getMessage());
				messageBodyPart.setContent(outboxMessage.getMessage(), "text/html; charset=utf-8");
				multipart.addBodyPart(messageBodyPart);
			}

			if (ArgUtil.is(outboxMessage.getAttachments())) {
				int i = 0;
				for (Attachment attch : outboxMessage.getAttachments()) {
					messageBodyPart = new MimeBodyPart();
					URLBuilder ub = Urly.parse(attch.getMediaURL()).protocol("https");
					// URL url = new URL(java.net.URLEncoder.encode(attch.getMediaURL(), "UTF-8"));
					URL url = new URL(ub.getURL());
					URLDataSource source = new URLDataSource(url);
					messageBodyPart.setDataHandler(new DataHandler(source));
					messageBodyPart.setHeader("Content-ID", "attachment_" + i);
					messageBodyPart.setDisposition(MimeBodyPart.INLINE);
					messageBodyPart.setFileName(
							ArgUtil.nonEmpty(attch.getMediaName(), attch.getMediaCaption(), "ATTACHMENT_" + i));
					multipart.addBodyPart(messageBodyPart);
					i++;
				}
			}
			// Send the complete message parts
			replyMessage.setContent(multipart);
			Transport t = session.getTransport("smtp");
			try {
				// connect to the smpt server using transport instance
				// change the user and password accordingly
				t.connect(channelConfig.getEmail().getSmtpUser(), channelConfig.getEmail().getSmtpPass());
				t.sendMessage(replyMessage, replyMessage.getAllRecipients());

				outboxMessage.setMessageIdExt(CollectionUtil.first(replyMessage.getHeader("Message-ID")));

			} finally {
				t.close();
			}
			return Message.Status.SENT;

		} catch (AmxApiException | MessagingException | MalformedURLException | UnsupportedEncodingException
				| URISyntaxException e) {
			onException(channelConfig, chatContactDoc, outboxMessage, e);
			return Message.Status.SENT_EXC;
		}
	}

	@Override
	public MessageBoxEvent inboundMessageBoxEvent(ChannelConfig channelConfig, MapModel requestMap,
			MessageBoxEvent messageBoxEvent) {
		return messageBoxEvent;
	}

}
