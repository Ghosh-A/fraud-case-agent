package com.capstone.fraudcase.api.dto;

import com.capstone.fraudcase.domain.Disposition;
import com.capstone.fraudcase.domain.Route;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReviewRequest {

    @NotNull
    private Disposition disposition;

    @NotBlank(message = "a reviewer rationale is mandatory")
    @Size(max = 1500)
    private String reason;

    /** Required for REROUTE/OVERRIDE/ESCALATE. */
    private Route chosenRoute;

    public Disposition getDisposition() {
        return disposition;
    }

    public void setDisposition(Disposition disposition) {
        this.disposition = disposition;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Route getChosenRoute() {
        return chosenRoute;
    }

    public void setChosenRoute(Route chosenRoute) {
        this.chosenRoute = chosenRoute;
    }
}
