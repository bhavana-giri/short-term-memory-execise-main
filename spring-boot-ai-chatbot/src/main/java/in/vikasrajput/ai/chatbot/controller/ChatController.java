package in.vikasrajput.ai.chatbot.controller;

import in.vikasrajput.ai.chatbot.RedisChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@RestController
public class ChatController {

    private final OpenAiChatModel chatModel;
    private final RedisChatMemory chatMemory;

    @Autowired
    public ChatController(OpenAiChatModel chatModel, RedisChatMemory chatMemory) {
        this.chatModel = chatModel;
        this.chatMemory = chatMemory;
    }

  //  @GetMapping("/ai/chat/string")
    public Flux<String> streamResponse(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return chatModel.stream(message);
    }

    @GetMapping("/ai/chat/string")
    public Flux<String> streamResponseWithMemory(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        String CONVERSATION_ID = "conversation_id_1";

        // Retrieve previous conversation history from memory
        List<String> history = chatMemory.getMessages(CONVERSATION_ID);
        List<Message> promptMessages = new ArrayList<>();

        // Convert stored strings into appropriate Message types for prompt reconstruction
        for (String msg : history) {
            if (msg.startsWith("User: ")) {
                promptMessages.add(new UserMessage(msg.replaceFirst("User: ", "")));
            } else if (msg.startsWith("Assistant: ")) {
                promptMessages.add(new AssistantMessage(msg.replaceFirst("Assistant: ", "")));
            }
        }

        // Add the current user message to the prompt
        UserMessage userMessage = new UserMessage(message);
        promptMessages.add(userMessage);
        // Save the user message to memory
        chatMemory.saveMessage(CONVERSATION_ID, "User: " + message);

        Prompt prompt = new Prompt(promptMessages);

        Flux<String> replyChunks = chatModel.stream(prompt)
            .flatMap(response -> {
                if (response != null &&
                    response.getResult() != null &&
                    response.getResult().getOutput() != null &&
                    response.getResult().getOutput().getText() != null) {
                    return Flux.just(response.getResult().getOutput().getText());
            }
            return Flux.empty();
            });

        replyChunks
            .collectList()
            .map(chunks -> String.join("", chunks))
            .doOnNext(fullReply -> chatMemory.saveMessage(CONVERSATION_ID, "Assistant: " + fullReply))
            .subscribe();

        return replyChunks;

}


}
