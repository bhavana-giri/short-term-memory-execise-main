package in.vikasrajput.ai.chatbot;

import java.util.List;

public interface ChatMemory {
    void saveMessage(String conversationId, String message);
    List<String> getMessages(String conversationId);
}
