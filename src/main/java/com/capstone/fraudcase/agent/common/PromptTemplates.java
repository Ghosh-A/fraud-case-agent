package com.capstone.fraudcase.agent.common;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Loads and renders the shared prompt contract used by both AI adapters. */
@Component
public class PromptTemplates {

    private final String systemTemplate;
    private final String userTemplate;
    private final String outputSchema;

    public PromptTemplates() {
        this.systemTemplate = load("prompts/fraud-system.txt");
        this.userTemplate = load("prompts/fraud-user.txt");
        this.outputSchema = load("schema/recommendation.schema.json");
    }

    private static String load(String path) {
        try {
            return new String(new ClassPathResource(path).getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot load " + path, e);
        }
    }

    public String systemTemplate() {
        return systemTemplate;
    }

    public String userTemplate() {
        return userTemplate;
    }

    public String outputSchema() {
        return outputSchema;
    }

    /** Simple {{var}} rendering so the same template works for both frameworks. */
    public String renderUser(FraudCaseContext context) {
        String out = userTemplate;
        for (var e : context.promptVariables().entrySet()) {
            out = out.replace("{{" + e.getKey() + "}}", String.valueOf(e.getValue()));
        }
        return out;
    }

    public String renderSystem() {
        return systemTemplate.replace("{outputSchema}", outputSchema);
    }
}
