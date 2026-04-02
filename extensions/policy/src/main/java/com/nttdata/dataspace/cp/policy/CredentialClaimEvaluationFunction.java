package com.nttdata.dataspace.cp.policy;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Set;

/**
 * Evaluation function for credential attribute permission constraints.
 *
 * <p>Evaluates whether a participant's VerifiableCredential contains a named claim with the
 * required value, granting or denying access accordingly. Supports:
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
 * <p>Core evaluation logic is delegated to {@link CredentialClaimEvaluator}, which is also
 * shared by {@link ProhibitionCredentialClaimEvaluationFunction}.
 *
 * <p>See {@code README.md} in this module for full usage and how to register new functions.
 */
public class CredentialClaimEvaluationFunction<C extends ParticipantAgentPolicyContext>
        implements AtomicConstraintRuleFunction<Permission, C> {

    private final CredentialClaimEvaluator evaluator;
    private final String claimName;
    private final Monitor monitor;

    private CredentialClaimEvaluationFunction(String claimName, Set<String> allowedValues, Monitor monitor) {
        this.claimName = claimName;
        this.monitor = monitor;
        this.evaluator = new CredentialClaimEvaluator(claimName, allowedValues, monitor);
    }

    /**
     * Creates a new permission evaluation function for the given claim.
     *
     * @param claimName     the credentialSubject key to inspect (e.g. "euEeaStatus")
     * @param allowedValues valid rightOperand values; pass {@code Set.of()} for unconstrained fields
     * @param monitor       EDC monitor for logging
     */
    public static <C extends ParticipantAgentPolicyContext> CredentialClaimEvaluationFunction<C> create(
            String claimName, Set<String> allowedValues, Monitor monitor) {
        return new CredentialClaimEvaluationFunction<>(claimName, allowedValues, monitor) {};
    }

    /**
     * Evaluates the permission constraint.
     *
     * @return {@code true} if the participant's credential contains the required claim value
     *         (permission granted); {@code false} if the condition is not met or evaluation
     *         failed (permission denied)
     */
    @Override
    public boolean evaluate(Operator operator, Object rightOperand, Permission permission, C policyContext) {
        var result = evaluator.evaluate(operator, rightOperand, policyContext);

        if (result.failed()) {
            var msg = result.getFailureDetail();
            monitor.warning("CredentialClaim DENIED: " + msg);
            policyContext.reportProblem(msg);
            return false;
        }

        if (result.getContent()) {
            monitor.info("CredentialClaim '%s' SATISFIED: required value '%s' confirmed in credential"
                    .formatted(claimName, rightOperand));
            return true;
        }

        monitor.warning("CredentialClaim '%s' DENIED: claim not found or value '%s' not present in credential"
                .formatted(claimName, rightOperand));
        return false;
    }
}
