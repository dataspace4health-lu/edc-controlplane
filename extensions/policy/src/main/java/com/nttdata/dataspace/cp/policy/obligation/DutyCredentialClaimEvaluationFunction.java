package com.nttdata.dataspace.cp.policy.obligation;

import com.nttdata.dataspace.cp.policy.CredentialClaimEvaluator;
import com.nttdata.dataspace.cp.policy.permission.CredentialClaimEvaluationFunction;
import com.nttdata.dataspace.cp.policy.prohibition.ProhibitionCredentialClaimEvaluationFunction;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Set;

/**
 * Evaluation function for credential attribute obligation (duty) constraints.
 *
 * <p>Evaluates whether a participant's VerifiableCredential contains a claim value that satisfies
 * an obligation condition. Returns {@code true} when the obligation IS fulfilled (access
 * proceeds); {@code false} when the obligation is NOT fulfilled or evaluation failed (access
 * denied).
 *
 * <p>Supports the same claim types as {@link CredentialClaimEvaluationFunction}:
 * <ul>
 *   <li>Scalar claims (String): EQ checks direct equality —
 *       e.g. {@code dacApproval EQ "true"} passes if the participant has DAC approval</li>
 *   <li>List claims (List&lt;String&gt;): EQ checks the required value is present in the list</li>
 * </ul>
 *
 * <p>For OR obligation semantics (e.g. {@code dacApproval EQ "true"} OR
 * {@code permitIssued EQ "true"}), use a single obligation with an {@code odrl:or} constraint
 * in the policy JSON. The two atomic constraints are then evaluated with OR logic by EDC's
 * built-in {@code OrConstraint} visitor.
 *
 * <p>Core evaluation logic is delegated to {@link CredentialClaimEvaluator}, shared with
 * {@link CredentialClaimEvaluationFunction} and {@link ProhibitionCredentialClaimEvaluationFunction}.
 *
 * <p>See {@code README.md} in this module for full usage and how to register new functions.
 */
public class DutyCredentialClaimEvaluationFunction<C extends ParticipantAgentPolicyContext>
        implements AtomicConstraintRuleFunction<Duty, C> {

    private final CredentialClaimEvaluator evaluator;
    private final String claimName;
    private final Monitor monitor;

    private DutyCredentialClaimEvaluationFunction(String claimName, Set<String> allowedValues, Monitor monitor) {
        this.claimName = claimName;
        this.monitor = monitor;
        this.evaluator = new CredentialClaimEvaluator(claimName, allowedValues, monitor);
    }

    /**
     * Creates a new obligation evaluation function for the given claim.
     *
     * @param claimName     the credentialSubject key to inspect (e.g. "dacApproval")
     * @param allowedValues valid rightOperand values for the obligation;
     *                      pass {@code Set.of()} for unconstrained fields
     * @param monitor       EDC monitor for logging
     */
    public static <C extends ParticipantAgentPolicyContext> DutyCredentialClaimEvaluationFunction<C> create(
            String claimName, Set<String> allowedValues, Monitor monitor) {
        return new DutyCredentialClaimEvaluationFunction<>(claimName, allowedValues, monitor) {};
    }

    /**
     * Evaluates the obligation constraint.
     *
     * @return {@code true} if the obligation condition IS met in the participant's credential
     *         (obligation fulfilled — access proceeds); {@code false} if the condition is not
     *         met or evaluation failed (obligation not fulfilled — access denied)
     */
    @Override
    public boolean evaluate(Operator operator, Object rightOperand, Duty duty, C policyContext) {
        var result = evaluator.evaluate(operator, rightOperand, policyContext);

        if (result.failed()) {
            var msg = result.getFailureDetail();
            monitor.warning("ObligationClaim '%s' evaluation error — obligation not fulfilled: %s"
                    .formatted(claimName, msg));
            policyContext.reportProblem(msg);
            return false;
        }

        if (result.getContent()) {
            monitor.info("ObligationClaim '%s' FULFILLED: required value '%s' confirmed in credential"
                    .formatted(claimName, rightOperand));
            return true;
        }

        monitor.debug("ObligationClaim '%s': obligation condition not met for value '%s' — access denied by this obligation"
                .formatted(claimName, rightOperand));
        return false;
    }
}