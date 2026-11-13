package com.capstone.fraudcase.agent.langchain4j;

import com.capstone.fraudcase.application.CaseConversationMemory;
import com.capstone.fraudcase.persistence.entity.ChatMemoryEntity;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Bridges LangChain4j ChatMemory onto the shared case-keyed JPA store. */
@Component
public class Lc4jChatMemoryStore implements ChatMemoryStore {

    private final CaseConversationMemory store;
    private final com.capstone.fraudcase.persistence.repository.ChatMemoryRepository repo;

    public Lc4jChatMemoryStore(CaseConversationMemory store,
                               com.capstone.fraudcase.persistence.repository.ChatMemoryRepository repo) {
        this.store = store;
        this.repo = repo;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        List<ChatMessage> out = new ArrayList<>();
        for (ChatMemoryEntity e : store.messages(String.valueOf(memoryId))) {
            out.add(switch (e.getRole()) {
                case "assistant" -> new AiMessage(e.getContent());
                case "system" -> new SystemMessage(e.getContent());
                default -> new UserMessage(e.getContent());
            });
        }
        return out;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String caseId = String.valueOf(memoryId);
        repo.deleteByCaseId(caseId);
        for (ChatMessage m : messages) {
            String role;
            String text;
            if (m instanceof AiMessage ai) {
                role = "assistant";
                text = ai.text();
            } else if (m instanceof SystemMessage sys) {
                role = "system";
                text = sys.text();
            } else if (m instanceof UserMessage user) {
                role = "user";
                text = user.singleText();
            } else {
                role = "system";
                text = String.valueOf(m);
            }
            store.append(caseId, role, text);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        repo.deleteByCaseId(String.valueOf(memoryId));
    }
}
