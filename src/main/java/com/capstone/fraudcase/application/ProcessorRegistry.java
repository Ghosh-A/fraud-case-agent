package com.capstone.fraudcase.application;

import com.capstone.fraudcase.CapstoneProperties;
import com.capstone.fraudcase.agent.common.FraudCaseProcessor;
import com.capstone.fraudcase.agent.common.ProcessorUnavailableException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Selects the FraudCaseProcessor for the configured capstone.processor-mode. */
@Component
public class ProcessorRegistry {

    private final Map<String, FraudCaseProcessor> byMode = new HashMap<>();
    private final String configuredMode;

    public ProcessorRegistry(List<FraudCaseProcessor> processors, CapstoneProperties props) {
        for (FraudCaseProcessor p : processors) {
            byMode.put(p.mode(), p);
        }
        this.configuredMode = props.getProcessorMode();
    }

    public FraudCaseProcessor select() {
        FraudCaseProcessor p = byMode.get(configuredMode);
        if (p == null) {
            throw new ProcessorUnavailableException(
                    "processor mode '" + configuredMode + "' is not registered; available=" + byMode.keySet());
        }
        return p;
    }

    public FraudCaseProcessor rulesFallback() {
        return byMode.get("rules");
    }

    public String configuredMode() {
        return configuredMode;
    }
}
