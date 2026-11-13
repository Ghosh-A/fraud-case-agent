package com.capstone.fraudcase;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "capstone")
public class CapstoneProperties {

    /** rules | spring-ai | langchain4j */
    private String processorMode = "rules";
    private boolean humanReviewRequired = true;
    private boolean allowFinancialActions = false;
    private boolean allowCustomerContact = false;
    private String investigationGoal = "Triage the fraud or dispute report.";
    private Retrieval retrieval = new Retrieval();
    private Memory memory = new Memory();
    private Tools tools = new Tools();

    public String getProcessorMode() {
        return processorMode;
    }

    public void setProcessorMode(String processorMode) {
        this.processorMode = processorMode;
    }

    public boolean isHumanReviewRequired() {
        return humanReviewRequired;
    }

    public void setHumanReviewRequired(boolean humanReviewRequired) {
        this.humanReviewRequired = humanReviewRequired;
    }

    public boolean isAllowFinancialActions() {
        return allowFinancialActions;
    }

    public void setAllowFinancialActions(boolean allowFinancialActions) {
        this.allowFinancialActions = allowFinancialActions;
    }

    public boolean isAllowCustomerContact() {
        return allowCustomerContact;
    }

    public void setAllowCustomerContact(boolean allowCustomerContact) {
        this.allowCustomerContact = allowCustomerContact;
    }

    public String getInvestigationGoal() {
        return investigationGoal;
    }

    public void setInvestigationGoal(String investigationGoal) {
        this.investigationGoal = investigationGoal;
    }

    public Retrieval getRetrieval() {
        return retrieval;
    }

    public void setRetrieval(Retrieval retrieval) {
        this.retrieval = retrieval;
    }

    public Memory getMemory() {
        return memory;
    }

    public void setMemory(Memory memory) {
        this.memory = memory;
    }

    public Tools getTools() {
        return tools;
    }

    public void setTools(Tools tools) {
        this.tools = tools;
    }

    public static class Retrieval {
        private int topK = 4;
        private double similarityThreshold = 0.15;

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public double getSimilarityThreshold() {
            return similarityThreshold;
        }

        public void setSimilarityThreshold(double similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
        }
    }

    public static class Memory {
        private int maxMessages = 12;

        public int getMaxMessages() {
            return maxMessages;
        }

        public void setMaxMessages(int maxMessages) {
            this.maxMessages = maxMessages;
        }
    }

    public static class Tools {
        private long timeoutMs = 2500;
        private String deterministicSeed = "fraud-training-v1";

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public String getDeterministicSeed() {
            return deterministicSeed;
        }

        public void setDeterministicSeed(String deterministicSeed) {
            this.deterministicSeed = deterministicSeed;
        }
    }
}
