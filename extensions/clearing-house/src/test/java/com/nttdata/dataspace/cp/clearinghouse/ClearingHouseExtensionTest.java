package com.nttdata.dataspace.cp.clearinghouse;

import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationEvent;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessEvent;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClearingHouseExtensionTest {

    @Mock
    private Monitor monitor;

    @Mock
    private EventRouter eventRouter;

    @Mock
    private ContractNegotiationStore contractNegotiationStore;

    @Mock
    private TransferProcessStore transferProcessStore;

    @Mock
    private ServiceExtensionContext context;

    @InjectMocks
    private ClearingHouseExtension extension;

    @BeforeEach
    void setUp() {
        when(context.getMonitor()).thenReturn(monitor);
        when(monitor.withPrefix(anyString())).thenReturn(monitor);
    }

    // ── name ──────────────────────────────────────────────────────────────────

    @Test
    void name_returnsExpectedValue() {
        assertThat(extension.name()).isEqualTo("Digital Clearing House");
    }

    // ── initialize — no URL configured ────────────────────────────────────────

    @Test
    void initialize_whenUrlIsNull_logsWarning() {
        when(context.getSetting(ClearingHouseExtension.CLEARING_HOUSE_URL_SETTING, null))
                .thenReturn(null);

        extension.initialize(context);

        verify(monitor).warning(anyString());
    }

    @Test
    void initialize_whenUrlIsNull_doesNotRegisterEventListeners() {
        when(context.getSetting(ClearingHouseExtension.CLEARING_HOUSE_URL_SETTING, null))
                .thenReturn(null);

        extension.initialize(context);

        verify(eventRouter, never()).registerSync(any(), any());
    }

    @Test
    void initialize_whenUrlIsBlank_logsWarning() {
        when(context.getSetting(ClearingHouseExtension.CLEARING_HOUSE_URL_SETTING, null))
                .thenReturn("   ");

        extension.initialize(context);

        verify(monitor).warning(anyString());
    }

    @Test
    void initialize_whenUrlIsBlank_doesNotRegisterEventListeners() {
        when(context.getSetting(ClearingHouseExtension.CLEARING_HOUSE_URL_SETTING, null))
                .thenReturn("   ");

        extension.initialize(context);

        verify(eventRouter, never()).registerSync(any(), any());
    }

    // ── initialize — URL configured ───────────────────────────────────────────

    @Test
    void initialize_whenUrlIsSet_logsInfo() {
        when(context.getSetting(ClearingHouseExtension.CLEARING_HOUSE_URL_SETTING, null))
                .thenReturn("http://clearing-house:3010");
        when(context.getSetting(eq(ClearingHouseExtension.PARTICIPANT_ID_SETTING), anyString()))
                .thenReturn("did:web:participant");

        extension.initialize(context);

        verify(monitor, org.mockito.Mockito.atLeastOnce()).info(anyString());
    }

    @Test
    void initialize_whenUrlIsSet_registersContractNegotiationEventListener() {
        when(context.getSetting(ClearingHouseExtension.CLEARING_HOUSE_URL_SETTING, null))
                .thenReturn("http://clearing-house:3010");
        when(context.getSetting(eq(ClearingHouseExtension.PARTICIPANT_ID_SETTING), anyString()))
                .thenReturn("did:web:participant");

        extension.initialize(context);

        verify(eventRouter).registerSync(eq(ContractNegotiationEvent.class), any());
    }

    @Test
    void initialize_whenUrlIsSet_registersTransferProcessEventListener() {
        when(context.getSetting(ClearingHouseExtension.CLEARING_HOUSE_URL_SETTING, null))
                .thenReturn("http://clearing-house:3010");
        when(context.getSetting(eq(ClearingHouseExtension.PARTICIPANT_ID_SETTING), anyString()))
                .thenReturn("did:web:participant");

        extension.initialize(context);

        verify(eventRouter).registerSync(eq(TransferProcessEvent.class), any());
    }

    @Test
    void initialize_whenUrlIsSet_usesConfiguredParticipantId() {
        when(context.getSetting(ClearingHouseExtension.CLEARING_HOUSE_URL_SETTING, null))
                .thenReturn("http://clearing-house:3010");
        when(context.getSetting(eq(ClearingHouseExtension.PARTICIPANT_ID_SETTING), anyString()))
                .thenReturn("did:web:custom-participant");

        // Should complete without error — participant ID is read from context
        extension.initialize(context);

        verify(context).getSetting(eq(ClearingHouseExtension.PARTICIPANT_ID_SETTING), anyString());
    }

    @Test
    void initialize_whenUrlIsSet_logsListenersRegistered() {
        when(context.getSetting(ClearingHouseExtension.CLEARING_HOUSE_URL_SETTING, null))
                .thenReturn("http://clearing-house:3010");
        when(context.getSetting(eq(ClearingHouseExtension.PARTICIPANT_ID_SETTING), anyString()))
                .thenReturn("did:web:participant");

        extension.initialize(context);

        // Two info calls: one for init, one for listeners registered
        verify(monitor, org.mockito.Mockito.atLeast(2)).info(anyString());
    }

    // ── shutdown ──────────────────────────────────────────────────────────────

    @Test
    void shutdown_whenNeverInitialised_doesNotThrow() {
        // clearingHouseService is null — shutdown should handle it gracefully
        extension.shutdown();
    }

    @Test
    void shutdown_afterInitialisation_shutsDownService() {
        when(context.getSetting(ClearingHouseExtension.CLEARING_HOUSE_URL_SETTING, null))
                .thenReturn("http://clearing-house:3010");
        when(context.getSetting(eq(ClearingHouseExtension.PARTICIPANT_ID_SETTING), anyString()))
                .thenReturn("did:web:participant");

        extension.initialize(context);
        // Should not throw
        extension.shutdown();
    }
}
