package com.mobiera.ai.aifriends.multichannel.chatbot.svc;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.graalvm.collections.Pair;
import org.jboss.logging.Logger;
import org.jgroups.util.Base64;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mobiera.ai.aifriends.multichannel.chatbot.jms.MtProducer;
import com.mobiera.ai.aifriends.multichannel.chatbot.model.Memory;
import com.mobiera.ai.aifriends.multichannel.chatbot.model.Session;
import com.mobiera.ai.aifriends.multichannel.chatbot.res.c.KineticClient;
import com.mobiera.aircast.api.adsafe.GetPricepointRequest;
import com.mobiera.aircast.api.adsafe.GetVaServiceRequest;
import com.mobiera.aircast.api.adsafe.ListPricepointsRequest;
import com.mobiera.aircast.api.adsafe.MoRequest;
import com.mobiera.aircast.api.adsafe.MtRequest;
import com.mobiera.aircast.api.adsafe.OtpRequest;
import com.mobiera.aircast.api.adsafe.OtpRequestResponse;
import com.mobiera.aircast.api.adsafe.OtpValidation;
import com.mobiera.aircast.api.adsafe.SubscribeRequest;
import com.mobiera.aircast.api.adsafe.ValidationResponse;
import com.mobiera.aircast.api.vo.PricepointVO;
import com.mobiera.aircast.commons.enums.PricepointType;
import com.mobiera.ms.mno.api.json.ChargingEvent;
import com.mobiera.ms.mno.api.json.SubscriptionEvent;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.SystemMessage;
import io.twentysixty.sa.client.model.credential.CredentialType;
import io.twentysixty.sa.client.model.message.BaseMessage;
import io.twentysixty.sa.client.model.message.ContextualMenuItem;
import io.twentysixty.sa.client.model.message.ContextualMenuSelect;
import io.twentysixty.sa.client.model.message.ContextualMenuUpdate;
import io.twentysixty.sa.client.model.message.MediaItem;
import io.twentysixty.sa.client.model.message.MediaMessage;
import io.twentysixty.sa.client.model.message.MenuDisplayMessage;
import io.twentysixty.sa.client.model.message.MenuItem;
import io.twentysixty.sa.client.model.message.MenuSelectMessage;
import io.twentysixty.sa.client.model.message.TextMessage;
import io.twentysixty.sa.client.util.JsonUtil;
import io.twentysixty.sa.res.c.CredentialTypeResource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;



@ApplicationScoped
public class Service {

	private static Logger logger = Logger.getLogger(Service.class);

	@Inject EntityManager em;
	@Inject AnimatorService animService;
	
	
	@RestClient
	@Inject KineticClient kineticClient;
	
	
	@Inject MtProducer mtProducer;
	
	@Inject ChatBot bot;

	
	@RestClient
	@Inject CredentialTypeResource credentialTypeResource;
	
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.debug")
	Boolean debug;
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.fakekinetic")
	Boolean fakeKinetic;
	
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.enabled")
	Boolean authEnabled;
	
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.billing.enabled")
	Boolean billingEnabled;
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.messages.welcome")
	String WELCOME;
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.messages.welcome2")
	Optional<String> WELCOME2;

	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.messages.welcome3")
	Optional<String> WELCOME3;

	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.messages.auth_success")
	Optional<String> AUTH_SUCCESS;

	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.messages.nocred")
	String NO_CRED_MSG;

			
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.language")
	Optional<String> language;

		@ConfigProperty(name = "com.mobiera.ai.chatbot.anim.random.commands")
	Optional<String> changeCommands;
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.maxuserinputlength")
	Integer maxUserInputLength;
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.msisdn.prefix")
	String msisdnCountryPrefix;
	@ConfigProperty(name = "com.mobiera.ai.chatbot.msisdn.minlength")
	Integer msisdnMinLength;
	@ConfigProperty(name = "com.mobiera.ai.chatbot.msisdn.maxlength")
	Integer msisdnMaxLength;
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.msisdn.example")
	String msisdnExample;
	
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.vaservicefk")
	Long vaServiceFk;
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.authlandingfk")
	Long authLandingFk;
	
	
	
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.endpointfk")
	Long endpointFk;
	@ConfigProperty(name = "com.mobiera.ai.chatbot.password")
	String endpointPassword;
	
	
	private static String[] models = null;
	private static String defaultModel = null;
	
	
	private static String CMD_ROOT_MENU_AUTHENTICATE = "/auth";
	private static String CMD_ROOT_MENU_NO_CRED = "/nocred";
	private static String CMD_ROOT_MENU_OPTION1 = "/option1";
	private static String CMD_ROOT_MENU_LOGOUT = "/logout";
	
	private static String CMD_ROOT_MENU_HELP = "/help";
	private static String CMD_ROOT_MENU_ANIMATOR = "/anim";
	private static String CMD_ROOT_MENU_RANDOM = "/random";
	
	private static String CMD_ROOT_MENU_SUBSCRIBE = "/subscribe";
	
	private static final String SUBSCRIBE_CONFIRM_YES_VALUE = "/yes";
	private static final String SUBSCRIBE_CONFIRM_NO_VALUE = "/no";
	
	private static String CMD_ROOT_MENU_SET_MODEL = "/set";
	
	private static String CMD_ROOT_MENU_CLEAR = "/clear";
	
