package com.boot.jx.session;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.boot.jx.api.ApiResponse;
import com.boot.jx.chat.SessionCloseEventQueue;
import com.boot.jx.postman.model.ext.InBoundEvent;
import com.boot.jx.postman.model.ext.SessionBoundEvent;
import com.boot.utils.ArgUtil;

@RestController
@RequestMapping("/events")
public class SessionEventController {

	private static final Logger LOGGER = LoggerFactory.getLogger(SessionEventController.class);

	@Autowired
	private SessionCloseEventQueue sessionCloseEventQueue;

	@RequestMapping(value = "/api/v1/session/status/close", method = RequestMethod.POST)
	public ApiResponse<InBoundEvent, Object> closeSessionBySessionId(Model model, HttpServletRequest request,
			@RequestBody SessionBoundEvent event) throws InterruptedException {
		List<InBoundEvent> results = new ArrayList<InBoundEvent>();

		if (ArgUtil.is(event.getSessionId())) {
			sessionCloseEventQueue.publish(event);
		} else if (ArgUtil.is(event.getSessionIds())) {
			for (String sessionid : event.getSessionIds()) {
				SessionBoundEvent evt = new SessionBoundEvent();
				evt.sessionId = sessionid;
				evt.contactId = event.contactId;
				evt.type = event.type;
				sessionCloseEventQueue.publish(evt);
			}
		}
		return ApiResponse.buildResults(results);
	}

}
