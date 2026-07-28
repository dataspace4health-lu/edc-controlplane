package com.nttdata.dataspace.cp.legalcontracting;

/**
 * Unchecked exception thrown by the Legal Contracting extension when a
 * non-recoverable error occurs during PDF generation or storage.
 */
public class LegalContractingException extends RuntimeException {

    public LegalContractingException(String message) {
        super(message);
    }

    public LegalContractingException(String message, Throwable cause) {
        super(message, cause);
    }
}
