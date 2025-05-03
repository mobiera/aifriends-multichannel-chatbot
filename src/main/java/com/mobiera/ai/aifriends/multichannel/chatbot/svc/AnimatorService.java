package com.mobiera.ai.aifriends.multichannel.chatbot.svc;

import java.util.Optional;
import java.util.TreeMap;

import org.eclipse.microprofile.config.ConfigProvider;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AnimatorService {

	
	private TreeMap<Integer, Animator> animators = null;
	private static Object lockObj = new Object();
	
	
	public TreeMap<Integer, Animator> getAnimators() {
		
		synchronized (lockObj) {
			if (animators == null) {
				Integer maxAnims = ConfigProvider.getConfig().getValue("com.mobiera.ai.chatbot.anim.maxcount", Integer.class);
				animators = new TreeMap<Integer, Animator>();
				
				for (int i=0; i<maxAnims; i++) {
					Optional<Integer> age = ConfigProvider.getConfig().getOptionalValue("com.mobiera.ai.chatbot.anim.\"a" + i + "\".age", Integer.class);
					Optional<String> place = ConfigProvider.getConfig().getOptionalValue("com.mobiera.ai.chatbot.anim.\"a" + i + "\".place", String.class);
					Optional<String> language = ConfigProvider.getConfig().getOptionalValue("com.mobiera.ai.chatbot.anim.\"a" + i + "\".language", String.class);
					Optional<String> prompt = ConfigProvider.getConfig().getOptionalValue("com.mobiera.ai.chatbot.anim.\"a" + i + "\".prompt", String.class);
					Optional<String> name = ConfigProvider.getConfig().getOptionalValue("com.mobiera.ai.chatbot.anim.\"a" + i + "\".name", String.class);
					Optional<String> hello = ConfigProvider.getConfig().getOptionalValue("com.mobiera.ai.chatbot.anim.\"a" + i + "\".hello", String.class);
					
					if (	age.isPresent() 
							&& place.isPresent() && (place.get().length()>0)
							&& language.isPresent() && (language.get().length()>0)
							&& prompt.isPresent() && (prompt.get().length()>0)
							&& name.isPresent() && (name.get().length()>0)
							&& hello.isPresent() && (hello.get().length()>0)
							
							
								) {
						Animator a = Animator.create()
								.setAge(age.get())
								.setLanguage(language.get())
								.setPrompt(prompt.get())
								.setName(name.get())
								.setPlace(place.get())
								.setHello(hello.get());
						
						animators.put(i, a);	
								
					}
						
				}
				
			}
		}
		
		return animators;
		
	}
	
	public Animator get(int a) {
		return this.getAnimators().get(a);
	}
}
