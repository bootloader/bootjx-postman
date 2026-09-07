package com.boot.jx.postman.gupshup;

import java.util.List;

import org.springframework.stereotype.Component;

import com.boot.jx.postman.gupshup.GupShupConstants.SessionType;
import com.boot.jx.postman.model.MessageOptions;
import com.boot.jx.postman.model.TmplElement;
import com.boot.model.MapModel;

@Component
public class GupShupClientNotify extends GupShupClientAbstract {

	@Override
	public SessionType getSessionType() {
		return SessionType.NOTIFICATION;
	}

	@Override
	public boolean getIsHSM() {
		return true;
	}

	@Override
	public GupShupResp sendMessage(GupShupReq gupShupReq, MessageOptions options) {

		gupShupReq.method(GupShupConstants.Method.SendMessage);
		gupShupReq.setMessageType(GupShupConstants.MessageType.TEXT);

		MapModel optionModel = options.optionsAsModel();
		if (optionModel.entry("wa-template-id").exists()) {
			List<TmplElement> buttons = options.optionActionButtons();
			gupShupReq.setHsm(true);
			gupShupReq.setMessageType(GupShupConstants.MessageType.HSM);
			if (buttons.size() > 0) {
				gupShupReq.setIsTemplate(true); // Message is INteractive Buttons CTA,QRB
			}
		}
		return post(gupShupReq);
	}

}
