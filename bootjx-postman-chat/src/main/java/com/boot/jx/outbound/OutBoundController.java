package com.boot.jx.outbound;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.boot.jx.chat.SessionBoundEventQueue;
import com.boot.jx.postman.client.CommonServiceClient;
import com.boot.jx.postman.model.ext.SessionBoundEvent;

@RestController
@RequestMapping("/outbound")
public class OutBoundController {

	private static final Logger LOGGER = LoggerFactory.getLogger(OutBoundController.class);

	@Autowired
	private CommonServiceClient commonServiceClient;
	
	@Autowired
	private SessionBoundEventQueue sessionBoundEventQueue;

	@RequestMapping(value = "/chrono/session-event-timer/api/v1/message/status", method = RequestMethod.POST)
	public SessionBoundEvent publishSessionBoundEventMessageMock(Model model, HttpServletRequest request,
			@RequestBody SessionBoundEvent event) throws InterruptedException {
		sessionBoundEventQueue.publish(event);
		return event;
	}

	@RequestMapping(value = "/chrono/session-event-timer/api/v1/message/in-out", method = RequestMethod.POST)
	public SessionBoundEvent publishSessionBoundEventStatusMock(Model model, HttpServletRequest request,
			@RequestBody SessionBoundEvent event) throws InterruptedException {
		sessionBoundEventQueue.publish(event);
		return event;
	}

}
