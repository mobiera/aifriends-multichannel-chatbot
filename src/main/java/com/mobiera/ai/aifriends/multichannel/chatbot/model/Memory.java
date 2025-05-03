package com.mobiera.ai.aifriends.multichannel.chatbot.model;

import java.util.UUID;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name="memory", indexes = {
        @Index(name = "memory_idx", columnList = "animator, connectionId")})
@DynamicUpdate
@DynamicInsert
@NamedQueries({
	@NamedQuery(name="Session.findMemory", query="SELECT m FROM Memory m where m.animator=:animator and m.connectionId=:connectionId "),
})
public class Memory {

	
	@Id
	private UUID memoryId;
	
	private int animator;
	public int getAnimator() {
		return animator;
	}

	public void setAnimator(int animator) {
		this.animator = animator;
	}

	private UUID sessionId;
	
	@Column(columnDefinition="text")
	private String memory;

	public UUID getMemoryId() {
		return memoryId;
	}

	public void setMemoryId(UUID memoryId) {
		this.memoryId = memoryId;
	}

	

	public String getMemory() {
		return memory;
	}

	public void setMemory(String memory) {
		this.memory = memory;
	}

	public UUID getSessionId() {
		return sessionId;
	}

	public void setSessionId(UUID sessionId) {
		this.sessionId = sessionId;
	}
	
		
}
