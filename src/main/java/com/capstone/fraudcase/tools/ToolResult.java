package com.capstone.fraudcase.tools;

import com.capstone.fraudcase.common.Ids;
import com.capstone.fraudcase.domain.ToolStatus;
import java.time.Instant;

/** Base for every tool result: provenance + safe-failure status. */
public abstract class ToolResult {

    private String toolResultId = Ids.toolResultId();
    private String observedAt = Instant.now().toString();
    private String origin = "synthetic-fixture";
    private ToolStatus status = ToolStatus.SUCCESS;
    private String unavailableReason;

    public boolean isAvailable() {
        return status == ToolStatus.SUCCESS || status == ToolStatus.NOT_FOUND;
    }

    public String getToolResultId() {
        return toolResultId;
    }

    public void setToolResultId(String toolResultId) {
        this.toolResultId = toolResultId;
    }

    public String getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(String observedAt) {
        this.observedAt = observedAt;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public ToolStatus getStatus() {
        return status;
    }

    public void setStatus(ToolStatus status) {
        this.status = status;
    }

    public String getUnavailableReason() {
        return unavailableReason;
    }

    public void setUnavailableReason(String unavailableReason) {
        this.unavailableReason = unavailableReason;
    }

    protected void markUnavailable(ToolStatus status, String reason) {
        this.status = status;
        this.unavailableReason = reason;
    }

    /** One-line redacted summary for traces and the UI. */
    public abstract String summary();
}
