package com.nttdata.dataspace.cp.legalcontracting;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.util.List;

/**
 * Paginates a list of {@link ContentLine} objects into an A4 PDFBox document.
 *
 * <p>Handles automatic page breaks, horizontal separator rules, and field
 * rendering (bold label + regular value on the same line).
 */
public class PdfPageWriter {

    private static final float MARGIN = 50f;
    private static final float LINE_HEIGHT_BODY = 13f;
    private static final float LINE_HEIGHT_TITLE = 20f;
    private static final float LINE_HEIGHT_HEADING = 16f;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float USABLE_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    private static final float BOTTOM_MARGIN = 50f;

    // Font sizes
    private static final float TITLE_SIZE = 14f;
    private static final float SUBTITLE_SIZE = 11f;
    private static final float HEADING_SIZE = 11f;
    private static final float SUBHEADING_SIZE = 9.5f;
    private static final float BODY_SIZE = 9f;
    private static final float SMALL_SIZE = 7.5f;
    private static final float MONO_SIZE = 7.5f;
    private static final float FIELD_LABEL_SIZE = 9f;
    private static final float FIELD_VALUE_SIZE = 9f;

    private static final float LOGO_HEIGHT = 36f;

    private final PDDocument doc;
    private final PDType1Font fontBold;
    private final PDType1Font fontRegular;
    private final PDType1Font fontMono;
    private final PDType1Font fontBoldItalic;
    private final byte[] logoBytes;

    private PDPage currentPage;
    private PDPageContentStream cs;
    private float cursorY;
    private int pageNumber = 0;

    /** Creates a writer without a logo. */
    public PdfPageWriter(PDDocument doc) throws IOException {
        this(doc, null);
    }

