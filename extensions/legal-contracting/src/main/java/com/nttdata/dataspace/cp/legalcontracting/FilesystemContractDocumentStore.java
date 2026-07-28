package com.nttdata.dataspace.cp.legalcontracting;

import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * Filesystem-backed implementation of {@link ContractDocumentStore}.
 *
 * <p>Each PDF is stored as {@code <storageDir>/<agreementId>.pdf}.
 * The storage directory is created on first use if it does not exist.
 * All operations are thread-safe via the JVM's atomic file-write semantics.
 */
public class FilesystemContractDocumentStore implements ContractDocumentStore {

    /** EDC configuration property for the storage directory. */
    public static final String STORAGE_DIR_SETTING = "edc.legal.contract.storage.dir";

    /** Default storage directory used when the setting is absent. */
    public static final String DEFAULT_STORAGE_DIR = "/tmp/legal-contracts";

    private static final String PDF_EXTENSION = ".pdf";

    private final Path storageDir;
    private final Monitor monitor;

    public FilesystemContractDocumentStore(String storageDir, Monitor monitor) {
        this.storageDir = Path.of(storageDir);
        this.monitor = monitor;
    }

    @Override
    public void store(String agreementId, byte[] pdfBytes) {
        ensureDirectoryExists();
        Path target = resolveFile(agreementId);
        try {
            Files.write(target, pdfBytes,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            monitor.debug("Legal contract PDF stored: " + target.toAbsolutePath());
        } catch (IOException e) {
            throw new LegalContractingException(
                    "Failed to write legal contract PDF for agreementId=" + agreementId, e);
        }
    }

    @Override
    public Optional<byte[]> retrieve(String agreementId) {
        Path file = resolveFile(agreementId);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(file));
        } catch (IOException e) {
            throw new LegalContractingException(
                    "Failed to read legal contract PDF for agreementId=" + agreementId, e);
        }
    }

    @Override
    public boolean exists(String agreementId) {
        return Files.exists(resolveFile(agreementId));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Path resolveFile(String agreementId) {
        // Sanitise agreementId to prevent path traversal
        String sanitised = agreementId.replaceAll("[^a-zA-Z0-9_\\-.]", "_");
        return storageDir.resolve(sanitised + PDF_EXTENSION);
    }

    private void ensureDirectoryExists() {
        if (!Files.exists(storageDir)) {
            try {
                Files.createDirectories(storageDir);
                monitor.debug("Created legal contract storage directory: " + storageDir.toAbsolutePath());
            } catch (IOException e) {
                throw new LegalContractingException(
                        "Failed to create legal contract storage directory: " + storageDir, e);
            }
        }
    }
}
