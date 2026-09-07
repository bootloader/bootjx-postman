package com.boot.jx.postman.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.boot.jx.postman.PMConstants;
import com.boot.jx.postman.doc.ChatContactDoc;
import com.boot.jx.postman.doc.ChatProfileDoc;
import com.boot.jx.postman.doc.ChatSessionDoc;
import com.boot.jx.postman.doc.MessageDoc;
import com.boot.jx.postman.dto.ChatMessageDTO;
import com.boot.jx.postman.dto.ChatProfileDTO;
import com.boot.jx.postman.dto.ChatSessionDTO;
import com.boot.jx.postman.dto.ContactDTO;
import com.boot.jx.postman.model.ContactMeta;
import com.boot.jx.postman.model.MessageDefinitions.IMessageId;
import com.boot.jx.postman.model.OutboxMessage;
import com.boot.jx.postman.model.outbound.OutBoundContact;
import com.boot.jx.postman.model.outbound.OutBoundMsgBasic.ApiOutBoundMsg;
import com.boot.jx.utils.PostManUtil;
import com.boot.utils.ArgUtil;
import com.boot.utils.Constants;
import com.boot.utils.EntityDtoUtil;
import com.boot.utils.NumberUtil;

public class ChatDTOUtil {

	public static ChatProfileDTO getProfileDTO(ChatProfileDoc profileDoc) {
		ChatProfileDTO dto = EntityDtoUtil.entityToDto(profileDoc, new ChatProfileDTO());
		return dto;
	}

	public static ContactMeta getContactMeta(ChatContactDoc chatContactDoc) {
		ContactMeta contact = new ContactMeta();
		contact.setName(chatContactDoc.getName());
		contact.phone(chatContactDoc.phone());
		contact.setEmail(chatContactDoc.getEmail());
		contact.setContactType(chatContactDoc.getContactType());
		return contact;
	}

	public static ContactDTO getContactDTO(ChatContactDoc chatContactDoc) {
		ContactDTO contact = new ContactDTO();

		if (ArgUtil.is(chatContactDoc)) {
			contact.setContactId(chatContactDoc.getContactId());
			contact.setContactType(chatContactDoc.getContactType());
			contact.setChannelType(chatContactDoc.getChannelType());
			contact.setName(chatContactDoc.getName());
			contact.phone(chatContactDoc.phone());
			contact.setEmail(chatContactDoc.getEmail());
			contact.setPhoneVerified(chatContactDoc.getPhoneVerified());
			contact.setEmailVerified(chatContactDoc.getEmailVerified());
			contact.setLabelId(chatContactDoc.getLabelId());
			contact.setProfilePic(chatContactDoc.getProfilePic());
			contact.setProfile(chatContactDoc.getProfile());
			contact.setLane(chatContactDoc.getLane());
			contact.setCsid(chatContactDoc.getCsid());

			contact.setCreatedBy(chatContactDoc.getCreatedBy());
			contact.setCreatedStamp(chatContactDoc.getCreatedStamp());
			contact.setLastInBoundStamp(chatContactDoc.getLastInBoundStamp());
			contact.setLastOutBoundStamp(chatContactDoc.getLastOutBoundStamp());
			contact.setLastOptInStamp(chatContactDoc.getLastOptInStamp());
			contact.setLastPushStamp(chatContactDoc.getLastPushStamp());
			contact.setLastReplyStamp(chatContactDoc.getLastReplyStamp());

			contact.setSessionId(chatContactDoc.getSessionId());
			contact.setVCallMeta(chatContactDoc.getVCallMeta());
		}

		return contact;
	}

	public static List<ContactDTO> getContactDTO(List<ChatContactDoc> chatContactDocs) {
		return chatContactDocs.stream().map(chatContactDoc -> ChatDTOUtil.getContactDTO(chatContactDoc))
				.collect(Collectors.toList());
	}

