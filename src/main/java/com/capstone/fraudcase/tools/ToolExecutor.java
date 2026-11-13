package com.capstone.fraudcase.tools;

import com.capstone.fraudcase.CapstoneProperties;
import com.capstone.fraudcase.common.Hashing;
import com.capstone.fraudcase.common.Ids;
import com.capstone.fraudcase.domain.ToolStatus;
import com.capstone.fraudcase.persistence.entity.ToolTraceEntity;
import com.capstone.fraudcase.persistence.repository.ToolTraceRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Runs an allow-listed tool with a bounded timeout, records a redacted
 * ToolTrace (request hash, status, duration, summary) and converts failures
 * into structured unavailable results — never throwing internals to callers.
 */
@Component
public class ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);

    private final ToolTraceRepository traces;
    private final long timeoutMs;
    private final MeterRegistry meterRegistry;
    private final ExecutorService pool = Executors.newCachedThreadPool();

    @Autowired
    public ToolExecutor(ToolTraceRepository traces, CapstoneProperties props,
                        MeterRegistry meterRegistry) {
        this.traces = traces;
        this.timeoutMs = props.getTools().getTimeoutMs();
        this.meterRegistry = meterRegistry != null ? meterRegistry : new SimpleMeterRegistry();
    }

    /** Test-friendly constructor — metrics go to a throwaway registry. */
    public ToolExecutor(ToolTraceRepository traces, CapstoneProperties props) {
        this(traces, props, null);
    }

    public <T extends ToolResult> T execute(String caseId, String toolName, String requestMaterial,
                                            Supplier<T> call, Supplier<T> unavailableFactory) {
        Instant started = Instant.now();
        long startNanos = System.nanoTime();
        T result;
        ToolStatus status;
        try {
            result = CompletableFuture.supplyAsync(call, pool).get(timeoutMs, TimeUnit.MILLISECONDS);
            status = result.getStatus();
        } catch (TimeoutException e) {
            result = unavailableFactory.get();
            result.markUnavailable(ToolStatus.TIMEOUT, "exceeded " + timeoutMs + "ms budget");
            status = ToolStatus.TIMEOUT;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result = unavailableFactory.get();
            result.markUnavailable(ToolStatus.FAILED, "interrupted");
            status = ToolStatus.FAILED;
        } catch (ExecutionException e) {
            result = unavailableFactory.get();
            result.markUnavailable(ToolStatus.FAILED, "tool error: " + e.getCause().getClass().getSimpleName());
            status = ToolStatus.FAILED;
        }
        long duration = (System.nanoTime() - startNanos) / 1_000_000;
        meterRegistry.timer("fraud.tool.duration", "tool", toolName,
                "status", status.name()).record(duration, TimeUnit.MILLISECONDS);
        if (status != ToolStatus.SUCCESS) {
            meterRegistry.counter("fraud.tool.failures",
                    "tool", toolName, "status", status.name()).increment();
        }

        ToolTraceEntity trace = new ToolTraceEntity();
        trace.setTraceId(Ids.traceId());
        trace.setCaseId(caseId);
        trace.setToolName(toolName);
        trace.setRequestHash(Hashing.sha256Hex(toolName + "|" + requestMaterial));
        trace.setStatus(status);
        trace.setDurationMs(duration);
        trace.setResultSummary(result.summary());
        trace.setCreatedAt(started);
        traces.save(trace);
        result.setToolResultId(trace.getTraceId());
        log.info("tool {} case={} status={} {}ms", toolName, caseId, status, duration);
        return result;
    }

    /** Deterministic, timed execution of the whole bounded plan. */
    public List<ToolTraceEntity> tracesFor(String caseId) {
        return traces.findByCaseIdOrderByCreatedAtAsc(caseId);
    }
}
