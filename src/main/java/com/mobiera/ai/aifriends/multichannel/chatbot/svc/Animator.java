package com.mobiera.ai.aifriends.multichannel.chatbot.svc;

public class Animator {

	private int age;
	private String name;
	private String language;
	private String prompt;
	private String place;
	private String hello;
	
	public static Animator create() {
		return new Animator();
	}
	public int getAge() {
		return age;
	}
	public Animator setAge(int age) {
		this.age = age;
		return this;
	}
	public String getName() {
		return name;
	}
	public Animator setName(String name) {
		this.name = name;
		return this;
	}
	public String getLanguage() {
		return language;
	}
	public Animator setLanguage(String language) {
		this.language = language;
		return this;
	}
	public String getPrompt() {
		return prompt;
	}
	public Animator setPrompt(String prompt) {
		this.prompt = prompt;
		return this;
	}
	public String getPlace() {
		return place;
	}
	public Animator setPlace(String place) {
		this.place = place;
		return this;
	}
	
	public String getSummary() {
		return (this.getName() + ", " + this.getAge() + ", " + this.getPlace() + " (" + this.getLanguage() + ")");
	}
	public String getHello() {
		return hello;
	}
	public Animator setHello(String hello) {
		this.hello = hello;
		return this;
	}
	
}
