package com.mobiera.ai.aifriends.multichannel.chatbot.res.c;

import java.util.List;

import com.mobiera.aircast.api.adsafe.GetIdentifierRequest;
import com.mobiera.aircast.api.adsafe.GetLandingRequest;
import com.mobiera.aircast.api.adsafe.GetPricepointRequest;
import com.mobiera.aircast.api.adsafe.GetSubscriptionRequest;
import com.mobiera.aircast.api.adsafe.GetVaServiceRequest;
import com.mobiera.aircast.api.adsafe.HeValidation;
import com.mobiera.aircast.api.adsafe.ListLandingsRequest;
import com.mobiera.aircast.api.adsafe.ListPricepointsRequest;
import com.mobiera.aircast.api.adsafe.ListSubscriptionsRequest;
import com.mobiera.aircast.api.adsafe.ListVaServicesRequest;
import com.mobiera.aircast.api.adsafe.Logout;
import com.mobiera.aircast.api.adsafe.MtRequest;
import com.mobiera.aircast.api.adsafe.OtpRequest;
import com.mobiera.aircast.api.adsafe.OtpRequestResponse;
import com.mobiera.aircast.api.adsafe.OtpValidation;
import com.mobiera.aircast.api.adsafe.SubscribeRequest;
import com.mobiera.aircast.api.adsafe.UnsubscribeRequest;
import com.mobiera.aircast.api.adsafe.ValidationResponse;
import com.mobiera.aircast.api.vo.IdentifierVO;
import com.mobiera.aircast.api.vo.LandingVO;
import com.mobiera.aircast.api.vo.PricepointVO;
import com.mobiera.aircast.api.vo.VaServiceVO;
import com.mobiera.ms.mno.api.json.SubscriptionEvent;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Path("")
public interface KineticClient {

	@POST
	@Path("/adsafe/auth/he/validate")
	@Consumes("application/json")
	@Produces("application/json")
	public Response heValidation(HeValidation heValidation);

	@POST
	@Path("/adsafe/auth/logout")
	@Consumes("application/json")
	@Produces("application/json")
	public Response logout(Logout logout);
	
	@POST
	@Path("/adsafe/authz/subscribe")
	@Consumes("application/json")
	//@Produces("application/json")
	public Response subscribe(SubscribeRequest event);
	
	@POST
	@Path("/adsafe/authz/unsubscribe")
	@Consumes("application/json")
	//@Produces("application/json")
	public Response unsubscribe(UnsubscribeRequest event);

	@POST
	@Path("/adsafe/auth/otp/request")
	@Consumes("application/json")
	@Produces("application/json")
	public OtpRequestResponse otpRequest(OtpRequest event);

	@POST
	@Path("/adsafe/auth/otp/validation")
	@Consumes("application/json")
	@Produces("application/json")
	public ValidationResponse otpValidation(OtpValidation event);

	@POST
	@Path("/adsafe/subscription/get")
	@Consumes("application/json")
	@Produces("application/json")
	public SubscriptionEvent getSubscription(GetSubscriptionRequest gs);
	
	@POST
	@Path("/adsafe/subscriptions/list")
	@Consumes("application/json")
	@Produces("application/json")
	public List<SubscriptionEvent> listSubscriptions(ListSubscriptionsRequest ls);
	
	@POST
	@Path("/adsafe/identifier/get")
	@Produces("application/json")
	@Consumes("application/json")
	public IdentifierVO getIdentifier(GetIdentifierRequest gi);
	
	@POST
	@Path("/adsafe/landing/get")
	@Produces("application/json")
	@Consumes("application/json")
	public LandingVO getLanding(GetLandingRequest gi);
	
	@POST
	@Path("/adsafe/landing/list")
	@Produces("application/json")
	@Consumes("application/json")
	public List<LandingVO> listLandings(ListLandingsRequest lis);
	
	@POST
	@Path("/adsafe/pricepoint/get")
	@Produces("application/json")
	@Consumes("application/json")
	public PricepointVO getPricepoint(GetPricepointRequest gp);
	
	@POST
	@Path("/adsafe/pricepoints/list")
	@Produces("application/json")
	@Consumes("application/json")
	public List<PricepointVO> listPricepoints(ListPricepointsRequest lps);
	
	@POST
	@Path("/adsafe/va_service/get")
	@Produces("application/json")
	@Consumes("application/json")
	public VaServiceVO getVaService(GetVaServiceRequest gv);
	
	@POST
	@Path("/adsafe/va_services/list")
	@Produces("application/json")
	@Consumes("application/json")
	public List<VaServiceVO> listVaServices(ListVaServicesRequest lvs);
	
	@POST
	@Path("/mt")
	@Produces("application/json")
	public Response sentMt(MtRequest lvs);
		
}