package com.nttdata.dataspace.cp.policy;

import java.util.Set;

/** String constants used across the membership policy evaluation extension. See README.md for full details. */
public class PolicyConstants {

    private PolicyConstants() {
    }

    /**
     * EDC's default @vocab namespace applied by the JSON-LD processor.
     * All leftOperand short names in a policy definition are expanded with this prefix,
     * so every constraint key registered with EDC must use this prefix too.
     */
    static final String EDC_NAMESPACE = "https://w3id.org/edc/v0.0.1/ns/";

    // ─────────────────────────────────────────────────────────────────────────
    // Membership credential — base identity check
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Short type name present in the VC's "type" array.
     * Matches: credential.type = ["MembershipCredential"]
     */
    public static final String MEMBERSHIP_CREDENTIAL_TYPE = "MembershipCredential";

    /**
     * Fully-expanded IRI constraint key for the membership active check.
     * Policy leftOperand "MembershipCredential" expands to this after JSON-LD processing.
     * Used by MembershipCredentialEvaluationFunction.
     */
    public static final String MEMBERSHIP_CONSTRAINT_KEY = EDC_NAMESPACE + MEMBERSHIP_CREDENTIAL_TYPE;

    /** Key of the membership object inside credentialSubject. Maps to: credentialSubject.membership */
    public static final String MEMBERSHIP_CLAIM = "membership";

    /** Key of the start date inside the membership object. Maps to: credentialSubject.membership.since */
    public static final String SINCE_CLAIM = "since";

    /** Required rightOperand value for the membership active constraint. */
    public static final String ACTIVE = "active";

    // ─────────────────────────────────────────────────────────────────────────
    // Credential attribute claim keys
    // These are top-level properties inside credentialSubject, e.g.:
    //   credentialSubject.jurisdictionCountry = "DE"
    //   credentialSubject.organizationRoles   = ["health-data-holder"]
    // ─────────────────────────────────────────────────────────────────────────

    /** ISO 3166-1 alpha-2 country code. credentialSubject.jurisdictionCountry = "DE" */
    public static final String JURISDICTION_COUNTRY_CLAIM = "jurisdictionCountry";

    /** EU/EEA membership status. credentialSubject.euEeaStatus = "eu-member-state" */
    public static final String EU_EEA_STATUS_CLAIM = "euEeaStatus";

    /** Ownership type of the organization. credentialSubject.organizationOwnershipType = "public" */
    public static final String ORGANIZATION_OWNERSHIP_TYPE_CLAIM = "organizationOwnershipType";

    /** Type of organization. credentialSubject.organizationType = "hospital" */
    public static final String ORGANIZATION_TYPE_CLAIM = "organizationType";

    /**
     * Roles the organization holds in the dataspace.
     * credentialSubject.organizationRoles may be a String or List<String>.
     * EQ check tests if the required role is present anywhere in the list.
     */
    public static final String ORGANIZATION_ROLES_CLAIM = "organizationRoles";

    /** GDPR data protection role. credentialSubject.dataProtectionRole = "controller" */
    public static final String DATA_PROTECTION_ROLE_CLAIM = "dataProtectionRole";

    /** Current membership standing. credentialSubject.membershipStatus = "active" */
    public static final String MEMBERSHIP_STATUS_CLAIM = "membershipStatus";

    /**
     * Data exchange protocols supported by the participant.
     * credentialSubject.supportedProtocols may be a String or List<String>.
     */
    public static final String SUPPORTED_PROTOCOLS_CLAIM = "supportedProtocols";

    // ─────────────────────────────────────────────────────────────────────────
    // Obligation claim keys
    // These are top-level boolean properties inside credentialSubject, e.g.:
    //   credentialSubject.dacApproval   = "true"
    //   credentialSubject.permitIssued  = "true"
    // At least one must be true for the obligation to be fulfilled.
    // ─────────────────────────────────────────────────────────────────────────

    /** Data Access Committee approval flag. credentialSubject.dacApproval = "true" */
    public static final String DAC_APPROVAL_CLAIM = "dacApproval";

