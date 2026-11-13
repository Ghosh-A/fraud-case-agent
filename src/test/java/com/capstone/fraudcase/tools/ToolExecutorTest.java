package com.capstone.fraudcase.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.capstone.fraudcase.CapstoneProperties;
import com.capstone.fraudcase.domain.ToolStatus;
import com.capstone.fraudcase.persistence.entity.ToolTraceEntity;
import com.capstone.fraudcase.persistence.repository.ToolTraceRepository;
import com.capstone.fraudcase.tools.ToolResults.DeviceSignalsResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** T08 — tool timeout and not-found become structured unavailable results. */
class ToolExecutorTest {

    private final ToolTraceRepository traces = mock(ToolTraceRepository.class);
    private final ToolExecutor executor;

    ToolExecutorTest() {
        CapstoneProperties props = new CapstoneProperties();
        props.getTools().setTimeoutMs(200);
        this.executor = new ToolExecutor(traces, props);
        when(traces.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void successRecordsTrace() {
        DeviceSignalsResult r = executor.execute("CASE-1", "getDeviceSignals", "ref",
                DeviceSignalsResult::new, () -> DeviceSignalsResult.unavailable("x"));
        assertThat(r.getStatus()).isEqualTo(ToolStatus.SUCCESS);
        ArgumentCaptor<ToolTraceEntity> captor = ArgumentCaptor.forClass(ToolTraceEntity.class);
        verify(traces).save(captor.capture());
        ToolTraceEntity t = captor.getValue();
        assertThat(t.getToolName()).isEqualTo("getDeviceSignals");
        assertThat(t.getStatus()).isEqualTo(ToolStatus.SUCCESS);
        assertThat(t.getRequestHash()).hasSize(64);
        assertThat(t.getDurationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void timeoutReturnsStructuredUnavailable() {
        DeviceSignalsResult r = executor.execute("CASE-1", "getDeviceSignals", "ref",
                () -> {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return new DeviceSignalsResult();
                },
                () -> DeviceSignalsResult.unavailable("x"));
        assertThat(r.getStatus()).isEqualTo(ToolStatus.TIMEOUT);
        assertThat(r.isAvailable()).isFalse();
        assertThat(r.getUnavailableReason()).contains("200ms");
    }

    @Test
    void thrownErrorReturnsFailed() {
        DeviceSignalsResult r = executor.execute("CASE-1", "getDeviceSignals", "ref",
                () -> {
                    throw new IllegalStateException("boom");
                },
                () -> DeviceSignalsResult.unavailable("x"));
        assertThat(r.getStatus()).isEqualTo(ToolStatus.FAILED);
        assertThat(r.isAvailable()).isFalse();
    }
}
