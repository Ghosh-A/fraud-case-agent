package com.capstone.fraudcase.tools;

import com.capstone.fraudcase.agent.common.PolicyCitationRef;
import com.capstone.fraudcase.domain.ToolStatus;
import java.util.ArrayList;
import java.util.List;

/** Result DTOs for the approved tool catalogue. */
public final class ToolResults {

    private ToolResults() {
    }

    public static class TransactionDetailsResult extends ToolResult {
        private boolean found;
        private String txnStatus;
        private String timestamp;
        private String amount;
        private String channel;
        private String authOutcome;
        private String maskedMerchant;
        private boolean duplicateFingerprint;

        public static TransactionDetailsResult notFound(String ref) {
            TransactionDetailsResult r = new TransactionDetailsResult();
            r.found = false;
            r.markUnavailable(ToolStatus.NOT_FOUND, "transaction " + ref + " not found in synthetic ledger");
            return r;
        }

        public static TransactionDetailsResult unavailable(String reason) {
            TransactionDetailsResult r = new TransactionDetailsResult();
            r.markUnavailable(ToolStatus.UNAVAILABLE, reason);
            return r;
        }

        @Override
        public String summary() {
            return found ? "txn " + txnStatus + " " + amount + " via " + channel + ", auth=" + authOutcome
                    : "transaction unavailable: " + getUnavailableReason();
        }

        public boolean isFound() {
            return found;
        }

        public void setFound(boolean found) {
            this.found = found;
        }

        public String getTxnStatus() {
            return txnStatus;
        }

