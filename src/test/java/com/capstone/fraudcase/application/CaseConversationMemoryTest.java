package com.capstone.fraudcase.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.capstone.fraudcase.CapstoneProperties;
import com.capstone.fraudcase.persistence.repository.ChatMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** T10 — memory is bounded and strictly isolated per case. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@org.springframework.test.context.ActiveProfiles("test")
class CaseConversationMemoryTest {

    @Autowired
    private ChatMemoryRepository repo;

    private CaseConversationMemory memory;

    @BeforeEach
    void setup() {
        memory = new CaseConversationMemory(repo, new CapstoneProperties());
    }

    @Test
    void boundedWindowEvictsOldest() {
        int max = memory.maxMessages();
        for (int i = 0; i < max + 5; i++) {
            memory.append("CASE-A", "user", "msg-" + i);
        }
        assertThat(memory.messages("CASE-A")).hasSize(max);
        assertThat(memory.messages("CASE-A").get(0).getContent()).isEqualTo("msg-5");
    }

    @Test
    void caseACannotRecallCaseB() {
        memory.append("CASE-A", "user", "secret-fact-about-case-A");
        memory.append("CASE-B", "user", "unrelated");
        assertThat(memory.messages("CASE-B"))
                .noneMatch(m -> m.getContent().contains("secret-fact-about-case-A"));
        assertThat(memory.renderForPrompt("CASE-B")).doesNotContain("secret-fact-about-case-A");
    }

    @Test
    void persistsAcrossInstances() {
        memory.append("CASE-P", "user", "persist me");
        CaseConversationMemory second = new CaseConversationMemory(repo, new CapstoneProperties());
        assertThat(second.messages("CASE-P")).hasSize(1);
    }
}
