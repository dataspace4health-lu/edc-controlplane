package com.nttdata.dataspace.cp.policy;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;

import java.util.List;
import java.util.Set;

/**
 * Core credential claim evaluation logic, shared between permission and prohibition functions.
 *
 * <p>Evaluates whether a named claim inside a participant's VerifiableCredential satisfies a
 * constraint. Returns:
 * <ul>
 *   <li>{@link Result#failure} — validation or extraction error (unsupported operator, null
 *       rightOperand, missing agent, no VCs in presentation)</li>
 *   <li>{@code Result.success(true)} — the claim condition IS met:
 *       for {@link Operator#EQ}, the claim value equals rightOperand;
 *       for {@link Operator#NEQ}, the claim value differs from rightOperand</li>
 *   <li>{@code Result.success(false)} — evaluation completed but condition is NOT met
 *       (claim absent, or EQ value differs, or NEQ value matches)</li>
 * </ul>
 *
 * <p>Callers interpret the result according to their rule semantics:
 * <ul>
 *   <li>{@link CredentialClaimEvaluationFunction} (Permission): {@code success(true)} = grant,
 *       {@code failure} or {@code success(false)} = deny</li>
 *   <li>{@link ProhibitionCredentialClaimEvaluationFunction} (Prohibition):
 *       {@code success(true)} = prohibition applies (deny),
 *       {@code success(false)} or {@code failure} = prohibition does not apply</li>
 * </ul>
 */
class CredentialClaimEvaluator extends AbstractCredentialEvaluationFunction {

    private final String claimName;
    private final Set<String> allowedValues;
    private final Monitor monitor;

    CredentialClaimEvaluator(String claimName, Set<String> allowedValues, Monitor monitor) {
        this.claimName = claimName;
        this.allowedValues = allowedValues == null ? Set.of() : allowedValues;
        this.monitor = monitor;
    }

    /**
     * Evaluates whether the named credential claim satisfies the constraint.
     *
     * @param operator      the policy constraint operator ({@link Operator#EQ} or {@link Operator#NEQ})
     * @param rightOperand  the required value from the policy constraint
     * @param policyContext the evaluation context providing the participant agent
     * @return {@link Result#failure} on validation/extraction errors;
     *         {@code success(true)} if condition is met; {@code success(false)} otherwise
     */
    Result<Boolean> evaluate(Operator operator, Object rightOperand, ParticipantAgentPolicyContext policyContext) {
        monitor.debug("CredentialClaim '%s' evaluation started: operator=%s, rightOperand=%s"
                .formatted(claimName, operator, rightOperand));

        // ── Step 1: validate operator ────────────────────────────────────────
        if (!operator.equals(Operator.EQ) && !operator.equals(Operator.NEQ)) {
            return Result.failure("Constraint '%s': invalid operator '%s', only EQ and NEQ are accepted"
                    .formatted(claimName, operator));
        }

        // ── Step 2: validate rightOperand is non-null ─────────────────────────
        if (rightOperand == null) {
            return Result.failure("Constraint '%s': rightOperand must not be null".formatted(claimName));
        }

        var requiredValue = rightOperand.toString();

        // ── Step 3: validate rightOperand is a recognized value ───────────────
        // Skipped for free-form fields (allowedValues empty, e.g. jurisdictionCountry)
        if (!allowedValues.isEmpty() && !allowedValues.contains(requiredValue)) {
            return Result.failure("Constraint '%s': rightOperand '%s' is not a recognized value. Allowed: %s"
                    .formatted(claimName, requiredValue, allowedValues));
        }

        // ── Step 4: get ParticipantAgent ──────────────────────────────────────
        var pa = policyContext.participantAgent();
        if (pa == null) {
            return Result.failure("No ParticipantAgent found on context.");
        }

        monitor.debug("CredentialClaim '%s': resolving credentials for participant claims: %s"
                .formatted(claimName, pa.getClaims().keySet()));

        // ── Step 5: extract VC list via base class ────────────────────────────
        var credentialResult = getCredentialList(pa);
        if (credentialResult.failed()) {
            return Result.failure(credentialResult.getFailureDetail());
        }

        monitor.debug("CredentialClaim '%s': found %d VerifiableCredential(s)"
                .formatted(claimName, credentialResult.getContent().size()));

        // ── Step 6: search all credentials for the claim ─────────────────────
        // Note: we do NOT filter by credential type. The claim (e.g. euEeaStatus) may
        // reside in any credential type. Type-specific checks are out of scope here.
        boolean conditionMet = credentialResult.getContent().stream()
                .flatMap(vc -> vc.getCredentialSubject().stream())
                .anyMatch(cs -> claimSatisfied(cs, requiredValue, operator));

        return Result.success(conditionMet);
    }

    /**
     * Checks whether the given credentialSubject satisfies the claim requirement.
     *
     * <p>For {@link Operator#EQ}:
     * <ul>
     *   <li>Scalar claims (String): requires exact equality.</li>
     *   <li>List claims (List): requires {@code requiredValue} to be present anywhere in the list.</li>
     * </ul>
     * <p>For {@link Operator#NEQ}:
     * <ul>
     *   <li>Scalar claims (String): requires the claim value to differ from {@code requiredValue}.</li>
     *   <li>List claims (List): requires {@code requiredValue} to be absent from the list.</li>
     * </ul>
     * <p>Absent claims always return {@code false} regardless of operator.
     */
    @SuppressWarnings("unchecked")
    private boolean claimSatisfied(CredentialSubject subject, String requiredValue, Operator operator) {
        var claimValue = subject.getClaims().get(claimName);

        if (claimValue == null) {
            monitor.debug("CredentialClaim '%s': claim not present in credentialSubject".formatted(claimName));
            return false;
        }

        // List claim — e.g. organizationRoles: ["health-data-holder", "connector-operator"]
        if (claimValue instanceof List) {
            var list = (List<Object>) claimValue;
            if (operator == Operator.NEQ) {
                boolean notFound = list.stream().noneMatch(v -> requiredValue.equals(v.toString()));
                monitor.debug("CredentialClaim '%s' list NEQ check: required='%s', values=%s, notFound=%s"
                        .formatted(claimName, requiredValue, list, notFound));
                return notFound;
            }
            boolean found = list.stream().anyMatch(v -> requiredValue.equals(v.toString()));
            monitor.debug("CredentialClaim '%s' list EQ check: required='%s', values=%s, found=%s"
                    .formatted(claimName, requiredValue, list, found));
            return found;
        }

        // Scalar claim — e.g. euEeaStatus: "eu-member-state"
        boolean match = requiredValue.equals(claimValue.toString());
        if (operator == Operator.NEQ) {
            monitor.debug("CredentialClaim '%s' scalar NEQ check: required='%s', actual='%s', notMatch=%s"
                    .formatted(claimName, requiredValue, claimValue, !match));
            return !match;
        }
        monitor.debug("CredentialClaim '%s' scalar EQ check: required='%s', actual='%s', match=%s"
                .formatted(claimName, requiredValue, claimValue, match));
        return match;
    }
}
