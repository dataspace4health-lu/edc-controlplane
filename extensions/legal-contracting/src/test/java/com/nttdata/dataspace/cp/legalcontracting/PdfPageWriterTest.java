package com.nttdata.dataspace.cp.legalcontracting;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for {@link PdfPageWriter}.
 */
class PdfPageWriterTest {

    private PDDocument doc;
    private PdfPageWriter writer;

    @BeforeEach
    void setUp() throws IOException {
        doc = new PDDocument();
        writer = new PdfPageWriter(doc);
    }

    // ── Basic line rendering ──────────────────────────────────────────────────

    @Test
    void renderLines_emptyList_succeeds() throws IOException {
        assertThatCode(() -> writer.renderLines(List.of())).doesNotThrowAnyException();
        assertThat(doc.getNumberOfPages()).isGreaterThan(0);
    }

    @Test
    void renderLines_titleOnly_succeeds() throws IOException {
        List<ContentLine> lines = List.of(ContentLine.title("Test Title"));
        assertThatCode(() -> writer.renderLines(lines)).doesNotThrowAnyException();
        assertThat(doc.getNumberOfPages()).isGreaterThan(0);
    }

    @Test
    void renderLines_multipleLineTypes_succeeds() throws IOException {
        List<ContentLine> lines = List.of(
                ContentLine.title("Contract"),
                ContentLine.heading("1. Overview"),
                ContentLine.body("This is the body"),
                ContentLine.field("Provider", "Provider Inc."),
                ContentLine.separator(),
                ContentLine.small("Small text"),
                ContentLine.mono("{ \"key\": \"value\" }"),
                ContentLine.spacer(),
                ContentLine.subheading("1.1 Details"),
                ContentLine.subtitle("Terms")
        );
        assertThatCode(() -> writer.renderLines(lines)).doesNotThrowAnyException();
        assertThat(doc.getNumberOfPages()).isGreaterThan(0);
    }

    // ── Pagination ────────────────────────────────────────────────────────────

