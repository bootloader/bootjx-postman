package com.boot.jx.postman.tg;

import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

public class TelegramModels {

	public static interface ChatId<T> {
		public void setChatId(String id);

		@SuppressWarnings("unchecked")
		default public T chatId(String id) {
			this.setChatId(id);
			return (T) this;
		}
	}

	public static interface Caption<T> extends ChatId<T> {

		public void setCaption(String caption);

		@SuppressWarnings("unchecked")
		default public T caption(String caption) {
			this.setCaption(caption);
			return (T) this;
		}
	}

	@JsonInclude(Include.NON_NULL)
	@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
	public static class TGSendMessage extends SendMessage implements ChatId<TGSendMessage> {
		public SendMessage text(String text) {
			this.setText(text);
			return this;
		}
	}

	@JsonInclude(Include.NON_NULL)
	@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
	public static class TGSendPhoto extends SendPhoto implements Caption<TGSendPhoto> {
		public TGSendPhoto photo(InputFile inputFile) {
			this.setPhoto(inputFile);
			return this;
		}

		public TGSendPhoto photo(String inputFile) {
			this.setPhoto(new InputFile(inputFile));
			return this;
		}
	}

	@JsonInclude(Include.NON_NULL)
	@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
	public static class TGSendVideo extends SendVideo implements Caption<TGSendVideo> {
		public TGSendVideo video(InputFile inputFile) {
			this.setVideo(inputFile);
			return this;
		}

		public TGSendVideo video(String inputFile) {
			this.setVideo(new InputFile(inputFile));
			return this;
		}
	}

	@JsonInclude(Include.NON_NULL)
	@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
	public static class TGSendDocument extends SendDocument implements Caption<TGSendDocument> {

		public TGSendDocument document(InputFile inputFile) {
			this.setDocument(inputFile);
			return this;
		}

		public TGSendDocument document(String inputFile) {
			this.setDocument(new InputFile(inputFile));
			return this;
		}

	}

	@JsonInclude(Include.NON_NULL)
	@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
	public static class TGMessage extends org.telegram.telegrambots.meta.api.objects.Message {
		private static final long serialVersionUID = 1L;
	}

	@JsonInclude(Include.NON_NULL)
	@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
	public static class TGFile extends org.telegram.telegrambots.meta.api.objects.File {
		private static final long serialVersionUID = 1L;
		String fileUrl;

		public String getFileUrl() {
			return fileUrl;
		}

		public void setFileUrl(String fileUrl) {
			this.fileUrl = fileUrl;
		}

		public TGFile updateFileUrl(String botToken) {
			this.fileUrl = this.getFileUrl(botToken);
			return this;
		}

	}

	@JsonInclude(Include.NON_NULL)
	@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
	public static class TGGetFile extends org.telegram.telegrambots.meta.api.methods.GetFile {

		public TGGetFile fileId(String fileId) {
			this.setFileId(fileId);
			return this;
		}

	}
}
