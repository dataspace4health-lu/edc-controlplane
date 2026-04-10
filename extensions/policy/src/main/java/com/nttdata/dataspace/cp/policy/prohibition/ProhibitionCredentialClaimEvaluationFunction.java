package com.nttdata.dataspace.cp.policy.prohibition;

import com.nttdata.dataspace.cp.policy.CredentialClaimEvaluator;
import com.nttdata.dataspace.cp.policy.permission.CredentialClaimEvaluationFunction;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Set;

/**
 * Evaluation function for credential attribute prohibition constraints.
 *
 * <p>Evaluates whether a participant's VerifiableCredential contains a claim value that matches
 * a <em>prohibited</em> condition. Returns {@code true} when the prohibition applies (access
 * denied), {@code false} when the prohibition does not apply (this rule does not block access).
 *
 * <p>Supports the same claim types as {@link CredentialClaimEvaluationFunction}, with both
 * {@link org.eclipse.edc.policy.model.Operator#EQ EQ} and
 * {@link org.eclipse.edc.policy.model.Operator#NEQ NEQ} operators:
 * <ul>
 *   <li>Scalar claims (String): EQ prohibits when the claim equals the value —
 *       e.g. {@code euEeaStatus EQ "third-country"} prohibits third-country participants;
 *       NEQ prohibits when the claim differs —
 *       e.g. {@code euEeaStatus NEQ "third-country"} prohibits non-third-country participants</li>
 *   <li>List claims (List&lt;String&gt;): EQ prohibits when the list contains the value;
 *       NEQ prohibits when the list does NOT contain the value</li>
 * </ul>
 *
 * <p>Semantic note: unlike permission evaluation, a validation error (bad operator, missing
 * agent, etc.) returns {@code false} — the prohibition is <em>not</em> applied when the
 * condition cannot be confirmed. The problem is still reported via
 * {@link org.eclipse.edc.policy.engine.spi.PolicyContext#reportProblem} to surface the
 * authoring error.
 *
 * <p>Core evaluation logic is delegated to {@link CredentialClaimEvaluator}, shared with
 * {@link CredentialClaimEvaluationFunction}.
 *
 * <p>See {@code README.md} in this module for full usage and how to register new functions.
 */
public class ProhibitionCredentialClaimEvaluationFunction<C extends ParticipantAgentPolicyContext>
        implements AtomicConstraintRuleFunction<Prohibition, C> {

    private final CredentialClaimEvaluator evaluator;
    private final String claimName;
    private final Monitor monitor;

    private ProhibitionCredentialClaimEvaluationFunction(String claimName, Set<String> allowedValues, Monitor monitor) {
        this.claimName = claimName;
        this.monitor = monitor;
        this.evaluator = new CredentialClaimEvaluator(claimName, allowedValues, monitor);
    }

    /**
     * Creates a new prohibition evaluation function for the given claim.
     *
     * @param claimName     the credentialSubject key to inspect (e.g. "euEeaStatus")
     * @param allowedValues valid rightOperand values for the prohibition;
     *                      pass {@code Set.of()} for unconstrained fields
     * @param monitor       EDC monitor for logging
     */
    public static <C extends ParticipantAgentPolicyContext> ProhibitionCredentialClaimEvaluationFunction<C> create(
            String claimName, Set<String> allowedValues, Monitor monitor) {
        return new ProhibitionCredentialClaimEvaluationFunction<>(claimName, allowedValues, monitor) {};
    }

    /**
     * Evaluates the prohibition constraint.
     *
     * @return {@code true} if the prohibited condition IS met in the participant's credential
     *         (prohibition applies — access denied); {@code false} if the condition is not met
     *         or evaluation failed (prohibition does not apply)
     */
    @Override
    public boolean evaluate(Operator operator, Object rightOperand, Prohibition prohibition, C policyContext) {
        var result = evaluator.evaluate(operator, rightOperand, policyContext);

        if (result.failed()) {
            var msg = result.getFailureDetail();
            monitor.warning("ProhibitionClaim '%s' evaluation error — prohibition not applied: %s"
                    .formatted(claimName, msg));
            policyContext.reportProblem(msg);
            return false;
        }

        if (result.getContent()) {
            monitor.warning("ProhibitionClaim '%s' APPLIES: prohibited value '%s' found in credential — access denied"
                    .formatted(claimName, rightOperand));
            return true;
        }

        monitor.debug("ProhibitionClaim '%s': prohibited condition not met for value '%s' — access not blocked by this prohibition"
                .formatted(claimName, rightOperand));
        return false;
    }
}