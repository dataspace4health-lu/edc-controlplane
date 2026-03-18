package com.nttdata.dataspace.cp.policy;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialClaimEvaluationFunctionTest {

    private static final String ISSUER_DID = "did:example:issuer";
    private static final String HOLDER_DID = "did:example:holder";

    // Test claim + allowed values (mimicking euEeaStatus)
    private static final String CLAIM_NAME = "euEeaStatus";
    private static final Set<String> ALLOWED_VALUES = Set.of("eu-member-state", "eea-member", "third-country");

    // Test claim with list semantics (mimicking organizationRoles)
    private static final String LIST_CLAIM_NAME = "organizationRoles";
    private static final Set<String> LIST_ALLOWED_VALUES = Set.copyOf(PolicyConstants.VALID_ORGANIZATION_ROLES);

    @Mock
    private Monitor monitor;

    @Mock
    private ParticipantAgentPolicyContext policyContext;

    private CredentialClaimEvaluationFunction<ParticipantAgentPolicyContext> function;
    private CredentialClaimEvaluationFunction<ParticipantAgentPolicyContext> listFunction;

    @BeforeEach
    void setUp() {
        function = CredentialClaimEvaluationFunction.create(CLAIM_NAME, ALLOWED_VALUES, monitor);
        listFunction = CredentialClaimEvaluationFunction.create(LIST_CLAIM_NAME, LIST_ALLOWED_VALUES, monitor);
    }

    // ── Operator validation ───────────────────────────────────────────────────

    @Test
    void evaluate_invalidOperatorNEQ_returnsFalseAndReportsProblem() {
        var result = function.evaluate(Operator.NEQ, "eu-member-state",
                Permission.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
        verify(policyContext).reportProblem(anyString());
    }

    @Test
    void evaluate_invalidOperatorIN_returnsFalseAndReportsProblem() {
        var result = function.evaluate(Operator.IN, "eu-member-state",
                Permission.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
        verify(policyContext).reportProblem(anyString());
    }

    // ── Null rightOperand ────────────────────────────────────────────────────

    @Test
    void evaluate_nullRightOperand_returnsFalseAndReportsProblem() {
        var result = function.evaluate(Operator.EQ, null,
                Permission.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
        verify(policyContext).reportProblem(anyString());
    }

    // ── Invalid rightOperand value (not in allowed set) ───────────────────────

    @Test
    void evaluate_rightOperandNotInAllowedValues_returnsFalseAndReportsProblem() {
        var result = function.evaluate(Operator.EQ, "unknown-status",
                Permission.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
        verify(policyContext).reportProblem(anyString());
    }

    // ── Free-form field (empty allowed-values set) ────────────────────────────

    @Test
    void evaluate_freeFormField_anyRightOperandProceedsToClaim() {
        // jurisdictionCountry-style: no allowed-values validation, any ISO code passes
        var freeFormFunction = CredentialClaimEvaluationFunction.<ParticipantAgentPolicyContext>create(
                "jurisdictionCountry", Set.of(), monitor);

        // No agent → reports problem about agent, but gets past allowed-values check
        when(policyContext.participantAgent()).thenReturn(null);

        var result = freeFormFunction.evaluate(Operator.EQ, "DE",
                Permission.Builder.newInstance().build(), policyContext);

        // Fails later (no agent), but the invalid-value gate was NOT triggered
        assertThat(result).isFalse();
        verify(policyContext).reportProblem(anyString());
    }

    @Test
    void evaluate_nullAllowedValues_treatedAsEmptySet_andEvaluatesClaim() {
        var nullAllowedValuesFunction = CredentialClaimEvaluationFunction.<ParticipantAgentPolicyContext>create(
                CLAIM_NAME, null, monitor);

        var agent = buildAgentWithScalarClaim(CLAIM_NAME, "eu-member-state");
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = nullAllowedValuesFunction.evaluate(Operator.EQ, "eu-member-state",
                Permission.Builder.newInstance().build(), policyContext);

        assertThat(result).isTrue();
    }

    // ── Participant agent ─────────────────────────────────────────────────────

    @Test
    void evaluate_nullParticipantAgent_returnsFalseAndReportsProblem() {
        when(policyContext.participantAgent()).thenReturn(null);

        var result = function.evaluate(Operator.EQ, "eu-member-state",
                Permission.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
        verify(policyContext).reportProblem(anyString());
    }

    @Test
    void evaluate_noVcClaimInAgent_returnsFalseAndReportsProblem() {
        var agent = new ParticipantAgent(Map.of(), Map.of());
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = function.evaluate(Operator.EQ, "eu-member-state",
                Permission.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
        verify(policyContext).reportProblem(anyString());
    }

    // ── Credential type filtering ─────────────────────────────────────────────

    @Test
    void evaluate_vcClaimPresentInAnyCredentialType_returnsTrue() {
        // The function scans all credentials regardless of type.
        // A claim in a non-MembershipCredential (e.g. LegalPerson) must still be found.
        var cs = CredentialSubject.Builder.newInstance()
                .id(HOLDER_DID)
                .claim(CLAIM_NAME, "eu-member-state")
                .build();
        var vc = VerifiableCredential.Builder.newInstance()
                .type("SomeOtherCredential")
                .credentialSubject(cs)
                .issuer(new Issuer(ISSUER_DID, Map.of()))
                .issuanceDate(Instant.MIN)
                .build();

        var agent = new ParticipantAgent(Map.of("vc", List.of(vc)), Map.of());
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = function.evaluate(Operator.EQ, "eu-member-state",
                Permission.Builder.newInstance().build(), policyContext);

        assertThat(result).isTrue();
    }

    // ── Scalar claim evaluation ───────────────────────────────────────────────

    @Test
    void evaluate_scalarClaimMatches_returnsTrue() {
        var agent = buildAgentWithScalarClaim(CLAIM_NAME, "eu-member-state");
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = function.evaluate(Operator.EQ, "eu-member-state",
                Permission.Builder.newInstance().build(), policyContext);

        assertThat(result).isTrue();
    }

    @Test
    void evaluate_scalarClaimMismatch_returnsFalse() {
        var agent = buildAgentWithScalarClaim(CLAIM_NAME, "eu-member-state");
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = function.evaluate(Operator.EQ, "third-country",
                Permission.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
    }

    @Test
    void evaluate_claimMissingFromSubject_returnsFalse() {
        // Credential subject exists but does not contain the claim key
        var cs = CredentialSubject.Builder.newInstance()
                .id(HOLDER_DID)
                .claim("someOtherClaim", "someValue")
                .build();
        var vc = buildVcWithSubject(cs);
        var agent = new ParticipantAgent(Map.of("vc", List.of(vc)), Map.of());
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = function.evaluate(Operator.EQ, "eu-member-state",
                Permission.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
    }

    // ── List claim evaluation ─────────────────────────────────────────────────

    @Test
    void evaluate_listClaimContainsRequiredRole_returnsTrue() {
        var agent = buildAgentWithListClaim(LIST_CLAIM_NAME,
                List.of("health-data-holder", "connector-operator"));
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = listFunction.evaluate(Operator.EQ, "health-data-holder",
                Permission.Builder.newInstance().build(), policyContext);

        assertThat(result).isTrue();
    }

    @Test
    void evaluate_listClaimDoesNotContainRequiredRole_returnsFalse() {
        var agent = buildAgentWithListClaim(LIST_CLAIM_NAME,
                List.of("connector-operator"));
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = listFunction.evaluate(Operator.EQ, "health-data-holder",
                Permission.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
    }

    @Test
    void evaluate_listClaimSingleEntryMatches_returnsTrue() {
        var agent = buildAgentWithListClaim(LIST_CLAIM_NAME, List.of("health-data-holder"));
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = listFunction.evaluate(Operator.EQ, "health-data-holder",
                Permission.Builder.newInstance().build(), policyContext);

        assertThat(result).isTrue();
    }

    // ── Monitor logging ───────────────────────────────────────────────────────

    @Test
    void evaluate_onSuccess_logsInfoMessage() {
        var agent = buildAgentWithScalarClaim(CLAIM_NAME, "eu-member-state");
        when(policyContext.participantAgent()).thenReturn(agent);

        function.evaluate(Operator.EQ, "eu-member-state",
                Permission.Builder.newInstance().build(), policyContext);

        verify(monitor, atLeastOnce()).info(anyString());
    }

    @Test
    void evaluate_onFailure_logsWarningMessage() {
        when(policyContext.participantAgent()).thenReturn(null);

        function.evaluate(Operator.EQ, "eu-member-state",
                Permission.Builder.newInstance().build(), policyContext);

        verify(monitor, atLeastOnce()).warning(anyString());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ParticipantAgent buildAgentWithScalarClaim(String claimName, String value) {
        var claims = new HashMap<String, Object>();
        claims.put(claimName, value);
        var cs = CredentialSubject.Builder.newInstance()
                .id(HOLDER_DID)
                .claims(claims)
                .build();
        return new ParticipantAgent(Map.of("vc", List.of(buildVcWithSubject(cs))), Map.of());
    }

    private ParticipantAgent buildAgentWithListClaim(String claimName, List<String> values) {
        var claims = new HashMap<String, Object>();
        claims.put(claimName, values);
        var cs = CredentialSubject.Builder.newInstance()
                .id(HOLDER_DID)
                .claims(claims)
                .build();
        return new ParticipantAgent(Map.of("vc", List.of(buildVcWithSubject(cs))), Map.of());
    }

    private VerifiableCredential buildVcWithSubject(CredentialSubject cs) {
        return VerifiableCredential.Builder.newInstance()
                .type(PolicyConstants.MEMBERSHIP_CREDENTIAL_TYPE)
                .credentialSubject(cs)
                .issuer(new Issuer(ISSUER_DID, Map.of()))
                .issuanceDate(Instant.MIN)
                .build();
    }
}
