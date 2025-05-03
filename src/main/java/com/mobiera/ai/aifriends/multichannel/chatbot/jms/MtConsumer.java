package com.mobiera.ai.aifriends.multichannel.chatbot.jms;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.twentysixty.sa.client.jms.AbstractConsumer;
import io.twentysixty.sa.client.jms.ConsumerInterface;
import io.twentysixty.sa.client.model.message.BaseMessage;
import io.twentysixty.sa.res.c.MessageResource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;

@ApplicationScoped
public class MtConsumer extends AbstractConsumer<BaseMessage> implements ConsumerInterface<BaseMessage> {

	@RestClient
	@Inject
	MessageResource messageResource;
	

	@Inject
    ConnectionFactory _connectionFactory;

	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.jms.ex.delay")
	Long _exDelay;
	
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.jms.mt.queue.name")
	String _queueName;
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.jms.mt.consumer.threads")
	Integer _threads;
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.debug")
	Boolean _debug;
	
	
	private static final Logger logger = Logger.getLogger(MtConsumer.class);

	
	
	void onStart(@Observes StartupEvent ev) {
    	
		logger.info("onStart: BeConsumer queueName: " + _queueName);
		
		this.setExDelay(_exDelay);
		this.setDebug(_debug);
		this.setQueueName(_queueName);
		this.setThreads(_threads);
		this.setConnectionFactory(_connectionFactory);
		super._onStart();
		
    }

    void onStop(@Observes ShutdownEvent ev) {
    	
    	logger.info("onStop: BeConsumer");
		
    	
    	super._onStop();
    	
    }
	
    @Override
	public void receiveMessage(BaseMessage message) throws Exception {
		
		messageResource.sendMessage(message);
		
	}

	
}
