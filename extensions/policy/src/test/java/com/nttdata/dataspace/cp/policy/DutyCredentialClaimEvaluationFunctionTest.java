package com.nttdata.dataspace.cp.policy;

import com.nttdata.dataspace.cp.policy.obligation.DutyCredentialClaimEvaluationFunction;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Operator;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DutyCredentialClaimEvaluationFunctionTest {

    private static final String ISSUER_DID = "did:example:issuer";
    private static final String HOLDER_DID = "did:example:holder";

    // Test claim + allowed values (obligation: dacApproval EQ "true")
    private static final String CLAIM_NAME = "dacApproval";
    private static final Set<String> ALLOWED_VALUES = PolicyConstants.VALID_BOOLEAN_VALUES;

    @Mock
    private Monitor monitor;

    @Mock
    private ParticipantAgentPolicyContext policyContext;

    private DutyCredentialClaimEvaluationFunction<ParticipantAgentPolicyContext> function;

    @BeforeEach
    void setUp() {
        function = DutyCredentialClaimEvaluationFunction.create(CLAIM_NAME, ALLOWED_VALUES, monitor);
    }

    // ── Operator validation ───────────────────────────────────────────────────

    @Test
    void evaluate_eqOperator_claimMatchesRightOperand_returnsTrue() {
        // Obligation: dacApproval EQ "true" — participant has dacApproval = "true" → fulfilled
        var agent = buildAgentWithScalarClaim(CLAIM_NAME, "true");
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = function.evaluate(Operator.EQ, "true",
                Duty.Builder.newInstance().build(), policyContext);

        assertThat(result).isTrue();
    }

    @Test
    void evaluate_eqOperator_claimDiffersFromRightOperand_returnsFalse() {
        // Obligation: dacApproval EQ "true" — participant has dacApproval = "false" → not fulfilled
        var agent = buildAgentWithScalarClaim(CLAIM_NAME, "false");
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = function.evaluate(Operator.EQ, "true",
                Duty.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
    }

    @Test
    void evaluate_neqOperator_claimDiffersFromRightOperand_returnsTrue() {
        // Obligation: dacApproval NEQ "false" — participant has dacApproval = "true" → fulfilled
        var agent = buildAgentWithScalarClaim(CLAIM_NAME, "true");
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = function.evaluate(Operator.NEQ, "false",
                Duty.Builder.newInstance().build(), policyContext);

        assertThat(result).isTrue();
    }

    @Test
    void evaluate_neqOperator_claimMatchesRightOperand_returnsFalse() {
        // Obligation: dacApproval NEQ "false" — participant has dacApproval = "false" → not fulfilled
        var agent = buildAgentWithScalarClaim(CLAIM_NAME, "false");
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = function.evaluate(Operator.NEQ, "false",
                Duty.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
    }

    @Test
    void evaluate_invalidOperatorIN_returnsFalseAndReportsProblem() {
        var result = function.evaluate(Operator.IN, "true",
                Duty.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
        verify(policyContext).reportProblem(anyString());
    }

    // ── Null rightOperand ────────────────────────────────────────────────────

    @Test
    void evaluate_nullRightOperand_returnsFalseAndReportsProblem() {
        var result = function.evaluate(Operator.EQ, null,
                Duty.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
        verify(policyContext).reportProblem(anyString());
    }

    // ── Invalid rightOperand value (not in allowed set) ───────────────────────

    @Test
    void evaluate_rightOperandNotInAllowedValues_returnsFalseAndReportsProblem() {
        var result = function.evaluate(Operator.EQ, "yes",
                Duty.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
        verify(policyContext).reportProblem(anyString());
    }

    // ── Free-form field (empty allowed-values set) ────────────────────────────

    @Test
    void evaluate_freeFormField_anyRightOperandProceedsToClaim() {
        var freeFormFunction = DutyCredentialClaimEvaluationFunction
                .<ParticipantAgentPolicyContext>create("permitIssued", Set.of(), monitor);

        // No agent → reports problem, but gets past allowed-values check
        when(policyContext.participantAgent()).thenReturn(null);

        var result = freeFormFunction.evaluate(Operator.EQ, "true",
                Duty.Builder.newInstance().build(), policyContext);

        // Fails at agent extraction, but the invalid-value gate was NOT triggered
        assertThat(result).isFalse();
        verify(policyContext).reportProblem(anyString());
    }

    @Test
    void evaluate_nullAllowedValues_treatedAsEmptySet_andEvaluatesClaim() {
        var nullAllowedValuesFunction = DutyCredentialClaimEvaluationFunction
                .<ParticipantAgentPolicyContext>create(CLAIM_NAME, null, monitor);

        var agent = buildAgentWithScalarClaim(CLAIM_NAME, "true");
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = nullAllowedValuesFunction.evaluate(Operator.EQ, "true",
                Duty.Builder.newInstance().build(), policyContext);

        assertThat(result).isTrue();
    }

    // ── Participant agent ─────────────────────────────────────────────────────

    @Test
    void evaluate_nullParticipantAgent_returnsFalseAndReportsProblem() {
        when(policyContext.participantAgent()).thenReturn(null);

        var result = function.evaluate(Operator.EQ, "true",
                Duty.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
        verify(policyContext).reportProblem(anyString());
    }

    @Test
    void evaluate_noVcClaimInAgent_returnsFalseAndReportsProblem() {
        var agent = new ParticipantAgent(Map.of(), Map.of());
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = function.evaluate(Operator.EQ, "true",
                Duty.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
        verify(policyContext).reportProblem(anyString());
    }

    // ── Core semantic: obligation condition MET → returns true ────────────────

    @Test
    void evaluate_obligationConditionMet_returnsTrue() {
        // Obligation: dacApproval EQ "true" — participant has dacApproval = "true" → fulfilled
        var agent = buildAgentWithScalarClaim(CLAIM_NAME, "true");
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = function.evaluate(Operator.EQ, "true",
                Duty.Builder.newInstance().build(), policyContext);

        assertThat(result).isTrue();
    }

    @Test
    void evaluate_obligationConditionNotMet_returnsFalse() {
        // Obligation: dacApproval EQ "true" — participant has dacApproval = "false" → not fulfilled
        var agent = buildAgentWithScalarClaim(CLAIM_NAME, "false");
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = function.evaluate(Operator.EQ, "true",
                Duty.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
    }

    @Test
    void evaluate_claimMissingFromSubject_returnsFalse() {
        // Credential subject exists but does not contain the claim key → obligation not fulfilled
        var cs = CredentialSubject.Builder.newInstance()
                .id(HOLDER_DID)
                .claim("someOtherClaim", "someValue")
                .build();
        var vc = buildVcWithSubject(cs);
        var agent = new ParticipantAgent(Map.of("vc", List.of(vc)), Map.of());
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = function.evaluate(Operator.EQ, "true",
                Duty.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
    }

    // ── Credential type filtering ─────────────────────────────────────────────

    @Test
    void evaluate_obligationConditionInAnyCredentialType_returnsTrue() {
        // Obligation scanning works across all VC types (not just MembershipCredential)
        var cs = CredentialSubject.Builder.newInstance()
                .id(HOLDER_DID)
                .claim(CLAIM_NAME, "true")
                .build();
        var vc = VerifiableCredential.Builder.newInstance()
                .type("SomeOtherCredential")
                .credentialSubject(cs)
                .issuer(new Issuer(ISSUER_DID, Map.of()))
                .issuanceDate(Instant.MIN)
                .build();

        var agent = new ParticipantAgent(Map.of("vc", List.of(vc)), Map.of());
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = function.evaluate(Operator.EQ, "true",
                Duty.Builder.newInstance().build(), policyContext);

        assertThat(result).isTrue();
    }

    // ── permitIssued claim (reuses same function pattern) ────────────────────

    @Test
    void evaluate_permitIssuedClaim_conditionMet_returnsTrue() {
        var permitFunction = DutyCredentialClaimEvaluationFunction
                .<ParticipantAgentPolicyContext>create(
                        PolicyConstants.PERMIT_ISSUED_CLAIM,
                        PolicyConstants.VALID_BOOLEAN_VALUES,
                        monitor);

        var agent = buildAgentWithScalarClaim(PolicyConstants.PERMIT_ISSUED_CLAIM, "true");
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = permitFunction.evaluate(Operator.EQ, "true",
                Duty.Builder.newInstance().build(), policyContext);

        assertThat(result).isTrue();
    }

    @Test
    void evaluate_permitIssuedClaim_conditionNotMet_returnsFalse() {
        var permitFunction = DutyCredentialClaimEvaluationFunction
                .<ParticipantAgentPolicyContext>create(
                        PolicyConstants.PERMIT_ISSUED_CLAIM,
                        PolicyConstants.VALID_BOOLEAN_VALUES,
                        monitor);

        var agent = buildAgentWithScalarClaim(PolicyConstants.PERMIT_ISSUED_CLAIM, "false");
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = permitFunction.evaluate(Operator.EQ, "true",
                Duty.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
    }

    // ── Monitor logging ───────────────────────────────────────────────────────

    @Test
    void evaluate_onObligationFulfilled_logsInfoMessage() {
        var agent = buildAgentWithScalarClaim(CLAIM_NAME, "true");
        when(policyContext.participantAgent()).thenReturn(agent);

        function.evaluate(Operator.EQ, "true",
                Duty.Builder.newInstance().build(), policyContext);

        verify(monitor, atLeastOnce()).info(anyString());
    }

    @Test
    void evaluate_onObligationFulfilled_doesNotLogWarning() {
        var agent = buildAgentWithScalarClaim(CLAIM_NAME, "true");
        when(policyContext.participantAgent()).thenReturn(agent);

        function.evaluate(Operator.EQ, "true",
                Duty.Builder.newInstance().build(), policyContext);

        verify(monitor, never()).warning(anyString());
    }

    @Test
    void evaluate_onObligationNotFulfilled_doesNotLogWarning() {
        // Condition not met is a normal evaluation outcome — not an authoring error
        var agent = buildAgentWithScalarClaim(CLAIM_NAME, "false");
        when(policyContext.participantAgent()).thenReturn(agent);

        function.evaluate(Operator.EQ, "true",
                Duty.Builder.newInstance().build(), policyContext);

        verify(monitor, never()).warning(anyString());
    }

    @Test
    void evaluate_onObligationFulfilled_doesNotReportProblem() {
        var agent = buildAgentWithScalarClaim(CLAIM_NAME, "true");
        when(policyContext.participantAgent()).thenReturn(agent);

        function.evaluate(Operator.EQ, "true",
                Duty.Builder.newInstance().build(), policyContext);

        verify(policyContext, never()).reportProblem(anyString());
    }

    @Test
    void evaluate_onValidationError_logsWarningAndReportsProblem() {
        // Validation errors (null agent, invalid operator, etc.) are policy authoring errors
        when(policyContext.participantAgent()).thenReturn(null);

        function.evaluate(Operator.EQ, "true",
                Duty.Builder.newInstance().build(), policyContext);

        verify(monitor, atLeastOnce()).warning(anyString());
        verify(policyContext).reportProblem(anyString());
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

    private VerifiableCredential buildVcWithSubject(CredentialSubject cs) {
        return VerifiableCredential.Builder.newInstance()
                .type(PolicyConstants.MEMBERSHIP_CREDENTIAL_TYPE)
                .credentialSubject(cs)
                .issuer(new Issuer(ISSUER_DID, Map.of()))
                .issuanceDate(Instant.MIN)
                .build();
    }
}
