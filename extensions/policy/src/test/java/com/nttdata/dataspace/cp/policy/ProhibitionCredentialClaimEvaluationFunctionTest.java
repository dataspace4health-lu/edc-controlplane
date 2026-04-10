package com.nttdata.dataspace.cp.policy;

import com.nttdata.dataspace.cp.policy.prohibition.ProhibitionCredentialClaimEvaluationFunction;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Prohibition;
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
class ProhibitionCredentialClaimEvaluationFunctionTest {

    private static final String ISSUER_DID = "did:example:issuer";
    private static final String HOLDER_DID = "did:example:holder";

    // Test claim + allowed values (mimicking euEeaStatus prohibition)
    private static final String CLAIM_NAME = "euEeaStatus";
    private static final Set<String> ALLOWED_VALUES = Set.of("eu-member-state", "eea-member", "third-country");

    // Test claim with list semantics (mimicking organizationRoles prohibition)
    private static final String LIST_CLAIM_NAME = "organizationRoles";
    private static final Set<String> LIST_ALLOWED_VALUES = Set.copyOf(PolicyConstants.VALID_ORGANIZATION_ROLES);

    @Mock
    private Monitor monitor;

    @Mock
    private ParticipantAgentPolicyContext policyContext;

    private ProhibitionCredentialClaimEvaluationFunction<ParticipantAgentPolicyContext> function;
    private ProhibitionCredentialClaimEvaluationFunction<ParticipantAgentPolicyContext> listFunction;

    @BeforeEach
    void setUp() {
        function = ProhibitionCredentialClaimEvaluationFunction.create(CLAIM_NAME, ALLOWED_VALUES, monitor);
        listFunction = ProhibitionCredentialClaimEvaluationFunction.create(LIST_CLAIM_NAME, LIST_ALLOWED_VALUES, monitor);
    }

    // ── Operator validation ───────────────────────────────────────────────────