	public static ChatMessageDTO getChatMessageDTO(MessageDoc messageDoc, String contactName, String defaultSender) {
		ChatMessageDTO messageDto = new ChatMessageDTO();
		if (!ArgUtil.is(messageDoc)) {
			return messageDto;
		}

		messageDto.setType(messageDoc.getType());
		messageDto.setText(messageDoc.getMessage());
		messageDto.setMessageTrail(messageDoc.getMessageTrail());
		messageDto.setTemplate(messageDoc.getTemplate());
		messageDto.setTemplateId(messageDoc.getTemplateId());
		messageDto.setTimestamp(messageDoc.getTimestamp());
		messageDto.setSessionId(messageDoc.getSessionId());
		messageDto.setMessageId(messageDoc.getMessageId());
		messageDto.setMessageIdExt(ArgUtil.parseAsString(messageDoc.getMessageIdExt(), Constants.BLANK));
		messageDto.setMessageIdRef(ArgUtil.parseAsString(messageDoc.getMessageIdRef(), Constants.BLANK));
		messageDto.setMessageIdResend(ArgUtil.parseAsString(messageDoc.getMessageIdResend(), Constants.BLANK));

		messageDto.setReplyId(ArgUtil.parseAsString(messageDoc.getReplyId(), Constants.BLANK));
		messageDto.setReplyIdExt(ArgUtil.parseAsString(messageDoc.getReplyIdExt(), Constants.BLANK));
		if (ArgUtil.is(messageDoc.getTags())) {
			messageDto.setTags(messageDoc.getTags());
		}
		if (ArgUtil.is(messageDoc.getAttachments())) {
			messageDto.setAttachments(messageDoc.getAttachments());
		}
		if (ArgUtil.is(messageDoc.getVccards())) {
			messageDto.setVccards(messageDoc.getVccards());
		}

		messageDto.setAction(ArgUtil.parseAsString(messageDoc.getAction(), Constants.BLANK));
		messageDto.setStatus(messageDoc.getStatus());
		messageDto.setBulkSessionId(messageDoc.getBulkSessionId());
		messageDto.setMeta(messageDoc.getMeta());
		messageDto.setForm(messageDoc.getForm());
		messageDto.setTimeout(messageDoc.getTimeout());
		messageDto.setCall(messageDoc.getCall());
		messageDto.setTimer(messageDoc.getTimer());
		messageDto.setOrigin(messageDoc.origin());

		messageDto.setReferral(messageDoc.getReferral());
		messageDto.setOrder(messageDoc.getOrder());
		if (ArgUtil.is(messageDoc.getReplyTo())) {
			messageDto.setReplyTo(messageDoc.getReplyTo());
		} else {
			messageDto.setReplyTo(getDefaultMap(messageDoc.replyTo()));
		}

		if (ArgUtil.is(messageDoc.getOptions())) {
			messageDto.setOptions(messageDoc.getOptions());
		} else {
			messageDto.setOptions(getDefaultMap(messageDoc.options()));
		}

		if (ArgUtil.is(messageDoc.getContact())) {
			messageDto.setContact(EntityDtoUtil.entityToDto(messageDoc.getContact(), new ContactDTO()));
		}

		if (ArgUtil.isEmpty(messageDto.getStamps()) && ArgUtil.is(messageDto.getStatus())) {
			Map<String, Long> stamps = new HashMap<String, Long>();
			stamps.put(messageDto.getStatus(), messageDto.getTimestamp());
			messageDto.setStamps(stamps);
		}

		messageDto.setRoute(messageDoc.getRoute());
		if (PostManUtil.isOutBound(messageDoc.getType())) {
			messageDto.setSender(ArgUtil.nonEmpty(messageDoc.route().getSenderCode(), messageDoc.route().getQueueCode(),
					messageDoc.getAgent(), messageDoc.getQueue(), defaultSender));
		} else if (PostManUtil.isInBound(messageDoc.getType())) {
			messageDto.setSender(ArgUtil.nonEmpty(messageDto.getName(), contactName));
		} else {
			messageDto.setSender(ArgUtil.nonEmpty(messageDoc.getAgent(), defaultSender));
		}

		if (ArgUtil.isEmpty(messageDto.getName())) {
			messageDto.setName(messageDto.getSender());
		}
		messageDto.setStamps(messageDoc.getStamps());
		if (ArgUtil.is(messageDoc.getLogs())) {
			messageDto.setLogs(getUniqueLogs(messageDoc.getLogs()));
		}

		return messageDto;
	}

