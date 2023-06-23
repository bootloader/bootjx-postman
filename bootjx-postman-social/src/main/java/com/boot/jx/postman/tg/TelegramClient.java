package com.boot.jx.postman.tg;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.ApiResponse;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import com.boot.jx.dict.FileType;
import com.boot.jx.postman.TmplElement;
import com.boot.jx.postman.model.Attachment;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.tg.TelegramModels.TGFile;
import com.boot.jx.postman.tg.TelegramModels.TGGetFile;
import com.boot.jx.postman.tg.TelegramModels.TGMessage;
import com.boot.jx.postman.tg.TelegramModels.TGSendDocument;
import com.boot.jx.postman.tg.TelegramModels.TGSendMessage;
import com.boot.jx.postman.tg.TelegramModels.TGSendPhoto;
import com.boot.jx.postman.tg.TelegramModels.TGSendVideo;
import com.boot.jx.postman.tg.TelegramPlugin.TelegramConfigDetails;
import com.boot.jx.rest.RestService;
import com.boot.model.MapModel;
import com.boot.utils.ArgUtil;
import com.boot.utils.CollectionUtil;

@Component
public class TelegramClient {

	boolean isRegistered;

	private static final Logger LOGGER = LoggerFactory.getLogger(TelegramClient.class);

	public static class PATH {
		public static final String URL = "https://api.telegram.org";
		public static final String BOT = "/bot{accessToken}";
		public static final String BOT_SET_WEBHOOK = BOT + "/setWebHook";
		public static final String BOT_SEND_MESSAGE = BOT + "/sendMessage";
	}

	@Autowired
	private RestService restService;

	public String registerWebHook(TelegramConfigDetails channelConfig, String webhookUrl) {
		return restService.ajax(PATH.URL).path(PATH.BOT_SET_WEBHOOK)
				.pathParam("accessToken", channelConfig.getAccessToken()).field("url", webhookUrl)
				.queryParam("url", webhookUrl).post().asString();
	}

	public TGMessage sendReply(TelegramConfigDetails channelConfig, String id, SendMessage sendMessage) {
		SendMessage message = sendMessage; // Create a SendMessage object with mandatory fields
		sendMessage.setChatId(id);
		return restService.ajax(PATH.URL).path(PATH.BOT_SEND_MESSAGE)
				.pathParam("accessToken", channelConfig.getAccessToken()).post(message)
				.as(new ParameterizedTypeReference<ApiResponse<TGMessage>>() {
				}).getResult();
	}

	public TGMessage sendReply(TelegramConfigDetails channelConfig, String id, String text) {
		SendMessage message = new TGSendMessage()// Create a SendMessage object with mandatory fields
				.chatId(id).text(text);
		return restService.ajax(PATH.URL).path(PATH.BOT_SEND_MESSAGE)
				.pathParam("accessToken", channelConfig.getAccessToken()).post(message)
				.as(new ParameterizedTypeReference<ApiResponse<TGMessage>>() {
				}).getResult();
	}

	public TGMessage sendPhoto(TelegramConfigDetails channelConfig, String id, String photo, String caption) {
		SendPhoto message = new TGSendPhoto() // Create a SendMessage object with mandatory fields
				.chatId(id).photo(new InputFile(photo)).caption(caption);
		return restService.ajax(PATH.URL).path(PATH.BOT).path("/sendPhoto")
				.pathParam("accessToken", channelConfig.getAccessToken()).post(message)
				.as(new ParameterizedTypeReference<ApiResponse<TGMessage>>() {
				}).getResult();
	}

	public TGMessage sendVideo(TelegramConfigDetails channelConfig, String id, String video, String caption) {
		SendVideo message = new TGSendVideo() // Create a SendMessage object with mandatory fields
				.chatId(id).video(video).caption(caption);
		return restService.ajax(PATH.URL).path(PATH.BOT).path("/sendPhoto")
				.pathParam("accessToken", channelConfig.getAccessToken()).post(message)
				.as(new ParameterizedTypeReference<ApiResponse<TGMessage>>() {
				}).getResult();
	}

	public TGMessage sendDocument(TelegramConfigDetails channelConfig, String id, String document, String caption) {
		SendDocument message = new TGSendDocument() // Create a SendMessage object with mandatory fields
				.chatId(id).document(new InputFile(document)).caption(caption);
		return restService.ajax(PATH.URL).path(PATH.BOT).path("/sendDocument")
				.pathParam("accessToken", channelConfig.getAccessToken()).post(message)
				.as(new ParameterizedTypeReference<ApiResponse<TGMessage>>() {
				}).getResult();
	}