    /** Data permit issued flag. credentialSubject.permitIssued = "true" */
    public static final String PERMIT_ISSUED_CLAIM = "permitIssued";

    // ─────────────────────────────────────────────────────────────────────────
    // Constraint keys (fully-expanded IRIs used for RuleBindingRegistry and
    // PolicyEngine registration). These match the JSON-LD-expanded leftOperand.
    // ─────────────────────────────────────────────────────────────────────────

    public static final String JURISDICTION_COUNTRY_CONSTRAINT_KEY     = EDC_NAMESPACE + JURISDICTION_COUNTRY_CLAIM;
    public static final String EU_EEA_STATUS_CONSTRAINT_KEY            = EDC_NAMESPACE + EU_EEA_STATUS_CLAIM;
    public static final String ORGANIZATION_OWNERSHIP_TYPE_CONSTRAINT_KEY = EDC_NAMESPACE + ORGANIZATION_OWNERSHIP_TYPE_CLAIM;
    public static final String ORGANIZATION_TYPE_CONSTRAINT_KEY        = EDC_NAMESPACE + ORGANIZATION_TYPE_CLAIM;
    public static final String ORGANIZATION_ROLES_CONSTRAINT_KEY       = EDC_NAMESPACE + ORGANIZATION_ROLES_CLAIM;
    public static final String DATA_PROTECTION_ROLE_CONSTRAINT_KEY     = EDC_NAMESPACE + DATA_PROTECTION_ROLE_CLAIM;
    public static final String MEMBERSHIP_STATUS_CONSTRAINT_KEY        = EDC_NAMESPACE + MEMBERSHIP_STATUS_CLAIM;
    public static final String SUPPORTED_PROTOCOLS_CONSTRAINT_KEY      = EDC_NAMESPACE + SUPPORTED_PROTOCOLS_CLAIM;
    public static final String DAC_APPROVAL_CONSTRAINT_KEY             = EDC_NAMESPACE + DAC_APPROVAL_CLAIM;
    public static final String PERMIT_ISSUED_CONSTRAINT_KEY            = EDC_NAMESPACE + PERMIT_ISSUED_CLAIM;

    // ─────────────────────────────────────────────────────────────────────────
    // Valid values for each enumerated field.
    // The evaluation function validates that the policy rightOperand is one of
    // these values before inspecting the credential — catching authoring errors
    // at evaluation time rather than silently returning false.
    // jurisdictionCountry has no fixed set; it accepts any ISO 3166-1 alpha-2 code.
    // ─────────────────────────────────────────────────────────────────────────

    public static final Set<String> VALID_EU_EEA_STATUSES = Set.of(
            "eu-member-state", "eea-member", "third-country");

    public static final Set<String> VALID_ORGANIZATION_OWNERSHIP_TYPES = Set.of(
            "public", "private");

    public static final Set<String> VALID_ORGANIZATION_TYPES = Set.of(
            "healthcare-provider", "hospital", "university-hospital",
            "research-organization", "research-institute", "academic-institution",
            "biobank", "public-health-authority", "health-data-access-body",
            "pharmaceutical-company", "medical-technology-company");

    public static final Set<String> VALID_ORGANIZATION_ROLES = Set.of(
            "health-data-holder", "trusted-health-data-holder", "health-data-user",
            "health-data-applicant", "health-data-access-body",
            "federator", "connector-operator");

    public static final Set<String> VALID_DATA_PROTECTION_ROLES = Set.of(
            "controller", "joint-controller", "processor");

    public static final Set<String> VALID_MEMBERSHIP_STATUSES = Set.of(
            "active", "suspended", "revoked", "pending");

    public static final Set<String> VALID_SUPPORTED_PROTOCOLS = Set.of(
            "dataspace-protocol-http:2025-1");

    /** Valid values for boolean obligation claims (dacApproval, permitIssued). */
    public static final Set<String> VALID_BOOLEAN_VALUES = Set.of("true", "false");
}