	public static ChatMessageDTO getChatMessageDTO(MessageDoc messageDoc) {
		if (!ArgUtil.is(messageDoc)) {
			return null;
		}
		return getChatMessageDTO(messageDoc, null, ArgUtil.nonEmpty(messageDoc.getAgent(), messageDoc.getQueue()));
	}

	public static List<ChatMessageDTO> getChatMessageDTO(List<MessageDoc> messageDocs, String contactName,
			String agentName) {
		List<ChatMessageDTO> messageDtos = new ArrayList<ChatMessageDTO>();
		for (MessageDoc messageDoc : messageDocs) {
			ChatMessageDTO messageDto = getChatMessageDTO(messageDoc, contactName, agentName);
			messageDtos.add(messageDto);
		}
		return messageDtos;
	}

	public static <T extends IMessageId> T latestMessage(T messageDoc1, T messageDoc2) {
		if (!ArgUtil.is(messageDoc1)) {
			return messageDoc2;
		}
		if (!ArgUtil.is(messageDoc2)) {
			return messageDoc1;
		}

		if (messageDoc1.getTimestamp() > messageDoc2.getTimestamp()) {
			return messageDoc1;
		}
		return messageDoc2;
	}

	public static ChatSessionDTO getChatSessionDTO(ChatSessionDoc chatSessionDoc) {
		ChatSessionDTO chatSessionDto = EntityDtoUtil.entityToDto(chatSessionDoc, new ChatSessionDTO());
		chatSessionDto.setSessionId(chatSessionDto.getSessionId());
		if (ArgUtil.is(chatSessionDoc.getAssignedToAgent())) {
			chatSessionDto.setAssignedToAgent(chatSessionDoc.getAssignedToAgent());
		}
		chatSessionDto.setAssignedToDept(chatSessionDoc.getAssignedToDept());
		chatSessionDto.setAssignedToBot(chatSessionDoc.getAssignedToBot());
		chatSessionDto.setActive(chatSessionDoc.isActive());
		chatSessionDto.setStatus(chatSessionDoc.getStatus());
		chatSessionDto.setName(chatSessionDoc.getContactName());

		chatSessionDto.setAssignedAgentStamp(chatSessionDoc.getAssignedAgentStamp());
		chatSessionDto.setAssignedDeptStamp(chatSessionDoc.getAssignedDeptStamp());
		chatSessionDto.setLastInComingStamp(chatSessionDoc.getLastInComingStamp());
		chatSessionDto.setLastResponseStamp(chatSessionDoc.getLastResponseStamp());

		chatSessionDto.setTpMeta(chatSessionDoc.getTpMeta());

		chatSessionDto.setUpdatedStamp(chatSessionDoc.updatedStamp());
		chatSessionDto.setOrder(chatSessionDoc.getOrder());

		if (chatSessionDto.getAgentSessionStamp() == 0L) {
			chatSessionDto.setAgentSessionStamp(chatSessionDoc.getAssignedAgentStamp());
		}

		chatSessionDto.setUpdatedStamp(NumberUtil.max(chatSessionDto.getUpdatedStamp(),
				chatSessionDto.getLastInComingStamp(), chatSessionDto.getLastResponseStamp()));

		Map<String, ChatMessageDTO> msg = chatSessionDto.msg();

		if (!msg.containsKey("lastInBoundMsg")) {
			chatSessionDto.msg().put("lastInBoundMsg", chatSessionDoc.lastInBoundMsg());
		}
		if (!msg.containsKey("lastOutBoundMsg")) {
			chatSessionDto.msg().put("lastOutBoundMsg", chatSessionDoc.lastOutBoundMsg());
		}
		if (!msg.containsKey("lastMsg")) {
			chatSessionDto.msg().put("lastMsg", latestMessage(chatSessionDoc.lastMsg(),
					latestMessage(chatSessionDoc.lastInBoundMsg(), chatSessionDoc.lastOutBoundMsg())
			// Only Last Inoboud
			// ArgUtil.nonEmpty(chatSessionDoc.getLastOutBoundMsg(),chatSessionDoc.getLastMsg()))
			));
		}

		if (!ArgUtil.is(chatSessionDto.getStatus())) {
			if (chatSessionDto.isExpired()) {
				chatSessionDto.setStatus(PMConstants.CHAT_STATUS.EXPIRED.toString());
			} else if (!chatSessionDto.isActive()) {
				chatSessionDto.setStatus(PMConstants.CHAT_STATUS.CLOSED.toString());
			} else if (chatSessionDto.isResolved()) {
				chatSessionDto.setStatus(PMConstants.CHAT_STATUS.RESOLVED.toString());
			} else if (chatSessionDto.getAssignedAgentStamp() == 0) {
				chatSessionDto.setStatus(PMConstants.CHAT_STATUS.UNASSIGNED.toString());
			} else {
				chatSessionDto.setStatus(PMConstants.CHAT_STATUS.OPEN.toString());
			}
		}

		if (!ArgUtil.is(chatSessionDto.getState())) {
			if (chatSessionDto.isExpired()) {
				chatSessionDto.setState(PMConstants.CHAT_STATE.EXPIRED.toString());
			} else if (!chatSessionDto.isActive()) {
				chatSessionDto.setState(PMConstants.CHAT_STATE.CLOSED.toString());
			} else if (chatSessionDto.getAssignedAgentStamp() == 0) {
				chatSessionDto.setState(PMConstants.CHAT_STATE.UNATTENDED.toString());
			} else if (!chatSessionDto.isActive()) {
				chatSessionDto.setStatus(PMConstants.CHAT_STATE.ACTIVE.toString());
			}
		}

		return chatSessionDto;
	}

