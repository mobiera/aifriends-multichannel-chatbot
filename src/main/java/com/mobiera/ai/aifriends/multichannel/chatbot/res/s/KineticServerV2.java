package com.mobiera.ai.aifriends.multichannel.chatbot.res.s;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mobiera.ai.aifriends.multichannel.chatbot.svc.Service;
import com.mobiera.aircast.api.adsafe.MoRequest;
import com.mobiera.ms.mno.api.json.ChargingEvent;
import com.mobiera.ms.mno.api.json.SubscriptionEvent;

import io.twentysixty.sa.client.util.JsonUtil;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("")
public class KineticServerV2 {

	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.debug")
	Boolean debug;
	
	
	
	@Inject Service service;
	
	private static final Logger logger = Logger.getLogger(KineticServerV2.class);

	
	@POST
    @Path("/subscription/event")
	@Consumes(MediaType.APPLICATION_JSON)
    public Response notifySubscription(SubscriptionEvent event) {

		if (debug) {
			try {
				logger.info("notifySubscription: " + JsonUtil.serialize(event, false));
			} catch (JsonProcessingException e) {
				logger.error("", e);
			}
		}
		
		try {
			service.kineticNotifyPhoneSubscription(event);
		} catch (Exception e) {
			
			logger.error("", e);
			return  Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		return  Response.status(Status.OK).build();
	}
	
	@POST
    @Path("/charging/event")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response notifyCharging(ChargingEvent event) {
		
		if (debug) {
			try {
				logger.info("notifyCharging: " + JsonUtil.serialize(event, false));
			} catch (JsonProcessingException e) {
				logger.error("", e);
			}
		}
		
		try {
			service.kineticNotifyPhoneCharging(event);
		} catch (Exception e) {
			
			logger.error("", e);
			return  Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		return  Response.status(Status.OK).build();
	}
	
	
	@POST
    @Path("/messaging/mo")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response mo(MoRequest request) {
		try {
			service.kineticNotifyMo(request);
			
		} catch (Exception e) {
			
			logger.error("", e);
			return  Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		return  Response.status(Status.OK).build();
	}
	
}
