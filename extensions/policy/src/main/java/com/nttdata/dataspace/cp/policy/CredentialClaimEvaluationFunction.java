package com.nttdata.dataspace.cp.policy;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.List;
import java.util.Set;

/**
 * Generic evaluation function for credential attribute constraints.
 *
 * <p>Handles policy constraints that check a named claim inside the
 * {@code credentialSubject} of a {@code MembershipCredential}. Supports:
 * <ul>
 *   <li>Scalar claims (String): EQ checks direct equality
 *       e.g. {@code euEeaStatus EQ "eu-member-state"}</li>
 *   <li>List claims (List&lt;String&gt;): EQ checks the required value is present in the list
 *       e.g. {@code organizationRoles EQ "health-data-holder"} passes if the list contains that role</li>
 * </ul>
 *
 * <p>If an {@code allowedValues} set is provided, the policy {@code rightOperand} is validated
 * against it before the credential is inspected — catching policy authoring errors early.
 * Pass an empty set ({@code Set.of()}) for free-form fields like {@code jurisdictionCountry}.
 *
 * <p>See {@code README.md} in this module for full usage and how to register new functions.
 */
public class CredentialClaimEvaluationFunction<C extends ParticipantAgentPolicyContext>
        extends AbstractCredentialEvaluationFunction
        implements AtomicConstraintRuleFunction<Permission, C> {

    /** Name of the credentialSubject claim this function evaluates. */
    private final String claimName;

    /**
     * Set of recognized values for the rightOperand in the policy constraint.
     * Empty set means any value is accepted (e.g. jurisdictionCountry = free-form ISO code).
     */
    private final Set<String> allowedValues;

    private final Monitor monitor;

    private CredentialClaimEvaluationFunction(String claimName, Set<String> allowedValues, Monitor monitor) {
        this.claimName = claimName;
        this.allowedValues = allowedValues == null ? Set.of() : allowedValues;
        this.monitor = monitor;
    }

    /**
     * Creates a new instance for the given claim.
     *
     * @param claimName     the credentialSubject key to inspect (e.g. "euEeaStatus")
     * @param allowedValues valid rightOperand values; pass Set.of() for unconstrained fields
     * @param monitor       EDC monitor for logging
     */
    public static <C extends ParticipantAgentPolicyContext> CredentialClaimEvaluationFunction<C> create(
            String claimName, Set<String> allowedValues, Monitor monitor) {
        return new CredentialClaimEvaluationFunction<>(claimName, allowedValues, monitor) {};
    }

    @Override
    public boolean evaluate(Operator operator, Object rightOperand, Permission permission, C policyContext) {
        monitor.debug("CredentialClaim '%s' evaluation started: operator=%s, rightOperand=%s"
                .formatted(claimName, operator, rightOperand));

        // ── Step 1: validate operator ────────────────────────────────────────
        if (!operator.equals(Operator.EQ)) {
            var msg = "Constraint '%s': invalid operator '%s', only EQ is accepted".formatted(claimName, operator);
            monitor.warning("CredentialClaim DENIED: " + msg);
            policyContext.reportProblem(msg);
            return false;
        }

        // ── Step 2: validate rightOperand is non-null ─────────────────────────
        if (rightOperand == null) {
            var msg = "Constraint '%s': rightOperand must not be null".formatted(claimName);
            monitor.warning("CredentialClaim DENIED: " + msg);
            policyContext.reportProblem(msg);
            return false;
        }

        var requiredValue = rightOperand.toString();

        // ── Step 3: validate rightOperand is a recognized value ───────────────
        // Skipped for free-form fields (allowedValues empty, e.g. jurisdictionCountry)
        if (!allowedValues.isEmpty() && !allowedValues.contains(requiredValue)) {
            var msg = "Constraint '%s': rightOperand '%s' is not a recognized value. Allowed: %s"
                    .formatted(claimName, requiredValue, allowedValues);
            monitor.warning("CredentialClaim DENIED: " + msg);
            policyContext.reportProblem(msg);
            return false;
        }

        // ── Step 4: get ParticipantAgent ──────────────────────────────────────
        var pa = policyContext.participantAgent();
        if (pa == null) {
            var msg = "No ParticipantAgent found on context.";
            monitor.warning("CredentialClaim '%s' DENIED: %s".formatted(claimName, msg));
            policyContext.reportProblem(msg);
            return false;
        }

        monitor.debug("CredentialClaim '%s': resolving credentials for participant claims: %s"
                .formatted(claimName, pa.getClaims().keySet()));

        // ── Step 5: extract VC list via base class ────────────────────────────
        var credentialResult = getCredentialList(pa);
        if (credentialResult.failed()) {
            var msg = credentialResult.getFailureDetail();
            monitor.warning("CredentialClaim '%s' DENIED: %s".formatted(claimName, msg));
            policyContext.reportProblem(msg);
            return false;
        }

        monitor.debug("CredentialClaim '%s': found %d VerifiableCredential(s)"
                .formatted(claimName, credentialResult.getContent().size()));

        // ── Step 6: search all credentials for the claim ─────────────────────
        // Note: we do NOT filter by credential type here. The claim (e.g. membershipStatus,
        // euEeaStatus) may reside in any credential (e.g. LegalPerson). Type-specific checks
        // are the responsibility of MembershipCredentialEvaluationFunction.
        //todo: remove this debug log after testing
        // credentialResult.getContent().forEach(vc ->
        //         monitor.debug("CredentialClaim '%s': scanning credential types=%s subjects=%s"
        //                 .formatted(claimName, vc.getType(),
        //                         vc.getCredentialSubject().stream()
        //                                 .map(cs -> cs.getClaims().toString())
        //                                 .toList())));
 
        var result = credentialResult.getContent().stream()
                .flatMap(vc -> vc.getCredentialSubject().stream())
                .anyMatch(cs -> claimSatisfied(cs, requiredValue));

        if (result) {
            monitor.info("CredentialClaim '%s' SATISFIED: required value '%s' confirmed in credential"
                    .formatted(claimName, requiredValue));
        } else {
            monitor.warning("CredentialClaim '%s' DENIED: claim not found or value '%s' not present in credential"
                    .formatted(claimName, requiredValue));
        }

        return result;
    }

    /**
     * Checks whether the given credentialSubject satisfies the claim requirement.
     *
     * <p>For scalar claims (String): requires exact equality.
     * <p>For list claims (List): requires {@code requiredValue} to be present anywhere in the list.
     * This models "the participant must have AT LEAST this role/type" semantics.
     */
    @SuppressWarnings("unchecked")
    private boolean claimSatisfied(CredentialSubject subject, String requiredValue) {
        var claimValue = subject.getClaims().get(claimName);

        if (claimValue == null) {
            monitor.debug("CredentialClaim '%s': claim not present in credentialSubject".formatted(claimName));
            return false;
        }

        // List claim — e.g. organizationRoles: ["health-data-holder", "connector-operator"]
        // Passes if the required value appears anywhere in the list
        if (claimValue instanceof List) {
            var list = (List<Object>) claimValue;
            boolean found = list.stream().anyMatch(v -> requiredValue.equals(v.toString()));
            monitor.debug("CredentialClaim '%s' list check: required='%s', values=%s, found=%s"
                    .formatted(claimName, requiredValue, list, found));
            return found;
        }

        // Scalar claim — e.g. euEeaStatus: "eu-member-state"
        boolean match = requiredValue.equals(claimValue.toString());
        monitor.debug("CredentialClaim '%s' scalar check: required='%s', actual='%s', match=%s"
                .formatted(claimName, requiredValue, claimValue, match));
        return match;
    }
}
