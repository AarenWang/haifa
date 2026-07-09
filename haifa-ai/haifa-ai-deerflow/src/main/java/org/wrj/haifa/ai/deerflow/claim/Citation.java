package org.wrj.haifa.ai.deerflow.claim;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "deerflow_citations")
public class Citation {

    @Id
    @Column(name = "citation_id", length = 64, nullable = false)
    private String citationId;

    @Column(name = "claim_id", length = 64, nullable = false)
    private String claimId;

    @Column(name = "evidence_id", length = 64, nullable = false)
    private String evidenceId;

    @Column(name = "source_id", length = 64, nullable = false)
    private String sourceId;

    @Column(name = "locator", length = 1000)
    private String locator;

    @Column(name = "status", length = 64, nullable = false)
    private String status; // valid | missing_source | weak_source | unsupported | stale

    @Column(name = "verified_at")
    private Instant verifiedAt;

    public Citation() {
    }

    public String getCitationId() {
        return citationId;
    }

    public void setCitationId(String citationId) {
        this.citationId = citationId;
    }

    public String getClaimId() {
        return claimId;
    }

    public void setClaimId(String claimId) {
        this.claimId = claimId;
    }

    public String getEvidenceId() {
        return evidenceId;
    }

    public void setEvidenceId(String evidenceId) {
        this.evidenceId = evidenceId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getLocator() {
        return locator;
    }

    public void setLocator(String locator) {
        this.locator = locator;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}
