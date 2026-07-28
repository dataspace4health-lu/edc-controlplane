package com.nttdata.dataspace.cp.legalcontracting;

import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class FilesystemContractDocumentStoreTest {

    @Mock
    private Monitor monitor;

    @TempDir
    Path tempDir;

    private FilesystemContractDocumentStore store;

    @BeforeEach
    void setUp() {
        store = new FilesystemContractDocumentStore(tempDir.toString(), monitor);
    }

    @Test
    void store_andRetrieve_roundTrip() {
        byte[] data = "PDF content".getBytes();
        store.store("agr-001", data);

        Optional<byte[]> result = store.retrieve("agr-001");
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(data);
    }

    @Test
    void exists_afterStore_returnsTrue() {
        store.store("agr-002", "data".getBytes());
        assertThat(store.exists("agr-002")).isTrue();
    }

    @Test
    void exists_beforeStore_returnsFalse() {
        assertThat(store.exists("agr-missing")).isFalse();
    }

    @Test
    void retrieve_nonExistent_returnsEmpty() {
        assertThat(store.retrieve("nonexistent")).isEmpty();
    }

    @Test
    void store_createsDirectoryLazily() {
        // Use a subdirectory that doesn't exist yet
        FilesystemContractDocumentStore lazyStore = new FilesystemContractDocumentStore(
                tempDir.resolve("subdir").toString(), monitor);
        lazyStore.store("agr-003", "data".getBytes());
        assertThat(lazyStore.exists("agr-003")).isTrue();
    }

    @Test
    void store_overwritesPreviousValue() {
        store.store("agr-004", "first".getBytes());
        store.store("agr-004", "second".getBytes());
        Optional<byte[]> result = store.retrieve("agr-004");
        assertThat(new String(result.get())).isEqualTo("second");
    }

    @Test
    void store_sanitizesAgreementId_preventsPathTraversal() {
        // An ID with path traversal characters should be sanitized to underscores
        store.store("../../etc/passwd", "data".getBytes());
        // The store should not fail and the file should be stored safely
        assertThat(store.exists("../../etc/passwd")).isTrue();
        // Verify the actual file is inside tempDir (not at the traversed path)
        assertThat(tempDir.resolve(".._.._etc_passwd.pdf").toFile().exists()).isTrue();
    }

    @Test
    void store_invalidDirectory_throwsLegalContractingException() {
        // Use a path where a file exists instead of a directory (can't create subdirs)
        FilesystemContractDocumentStore badStore = new FilesystemContractDocumentStore(
                tempDir.resolve("notadir").toString(), monitor);
        // Create a FILE at the path to block directory creation
        try {
            tempDir.resolve("notadir").toFile().createNewFile();
        } catch (Exception e) {
            return; // skip if we can't create the blocking file
        }
        assertThatThrownBy(() -> badStore.store("agr-x", "data".getBytes()))
                .isInstanceOf(LegalContractingException.class);
    }
}
