package com.mobiera.ai.aifriends.multichannel.chatbot.jms;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.mobiera.ai.aifriends.multichannel.chatbot.svc.Service;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.twentysixty.sa.client.jms.AbstractConsumer;
import io.twentysixty.sa.client.jms.ConsumerInterface;
import io.twentysixty.sa.client.model.message.BaseMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;

@ApplicationScoped
public class MoConsumer extends AbstractConsumer<BaseMessage> implements ConsumerInterface<BaseMessage> {

	@Inject Service gaiaService;

	@Inject
    ConnectionFactory _connectionFactory;

	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.jms.ex.delay")
	Long _exDelay;
	
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.jms.mo.queue.name")
	String _queueName;
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.jms.mo.consumer.threads")
	Integer _threads;
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.debug")
	Boolean _debug;
	
	private static final Logger logger = Logger.getLogger(MoConsumer.class);
	
	
	void onStart(@Observes StartupEvent ev) {
    	
		logger.info("onStart: SaConsumer queueName: " + _queueName);
		
		this.setExDelay(_exDelay);
		this.setDebug(_debug);
		this.setQueueName(_queueName);
		this.setThreads(_threads);
		this.setConnectionFactory(_connectionFactory);
		super._onStart();
		
    }

    void onStop(@Observes ShutdownEvent ev) {
    	
    	logger.info("onStop: SaConsumer");
    	
    	super._onStop();
    	
    }
	
    @Override
	public void receiveMessage(BaseMessage message) throws Exception {
		
		gaiaService.userInput(message);
		
	}

	
}