        public void setTxnStatus(String txnStatus) {
            this.txnStatus = txnStatus;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public String getAmount() {
            return amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getAuthOutcome() {
            return authOutcome;
        }

        public void setAuthOutcome(String authOutcome) {
            this.authOutcome = authOutcome;
        }

        public String getMaskedMerchant() {
            return maskedMerchant;
        }

        public void setMaskedMerchant(String maskedMerchant) {
            this.maskedMerchant = maskedMerchant;
        }

        public boolean isDuplicateFingerprint() {
            return duplicateFingerprint;
        }

        public void setDuplicateFingerprint(boolean duplicateFingerprint) {
            this.duplicateFingerprint = duplicateFingerprint;
        }
    }

    public static class DeviceSignalsResult extends ToolResult {
        private boolean deviceKnown;
        private String firstSeenAt;
        private String trustLevel;
        private List<String> observations = new ArrayList<>();

        public static DeviceSignalsResult unavailable(String reason) {
            DeviceSignalsResult r = new DeviceSignalsResult();
            r.markUnavailable(ToolStatus.UNAVAILABLE, reason);
            return r;
        }

        @Override
        public String summary() {
            return isAvailable() ? "device known=" + deviceKnown + ", trust=" + trustLevel
                    : "device signals unavailable: " + getUnavailableReason();
        }

        public boolean isDeviceKnown() {
            return deviceKnown;
        }

        public void setDeviceKnown(boolean deviceKnown) {
            this.deviceKnown = deviceKnown;
        }

        public String getFirstSeenAt() {
            return firstSeenAt;
        }

        public void setFirstSeenAt(String firstSeenAt) {
            this.firstSeenAt = firstSeenAt;
        }

        public String getTrustLevel() {
            return trustLevel;
        }

        public void setTrustLevel(String trustLevel) {
            this.trustLevel = trustLevel;
        }

        public List<String> getObservations() {
            return observations;
        }

        public void setObservations(List<String> observations) {
            this.observations = observations;
        }
    }

    public static class LocationCompareResult extends ToolResult {
        private double distanceKm;
        private String timeDelta;
        private String anomalyLevel;
        private List<String> observations = new ArrayList<>();

        public static LocationCompareResult unavailable(String reason) {
            LocationCompareResult r = new LocationCompareResult();
            r.markUnavailable(ToolStatus.UNAVAILABLE, reason);
            return r;
        }

        @Override
        public String summary() {
            return isAvailable() ? "distance=" + distanceKm + "km, anomaly=" + anomalyLevel
                    : "location comparison unavailable: " + getUnavailableReason();
        }

        public double getDistanceKm() {
            return distanceKm;
        }

        public void setDistanceKm(double distanceKm) {
            this.distanceKm = distanceKm;
        }

        public String getTimeDelta() {
            return timeDelta;
        }

        public void setTimeDelta(String timeDelta) {
            this.timeDelta = timeDelta;
        }

        public String getAnomalyLevel() {
            return anomalyLevel;
        }

        public void setAnomalyLevel(String anomalyLevel) {
            this.anomalyLevel = anomalyLevel;
        }

        public List<String> getObservations() {
            return observations;
        }

        public void setObservations(List<String> observations) {
            this.observations = observations;
        }
    }

    public static class MerchantRiskResult extends ToolResult {
        private String riskBand;
        private boolean blocklistMatch;
        private String category;
        private List<String> observations = new ArrayList<>();

        public static MerchantRiskResult unavailable(String reason) {
            MerchantRiskResult r = new MerchantRiskResult();
            r.markUnavailable(ToolStatus.UNAVAILABLE, reason);
            return r;
        }

        @Override
        public String summary() {
            return isAvailable() ? "riskBand=" + riskBand + ", blocklist=" + blocklistMatch
                    : "merchant profile unavailable: " + getUnavailableReason();
        }

        public String getRiskBand() {
            return riskBand;
        }

        public void setRiskBand(String riskBand) {
            this.riskBand = riskBand;
        }

        public boolean isBlocklistMatch() {
            return blocklistMatch;
        }

        public void setBlocklistMatch(boolean blocklistMatch) {
            this.blocklistMatch = blocklistMatch;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public List<String> getObservations() {
            return observations;
        }

        public void setObservations(List<String> observations) {
            this.observations = observations;
        }
    }

    public static class PriorDisputesResult extends ToolResult {
        private int count;
        private int similarCount;
        private int lookbackDays;
        private List<String> observations = new ArrayList<>();

        public static PriorDisputesResult unavailable(String reason) {
            PriorDisputesResult r = new PriorDisputesResult();
            r.markUnavailable(ToolStatus.UNAVAILABLE, reason);
            return r;
        }

        @Override
        public String summary() {
            return isAvailable() ? "priorDisputes=" + count + ", similar=" + similarCount
                    : "prior dispute history unavailable: " + getUnavailableReason();
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getSimilarCount() {
            return similarCount;
        }

        public void setSimilarCount(int similarCount) {
            this.similarCount = similarCount;
        }

        public int getLookbackDays() {
            return lookbackDays;
        }

        public void setLookbackDays(int lookbackDays) {
            this.lookbackDays = lookbackDays;
        }

        public List<String> getObservations() {
            return observations;
        }

        public void setObservations(List<String> observations) {
            this.observations = observations;
        }
    }

    public static class SlaResult extends ToolResult {
        private String queue;
        private String priority;
        private String targetAt;
        private String explanation;

        public static SlaResult unavailable(String reason) {
            SlaResult r = new SlaResult();
            r.markUnavailable(ToolStatus.UNAVAILABLE, reason);
            return r;
        }

        @Override
        public String summary() {
            return isAvailable() ? "queue=" + queue + ", priority=" + priority + ", target=" + targetAt
                    : "SLA calculation unavailable: " + getUnavailableReason();
        }

        public String getQueue() {
            return queue;
        }

        public void setQueue(String queue) {
            this.queue = queue;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }

        public String getTargetAt() {
            return targetAt;
        }

        public void setTargetAt(String targetAt) {
            this.targetAt = targetAt;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }
    }

    public static class PolicySearchResult extends ToolResult {
        private List<PolicyCitationRef> citations = new ArrayList<>();
        private String query;

        public static PolicySearchResult unavailable(String reason) {
            PolicySearchResult r = new PolicySearchResult();
            r.markUnavailable(ToolStatus.UNAVAILABLE, reason);
            return r;
        }

        @Override
        public String summary() {
            return isAvailable() ? citations.size() + " policy passage(s) above threshold"
                    : "policy search unavailable: " + getUnavailableReason();
        }

        public List<PolicyCitationRef> getCitations() {
            return citations;
        }

        public void setCitations(List<PolicyCitationRef> citations) {
            this.citations = citations;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }
    }

    public static class AuditEventResult extends ToolResult {
        private String eventId;
        private String eventType;
        private String recordedAt;

        public static AuditEventResult unavailable(String reason) {
            AuditEventResult r = new AuditEventResult();
            r.markUnavailable(ToolStatus.UNAVAILABLE, reason);
            return r;
        }

        @Override
        public String summary() {
            return "audit event " + eventType + " recorded as " + eventId;
        }

        public String getEventId() {
            return eventId;
        }

        public void setEventId(String eventId) {
            this.eventId = eventId;
        }

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public String getRecordedAt() {
            return recordedAt;
        }

        public void setRecordedAt(String recordedAt) {
            this.recordedAt = recordedAt;
        }
    }
}