	public TGFile getFile(TelegramConfigDetails channelConfig, String fileId) {
		GetFile getFile = new TGGetFile().fileId(fileId);
		String accessToken = channelConfig.getAccessToken();
		return restService.ajax(PATH.URL).path(PATH.BOT).path("/getFile").pathParam("accessToken", accessToken)
				.post(getFile).as(new ParameterizedTypeReference<ApiResponse<TGFile>>() {
				}).getResult().updateFileUrl(accessToken);
	}

	public String promptShareNumber(TelegramConfigDetails channelConfig, String id, String text) {
		SendMessage message = new TGSendMessage() // Create a SendMessage object with mandatory fields
				.chatId(id).text(text);
		// message.setText(text);

		// create keyboard
		ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
		message.setReplyMarkup(replyKeyboardMarkup);
		replyKeyboardMarkup.setSelective(true);
		replyKeyboardMarkup.setResizeKeyboard(true);
		replyKeyboardMarkup.setOneTimeKeyboard(true);

		// new list
		List<KeyboardRow> keyboard = new ArrayList<>();

		// first keyboard line
		KeyboardRow keyboardFirstRow = new KeyboardRow();
		KeyboardButton keyboardButton = KeyboardButton.builder().text(text).requestContact(true).build();
		keyboardFirstRow.add(keyboardButton);
		// add array to list
		keyboard.add(keyboardFirstRow);
		// add list to our keyboard
		replyKeyboardMarkup.setKeyboard(keyboard);

		return restService.ajax(PATH.URL).path(PATH.BOT_SEND_MESSAGE)
				.pathParam("accessToken", channelConfig.getAccessToken()).post(message).asString();

	}

	@Async
	public String promptShareNumberAsync(TelegramConfigDetails channelConfig, String id, String text) {
		return this.promptShareNumber(channelConfig, id, text);
	}

	public OutboxMessage send(TelegramConfigDetails channelConfig, OutboxMessage message) {
		String to = CollectionUtil.getOne(message.getTo());

		TGMessage resp = null;
		StringJoiner msgIds = new StringJoiner(",");

		if (ArgUtil.is(message.getAttachments())) {
			for (Attachment attachment : message.getAttachments()) {
				if (ArgUtil.is(attachment.getMediaURL())) {
					if (ArgUtil.areEqual(attachment.getMediaType(), FileType.IMAGE.toString())) {
						resp = sendPhoto(channelConfig, to, attachment.getMediaURL(), attachment.getMediaCaption());
						if (ArgUtil.is(resp.getMessageId()))
							msgIds.add(ArgUtil.parseAsString(resp.getMessageId()));
					} else if (ArgUtil.areEqual(attachment.getMediaType(), FileType.VIDEO.toString())) {
						resp = sendVideo(channelConfig, to, attachment.getMediaURL(), attachment.getMediaCaption());
						if (ArgUtil.is(resp.getMessageId()))
							msgIds.add(ArgUtil.parseAsString(resp.getMessageId()));
					} else {
						resp = sendDocument(channelConfig, to, attachment.getMediaURL(), attachment.getMediaCaption());
						if (ArgUtil.is(resp.getMessageId()))
							msgIds.add(ArgUtil.parseAsString(resp.getMessageId()));
					}
				}
			}
		}

		if (ArgUtil.is(message.getMessage())) {
			SendMessage sendMessage = new SendMessage();
			sendMessage.setText(message.getMessage());
			if (message.options().containsKey("buttons")) {
				List<TmplElement> buttons = new MapModel(message.options()).entry("buttons").asList(TmplElement.class);
				ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
				replyKeyboardMarkup.setSelective(true);
				replyKeyboardMarkup.setResizeKeyboard(true);
				replyKeyboardMarkup.setOneTimeKeyboard(true);

				List<KeyboardRow> keyboard = new ArrayList<>();
				KeyboardRow keyboardFirstRow = new KeyboardRow();

				for (TmplElement button : buttons) {
					keyboardFirstRow.add(button.getLabel());
				}

				keyboard.add(keyboardFirstRow);

				/**
				 * KeyboardRow keyboardSecondRow = new KeyboardRow();
				 * keyboardSecondRow.add(getAlertsCommand(language));
				 * keyboardSecondRow.add(getBackCommand(language));
				 * keyboard.add(keyboardSecondRow);
				 **/

				replyKeyboardMarkup.setKeyboard(keyboard);
				sendMessage.setReplyMarkup(replyKeyboardMarkup);
			}
			resp = sendReply(channelConfig, to, sendMessage);
			if (ArgUtil.is(resp.getMessageId()))
				msgIds.add(ArgUtil.parseAsString(resp.getMessageId()));
		}
		message.setMessageIdExt(msgIds.toString());

		return message;
	}

}
