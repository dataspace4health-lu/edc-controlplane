package com.nttdata.dataspace.cp.legalcontracting;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LegalContractingExceptionTest {

    @Test
    void constructor_withMessage_storesMessage() {
        LegalContractingException ex = new LegalContractingException("something went wrong");
        assertThat(ex.getMessage()).isEqualTo("something went wrong");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void constructor_withMessageAndCause_storesBoth() {
        RuntimeException cause = new RuntimeException("root cause");
        LegalContractingException ex = new LegalContractingException("wrapper", cause);
        assertThat(ex.getMessage()).isEqualTo("wrapper");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void exceptionIsRuntimeException() {
        assertThat(new LegalContractingException("x")).isInstanceOf(RuntimeException.class);
    }
}
