package com.nttdata.dataspace.cp.legalcontracting;

/**
 * Represents a single logical line of content in the contract PDF.
 * The {@link LineType} controls font, size, and indentation when rendered.
 */
public final class ContentLine {

    public enum LineType {
        TITLE, SUBTITLE, HEADING, SUBHEADING, FIELD, BODY, SMALL, SEPARATOR, SPACER, MONO
    }

    private final LineType type;
    /** Primary text (label for FIELD, full text for others). */
    private final String text;
    /** Value text, only used for FIELD type. */
    private final String value;

    private ContentLine(LineType type, String text, String value) {
        this.type = type;
        this.text = text;
        this.value = value;
    }

    public static ContentLine title(String text) {
        return new ContentLine(LineType.TITLE, text, null);
    }

    public static ContentLine subtitle(String text) {
        return new ContentLine(LineType.SUBTITLE, text, null);
    }

    public static ContentLine heading(String text) {
        return new ContentLine(LineType.HEADING, text, null);
    }

    public static ContentLine subheading(String text) {
        return new ContentLine(LineType.SUBHEADING, text, null);
    }

    /** Renders as "  label: value" with label in bold and value in regular font. */
    public static ContentLine field(String label, String value) {
        return new ContentLine(LineType.FIELD, label, value);
    }

    public static ContentLine body(String text) {
        return new ContentLine(LineType.BODY, text, null);
    }

    public static ContentLine small(String text) {
        return new ContentLine(LineType.SMALL, text, null);
    }

    /** A horizontal rule (thin separator line). */
    public static ContentLine separator() {
        return new ContentLine(LineType.SEPARATOR, null, null);
    }

    /** A blank line of normal body height. */
    public static ContentLine spacer() {
        return new ContentLine(LineType.SPACER, null, null);
    }

    /** Monospaced body text, used for JSON/code blocks. */
    public static ContentLine mono(String text) {
        return new ContentLine(LineType.MONO, text, null);
    }

    public LineType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public String getValue() {
        return value;
    }
}
