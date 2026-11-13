package com.capstone.fraudcase.agent.springai;

import com.capstone.fraudcase.application.CaseConversationMemory;
import com.capstone.fraudcase.persistence.entity.ChatMemoryEntity;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

/** Bridges Spring AI chat memory onto the shared case-keyed JPA store. */
@Component
public class SpringAiChatMemoryRepository implements ChatMemoryRepository {

    private final CaseConversationMemory store;
    private final com.capstone.fraudcase.persistence.repository.ChatMemoryRepository repo;

    public SpringAiChatMemoryRepository(CaseConversationMemory store,
                                        com.capstone.fraudcase.persistence.repository.ChatMemoryRepository repo) {
        this.store = store;
        this.repo = repo;
    }

    @Override
    public List<String> findConversationIds() {
        return repo.findAll().stream().map(ChatMemoryEntity::getCaseId).distinct().toList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        List<Message> out = new ArrayList<>();
        for (ChatMemoryEntity e : store.messages(conversationId)) {
            out.add(switch (e.getRole()) {
                case "assistant" -> new AssistantMessage(e.getContent());
                case "system" -> new SystemMessage(e.getContent());
                default -> new UserMessage(e.getContent());
            });
        }
        return out;
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        repo.deleteByCaseId(conversationId);
        for (Message m : messages) {
            String role = m.getMessageType() == MessageType.ASSISTANT ? "assistant"
                    : m.getMessageType() == MessageType.SYSTEM ? "system" : "user";
            store.append(conversationId, role, m.getText());
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        repo.deleteByCaseId(conversationId);
    }
}
