package com.mobiera.ai.aifriends.multichannel.chatbot.jms;


import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.twentysixty.sa.client.jms.AbstractProducer;
import io.twentysixty.sa.client.jms.ProducerInterface;
import io.twentysixty.sa.client.model.message.BaseMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;


@ApplicationScoped
public class MoProducer extends AbstractProducer<BaseMessage> implements ProducerInterface<BaseMessage> {

	@Inject
    ConnectionFactory _connectionFactory;

	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.jms.ex.delay")
	Long _exDelay;
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.jms.mo.queue.name")
	String _queueName;
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.jms.mo.producer.threads")
	Integer _threads;
	
	@ConfigProperty(name = "com.mobiera.ai.chatbot.auth.debug")
	Boolean _debug;
	
	
	
	private static final Logger logger = Logger.getLogger(MoProducer.class);
	
    
    void onStart(@Observes StartupEvent ev) {
    	logger.info("onStart: SaProducer");
    	
    	this.setExDelay(_exDelay);
		this.setDebug(_debug);
		this.setQueueName(_queueName);
		this.setThreads(_threads);
		this.setConnectionFactory(_connectionFactory);
    	this.setProducerCount(_threads);
    	
    }

    void onStop(@Observes ShutdownEvent ev) {
    	logger.info("onStop: SaProducer");
    }
 
 
    @Override
    public void sendMessage(BaseMessage message) throws Exception {
    	this.spool(message, 0);
    }

    
	
    
    
    

}