	private static List<Object> getUniqueLogs(List<Object> listWithDuplicates) {
		Set<Object> uniqueSet = new HashSet<>(listWithDuplicates);
		return new ArrayList<>(uniqueSet);
	}

	private static Map<String, Object> getDefaultMap(Map<String, Object> defMap) {
		defMap.put("", "");
		return defMap;
	}

	public static ApiOutBoundMsg toOutBoundMsg(MessageDoc resendMsg) {
		ApiOutBoundMsg outBoundMsg = new ApiOutBoundMsg();
		OutBoundContact c = new OutBoundContact();
		c.copyFrom(resendMsg.getContact());
		outBoundMsg.setToContact(c);
		outBoundMsg.setChannelId(PostManUtil.CHANNEL_ID(resendMsg.getContact()));
		outBoundMsg.setType(resendMsg.getFormatType());

		return outBoundMsg;
	}

	public static OutboxMessage toOutboxMessage(MessageDoc resendMsg) {
		OutboxMessage outMessage = new OutboxMessage();
		// outBoundMsg.setMessageIdResend(resendMsg.getMessageId());
		outMessage.setFormatType(resendMsg.getFormatType());
		outMessage.setFormatSubType(resendMsg.getFormatSubType());
		outMessage.contact().copyFrom(resendMsg.getContact());

		outMessage.template(resendMsg.getTemplate());
		outMessage.setHsm(resendMsg.getHsm());
		outMessage.setModel(resendMsg.getModel());

		outMessage.setSubject(resendMsg.getSubject());
		outMessage.setMessage(resendMsg.getMessage());

		outMessage.setAttachments(resendMsg.getAttachments());
		outMessage.setVccards(resendMsg.getVccards());

		outMessage.options().putAll(resendMsg.options());

		return outMessage;
	}
}
