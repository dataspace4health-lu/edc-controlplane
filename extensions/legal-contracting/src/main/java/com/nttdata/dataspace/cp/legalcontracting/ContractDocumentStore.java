package com.nttdata.dataspace.cp.legalcontracting;

import java.util.Optional;

/**
 * Storage abstraction for generated legal contract PDF documents.
 *
 * <p>Each contract agreement is identified by its {@code agreementId}.
 * Implementations must be thread-safe.
 */
public interface ContractDocumentStore {

    /**
     * Stores the PDF bytes for the given agreement ID, overwriting any
     * previously stored document for the same ID.
     *
     * @param agreementId the contract agreement ID (used as document key)
     * @param pdfBytes    the serialised PDF document
     */
    void store(String agreementId, byte[] pdfBytes);

    /**
     * Retrieves the PDF bytes for the given agreement ID.
     *
     * @param agreementId the contract agreement ID
     * @return an {@link Optional} containing the PDF bytes, or empty if none found
     */
    Optional<byte[]> retrieve(String agreementId);

    /**
     * Returns {@code true} if a document has already been stored for the given ID.
     *
     * @param agreementId the contract agreement ID
     * @return {@code true} if present
     */
    boolean exists(String agreementId);
}