    @Test
    void evaluate_neqOperator_claimMatchesRightOperand_returnsFalse() {
        // Policy: prohibit euEeaStatus NEQ "third-country"
        // Participant has euEeaStatus = "third-country" → IS equal, NEQ condition NOT met → no prohibition
        var agent = buildAgentWithScalarClaim(CLAIM_NAME, "third-country");
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = function.evaluate(Operator.NEQ, "third-country",
                Prohibition.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
    }

    @Test
    void evaluate_neqOperator_claimDiffersFromRightOperand_returnsTrue() {
        // Policy: prohibit euEeaStatus NEQ "third-country"
        // Participant has euEeaStatus = "eu-member-state" → differs → prohibition applies → deny
        var agent = buildAgentWithScalarClaim(CLAIM_NAME, "eu-member-state");
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = function.evaluate(Operator.NEQ, "third-country",
                Prohibition.Builder.newInstance().build(), policyContext);

        assertThat(result).isTrue();
    }

    @Test
    void evaluate_invalidOperatorIN_returnsFalseAndReportsProblem() {
        var result = function.evaluate(Operator.IN, "third-country",
                Prohibition.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
        verify(policyContext).reportProblem(anyString());
    }

    // ── Null rightOperand ────────────────────────────────────────────────────

    @Test
    void evaluate_nullRightOperand_returnsFalseAndReportsProblem() {
        var result = function.evaluate(Operator.EQ, null,
                Prohibition.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
        verify(policyContext).reportProblem(anyString());
    }

    // ── Invalid rightOperand value (not in allowed set) ───────────────────────

    @Test
    void evaluate_rightOperandNotInAllowedValues_returnsFalseAndReportsProblem() {
        var result = function.evaluate(Operator.EQ, "unknown-status",
                Prohibition.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
        verify(policyContext).reportProblem(anyString());
    }

    // ── Free-form field (empty allowed-values set) ────────────────────────────

    @Test
    void evaluate_freeFormField_anyRightOperandProceedsToClaim() {
        var freeFormFunction = ProhibitionCredentialClaimEvaluationFunction
                .<ParticipantAgentPolicyContext>create("jurisdictionCountry", Set.of(), monitor);

        // No agent → reports problem, but gets past allowed-values check
        when(policyContext.participantAgent()).thenReturn(null);

        var result = freeFormFunction.evaluate(Operator.EQ, "CN",
                Prohibition.Builder.newInstance().build(), policyContext);

        // Fails at agent extraction, but the invalid-value gate was NOT triggered
        assertThat(result).isFalse();
        verify(policyContext).reportProblem(anyString());
    }

    @Test
    void evaluate_nullAllowedValues_treatedAsEmptySet_andEvaluatesClaim() {
        var nullAllowedValuesFunction = ProhibitionCredentialClaimEvaluationFunction
                .<ParticipantAgentPolicyContext>create(CLAIM_NAME, null, monitor);

        // Participant has the prohibited value → prohibition applies
        var agent = buildAgentWithScalarClaim(CLAIM_NAME, "third-country");
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = nullAllowedValuesFunction.evaluate(Operator.EQ, "third-country",
                Prohibition.Builder.newInstance().build(), policyContext);

        assertThat(result).isTrue();
    }

    // ── Participant agent ─────────────────────────────────────────────────────

    @Test
    void evaluate_nullParticipantAgent_returnsFalseAndReportsProblem() {
        when(policyContext.participantAgent()).thenReturn(null);

        var result = function.evaluate(Operator.EQ, "third-country",
                Prohibition.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
        verify(policyContext).reportProblem(anyString());
    }

    @Test
    void evaluate_noVcClaimInAgent_returnsFalseAndReportsProblem() {
        var agent = new ParticipantAgent(Map.of(), Map.of());
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = function.evaluate(Operator.EQ, "third-country",
                Prohibition.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
        verify(policyContext).reportProblem(anyString());
    }

    // ── Core semantic: prohibited condition MET → returns true ────────────────

    @Test
    void evaluate_prohibitedScalarClaimMatches_returnsTrue() {
        // Policy: prohibit euEeaStatus EQ "third-country"
        // Participant has euEeaStatus = "third-country" → prohibition applies → deny
        var agent = buildAgentWithScalarClaim(CLAIM_NAME, "third-country");
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = function.evaluate(Operator.EQ, "third-country",
                Prohibition.Builder.newInstance().build(), policyContext);

        assertThat(result).isTrue();
    }

    @Test
    void evaluate_prohibitedScalarClaimDoesNotMatch_returnsFalse() {
        // Policy: prohibit euEeaStatus EQ "third-country"
        // Participant has euEeaStatus = "eu-member-state" → prohibition does NOT apply → allow
        var agent = buildAgentWithScalarClaim(CLAIM_NAME, "eu-member-state");
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = function.evaluate(Operator.EQ, "third-country",
                Prohibition.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
    }

    @Test
    void evaluate_claimMissingFromSubject_returnsFalse() {
        // Credential subject exists but does not contain the claim key → prohibition doesn't apply
        var cs = CredentialSubject.Builder.newInstance()
                .id(HOLDER_DID)
                .claim("someOtherClaim", "someValue")
                .build();
        var vc = buildVcWithSubject(cs);
        var agent = new ParticipantAgent(Map.of("vc", List.of(vc)), Map.of());
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = function.evaluate(Operator.EQ, "third-country",
                Prohibition.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
    }

    // ── Credential type filtering ─────────────────────────────────────────────

    @Test
    void evaluate_prohibitedConditionInAnyCredentialType_returnsTrue() {
        // Prohibition scanning works across all VC types (not just MembershipCredential)
        var cs = CredentialSubject.Builder.newInstance()
                .id(HOLDER_DID)
                .claim(CLAIM_NAME, "third-country")
                .build();
        var vc = VerifiableCredential.Builder.newInstance()
                .type("SomeOtherCredential")
                .credentialSubject(cs)
                .issuer(new Issuer(ISSUER_DID, Map.of()))
                .issuanceDate(Instant.MIN)
                .build();

        var agent = new ParticipantAgent(Map.of("vc", List.of(vc)), Map.of());
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = function.evaluate(Operator.EQ, "third-country",
                Prohibition.Builder.newInstance().build(), policyContext);

        assertThat(result).isTrue();
    }

    // ── List claim evaluation ─────────────────────────────────────────────────

    @Test
    void evaluate_prohibitedListClaimContainsProhibitedValue_returnsTrue() {
        // Policy: prohibit organizationRoles EQ "health-data-applicant"
        // Participant has that role in list → prohibition applies → deny
        var agent = buildAgentWithListClaim(LIST_CLAIM_NAME,
                List.of("health-data-applicant", "connector-operator"));
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = listFunction.evaluate(Operator.EQ, "health-data-applicant",
                Prohibition.Builder.newInstance().build(), policyContext);

        assertThat(result).isTrue();
    }

    @Test
    void evaluate_prohibitedListClaimDoesNotContainProhibitedValue_returnsFalse() {
        // Policy: prohibit organizationRoles EQ "health-data-applicant"
        // Participant does not have that role → prohibition does NOT apply → allow
        var agent = buildAgentWithListClaim(LIST_CLAIM_NAME,
                List.of("health-data-holder", "connector-operator"));
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = listFunction.evaluate(Operator.EQ, "health-data-applicant",
                Prohibition.Builder.newInstance().build(), policyContext);

        assertThat(result).isFalse();
    }

    @Test
    void evaluate_listClaimSingleEntryMatchesProhibition_returnsTrue() {
        var agent = buildAgentWithListClaim(LIST_CLAIM_NAME, List.of("health-data-applicant"));
        when(policyContext.participantAgent()).thenReturn(agent);

        var result = listFunction.evaluate(Operator.EQ, "health-data-applicant",
                Prohibition.Builder.newInstance().build(), policyContext);

        assertThat(result).isTrue();
    }

    // ── Monitor logging ───────────────────────────────────────────────────────

    @Test
    void evaluate_onProhibitionApplied_logsWarningMessage() {
        var agent = buildAgentWithScalarClaim(CLAIM_NAME, "third-country");
        when(policyContext.participantAgent()).thenReturn(agent);

        function.evaluate(Operator.EQ, "third-country",
                Prohibition.Builder.newInstance().build(), policyContext);

        verify(monitor, atLeastOnce()).warning(anyString());
    }

    @Test
    void evaluate_onProhibitionNotApplied_doesNotLogWarning() {
        // When prohibition condition is NOT met, only a debug message is expected
        var agent = buildAgentWithScalarClaim(CLAIM_NAME, "eu-member-state");
        when(policyContext.participantAgent()).thenReturn(agent);

        function.evaluate(Operator.EQ, "third-country",
                Prohibition.Builder.newInstance().build(), policyContext);

        verify(monitor, never()).warning(anyString());
    }

    @Test
    void evaluate_onProhibitionApplied_doesNotReportProblem() {
        // Prohibition applying is the normal/expected path — not a policy error
        var agent = buildAgentWithScalarClaim(CLAIM_NAME, "third-country");
        when(policyContext.participantAgent()).thenReturn(agent);

        function.evaluate(Operator.EQ, "third-country",
                Prohibition.Builder.newInstance().build(), policyContext);

        verify(policyContext, never()).reportProblem(anyString());
    }

    @Test
    void evaluate_onValidationError_logsWarningAndReportsProblem() {
        // Validation errors are reported as problems (policy authoring error)
        when(policyContext.participantAgent()).thenReturn(null);

        function.evaluate(Operator.EQ, "third-country",
                Prohibition.Builder.newInstance().build(), policyContext);

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