    /** Creates a writer that renders {@code logoBytes} (PNG/JPEG) at the top of page 1. */
    public PdfPageWriter(PDDocument doc, byte[] logoBytes) throws IOException {
        this.doc = doc;
        this.logoBytes = logoBytes;
        this.fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        this.fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        this.fontMono = new PDType1Font(Standard14Fonts.FontName.COURIER);
        this.fontBoldItalic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);
        newPage();
    }

    /**
     * Renders all content lines into the document, paginating automatically.
     *
     * @param lines the ordered list of content lines to render
     */
    public void renderLines(List<ContentLine> lines) throws IOException {
        for (ContentLine line : lines) {
            renderLine(line);
        }
        if (cs != null) {
            cs.close();
        }
    }

    // ── Per-line rendering ────────────────────────────────────────────────────

    private void renderLine(ContentLine line) throws IOException {
        switch (line.getType()) {
            case TITLE:
                ensureSpace(LINE_HEIGHT_TITLE + 4);
                drawBrandedTitle(line.getText(), MARGIN, LINE_HEIGHT_TITLE);
                break;

            case SUBTITLE:
                ensureSpace(LINE_HEIGHT_HEADING + 4);
                drawText(line.getText(), fontBoldItalic, SUBTITLE_SIZE, MARGIN, LINE_HEIGHT_HEADING);
                break;

            case HEADING:
                ensureSpace(LINE_HEIGHT_HEADING + 6);
                cursorY -= 4; // extra top gap before sections
                drawText(line.getText(), fontBold, HEADING_SIZE, MARGIN, LINE_HEIGHT_HEADING);
                break;

            case SUBHEADING:
                ensureSpace(LINE_HEIGHT_BODY + 4);
                cursorY -= 2;
                drawText(line.getText(), fontBold, SUBHEADING_SIZE, MARGIN + 8, LINE_HEIGHT_BODY);
                break;

            case FIELD:
                ensureSpace(LINE_HEIGHT_BODY);
                drawField(line.getText(), line.getValue());
                break;

            case BODY:
                ensureSpace(LINE_HEIGHT_BODY);
                drawText(line.getText(), fontRegular, BODY_SIZE, MARGIN + 4, LINE_HEIGHT_BODY);
                break;

            case SMALL:
                ensureSpace(LINE_HEIGHT_BODY - 2);
                drawText(line.getText(), fontRegular, SMALL_SIZE, MARGIN, LINE_HEIGHT_BODY - 2);
                break;

            case MONO:
                ensureSpace(LINE_HEIGHT_BODY - 1);
                drawText(line.getText(), fontMono, MONO_SIZE, MARGIN + 4, LINE_HEIGHT_BODY - 1);
                break;

            case SEPARATOR:
                ensureSpace(10);
                drawSeparator();
                cursorY -= 4;
                break;

            case SPACER:
                ensureSpace(LINE_HEIGHT_BODY);
                cursorY -= LINE_HEIGHT_BODY;
                break;

            default:
                break;
        }
    }

    // ── Drawing primitives ────────────────────────────────────────────────────

    private void drawText(String text, PDType1Font font, float size, float x, float lineH) throws IOException {
        if (text == null || text.isEmpty()) {
            cursorY -= lineH;
            return;
        }
        // Wrap long lines
        List<String> wrapped = wrapText(text, font, size, USABLE_WIDTH - (x - MARGIN));
        for (String segment : wrapped) {
            ensureSpace(lineH);
            cs.beginText();
            cs.setFont(font, size);
            cs.newLineAtOffset(x, cursorY);
            cs.showText(sanitise(segment));
            cs.endText();
            cursorY -= lineH;
        }
    }

    private void drawField(String label, String value) throws IOException {
        String safeLabel = sanitise(label != null ? label : "");
        String safeValue = sanitise(value != null ? value : "N/A");

        float indent = MARGIN + 8;
        float labelWidth = 0;
        try {
            labelWidth = fontBold.getStringWidth(safeLabel + ": ") / 1000 * FIELD_LABEL_SIZE;
        } catch (Exception e) {
            labelWidth = 120;
        }

        // Draw label in bold
        cs.beginText();
        cs.setFont(fontBold, FIELD_LABEL_SIZE);
        cs.newLineAtOffset(indent, cursorY);
        cs.showText(safeLabel + ": ");
        cs.endText();

        // Draw value in regular, possibly wrapping
        float valueX = indent + labelWidth;
        float valueWidth = USABLE_WIDTH - (valueX - MARGIN);
        List<String> wrapped = wrapText(safeValue, fontRegular, FIELD_VALUE_SIZE, valueWidth);
        boolean first = true;
        for (String segment : wrapped) {
            if (first) {
                cs.beginText();
                cs.setFont(fontRegular, FIELD_VALUE_SIZE);
                cs.newLineAtOffset(valueX, cursorY);
                cs.showText(segment);
                cs.endText();
                cursorY -= LINE_HEIGHT_BODY;
                first = false;
            } else {
                ensureSpace(LINE_HEIGHT_BODY);
                cs.beginText();
                cs.setFont(fontRegular, FIELD_VALUE_SIZE);
                cs.newLineAtOffset(indent + 12, cursorY);
                cs.showText(segment);
                cs.endText();
                cursorY -= LINE_HEIGHT_BODY;
            }
        }
    }

    private void drawSeparator() throws IOException {
        cs.setLineWidth(0.5f);
        cs.moveTo(MARGIN, cursorY);
        cs.lineTo(PAGE_WIDTH - MARGIN, cursorY);
        cs.stroke();
        cursorY -= 4;
    }

    // ── Pagination ────────────────────────────────────────────────────────────

    private void ensureSpace(float needed) throws IOException {
        if (cursorY - needed < BOTTOM_MARGIN) {
            newPage();
        }
    }

    private void newPage() throws IOException {
        if (cs != null) {
            cs.close();
        }
        currentPage = new PDPage(PDRectangle.A4);
        doc.addPage(currentPage);
        cs = new PDPageContentStream(doc, currentPage);
        cursorY = PAGE_HEIGHT - MARGIN;
        pageNumber++;

        // Page number footer
        cs.beginText();
        cs.setFont(fontRegular, SMALL_SIZE);
        cs.newLineAtOffset(PAGE_WIDTH / 2 - 20, BOTTOM_MARGIN - 20);
        cs.showText("Page " + pageNumber + "  |  Contract Evidence - DS4H");
        cs.endText();

        // Logo on page 1 only
        if (pageNumber == 1 && logoBytes != null) {
            try {
                PDImageXObject logo = PDImageXObject.createFromByteArray(doc, logoBytes, "logo");
                float logoWidth = logo.getWidth() * (LOGO_HEIGHT / logo.getHeight());
                // Right-align the logo
                float logoX = PAGE_WIDTH - MARGIN - logoWidth;
                cs.drawImage(logo, logoX, cursorY - LOGO_HEIGHT, logoWidth, LOGO_HEIGHT);
                cursorY -= (LOGO_HEIGHT + 8f);
            } catch (Exception e) {
                // Logo rendering failure must never abort PDF generation
            }
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Wraps text to fit within {@code maxWidth} points.
     */
    private static List<String> wrapText(String text, PDType1Font font, float fontSize, float maxWidth) {
        List<String> result = new java.util.ArrayList<>();
        if (text == null || text.isEmpty()) {
            result.add("");
            return result;
        }
        // Split on existing newlines first
        for (String paragraph : text.split("\n", -1)) {
            String[] words = paragraph.split(" ");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                String candidate = current.length() == 0 ? word : current + " " + word;
                float w = 0;
                try {
                    w = font.getStringWidth(sanitise(candidate)) / 1000 * fontSize;
                } catch (Exception e) {
                    w = candidate.length() * fontSize * 0.55f;
                }
                if (w > maxWidth && current.length() > 0) {
                    result.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            result.add(current.toString());
        }
        return result;
    }

    /**
     * Renders the document title. If the text starts with "DATASPACE4HEALTH", the
     * brand name is split into two colored segments:
     *   "DATASPACE4" → DS4H red  (238, 34, 40)
     *   "HEALTH"     → DS4H blue (8, 176, 239)
     * The remainder of the text is drawn in black. Any other title text is drawn
     * in plain black using the standard drawText path.
     */
    private void drawBrandedTitle(String text, float x, float lineH) throws IOException {
        if (text == null || !text.startsWith("DATASPACE4HEALTH")) {
            drawText(text, fontBold, TITLE_SIZE, x, lineH);
            return;
        }

        // Split: "DATASPACE4" | "HEALTH" | rest
        String part1 = "DATASPACE4";
        String part2 = "HEALTH";
        String rest  = sanitise(text.substring("DATASPACE4HEALTH".length()));

        float y = cursorY;

        float w1 = fontBold.getStringWidth(part1) / 1000f * TITLE_SIZE;
        float w2 = fontBold.getStringWidth(part2) / 1000f * TITLE_SIZE;

        // "DATASPACE4" in red
        cs.setNonStrokingColor(238f / 255f, 34f / 255f, 40f / 255f);
        cs.beginText();
        cs.setFont(fontBold, TITLE_SIZE);
        cs.newLineAtOffset(x, y);
        cs.showText(part1);
        cs.endText();

        // "HEALTH" in blue
        cs.setNonStrokingColor(8f / 255f, 176f / 255f, 239f / 255f);
        cs.beginText();
        cs.setFont(fontBold, TITLE_SIZE);
        cs.newLineAtOffset(x + w1, y);
        cs.showText(part2);
        cs.endText();

        // rest in black
        if (!rest.isEmpty()) {
            cs.setNonStrokingColor(0f, 0f, 0f);
            cs.beginText();
            cs.setFont(fontBold, TITLE_SIZE);
            cs.newLineAtOffset(x + w1 + w2, y);
            cs.showText(rest);
            cs.endText();
        }

        // Reset to black for subsequent elements
        cs.setNonStrokingColor(0f, 0f, 0f);
        cursorY -= lineH;
    }

    /** Removes characters outside Latin-1 range that PDType1Font cannot encode. */
    private static String sanitise(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c < 32 && c != '\n') {
                continue;
            }
            if (c > 255) {
                sb.append('?');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
