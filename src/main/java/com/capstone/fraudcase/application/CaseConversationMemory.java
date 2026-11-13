package com.capstone.fraudcase.application;

import com.capstone.fraudcase.CapstoneProperties;
import com.capstone.fraudcase.persistence.entity.ChatMemoryEntity;
import com.capstone.fraudcase.persistence.repository.ChatMemoryRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Case-keyed conversation memory. Every read is scoped by caseId — a fact from
 * one case can never be recalled in another. Bounded to max-messages.
 */
@Service
public class CaseConversationMemory {

    private final ChatMemoryRepository repo;
    private final int maxMessages;

    public CaseConversationMemory(ChatMemoryRepository repo, CapstoneProperties props) {
        this.repo = repo;
        this.maxMessages = props.getMemory().getMaxMessages();
    }

    public void append(String caseId, String role, String content) {
        ChatMemoryEntity m = new ChatMemoryEntity();
        m.setCaseId(caseId);
        m.setSeq(repo.maxSeq(caseId) + 1);
        m.setRole(role);
        m.setContent(content == null ? "" : content.substring(0, Math.min(2000, content.length())));
        m.setCreatedAt(Instant.now());
        repo.save(m);
        trim(caseId);
    }

    public List<ChatMemoryEntity> messages(String caseId) {
        return repo.findByCaseIdOrderBySeqAsc(caseId);
    }

    public String renderForPrompt(String caseId) {
        List<ChatMemoryEntity> msgs = messages(caseId);
        if (msgs.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ChatMemoryEntity m : msgs) {
            sb.append(m.getRole()).append(": ").append(m.getContent()).append('\n');
        }
        return sb.toString();
    }

    private void trim(String caseId) {
        List<ChatMemoryEntity> all = repo.findByCaseIdOrderBySeqAsc(caseId);
        int excess = all.size() - maxMessages;
        for (int i = 0; i < excess; i++) {
            repo.delete(all.get(i));
        }
    }

    public int maxMessages() {
        return maxMessages;
    }
}
