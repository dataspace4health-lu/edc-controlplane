package com.nttdata.dataspace.cp.policy;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.spi.result.Result;

import java.util.List;

/**
 * Base class for credential evaluation functions.
 *
 * <p>Extracts the {@code List<VerifiableCredential>} that EDC's DCP layer stores in
 * the {@link org.eclipse.edc.participant.spi.ParticipantAgent} under the JWT {@code "vc"} claim.
 * Subclasses call {@link #getCredentialList} and then inspect the returned credentials.
 *
 * <p>See {@code README.md} in this module for a guide on writing new evaluation functions.
 */
public class AbstractCredentialEvaluationFunction {

    /**
     * The key under which EDC's DCP processor stores the parsed VerifiableCredential
     * objects in the ParticipantAgent's claims map.
     *
     * This is the standard JWT "vc" claim — written by EDC's identity/DCP layer
     * when it processes the counterpart's presentation token.
     */
    private static final String VC_CLAIM = "vc";

    /**
     * Extracts the list of VerifiableCredentials from the ParticipantAgent.
     *
     * <p>EDC's DCP layer puts all credentials presented by the counterpart into
     * the agent's claims map under "vc". This method retrieves and validates that list.
     *
     * @param agent the participant agent populated by EDC's identity layer
     * @return success with the VC list, or failure with a descriptive message
     */
    protected Result<List<VerifiableCredential>> getCredentialList(ParticipantAgent agent) {
        var vcListClaim = agent.getClaims().get(VC_CLAIM);

        if (vcListClaim == null) {
            return Result.failure("ParticipantAgent did not contain a '%s' claim.".formatted(VC_CLAIM));
        }
        if (!(vcListClaim instanceof List)) {
            return Result.failure("ParticipantAgent contains a '%s' claim, but the type is incorrect. Expected %s, received %s.".formatted(VC_CLAIM, List.class.getName(), vcListClaim.getClass().getName()));
        }
        var vcList = (List<VerifiableCredential>) vcListClaim;
        if (vcList.isEmpty()) {
            return Result.failure("ParticipantAgent contains a '%s' claim but it did not contain any VerifiableCredentials.".formatted(VC_CLAIM));
        }
        return Result.success(vcList);
    }
}