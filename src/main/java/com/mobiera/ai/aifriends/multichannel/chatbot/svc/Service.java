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
import com.mobiera.ai.aifriends.multichannel.chatbot.res.c.MediaResource;
import com.mobiera.aircast.api.adsafe.MoRequest;
import com.mobiera.aircast.api.adsafe.MtRequest;
import com.mobiera.aircast.commons.enums.ParameterName;
import com.mobiera.ms.mno.api.json.ChargingEvent;
import com.mobiera.ms.mno.api.json.SubscriptionEvent;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.SystemMessage;
import io.twentysixty.sa.client.model.credential.CredentialType;
import io.twentysixty.sa.client.model.message.BaseMessage;
import io.twentysixty.sa.client.model.message.Claim;
import io.twentysixty.sa.client.model.message.ContextualMenuItem;
import io.twentysixty.sa.client.model.message.ContextualMenuSelect;
import io.twentysixty.sa.client.model.message.ContextualMenuUpdate;
import io.twentysixty.sa.client.model.message.IdentityProofRequestMessage;
import io.twentysixty.sa.client.model.message.IdentityProofSubmitMessage;
import io.twentysixty.sa.client.model.message.InvitationMessage;
import io.twentysixty.sa.client.model.message.MediaItem;
import io.twentysixty.sa.client.model.message.MediaMessage;
import io.twentysixty.sa.client.model.message.MenuDisplayMessage;
import io.twentysixty.sa.client.model.message.MenuItem;
import io.twentysixty.sa.client.model.message.MenuSelectMessage;
import io.twentysixty.sa.client.model.message.RequestedProofItem;
import io.twentysixty.sa.client.model.message.SubmitProofItem;
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
	@Inject MediaResource mediaResource;
	
	@RestClient
	@Inject KineticClient kineticClient;
	
	
	@Inject MtProducer mtProducer;
	
	@Inject ChatBot bot;

	
	@RestClient
	@Inject CredentialTypeResource credentialTypeResource;
	
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.debug")
	Boolean debug;
	
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.credential_issuer")
	String credentialIssuer;
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.credential_issuer.avatar")
	String invitationImageUrl;
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.credential_issuer.label")
	String invitationLabel;
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.enabled")
	Boolean authEnabled;
	
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.id_credential_def")
	String credDef;
	
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

	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.request.citizenid")
	Boolean requestCitizenId;

	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.request.firstname")
	Boolean requestFirstname;

	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.request.lastname")
	Boolean requestLastname;

	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.request.photo")
	Boolean requestPhoto;

	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.request.avatarname")
	Boolean requestAvatarname;

		
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.language")
	Optional<String> language;

	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.vision.face.verification.url")
	String faceVerificationUrl;
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.vision.redirdomain")
	Optional<String> redirDomain;
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.vision.redirdomain.q")
	Optional<String> qRedirDomain;
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.vision.redirdomain.d")
	Optional<String> dRedirDomain;
	
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
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.vaservicefk")
	Long vaServiceFk;
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
		return TextMessage.build(connectionId, threadId, this.getMessage("ENTER_PHONE_NUMBER"));
	}


	private BaseMessage getPinAuthRequest(UUID connectionId, UUID threadId) {
		return TextMessage.build(connectionId, threadId, this.getMessage("ENTER_PIN"));
	}

	private BaseMessage getInvalidPin(UUID connectionId, UUID threadId) {
		return TextMessage.build(connectionId, threadId, this.getMessage("INVALID_PIN"));
	}


	private Session getSessionWIthConnectionId(UUID connectionId) {
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
	
	private Session getSessionWithMsisdn(String msisdn) {
		Query q = em.createNamedQuery("Session.findWithMsisdn");
		q.setParameter("msisdn", msisdn);
		Session session = (Session) q.getResultList().stream().findFirst().orElse(null);
		
		if (session == null) {
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
	
	private String buildVisionUrl(String url) {
		
		if(redirDomain.isPresent()) {
			url = url + "&rd=" +  redirDomain.get();
		}
		if(qRedirDomain.isPresent()) {
			url = url + "&q=" +  qRedirDomain.get();
		}
		if(dRedirDomain.isPresent()) {
			url = url + "&d=" +  dRedirDomain.get();
		}
		if (language.isPresent()) {
			url = url + "&lang=" +  language.get();
		}
		
		return url;
	}
	private BaseMessage generateFaceVerificationMediaMessage(UUID connectionId, UUID threadId, String token) {
		String url = faceVerificationUrl.replaceFirst("TOKEN", token);
		url = this.buildVisionUrl(url);
		
		MediaItem mi = new MediaItem();
		mi.setMimeType("text/html");
		mi.setUri(url);
		mi.setTitle(getMessage("FACE_VERIFICATION_HEADER"));
		mi.setDescription(getMessage("FACE_VERIFICATION_DESC"));
		mi.setOpeningMode("normal");
		List<MediaItem> mis = new ArrayList<MediaItem>();
		mis.add(mi);
		MediaMessage mm = new MediaMessage();
		mm.setConnectionId(connectionId);
		mm.setThreadId(threadId);
		mm.setDescription(getMessage("FACE_VERIFICATION_DESC"));
		mm.setItems(mis);
		return mm;
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
		
		Session session = this.getSessionWIthConnectionId(message.getConnectionId());
		
		
		
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
			
			if (content.length()>maxUserInputLength) {
				content = content.substring(0, maxUserInputLength);
			}
					
			if (content.equals(CMD_ROOT_MENU_AUTHENTICATE.toString())) {
				session.setVerifyingMsisdn(null);
				session.setVerifyingPin(null);
				mtProducer.sendMessage(this.getMsisdnAuthRequest(message.getConnectionId(), message.getThreadId()));
			} else if (content.equals(CMD_ROOT_MENU_OPTION1.toString())) {
				mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId() , OPTION1_MSG));

			} else if (content.equals(CMD_ROOT_MENU_LOGOUT.toString())) {
				if (session != null) {
					session.setAuthTs(null);
					session.setMsisdn(null);
					session = em.merge(session);
				}
				mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId() , this.getMessage("UNAUTHENTICATED")));

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
					if (session.getMemory() == null) {
						mtProducer.sendMessage(this.getAnimatorMenu(message.getConnectionId(), message.getThreadId()));
					} else {
						
						String result = bot.chat(session.getMemory().getMemoryId(), content);
						mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), result));
					}
				}
				
				
				
			} else {
				
				// try to get MSISDN
				
				if (session.getVerifyingMsisdn() != null) {
					// expecting pin
					if (session.getVerifyingPin().equals(content)) {
						
						// 
						
						this.successfullAuth(session, message.getConnectionId(), message.getThreadId());
						
					} else {
						mtProducer.sendMessage(this.getInvalidPin(message.getConnectionId(), message.getThreadId()));
					}
				} else if (this.isValidMsisdn(content)) {
					session.setVerifyingMsisdn(content);
					mtProducer.sendMessage(this.getPinAuthRequest(message.getConnectionId(), message.getThreadId()));
					
				} else {
					mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId() , this.getMessage("ERROR_NOT_AUTHENTICATED")));

				}
				
			}
			
			
		}
		mtProducer.sendMessage(this.getRootMenu(message.getConnectionId(), session));
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
		
		
		Session msisdnSession = this.getSessionWithMsisdn(session.getVerifyingMsisdn());
		
		
		if (msisdnSession == null) {
			session.setMsisdn(session.getVerifyingMsisdn());
			session.setVerifyingMsisdn(null);
			session.setVerifyingPin(null);
			session.setAuthTs(Instant.now());
			session = em.merge(session);
			
		} else if (msisdnSession.getId().equals(session.getId())) {
			session.setVerifyingMsisdn(null);
			session.setVerifyingPin(null);
			session.setAuthTs(Instant.now());
			session = em.merge(session);
		} else {
			session.setMsisdn(session.getVerifyingMsisdn());
			session.setVerifyingMsisdn(null);
			session.setVerifyingPin(null);
			session.setAuthTs(Instant.now());
			session = em.merge(session);
			
			msisdnSession.setMsisdn(null);
			msisdnSession.setAuthTs(null);
			
			msisdnSession = em.merge(msisdnSession);
		}
		
		if (AUTH_SUCCESS.isPresent()) {
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId , AUTH_SUCCESS.get()));
		} else {
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId , this.getMessage("AUTHENTICATION_SUCCESS")));
		}
		//mtProducer.sendMessage(TextMessage.build(connectionId, null , this.getMessage("SET_MODEL").replaceAll("MODEL", session.getModel())));
		mtProducer.sendMessage(this.getAnimatorMenu(connectionId, null));
		
		
		
	}

	
	
	public BaseMessage getAnimatorMenu(UUID connectionId, UUID threadId) {
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
			memory.setSessionId(session.getId());
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
			if (ROOT_MENU_NO_CRED.isPresent()) {
				options.add(ContextualMenuItem.build(CMD_ROOT_MENU_NO_CRED, ROOT_MENU_NO_CRED.get(), null));
			} else {
				options.add(ContextualMenuItem.build(CMD_ROOT_MENU_NO_CRED, getMessage("ROOT_MENU_NO_CRED"), null));
			}
			
			
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



	public void kineticNotifyPhoneCharging(ChargingEvent event) {
		Session session = this.getSessionWithMsisdn(event.getUserTpda());
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



	public void kineticNotifyPhoneSubscription(SubscriptionEvent event) {
		
		Session session = this.getSessionWithMsisdn(event.getUserTpda());
		
		
		switch (event.getSubscriptionEventType()) {
		case SUBSCRIBED: {
			session.setCanceledTs(null);
			session.setSubscriptionTs(Instant.now());
			session = em.merge(session);
			
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
			mt.setRequestId(UUID.randomUUID());
			mt.setVaServiceFk(vaServiceFk);
			mt.setEndpointFk(endpointFk);
			mt.setPassword(endpointPassword);
			
			
			kineticClient.sentMt(mt);
			
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



	public void kineticNotifyMo(MoRequest request) {
		
		if (!this.isValidMsisdn(request.getUserId())) {
			logger.error("kineticNotifyMo: ignoring invalid msisdn: " + request.getUserId() + " " + request.getText());
			return;
		}
		Session session = this.getSessionWithMsisdn(request.getUserId());
		
		String content = request.getText();
		
		if ( authEnabled && (session.getExpireTs() == null) || (session.getExpireTs().compareTo(Instant.now())<0)) {
			MtRequest mt = new MtRequest();
			mt.setText(this.getMessage("EXPIRED_SUBSCRIPTION"));
			mt.setUserId(request.getUserId());
			mt.setRequestId(UUID.randomUUID());
			mt.setVaServiceFk(vaServiceFk);
			mt.setEndpointFk(endpointFk);
			mt.setPassword(endpointPassword);
			
			
			kineticClient.sentMt(mt);
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
				
				
				kineticClient.sentMt(mt);
				

				String result = bot.chat(session.getMemory().getMemoryId(), animService.get(0).getHello());
				

				mt = new MtRequest();
				mt.setText(result);
				mt.setUserId(request.getUserId());
				mt.setRequestId(UUID.randomUUID());
				mt.setVaServiceFk(vaServiceFk);
				mt.setEndpointFk(endpointFk);
				mt.setPassword(endpointPassword);
				
				kineticClient.sentMt(mt);
				
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
				
				
				kineticClient.sentMt(mt);
				
				String result = bot.chat(session.getMemory().getMemoryId(), animService.get(0).getHello());
				

				mt = new MtRequest();
				mt.setText(result);
				mt.setUserId(request.getUserId());
				mt.setRequestId(UUID.randomUUID());
				mt.setVaServiceFk(vaServiceFk);
				mt.setEndpointFk(endpointFk);
				mt.setPassword(endpointPassword);
				
				kineticClient.sentMt(mt);
			}
			
			

		} else if (content.equals(CMD_ROOT_MENU_HELP.toString())) {
			MtRequest mt = new MtRequest();
			mt.setText(this.getMessage("USAGE"));
			mt.setUserId(request.getUserId());
			mt.setRequestId(UUID.randomUUID());
			mt.setVaServiceFk(vaServiceFk);
			mt.setEndpointFk(endpointFk);
			mt.setPassword(endpointPassword);
			
			kineticClient.sentMt(mt);

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
			
			
			kineticClient.sentMt(mt);

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
			
			
			kineticClient.sentMt(mt);
			
			
		}
		
	}
}
