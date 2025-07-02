package in.vikasrajput.ai.chatbot;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisChatMemory implements ChatMemory {

    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public RedisChatMemory(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String key(String conversationId) {
        return "chat:" + conversationId;
    }

    @Override
    public void saveMessage(String conversationId, String message) {
        redisTemplate.opsForList().rightPush(key(conversationId), message);
    }

    @Override
    public List<String> getMessages(String conversationId) {
        return redisTemplate.opsForList().range(key(conversationId), 0, -1);
    }
}
