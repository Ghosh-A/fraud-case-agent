package com.capstone.fraudcase.agent.springai;

import com.capstone.fraudcase.CapstoneProperties;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAiMemoryConfig {

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository repository, CapstoneProperties props) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(props.getMemory().getMaxMessages())
                .build();
    }
}