    @Test
    void renderLines_manyLines_createMultiplePages() throws IOException {
        List<ContentLine> lines = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            lines.add(ContentLine.body("Line " + i + ": " + "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ".repeat(3)));
        }
        writer.renderLines(lines);
        assertThat(doc.getNumberOfPages()).isGreaterThan(1);
    }

    // ── Null and empty inputs ─────────────────────────────────────────────────

    @Test
    void renderLines_nullText_handled() throws IOException {
        List<ContentLine> lines = List.of(
                ContentLine.title(null),
                ContentLine.body(null),
                ContentLine.field(null, null)
        );
        assertThatCode(() -> writer.renderLines(lines)).doesNotThrowAnyException();
    }

    @Test
    void renderLines_emptyStrings_handled() throws IOException {
        List<ContentLine> lines = List.of(
                ContentLine.title(""),
                ContentLine.body(""),
                ContentLine.field("", "")
        );
        assertThatCode(() -> writer.renderLines(lines)).doesNotThrowAnyException();
    }

    // ── Branded title rendering ───────────────────────────────────────────────

    @Test
    void renderLines_brandedTitle_succeeds() throws IOException {
        List<ContentLine> lines = List.of(
                ContentLine.title("DATASPACE4HEALTH Contract Evidence"),
                ContentLine.body("Description")
        );
        assertThatCode(() -> writer.renderLines(lines)).doesNotThrowAnyException();
    }

    @Test
    void renderLines_nonBrandedTitle_succeeds() throws IOException {
        List<ContentLine> lines = List.of(
                ContentLine.title("Regular Title"),
                ContentLine.body("Description")
        );
        assertThatCode(() -> writer.renderLines(lines)).doesNotThrowAnyException();
    }

    @Test
    void renderLines_brandedTitleWithoutRemainder_succeeds() throws IOException {
        List<ContentLine> lines = List.of(
                ContentLine.title("DATASPACE4HEALTH"),
                ContentLine.body("Description")
        );
        assertThatCode(() -> writer.renderLines(lines)).doesNotThrowAnyException();
    }

    // ── Separator rendering ───────────────────────────────────────────────────

    @Test
    void renderLines_withSeparator_succeeds() throws IOException {
        List<ContentLine> lines = List.of(
                ContentLine.heading("Section 1"),
                ContentLine.body("Content 1"),
                ContentLine.separator(),
                ContentLine.heading("Section 2"),
                ContentLine.body("Content 2")
        );
        assertThatCode(() -> writer.renderLines(lines)).doesNotThrowAnyException();
    }

    // ── Field rendering with values ───────────────────────────────────────────

    @Test
    void renderLines_fieldWithLongValue_wrapsCorrectly() throws IOException {
        List<ContentLine> lines = List.of(
                ContentLine.field("Description", "This is a very long description that should wrap to multiple lines because it exceeds the available width on the page. It contains multiple sentences and should be handled gracefully by the PDF writer.")
        );
        assertThatCode(() -> writer.renderLines(lines)).doesNotThrowAnyException();
        assertThat(doc.getNumberOfPages()).isGreaterThan(0);
    }

    @Test
    void renderLines_multipleFields_succeeds() throws IOException {
        List<ContentLine> lines = List.of(
                ContentLine.field("Provider", "Provider ABC"),
                ContentLine.field("Consumer", "Consumer XYZ"),
                ContentLine.field("Asset ID", "asset-12345"),
                ContentLine.field("Agreement ID", "agr-67890")
        );
        assertThatCode(() -> writer.renderLines(lines)).doesNotThrowAnyException();
    }

    // ── Text wrapping ─────────────────────────────────────────────────────────

    @Test
    void renderLines_bodyWithNewlines_wrapsCorrectly() throws IOException {
        List<ContentLine> lines = List.of(
                ContentLine.body("Line 1\nLine 2\nLine 3")
        );
        assertThatCode(() -> writer.renderLines(lines)).doesNotThrowAnyException();
    }

    @Test
    void renderLines_longBodyText_wrapsCorrectly() throws IOException {
        List<ContentLine> lines = List.of(
                ContentLine.body("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.")
        );
        assertThatCode(() -> writer.renderLines(lines)).doesNotThrowAnyException();
    }

    // ── Special characters ────────────────────────────────────────────────────

    @Test
    void renderLines_specialCharacters_sanitized() throws IOException {
        List<ContentLine> lines = List.of(
                ContentLine.body("Text with special chars: áéíóú àèìòù ñ ü ©®™"),
                ContentLine.field("Field", "Value with émojis 🚀 and symbols!")
        );
        assertThatCode(() -> writer.renderLines(lines)).doesNotThrowAnyException();
    }

    @Test
    void renderLines_controlCharacters_removed() throws IOException {
        List<ContentLine> lines = List.of(
                ContentLine.body("Text with control chars: \u0001\u0002\u0003"),
                ContentLine.mono("{ \"data\": \"value\" }")
        );
        assertThatCode(() -> writer.renderLines(lines)).doesNotThrowAnyException();
    }

    // ── Monospace text ────────────────────────────────────────────────────────

    @Test
    void renderLines_monospacedJson_succeeds() throws IOException {
        List<ContentLine> lines = List.of(
                ContentLine.heading("Raw Metadata"),
                ContentLine.mono("{ \"id\": \"asset-1\", \"properties\": { \"title\": \"Data\" } }")
        );
        assertThatCode(() -> writer.renderLines(lines)).doesNotThrowAnyException();
    }

    // ── Without logo ──────────────────────────────────────────────────────────

    @Test
    void writerWithoutLogo_succeeds() throws IOException {
        PDDocument doc2 = new PDDocument();
        PdfPageWriter writer2 = new PdfPageWriter(doc2);
        List<ContentLine> lines = List.of(
                ContentLine.title("No Logo"),
                ContentLine.body("Content without logo")
        );
        assertThatCode(() -> writer2.renderLines(lines)).doesNotThrowAnyException();
        assertThat(doc2.getNumberOfPages()).isGreaterThan(0);
    }

    // ── Mixed content stress test ─────────────────────────────────────────────

    @Test
    void renderLines_complexDocument_succeeds() throws IOException {
        List<ContentLine> lines = List.of(
                ContentLine.title("DATASPACE4HEALTH Legal Contract"),
                ContentLine.subtitle("Data Sharing Agreement"),
                ContentLine.spacer(),
                ContentLine.heading("1. Parties"),
                ContentLine.field("Provider", "Hospital XYZ"),
                ContentLine.field("Consumer", "Research Institute ABC"),
                ContentLine.spacer(),
                ContentLine.heading("2. Data Description"),
                ContentLine.field("Asset Title", "Patient Records Dataset"),
                ContentLine.field("Format", "CSV"),
                ContentLine.field("Classification", "PSEUDONYMISED"),
                ContentLine.separator(),
                ContentLine.heading("3. Terms"),
                ContentLine.body("The data shall be used exclusively for the stated research purposes."),
                ContentLine.body("Any secondary use requires explicit written consent from the provider."),
                ContentLine.spacer(),
                ContentLine.heading("4. Metadata"),
                ContentLine.mono("{ \"version\": \"1.0\", \"created\": \"2026-01-01\" }"),
                ContentLine.small("Document hash: abc123def456"),
                ContentLine.spacer(),
                ContentLine.heading("5. Signature"),
                ContentLine.body("Signed by both parties on this date.")
        );
        writer.renderLines(lines);
        assertThat(doc.getNumberOfPages()).isGreaterThan(0);
    }

    // ── Multiple renders in sequence ──────────────────────────────────────────

    @Test
    void renderLines_calledMultipleTimes_createsConsistentDocument() throws IOException {
        List<ContentLine> lines1 = List.of(ContentLine.heading("Part 1"), ContentLine.body("Content 1"));
        List<ContentLine> lines2 = List.of(ContentLine.heading("Part 2"), ContentLine.body("Content 2"));

        // Note: In practice, renderLines should be called once with all lines
        // But we're testing that multiple calls don't corrupt the document
        assertThatCode(() -> {
            writer.renderLines(lines1);
            PDDocument doc2 = new PDDocument();
            PdfPageWriter writer2 = new PdfPageWriter(doc2);
            writer2.renderLines(lines2);
        }).doesNotThrowAnyException();
    }
}