	private Random random = new Random();
	
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.messages.root.menu.title")
	String ROOT_MENU_TITLE;
	
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.messages.root.menu.option1")
	String ROOT_MENU_OPTION1;
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.messages.root.menu.no_cred")
	Optional<String> ROOT_MENU_NO_CRED;
	
	
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.messages.option1")
	String OPTION1_MSG;
	
	
	
	
	
	
	//private static HashMap<UUID, SessionData> sessions = new HashMap<UUID, SessionData>();
	private static CredentialType type = null;
	private static Object lockObj = new Object();
	
	
	
	
	
	
	public void newHologramConnection(UUID connectionId) throws Exception {
		UUID threadId = UUID.randomUUID();
		mtProducer.sendMessage(TextMessage.build(connectionId,threadId , WELCOME));
		if (WELCOME2.isPresent()) {
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, WELCOME2.get()));
		}
		if (WELCOME3.isPresent()) {
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, WELCOME3.get()));
		}
		
		
		mtProducer.sendMessage(this.getRootMenu(connectionId, null));
		
		if (authEnabled) {
			mtProducer.sendMessage(this.getMsisdnAuthRequest(connectionId, null));
			
		} else {
			mtProducer.sendMessage(this.getAnimatorMenu(connectionId, null));
		}
	}
	

	
	
	
	private BaseMessage getMsisdnAuthRequest(UUID connectionId, UUID threadId) {
		return TextMessage.build(connectionId, threadId, this.getMessage("ENTER_PHONE_NUMBER").replaceAll("EXAMPLE_NUMBER", msisdnExample)
				.replaceAll("PHONE_PREFIX", msisdnCountryPrefix)
				);
	}


	private Session getPinAuthRequest(Session session, UUID connectionId, UUID threadId, String msisdn) throws Exception {
		
		session.setVerifyingMsisdn(msisdn);
		session = em.merge(session);
		
		OtpRequest otpr = new OtpRequest();
		otpr.setEndpointFk(endpointFk);
		otpr.setPassword(endpointPassword);
		otpr.setRequestId(UUID.randomUUID());
		otpr.setUserId(msisdn);
		otpr.setUserIpAddr("8.8.8.8");
		otpr.setLandingFk(authLandingFk);
		
		if (fakeKinetic) {
			logger.info("fakeKinetic: " + JsonUtil.serialize(otpr, false));
		} else {
			logger.info("fakeKinetic: otpr: " + JsonUtil.serialize(otpr, false));
			OtpRequestResponse resp = kineticClient.otpRequest(otpr);
			session.setOtpRequestId(resp.getOtpRequestId());
			
			session = em.merge(session);
		}
		
		mtProducer.sendMessage(TextMessage.build(connectionId, threadId, this.getMessage("ENTER_PIN").replaceAll("NUMBER", msisdn)));
		return session;
	}

	private BaseMessage getInvalidPin(UUID connectionId, UUID threadId, String msisdn) {
		return TextMessage.build(connectionId, threadId, this.getMessage("INVALID_PIN").replaceAll("NUMBER", msisdn));
	}


	private Session getSessionWithConnectionId(UUID connectionId) {
		Query q = em.createNamedQuery("Session.findWithConnectionId");
		q.setParameter("connectionId", connectionId);
		Session session = (Session) q.getResultList().stream().findFirst().orElse(null);
		
		if (session == null) {
			session = new Session();
			session.setId(UUID.randomUUID());
			session.setConnectionId(connectionId);
			em.persist(session);
		}
		
		return session;
	}
	
	private Session getSessionWithMsisdn(String msisdn, boolean create) {
		Query q = em.createNamedQuery("Session.findWithMsisdn");
		q.setParameter("msisdn", msisdn);
		Session session = (Session) q.getResultList().stream().findFirst().orElse(null);
		
		if ((session == null) && create) {
			session = new Session();
			session.setId(UUID.randomUUID());
			session.setMsisdn(msisdn);
			em.persist(session);
		}
		
		return session;
	}
	
	
	Pair<String, byte[]> getImage(String image) {
		String mimeType = null;
		byte[] imageBytes = null;
		
		String[] separated =  image.split(";");
		if (separated.length>1) {
			String[] mimeTypeData = separated[0].split(":");
			String[] imageData = separated[1].split(",");
			
			if (mimeTypeData.length>1) {
				mimeType = mimeTypeData[1];
			}
			if (imageData.length>1) {
				String base64Image = imageData[1];
				if (base64Image != null) {
					try {
						imageBytes = Base64.decode(base64Image);
					} catch (IOException e) {
						logger.error("", e);
					}
				}
			}
			
		}
		
		if (mimeType == null) return null;
		if (imageBytes == null) return null;
		
		return Pair.create(mimeType, imageBytes);
		
	}
	ResourceBundle bundle = null; 
	
	private String getMessage(String messageName) {
		String retval = messageName;
		if (bundle == null) {
			if (language.isPresent()) {
				try {
					bundle = ResourceBundle.getBundle("META-INF/resources/Messages", new Locale(language.get())); 
				} catch (Exception e) {
					bundle = ResourceBundle.getBundle("META-INF/resources/Messages", new Locale("en")); 
				}
			} else {
				bundle = ResourceBundle.getBundle("META-INF/resources/Messages", new Locale("en")); 
			}
			
		}
		try {
			retval = bundle.getString(messageName);
		} catch (Exception e) {
			
		}
		
		
		return retval;
	}
	
	
	
	private boolean changeCommand(String content) {
		
		if (content.startsWith("/")) {
			if (changeCommands.isPresent()) {
				if (changeCommands.get().length()>0) {
					String[] commands = changeCommands.get().split(",");
					for (int i=0; i<commands.length; i++) {
						String cur = commands[i];
						if (cur!=null) {
							cur = cur.strip();
							
							if (content.equals("/" + cur)) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	@Transactional
	public void userInput(BaseMessage message) throws Exception {
		
		Session session = this.getSessionWithConnectionId(message.getConnectionId());
		
		
		
		String content = null;
		boolean contextual = false;
		MediaMessage mm = null;
		
		if (message instanceof TextMessage) {
			
			TextMessage textMessage = (TextMessage) message;
			content = textMessage.getContent();

		} else if ((message instanceof ContextualMenuSelect) ) {
			
			ContextualMenuSelect menuSelect = (ContextualMenuSelect) message;
			content = menuSelect.getSelectionId();
			contextual = true;
		} else if ((message instanceof MenuSelectMessage)) {
			
			MenuSelectMessage menuSelect = (MenuSelectMessage) message;
			content = menuSelect.getMenuItems().iterator().next().getId();
		} else if ((message instanceof MediaMessage)) {
			mm = (MediaMessage) message;
			content = "media";
		}
		
		
		
		
		
		if (content != null) {
			content = content.strip();
			
			
			boolean authenticated = true;
			
			if (authEnabled) {
				if (session.getAuthTs() == null) {
					authenticated = false;
					
					if (content.equals(CMD_ROOT_MENU_AUTHENTICATE.toString())) {
						
						session.setVerifyingMsisdn(null);
						session.setAuthCode(null);
						session.setOtpRequestId(null);
						mtProducer.sendMessage(this.getMsisdnAuthRequest(message.getConnectionId(), message.getThreadId()));
						
					} else if (session.getVerifyingMsisdn() != null) {
						// expecting pin
						
						OtpValidation ov = new OtpValidation();
						ov.setEndpointFk(endpointFk);
						ov.setPassword(endpointPassword);
						ov.setOtp(content);
						ov.setOtpRequestId(session.getOtpRequestId());
						
						if (fakeKinetic) {
							logger.info("fakeKinetic: " + JsonUtil.serialize(ov, false));
							
							
						} else {
							logger.info("fakeKinetic: userInput: " + JsonUtil.serialize(ov, false));
							
							ValidationResponse vr = null;
							
							try {
								vr = kineticClient.otpValidation(ov);
							} catch (Exception e) {
								logger.info("fakeKinetic: userInput: ", e);
								
							}
							
							
							if ((vr != null) && (vr.getAuthCode() != null)) {
								session.setAuthCode(vr.getAuthCode());
								successfullAuth(session, message.getConnectionId(), message.getThreadId());
								
								if (session.getMemory() != null) {
									mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), this.getMessage("ANIM_SELECTED").replaceAll("ANIMATOR", getAnimatorLabel(session))));

									String result = bot.chat(session.getMemory().getMemoryId(), animService.get(session.getMemory().getAnimator()).getHello());
									mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), result));
								} else {
									mtProducer.sendMessage(this.getAnimatorMenu(message.getConnectionId(), message.getThreadId()));
								}
							} else {
								mtProducer.sendMessage(this.getInvalidPin(message.getConnectionId(), message.getThreadId(), session.getVerifyingMsisdn()));
							}
						}
					} else if (this.isValidMsisdn(content)) {
						
						
						session = this.getPinAuthRequest(session, message.getConnectionId(), message.getThreadId(), content);
						
					} else {
						mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId() , this.getMessage("ERROR_NOT_AUTHENTICATED")));
						
						session.setVerifyingMsisdn(null);
						session.setOtpRequestId(null);
						session.setAuthCode(null);
						session.setMsisdn(null);
						mtProducer.sendMessage(this.getMsisdnAuthRequest(message.getConnectionId(), message.getThreadId()));

						

					}
				}
			}
			
			boolean charged = true;
			
			if (!authenticated) {
				charged = false;
			}
			
			if (authenticated && billingEnabled) {
				
				if (session.getSubscriptionTs() == null) {
					charged = false;
					
					// not subscribed
					if (content.startsWith(CMD_ROOT_MENU_SUBSCRIBE.toString())) {
						String[] sd = content.split(" ");
						if (sd.length>1) {
							Long ppId = null;
							try {
								ppId = Long.parseLong(sd[1]);
								mtProducer.sendMessage(this.buildConfirmationMenu(ppId, message.getConnectionId(), message.getThreadId()));
								
							} catch (Exception e) {
								logger.error("", e);
								mtProducer.sendMessage(buildSubscriptionOfferMenu(message.getConnectionId(), message.getThreadId()));

							}
						} else {
							mtProducer.sendMessage(buildSubscriptionOfferMenu(message.getConnectionId(), message.getThreadId()));

						}
					} if (content.startsWith(SUBSCRIBE_CONFIRM_YES_VALUE.toString())) {
						String[] sd = content.split(" ");
						if (sd.length>1) {
							Long ppId = null;
							try {
								ppId = Long.parseLong(sd[1]);
								GetPricepointRequest gpp = new GetPricepointRequest();
								gpp.setEndpointFk(endpointFk);
								gpp.setPassword(endpointPassword);
								gpp.setPricepointFk(ppId);
								PricepointVO pp = kineticClient.getPricepoint(gpp);
								
								if (pp != null) {
									Long landingFk = pp.getDefaultAuthzLandingFk();
									
									SubscribeRequest sr = new SubscribeRequest();
									sr.setPassword(endpointPassword);
									sr.setEndpointFk(endpointFk);
									sr.setLandingFk(landingFk);
									sr.setUserId(session.getMsisdn());
									sr.setRequestId(UUID.randomUUID());
									sr.setAuthCode(session.getAuthCode().toString());
									sr.setUserIpAddr("8.8.8.8");
									kineticClient.subscribe(sr);
									
									mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), this.getMessage("SUBSCRIPION_REQUESTED").replaceAll("PRICEPOINT", pp.getName())));

								}
								
							} catch (Exception e) {
								logger.error("", e);
								mtProducer.sendMessage(buildSubscriptionOfferMenu(message.getConnectionId(), message.getThreadId()));

							}
						} else {
							mtProducer.sendMessage(buildSubscriptionOfferMenu(message.getConnectionId(), message.getThreadId()));

						}
					} else {
						mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), this.getMessage("SUBSCRIPTION_REQUIRED")));
						mtProducer.sendMessage(buildSubscriptionOfferMenu(message.getConnectionId(), message.getThreadId()));
					}
					
				} else if ((session.getExpireTs() == null) || (session.getExpireTs().compareTo(Instant.now())<0)) {
					charged = false;
					// not charged or expired
					mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), this.getMessage("EXPIRED_SUBSCRIPTION")));

				}
			}
			//logger.info("uche2 authenticated:" + authenticated + " charged:" + charged);

			if (authenticated && charged) {
				
				if (content.length()>maxUserInputLength) {
					content = content.substring(0, maxUserInputLength);
				}
						
				if (content.equals(CMD_ROOT_MENU_AUTHENTICATE.toString()) && (session.getAuthTs() == null)) {
					
					session.setVerifyingMsisdn(null);
					session.setOtpRequestId(null);
					session.setAuthCode(null);
					session = em.merge(session);
					mtProducer.sendMessage(this.getMsisdnAuthRequest(message.getConnectionId(), message.getThreadId()));
					
				} else if (content.equals(CMD_ROOT_MENU_OPTION1.toString())) {
					mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId() , OPTION1_MSG));

				} else if (content.equals(CMD_ROOT_MENU_LOGOUT.toString())) {
					if (session != null) {
						session.setAuthTs(null);
						session.setOtpRequestId(null);
						session.setAuthCode(null);
						//session.setConnectionId(null);
						session = em.merge(session);
					}
					mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId() , this.getMessage("UNAUTHENTICATED")));

					if (authEnabled) {
						//session.setVerifyingMsisdn(null);
						//session.setVerifyingPin(null);
						mtProducer.sendMessage(this.getMsisdnAuthRequest(message.getConnectionId(), message.getThreadId()));
					}
					
				} else if (content.equals(CMD_ROOT_MENU_HELP.toString())) {
					
					mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId() , this.getMessage("USAGE")));

				} else if (content.equals(CMD_ROOT_MENU_CLEAR.toString())) {
					
					if (session != null) {
						
						if (session.getMemory() != null) {
							this.resetMemory(session.getMemory().getMemoryId());
						}
						
					}
					
					mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId() , this.getMessage("CLEANED_HISTORY")));

				}  else if ((session != null) && ( (session.getAuthTs() != null)|| (!authEnabled) )) {
					
					
						if (content.startsWith(CMD_ROOT_MENU_ANIMATOR.toString())) {
							logger.info("uche22");
							String[] specifiedAnim = content.split(" ");
							if (specifiedAnim.length == 1) {
								

								mtProducer.sendMessage(this.getAnimatorMenu(message.getConnectionId(), message.getThreadId()));
							} else {
								Integer anim = null;
								try {
									anim = Integer.parseInt(specifiedAnim[1]);
									this.setAnimator(session, anim);
									mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), this.getMessage("ANIM_SELECTED").replaceAll("ANIMATOR", getAnimatorLabel(session))));

									String result = bot.chat(session.getMemory().getMemoryId(), animService.get(anim).getHello());
									mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), result));
								} catch (Exception e) {
									
								}
							}
							

						} else if (content.startsWith(CMD_ROOT_MENU_RANDOM.toString()) || this.changeCommand(content)) {
							
							
							TreeMap<Integer,Animator> anims = animService.getAnimators();
							
							if (anims.size() == 1) {
								this.setAnimator(session, 0);
								mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), this.getMessage("ANIM_SELECTED").replaceAll("ANIMATOR", getAnimatorLabel(session))));

								String result = bot.chat(session.getMemory().getMemoryId(), animService.get(0).getHello());
								mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), result));
							} else {
								Set<Integer> keys = anims.keySet();
								List<Integer> others = new ArrayList<Integer>();
								Integer currentAnim = null;
								if (session.getMemory() != null) {
									currentAnim = session.getMemory().getAnimator();
								}
								for (Integer current: keys) {
									if (currentAnim != null) {
										if (current.intValue() != currentAnim.intValue()) {
											others.add(current);
										}
									} else {
										others.add(current);
									}
								}
								
								Integer newAnim = others.get(random.nextInt(others.size()));
								this.setAnimator(session, newAnim);
								mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), this.getMessage("ANIM_SELECTED").replaceAll("ANIMATOR", getAnimatorLabel(session))));

								String result = bot.chat(session.getMemory().getMemoryId(), animService.get(newAnim).getHello());
								mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), result));
							}
							
							

						} else {
							logger.info("uche23");
							if (session.getMemory() == null) {
								mtProducer.sendMessage(this.getAnimatorMenu(message.getConnectionId(), message.getThreadId()));
							} else {
								
								String result = bot.chat(session.getMemory().getMemoryId(), content);
								mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), result));
							}
						}
						
					
					
				}
			}
			
			
		}
		mtProducer.sendMessage(this.getRootMenu(message.getConnectionId(), session));
	}
	
	
	
	
	private BaseMessage buildSubscriptionOfferMenu(UUID connectionId, UUID threadId) {
		ListPricepointsRequest lpps = new ListPricepointsRequest();
		lpps.setEndpointFk(endpointFk);
		lpps.setVaServiceFk(vaServiceFk);
		lpps.setPassword(endpointPassword);
		List<PricepointVO> pps = kineticClient.listPricepoints(lpps);
		
		
		
		
		List<MenuItem> menuItems = new ArrayList<MenuItem>();
		
		MenuDisplayMessage offerList = new MenuDisplayMessage();
		offerList.setPrompt(getMessage("SUBSCRIPTION_OFFER_MENU_TITLE"));
		
		
		for (PricepointVO pp: pps) {
			MenuItem am = new MenuItem();
			am.setId(CMD_ROOT_MENU_SUBSCRIBE + " " + pp.getId());
			am.setText(pp.getName());
			menuItems.add(am);
			
		}
		
		offerList.setConnectionId(connectionId);
		offerList.setThreadId(threadId);
		offerList.setMenuItems(menuItems);
		return offerList;
		
	}

	
	private BaseMessage buildConfirmationMenu(Long pricepointFk, UUID connectionId, UUID threadId) {
		
		MenuDisplayMessage confirm = new MenuDisplayMessage();
		confirm.setPrompt(getMessage("SUBSCRIBE_CONFIRM_TITLE"));

		MenuItem yes = new MenuItem();
		yes.setId(SUBSCRIBE_CONFIRM_YES_VALUE + " " + pricepointFk);
		yes.setText(getMessage("SUBSCRIBE_CONFIRM_YES"));
		
		MenuItem no = new MenuItem();
		no.setId(SUBSCRIBE_CONFIRM_NO_VALUE);
		no.setText(getMessage("SUBSCRIBE_CONFIRM_NO"));
		
		List<MenuItem> menuItems = new ArrayList<MenuItem>();
		menuItems.add(yes);
		menuItems.add(no);
		
		
		confirm.setMenuItems(menuItems);


		confirm.setConnectionId(connectionId);
		confirm.setThreadId(threadId);
		
		return confirm;
	}




	private boolean isValidMsisdn(String src) {
		
		if (src == null) return false;
		
		
		
		String countryPrefix = msisdnCountryPrefix;
		
		if (countryPrefix != null) {
			countryPrefix = countryPrefix.strip();
		}

		int minLength = msisdnMinLength;
		int maxLength = msisdnMaxLength;
		
		
		if (!src.startsWith(countryPrefix)) return false;
		if (src.length()>maxLength) return false;
		if (src.length()<minLength) return false;
		if (!src.matches("[0-9]+")) return false;
		return true;
	}
	
	
	
	@Transactional
	public void successfullAuth(Session session, UUID connectionId, UUID threadId) throws Exception {
		
		
		Session msisdnSession = this.getSessionWithMsisdn(session.getVerifyingMsisdn(), false);
		
		if (msisdnSession == null) {
			session.setMsisdn(session.getVerifyingMsisdn());
			session.setVerifyingMsisdn(null);
			session.setAuthTs(Instant.now());
			session = em.merge(session);
			
			
		} else if (msisdnSession.getId().equals(session.getId())) {
			session.setVerifyingMsisdn(null);
			session.setAuthTs(Instant.now());
			session = em.merge(session);
			
		} else {
			
			session.setMsisdn(session.getVerifyingMsisdn());
			session.setVerifyingMsisdn(null);
			session.setAuthTs(Instant.now());
			session.setSubscriptionTs(msisdnSession.getSubscriptionTs());
			session.setExpireTs(msisdnSession.getExpireTs());
			session.setCanceledTs(msisdnSession.getCanceledTs());
			if (session.getMemory() == null) {
				if (msisdnSession.getMemory() != null) {
					session.setMemory(msisdnSession.getMemory());
					Memory m = session.getMemory();
					m.setSessionId(session.getId());
					m = em.merge(m);
				}
			}
			em.remove(msisdnSession);
			em.flush();
			
			session = em.merge(session);
			
		}
		
		if (AUTH_SUCCESS.isPresent()) {
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId , AUTH_SUCCESS.get()));
		} else {
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId , this.getMessage("AUTHENTICATION_SUCCESS")));
		}
		//mtProducer.sendMessage(TextMessage.build(connectionId, null , this.getMessage("SET_MODEL").replaceAll("MODEL", session.getModel())));
		//mtProducer.sendMessage(this.getAnimatorMenu(connectionId, null));
		
		
		
	}

	
	
	public BaseMessage getAnimatorMenu(UUID connectionId, UUID threadId) {
		
		
		//if (debug) logger.info("getAnimatorMenu");
		
		
		List<MenuItem> menuItems = new ArrayList<MenuItem>();
		
		MenuDisplayMessage confirm = new MenuDisplayMessage();
		confirm.setPrompt(getMessage("CHOOSE_AN_ANIMATOR"));
		
		TreeMap<Integer,Animator> anims = animService.getAnimators();
		
		for (Integer ak: anims.keySet()) {
			Animator a = anims.get(ak);
			MenuItem am = new MenuItem();
			am.setId(CMD_ROOT_MENU_ANIMATOR + " " + ak);
			am.setText(a.getName() + ", " + a.getAge() + ", " + a.getPlace() + " (" + a.getLanguage() + ")");
			menuItems.add(am);
		}
		
		confirm.setConnectionId(connectionId);
		confirm.setThreadId(threadId);
		confirm.setMenuItems(menuItems);
		return confirm;
	}
	 
	@Transactional
	public UUID setAnimator(Session session, int animator) {
		
		
		
		Query q = em.createNamedQuery("Session.findMemory");
		q.setParameter("sessionId", session.getId());
		q.setParameter("animator", animator);
		Memory memory = (Memory) q.getResultList().stream().findFirst().orElse(null);
		if (memory == null) {
			memory = new Memory();
			memory.setMemoryId(UUID.randomUUID());
			memory.setAnimator(animator);
			em.persist(memory);
			this.resetMemory(memory.getMemoryId());
		}
		if ((session.getMemory() == null) || (!session.getMemory().getMemoryId().equals(memory.getMemoryId()))) {
			session.setMemory(memory);
			session = em.merge(session);
		}
		return memory.getMemoryId();
		
	}
	
	@Transactional
	public void resetMemory(UUID memoryId) {
		Memory memory = em.find(Memory.class, memoryId);
		
		if (memory == null) return;
		
		Animator anim = animService.get(memory.getAnimator());
		List<ChatMessage> messages = new ArrayList<ChatMessage>(0);
		ChatMessage m = SystemMessage.systemMessage(anim.getPrompt());
		messages.add(m);
		memory.setMemory(ChatMessageSerializer.messagesToJson(messages));
		memory = em.merge(memory);
	}
	
	
	private String getAnimatorLabel(Session session) {
		if (session.getMemory() != null) {
			Animator a = animService.get(session.getMemory().getAnimator());
			return a.getSummary();
		} else {
			return null;
		}
	}
	
	public BaseMessage getRootMenu(UUID connectionId, Session session) {
		
		ContextualMenuUpdate menu = new ContextualMenuUpdate();
		menu.setTitle(ROOT_MENU_TITLE);
		
		
		List<ContextualMenuItem> options = new ArrayList<ContextualMenuItem>();
		
		
		if ((session == null) || (( authEnabled) && (session.getAuthTs() == null)  )){
			menu.setDescription(getMessage("ROOT_MENU_DEFAULT_DESCRIPTION"));
			options.add(ContextualMenuItem.build(CMD_ROOT_MENU_AUTHENTICATE, getMessage("ROOT_MENU_AUTHENTICATE"), null));
			/*if (ROOT_MENU_NO_CRED.isPresent()) {
				options.add(ContextualMenuItem.build(CMD_ROOT_MENU_NO_CRED, ROOT_MENU_NO_CRED.get(), null));
			} else {
				options.add(ContextualMenuItem.build(CMD_ROOT_MENU_NO_CRED, getMessage("ROOT_MENU_NO_CRED"), null));
			}*/
			
			
		} else {
			
			if ((session.getMsisdn() != null) ) {
				String animLabel = this.getAnimatorLabel(session);
				if (animLabel != null) {
					menu.setDescription(getMessage("ROOT_MENU_AUTHENTICATED_DESCRIPTION_ANIM_SELECTED").replaceAll("NUMBER", session.getMsisdn()).replaceAll("ANIMATOR", getAnimatorLabel(session)));

				} else {
					menu.setDescription(getMessage("ROOT_MENU_AUTHENTICATED_DESCRIPTION").replaceAll("NUMBER", session.getMsisdn()));

				}
				
			} 
			
			options.add(ContextualMenuItem.build(CMD_ROOT_MENU_HELP, this.getMessage("ROOT_MENU_HELP"), null));
			options.add(ContextualMenuItem.build(CMD_ROOT_MENU_ANIMATOR, this.getMessage("ROOT_MENU_ANIMS"), null));
			
			if (authEnabled) {
				options.add(ContextualMenuItem.build(CMD_ROOT_MENU_LOGOUT, this.getMessage("ROOT_MENU_LOGOUT"), null));
				
			}
			
		}
		
		
		
		menu.setOptions(options);
		


		if (debug) {
			try {
				logger.info("getRootMenu: " + JsonUtil.serialize(menu, false));
			} catch (JsonProcessingException e) {
			}
		}
		menu.setConnectionId(connectionId);
		menu.setId(UUID.randomUUID());
		menu.setTimestamp(Instant.now());
		
		return menu;
		

	}



	@Transactional
	public void kineticNotifyPhoneCharging(ChargingEvent event) {
		Session session = this.getSessionWithMsisdn(event.getUserTpda(), true);
		// 	FULL, PARTIAL, COMPLEMENT, NO_FUNDS, TEMP_ERROR, PERM_ERROR, DISABLED_ENTITY, GIVING_UP, CONNECTION_ERROR

		switch (event.getChargingEventType()) {
		case FULL:
		case PARTIAL:
		case COMPLEMENT: {
			session.setExpireTs(event.getSubscriptionExpireTs());
			session = em.merge(session);
			break;
		}
		default: {
			break;
		}
		}
	}


	@Transactional
	public void kineticNotifyPhoneSubscription(SubscriptionEvent event) throws JsonProcessingException {
		
		Session session = this.getSessionWithMsisdn(event.getUserTpda(), true);
		
		
		switch (event.getSubscriptionEventType()) {
		case SUBSCRIBED: {
			session.setCanceledTs(null);
			session.setSubscriptionTs(Instant.now());
			session = em.merge(session);
			
			GetVaServiceRequest req = new GetVaServiceRequest();
			req.setEndpointFk(endpointFk);
			req.setPassword(endpointPassword);
			req.setVaServiceFk(vaServiceFk);
			
			try {
				MtRequest mt = new MtRequest();
				
				mt.setText(this.getMessage("WELCOME_SEND_URL_TO_VS").replaceAll("VERIFIABLE_SERVICE_URL", kineticClient.getVaService(req).getUrl()));
				mt.setUserId(event.getUserId());
				if (mt.getUserId() == null) mt.setUserId(event.getUserTpda());
				mt.setRequestId(UUID.randomUUID());
				mt.setVaServiceFk(vaServiceFk);
				mt.setEndpointFk(endpointFk);
				mt.setPassword(endpointPassword);
				
				
				if (fakeKinetic) {
					logger.info("fakeKinetic: " + JsonUtil.serialize(mt, false));
				} else {
					logger.info("kineticNotifyPhoneSubscription: " + JsonUtil.serialize(mt, false));

					kineticClient.sentMt(mt);
				}
			} catch (Exception e) {
				logger.error("", e);
			}
			
			
			
			if (session.getConnectionId() == null) {
				MoRequest mo = new MoRequest();
				mo.setUserId(event.getUserTpda());
				this.kineticNotifyMo(mo);
			}
			break;
		}
		
		case UNSUBSCRIBED: {
			session.setCanceledTs(Instant.now());
			session = em.merge(session);
			
			MtRequest mt = new MtRequest();
			mt.setText(this.getMessage("UNSUBSCRIBED"));
			mt.setUserId(event.getUserId());
			if (mt.getUserId() == null) mt.setUserId(event.getUserTpda());
			mt.setRequestId(UUID.randomUUID());
			mt.setVaServiceFk(vaServiceFk);
			mt.setEndpointFk(endpointFk);
			mt.setPassword(endpointPassword);
			
			if (fakeKinetic) {
				logger.info("fakeKinetic: " + JsonUtil.serialize(mt, false));
			} else {
				logger.info("kineticNotifyPhoneSubscription: " + JsonUtil.serialize(mt, false));

				kineticClient.sentMt(mt);
			}
			
			if (session.getConnectionId() != null) {
				try {
					mtProducer.sendMessage(TextMessage.build(session.getConnectionId(), null , this.getMessage("UNSUBSCRIBED")));
				} catch (Exception e) {
					logger.error("kineticNotifyPhoneSubscription: ", e);
				}

			}
			
			break;
		}
		
		default: {
			
			break;
		}
		
		}
		
	}


	@Transactional
	public void kineticNotifyMo(MoRequest request) throws JsonProcessingException {
		
		if (!this.isValidMsisdn(request.getUserId())) {
			logger.error("kineticNotifyMo: ignoring invalid msisdn: " + request.getUserId() + " " + request.getText());
			return;
		}
		Session session = this.getSessionWithMsisdn(request.getUserId(), true);
		
		String content = request.getText();
		
		if ( billingEnabled && ((session.getSubscriptionTs() == null) )) {
			MtRequest mt = new MtRequest();
			mt.setText(this.getMessage("SUBSCRIPTION_REQUIRED"));
			mt.setUserId(request.getUserId());
			mt.setRequestId(UUID.randomUUID());
			mt.setVaServiceFk(vaServiceFk);
			mt.setEndpointFk(endpointFk);
			mt.setPassword(endpointPassword);
			
			if (fakeKinetic) {
				logger.info("fakeKinetic: " + JsonUtil.serialize(mt, false));
			} else {
				logger.info("kineticNotifyMo: sending mt: " + JsonUtil.serialize(mt, false));
				kineticClient.sentMt(mt);
			}
		} else if ( billingEnabled && ((session.getExpireTs() == null) || (session.getExpireTs().compareTo(Instant.now())<0))) {
			MtRequest mt = new MtRequest();
			mt.setText(this.getMessage("EXPIRED_SUBSCRIPTION"));
			mt.setUserId(request.getUserId());
			mt.setRequestId(UUID.randomUUID());
			mt.setVaServiceFk(vaServiceFk);
			mt.setEndpointFk(endpointFk);
			mt.setPassword(endpointPassword);
			
			if (fakeKinetic) {
				logger.info("fakeKinetic: " + JsonUtil.serialize(mt, false));
			} else {
				logger.info("kineticNotifyMo: sending mt: " + JsonUtil.serialize(mt, false));
				kineticClient.sentMt(mt);
			}
		} else if ((session.getMemory() == null) || content.startsWith(CMD_ROOT_MENU_RANDOM.toString()) || this.changeCommand(content)) {
			
			
			
			TreeMap<Integer,Animator> anims = animService.getAnimators();
			
			if (anims.size() == 1) {
				this.setAnimator(session, 0);
				
				MtRequest mt = new MtRequest();
				mt.setText(this.getMessage("ANIM_SELECTED").replaceAll("ANIMATOR", getAnimatorLabel(session)));
				mt.setUserId(request.getUserId());
				mt.setRequestId(UUID.randomUUID());
				mt.setVaServiceFk(vaServiceFk);
				mt.setEndpointFk(endpointFk);
				mt.setPassword(endpointPassword);
				
				if (fakeKinetic) {
					logger.info("fakeKinetic: " + JsonUtil.serialize(mt, false));
				} else {
					logger.info("kineticNotifyMo: sending mt: " + JsonUtil.serialize(mt, false));

					kineticClient.sentMt(mt);
				}
				

				String result = bot.chat(session.getMemory().getMemoryId(), animService.get(0).getHello());
				

				mt = new MtRequest();
				mt.setText(result);
				mt.setUserId(request.getUserId());
				mt.setRequestId(UUID.randomUUID());
				mt.setVaServiceFk(vaServiceFk);
				mt.setEndpointFk(endpointFk);
				mt.setPassword(endpointPassword);
				
				if (fakeKinetic) {
					logger.info("fakeKinetic: " + JsonUtil.serialize(mt, false));
				} else {
					logger.info("kineticNotifyMo: sending mt: " + JsonUtil.serialize(mt, false));

					kineticClient.sentMt(mt);
				}				
			} else {
				Set<Integer> keys = anims.keySet();
				List<Integer> others = new ArrayList<Integer>();
				Integer currentAnim = null;
				if (session.getMemory() != null) {
					currentAnim = session.getMemory().getAnimator();
				}
				for (Integer current: keys) {
					if (currentAnim != null) {
						if (current.intValue() != currentAnim.intValue()) {
							others.add(current);
						}
					} else {
						others.add(current);
					}
				}
				
				Integer newAnim = others.get(random.nextInt(others.size()));
				this.setAnimator(session, newAnim);
				
				
				MtRequest mt = new MtRequest();
				mt.setText(this.getMessage("ANIM_SELECTED").replaceAll("ANIMATOR", getAnimatorLabel(session)));
				mt.setUserId(request.getUserId());
				mt.setRequestId(UUID.randomUUID());
				mt.setVaServiceFk(vaServiceFk);
				mt.setEndpointFk(endpointFk);
				mt.setPassword(endpointPassword);
				
				
				if (fakeKinetic) {
					logger.info("fakeKinetic: " + JsonUtil.serialize(mt, false));
				} else {
					logger.info("kineticNotifyMo: sending mt: " + JsonUtil.serialize(mt, false));

					kineticClient.sentMt(mt);
				}				
				String result = bot.chat(session.getMemory().getMemoryId(), animService.get(0).getHello());
				

				mt = new MtRequest();
				mt.setText(result);
				mt.setUserId(request.getUserId());
				mt.setRequestId(UUID.randomUUID());
				mt.setVaServiceFk(vaServiceFk);
				mt.setEndpointFk(endpointFk);
				mt.setPassword(endpointPassword);
				
				if (fakeKinetic) {
					logger.info("fakeKinetic: " + JsonUtil.serialize(mt, false));
				} else {
					logger.info("kineticNotifyMo: sending mt: " + JsonUtil.serialize(mt, false));

					kineticClient.sentMt(mt);
				}			
				
			}
			
			

		} else if (content.equals(CMD_ROOT_MENU_HELP.toString())) {
			MtRequest mt = new MtRequest();
			mt.setText(this.getMessage("USAGE"));
			mt.setUserId(request.getUserId());
			mt.setRequestId(UUID.randomUUID());
			mt.setVaServiceFk(vaServiceFk);
			mt.setEndpointFk(endpointFk);
			mt.setPassword(endpointPassword);
			
			if (fakeKinetic) {
				logger.info("fakeKinetic: " + JsonUtil.serialize(mt, false));
			} else {
				logger.info("kineticNotifyMo: sending mt: " + JsonUtil.serialize(mt, false));

				kineticClient.sentMt(mt);
			}
		} else if (content.equals(CMD_ROOT_MENU_CLEAR.toString())) {
			
			if (session.getMemory() != null) {
				this.resetMemory(session.getMemory().getMemoryId());
				
				
			}
			MtRequest mt = new MtRequest();
			mt.setText(this.getMessage("CLEANED_HISTORY"));
			mt.setUserId(request.getUserId());
			mt.setRequestId(UUID.randomUUID());
			mt.setVaServiceFk(vaServiceFk);
			mt.setEndpointFk(endpointFk);
			mt.setPassword(endpointPassword);
			
			
			if (fakeKinetic) {
				logger.info("fakeKinetic: " + JsonUtil.serialize(mt, false));
			} else {
				logger.info("kineticNotifyMo: sending mt: " + JsonUtil.serialize(mt, false));

				kineticClient.sentMt(mt);
			}
		}  else {
			
			content = content.strip();
			
			if (content.length()>maxUserInputLength) {
				content = content.substring(0, maxUserInputLength);
			}
			
			String result = bot.chat(session.getMemory().getMemoryId(), content);
			
			MtRequest mt = new MtRequest();
			mt.setText(result);
			mt.setUserId(request.getUserId());
			mt.setRequestId(UUID.randomUUID());
			mt.setVaServiceFk(vaServiceFk);
			mt.setEndpointFk(endpointFk);
			mt.setPassword(endpointPassword);
			
			
			if (fakeKinetic) {
				logger.info("fakeKinetic: " + JsonUtil.serialize(mt, false));
			} else {
				logger.info("kineticNotifyMo: sending mt: " + JsonUtil.serialize(mt, false));

				kineticClient.sentMt(mt);
			}			
			
		}
		
	}
}
