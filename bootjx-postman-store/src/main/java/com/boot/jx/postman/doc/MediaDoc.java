package com.boot.jx.postman.doc;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.boot.jx.postman.model.Attachment;

@Document(collection = MediaDoc.COLLECTION_NAME)
@TypeAlias("MediaDoc")
public class MediaDoc extends Attachment {
	private static final long serialVersionUID = -7003453286628859075L;
	public static final String COLLECTION_NAME = "MEDIA";

	@Override
	@Id // Annotate the inherited field as @Id
	public String getMediaId() {
		return this.mediaId;
	}

}
