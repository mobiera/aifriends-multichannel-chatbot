package com.mobiera.ai.aifriends.multichannel.chatbot.svc;

import java.util.UUID;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService()
@ApplicationScoped
public interface ChatBot {

    String chat(@MemoryId UUID memoryId, @UserMessage String question);
}