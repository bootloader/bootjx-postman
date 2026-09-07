package com.boot.jx.tasks;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.boot.jx.AppContextUtil;
import com.boot.jx.aws.AWSFileStore;
import com.boot.jx.aws.SQSQueueProcessor;
import com.boot.jx.aws.SqsQueueManager.SQSQueue;
import com.boot.jx.dict.FileFormat;
import com.boot.jx.model.CommonFile;
import com.boot.jx.mongo.CommonMongoTemplate;
import com.boot.jx.postman.doc.MediaDoc;
import com.boot.utils.ArgUtil;
import com.boot.utils.StringUtils;

@Component
@EnableScheduling
public class MediaUploadQueue extends SQSQueueProcessor<MediaUploadEvent> {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaUploadQueue.class);

	public static final String QUEUE_NAME = "media-upload-confirm";
	public static final int DELAY_SECONDS = 60;
	public static final int MAX_ATTEMPTS = 2;

	@Autowired
	private AWSFileStore fileStore;

	@Autowired
	private CommonMongoTemplate commonMongoTemplate;

	@Override
	public String getQueueName() {
		return QUEUE_NAME;
	}

	@Override
	public SQSQueue initQueue(SQSQueue queue) {
		return queue.visibilityTimeout(120);
	}

	@Override
	public int delaySeconds(MediaUploadEvent report) {
		return DELAY_SECONDS;
	}

	@Scheduled(fixedDelayString = "${queue.media-upload.process.delay:2000}")
	public void scheduler() {
		process();
	}

	@Override
	public MediaUploadEvent preQueue(MediaUploadEvent event) {
		if (!ArgUtil.is(event.getTenant())) {
			event.setTenant(AppContextUtil.getTenant());
		}
		if (event.getAttempt() < 1) {
			event.setAttempt(1);
		}
		return event;
	}

	@Override
	public void process(MediaUploadEvent event) {
		String key = StringUtils.trim(ArgUtil.parseAsString(event.getKey()), '/');
		if (!ArgUtil.is(key) || key.contains("..")) {
			LOGGER.warn("Discard media-upload event: invalid key");
			return;
		}

		if (ArgUtil.is(event.getTenant())) {
			AppContextUtil.setTenant(event.getTenant());
		}

		CommonFile stat = fileStore.stat1(key);
		if (stat != null) {
			saveMedia(event, key, stat);
			return;
		}

		if (event.getAttempt() < MAX_ATTEMPTS) {
			LOGGER.info("Media not in S3 yet, retry key={} attempt={}", key, event.getAttempt());
			publish(event.attempt(event.getAttempt() + 1));
			return;
		}

		LOGGER.info("Discard media-upload event, file not uploaded key={}", key);
	}

	private void saveMedia(MediaUploadEvent event, String key, CommonFile stat) {
		String displayName = ArgUtil.nonEmpty(event.getName(), FilenameUtils.getName(key));
		FileFormat format = FileFormat.UNKNOWN;
		if (ArgUtil.is(stat.getContentType())) {
			format = FileFormat.from(stat.getContentType());
		}
		if (format == FileFormat.UNKNOWN) {
			format = formatFromFileName(displayName);
		}
		if (format == FileFormat.UNKNOWN) {
			format = formatFromFileName(key);
		}
		if (format == FileFormat.UNKNOWN && ArgUtil.is(stat.getContentType())
				&& stat.getContentType().toLowerCase().contains("image/")) {
			format = FileFormat.JPEG;
		}
		String drive = ArgUtil.nonEmpty(event.getDrive(), "drive");
		String mime = format == FileFormat.UNKNOWN ? stat.getContentType() : format.getContentType();

		MediaDoc media = new MediaDoc();
		media.drive(drive).mediaName(displayName).mediaURL(fileStore.publicUrl1(key)).mediaMimeType(mime)
				.mediaType(format.getFileType()).mediaCaption(event.getCaption()).mediaSize(stat.getContentLength());
		commonMongoTemplate.save(media, MediaDoc.COLLECTION_NAME);
		LOGGER.debug("MEDIA saved for key={} mime={} size={}", key, mime, stat.getContentLength());
	}

	private FileFormat formatFromFileName(String fileName) {
		String ext = FilenameUtils.getExtension(fileName);
		if (ArgUtil.is(ext)) {
			return FileFormat.from(ext);
		}
		return FileFormat.from(fileName);
	}
}
