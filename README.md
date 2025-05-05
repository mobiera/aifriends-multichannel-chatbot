# AI Friends - Hologram Verifiable AI Chatbot

![AI Friends](src/main/resources/META-INF/resources/avatar.png)

A simple conversational demo service (chatbot) for chatting with AI Friends. Uses langchain and OpenAI.

See [k8s/example](k8s/example/) for a deployment example.

Provides 2 channels:

- SMS channel
- Hologram channel

When user interacts through SMS channel, a random friend is selected. User can change friend by writing /change or any keyword defined in com.mobiera.ai.chatbot.anim.random.commands. Example:

```
com.mobiera.ai.chatbot.anim.random.commands=change,cambiar
```

Both channels can be used at the same time.

## Integration with Kinetic Server Aircast Module

Support Kinetic V2 protocol. Must use kinetic-server > v2.1-pre1 to use support for MO messages by using rest and not SMPP 

```
com.mobiera.ai.chatbot.msisdn.prefix=51
com.mobiera.ai.chatbot.msisdn.minlength=10
com.mobiera.ai.chatbot.msisdn.maxlength=11

com.mobiera.ai.chatbot.billing.enabled=true
com.mobiera.ai.chatbot.vaservicefk=5678
com.mobiera.ai.chatbot.endpointfk=1234
com.mobiera.ai.chatbot.password=password

com.mobiera.ai.aifriends.multichannel.chatbot.res.c.KineticClient/mp-rest/url=http://kinetic:8181/
```

## Enabling msisdn authentication for DIDComm AI Agent (required when using service with Hologram)

Sends a PIN by SMS to user to verify its phone number

```
com.mobiera.ai.chatbot.auth.enabled=true
```

## Activating Kinetic billing

```
com.mobiera.ai.chatbot.billing.enabled=true
```

## Setting OpenAI token

```
quarkus.langchain4j.openai.api-key=sk...
```