package com.boot.jx.connectors;

import java.io.IOException;
import java.util.Comparator;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;

import com.boot.jx.api.ApiResponseUtil;
import com.boot.jx.dict.ContactType;
import com.boot.jx.dict.FileType;
import com.boot.jx.model.CommonFile;
import com.boot.jx.model.CommonFileStream;
import com.boot.jx.postman.client.PMFileStoreClient;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.CustomerProfileDoc;
import com.boot.jx.postman.model.Attachment;
import com.boot.jx.postman.model.InboxMessage;
import com.boot.jx.postman.model.Message;
import com.boot.jx.postman.model.Message.Status;
import com.boot.jx.postman.model.MessageBoxEvent;
import com.boot.jx.postman.model.MessageDefinitions.Contactable;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.outbound.OutBoundMsgBasic.ApiOutBoundMsg;
import com.boot.jx.postman.plugin.ChannelConfig;
import com.boot.jx.postman.plugin.ChannelPluginProvider.ConnectorMapping;
import com.boot.jx.postman.plugin.TelegramPlugin;
import com.boot.jx.postman.plugin.TelegramPlugin.TelegramConfigDetails;
import com.boot.jx.postman.query.ChatContactQuery;
import com.boot.jx.postman.tg.TelegramClient;
import com.boot.jx.postman.tg.TelegramModels.TGFile;
import com.boot.jx.utils.PostManUtil;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.Constants;
import com.boot.utils.JsonUtil;

@Component
@ConnectorMapping(contactType = ContactType.TELEGRAM)
public class TelegramConnector extends AbstractConnector<TelegramConfigDetails, TelegramPlugin> {

	private static final Logger LOGGER = LoggerFactory.getLogger(TelegramConnector.class);

	@Autowired
	private TelegramClient telegramClient;

	@Autowired
	private PMFileStoreClient pmFileStoreClient;

	@Override
	public void registerWebhook(ChannelConfig channelConfig, String webhookUrl) {
		telegramClient.registerWebHook(channelConfig, webhookUrl);
	}

	public Status onSend(ChannelConfig channelConfig, ChatContactDoc chatContactDoc, OutboxMessage outboxMessage,
			ApiOutBoundMsg outBoundMsg) {
		telegramClient.send(channelConfig, outboxMessage);
		outboxMessage.updateStatus(OutboxMessage.Status.SENT);
		return Message.Status.SENT;
	}

	@Override
	public InboxMessage assignToAgent(InboxMessage inboxMessage) {
		return inboxMessage;
	}

	public InboxMessage toInboxMessage(ChannelConfig channelConfig, Update update) {

		// Create Default Message from Channel
		InboxMessage inboxMessage = this.createInboxMessage(channelConfig);

		// Set Contact info
		String csid = ArgUtil.parseAsString(update.getMessage().getChatId());

		inboxMessage.contact().setCsid(csid);

		// Set Additional info
		inboxMessage.setFrom(csid);

		// Extract Message Details
		inboxMessage.setOriginalMessage(update);
		inboxMessage.setMessageIdExt(
				String.format("%s-%s", update.getMessage().getChatId(), update.getMessage().getMessageId()));

		String text = ArgUtil.parseAsString(update.getMessage().getText(), Constants.BLANK);

		if (text.startsWith("/start ")) {
			inboxMessage.setMessage(text.replace("/start ", ""));
		} else {
			inboxMessage.setMessage(text);
		}

		if (ArgUtil.is(update.getMessage().getPhoto())) {
			Optional<PhotoSize> photo = update.getMessage().getPhoto().stream()
					.max(Comparator.comparing(PhotoSize::getWidth));

			if (photo.isPresent()) {
				try {
					TGFile file = telegramClient.getFile(channelConfig, photo.get().getFileId());
					/**
					 * Telegram Does not provide Image, so explicitly set Image File Type
					 */
					CommonFileStream srcFile = new CommonFileStream().url(file.getFileUrl()).fileType(FileType.IMAGE);
					CommonFile dstFile = pmFileStoreClient.uploadSessionFileAsync(srcFile,
							PostManUtil.createContactId(inboxMessage), inboxMessage.getMessageIdExt());

					inboxMessage.attachment(new Attachment().mediaURL(dstFile.getUrl()).mediaType(dstFile.getFileType())
							.mediaCaption(update.getMessage().getCaption()));
				} catch (IOException e) {
					logManager.error(inboxMessage, e);
				}
			}
		} else if (ArgUtil.is(update.getMessage().getDocument())) {
			try {
				TGFile file = telegramClient.getFile(channelConfig, update.getMessage().getDocument().getFileId());
				/**
				 * Telegram Does not provide Image, so explicitly set Image File Type
				 */
				CommonFileStream srcFile = new CommonFileStream().url(file.getFileUrl()).fileType(FileType.DOCUMENT);
				CommonFile dstFile = pmFileStoreClient.uploadSessionFileAsync(srcFile,
						PostManUtil.createContactId(inboxMessage), inboxMessage.getMessageIdExt());
				inboxMessage.attachment(new Attachment().mediaURL(dstFile.getUrl()).mediaType(dstFile.getFileType())
						.mediaCaption(update.getMessage().getCaption()));
			} catch (IOException e) {
				logManager.error(inboxMessage, e);
			}
		}
		return inboxMessage;
	}

	@Override
	protected CustomerProfileDoc findProfile(ChatContactDoc chatContactDoc) {
		return contactStore.findProfileByPhone(chatContactDoc.phone());
	}

	@Override
	public OutboxMessage initSession(ChatSessionDoc session, InboxMessage inboxMessage) {

		Update update = JsonUtil.parse(inboxMessage.getOriginalMessage(), Update.class);

		if (ArgUtil.is(update) && ArgUtil.is(update.getMessage()) && ArgUtil.is(update.getMessage().getFrom())
				&& ArgUtil.is(update.getMessage().getContact())) {

			if (ArgUtil.isEqual(update.getMessage().getFrom().getId(), update.getMessage().getContact().getUserId())) {

				ChatContactQuery contactQuery = messageContext.contact();

				contactQuery.setName(update.getMessage().getFrom().getFirstName() + " "
						+ update.getMessage().getFrom().getLastName());
				contactQuery.setPhone(update.getMessage().getContact().getPhoneNumber());
				contactQuery.setPhoneVerified(true);
			}

		}

		Contactable contactDoc = messageContext.contact().getDoc();

		if (ArgUtil.isEmpty(contactDoc.phone())) {
			ChannelConfig config = getChannelConfig(inboxMessage);
			telegramClient.promptShareNumber(config, inboxMessage.getFrom(),
					"Confirm that you would like to share your contact number and continue, by clicking on the button below");
			return OutboxMessage.NO_MESSAGE;
		}
		return null;
	}

	@Override
	public MessageBoxEvent inboundMessageBoxEvent(ChannelConfig channelConfig, MapModel requestMap,
			MessageBoxEvent messageBoxEvent) {
		Update update = requestMap.as(Update.class);
		if (ArgUtil.is(update.getMessage())) {
			return messageBoxEvent.addInboxMessage(toInboxMessage(channelConfig, update));
		} else {
			ApiResponseUtil.throwException("No Message found in Update");
		}
		return messageBoxEvent;
	}

}
