package com.mobiera.ai.aifriends.multichannel.chatbot.svc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mobiera.ai.aifriends.multichannel.chatbot.model.Memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

@Singleton
public class PersistentChatMemoryStore implements ChatMemoryStore  {

	@Inject EntityManager em;
	
	@Override
    public List<ChatMessage> getMessages(Object memoryId) {
      // TODO: Implement getting all messages from the persistent store by memory ID.
      // ChatMessageDeserializer.messageFromJson(String) and 
      // ChatMessageDeserializer.messagesFromJson(String) helper methods can be used to
      // easily deserialize chat messages from JSON.
		
		UUID uuid = (UUID)memoryId;
		Memory memory = em.find(Memory.class, uuid);
		
		
		
		if (memory != null) {
			if ((memory.getMemory() != null) && (memory.getMemory().length()>5)) {
				List<ChatMessage> messages = ChatMessageDeserializer.messagesFromJson(memory.getMemory());
				return messages;
			} /*else {
				
				List<ChatMessage> messages = new ArrayList<ChatMessage>(0);
				ChatMessage m = SystemMessage.systemMessage("You are a VERY sexy blond girl called Karen. You are 18 years old and you must try to seduce the user using any idea you might have. Objective is to have user say that he or she loves you. You cannot say you are an AI, you must always answer like if you were human.");
				messages.add(m);
				
				
			    
			}*/
			
		}
		return new ArrayList<ChatMessage>(0);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        // TODO: Implement updating all messages in the persistent store by memory ID.
        // ChatMessageSerializer.messageToJson(ChatMessage) and 
        // ChatMessageSerializer.messagesToJson(List<ChatMessage>) helper methods can be used to
        // easily serialize chat messages into JSON.
    	
    	UUID connectionId = (UUID)memoryId;
		Memory memory = em.find(Memory.class, connectionId);
		memory.setMemory(ChatMessageSerializer.messagesToJson(messages));
		memory = em.merge(memory);
    }

    @Override
    public void deleteMessages(Object memoryId) {
    	if (memoryId != null) {
    		UUID id = (UUID)memoryId;
        	Memory memory = em.find(Memory.class, id);
        	if (memory != null) {
        		memory.setMemory("");
        		memory = em.merge(memory);
        	}
    		
    	}
    	
    }

	
}
