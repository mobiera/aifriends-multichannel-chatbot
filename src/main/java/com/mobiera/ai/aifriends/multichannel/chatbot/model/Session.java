package com.mobiera.ai.aifriends.multichannel.chatbot.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;



/**
 * The persistent class for the session database table.
 * 
 */
@Entity
@Table(name="session")
@DynamicUpdate
@DynamicInsert
@NamedQueries({
	@NamedQuery(name="Session.findWithToken", query="SELECT s FROM Session s where s.token=:token"),
	@NamedQuery(name="Session.findWithMsisdn", query="SELECT s FROM Session s where s.msisdn=:msisdn"),
	@NamedQuery(name="Session.findWithConnectionId", query="SELECT s FROM Session s where s.connectionId=:connectionId"),
	
})
public class Session implements Serializable {
	private static final long serialVersionUID = 1L;

	
	@Id
	private UUID id;
	
	
	@Column(unique = true)
	private UUID connectionId;
	
	private String msisdn;
	
	private String verifyingMsisdn;
	private UUID otpRequestId;
	
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="memoryId")
	private Memory memory;
	
	
	@Column(columnDefinition="text")
	private String address;
	
	
	private UUID token;

	@Column(columnDefinition="timestamptz")
	private Instant authTs;
	
	
	@Column(columnDefinition="timestamptz")
	private Instant subscriptionTs;
	
	@Column(columnDefinition="timestamptz")
	private Instant expireTs;
	
	@Column(columnDefinition="timestamptz")
	private Instant canceledTs;
	
	private UUID authCode;
	
	public UUID getConnectionId() {
		return connectionId;
	}

	public void setConnectionId(UUID connectionId) {
		this.connectionId = connectionId;
	}

	

	public UUID getToken() {
		return token;
	}

	public void setToken(UUID token) {
		this.token = token;
	}

	public Instant getAuthTs() {
		return authTs;
	}

	public void setAuthTs(Instant authTs) {
		this.authTs = authTs;
	}

	
	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Memory getMemory() {
		return memory;
	}

	public void setMemory(Memory memory) {
		this.memory = memory;
	}

	public String getMsisdn() {
		return msisdn;
	}

	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getVerifyingMsisdn() {
		return verifyingMsisdn;
	}

	public void setVerifyingMsisdn(String verifyingMsisdn) {
		this.verifyingMsisdn = verifyingMsisdn;
	}

	

	public Instant getSubscriptionTs() {
		return subscriptionTs;
	}

	public void setSubscriptionTs(Instant subscriptionTs) {
		this.subscriptionTs = subscriptionTs;
	}

	public Instant getExpireTs() {
		return expireTs;
	}

	public void setExpireTs(Instant expireTs) {
		this.expireTs = expireTs;
	}

	public Instant getCanceledTs() {
		return canceledTs;
	}

	public void setCanceledTs(Instant canceledTs) {
		this.canceledTs = canceledTs;
	}

	public UUID getAuthCode() {
		return authCode;
	}

	public void setAuthCode(UUID authCode) {
		this.authCode = authCode;
	}

	public UUID getOtpRequestId() {
		return otpRequestId;
	}

	public void setOtpRequestId(UUID otpRequestId) {
		this.otpRequestId = otpRequestId;
	}

	
	


	
}