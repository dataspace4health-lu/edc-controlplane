package com.nttdata.dataspace.cp.legalcontracting;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContentLineTest {

    @Test
    void title_createsCorrectLine() {
        ContentLine line = ContentLine.title("My Title");
        assertThat(line.getType()).isEqualTo(ContentLine.LineType.TITLE);
        assertThat(line.getText()).isEqualTo("My Title");
        assertThat(line.getValue()).isNull();
    }

    @Test
    void subtitle_createsCorrectLine() {
        ContentLine line = ContentLine.subtitle("Sub");
        assertThat(line.getType()).isEqualTo(ContentLine.LineType.SUBTITLE);
        assertThat(line.getText()).isEqualTo("Sub");
    }

    @Test
    void heading_createsCorrectLine() {
        ContentLine line = ContentLine.heading("1. Heading");
        assertThat(line.getType()).isEqualTo(ContentLine.LineType.HEADING);
        assertThat(line.getText()).isEqualTo("1. Heading");
    }

    @Test
    void subheading_createsCorrectLine() {
        ContentLine line = ContentLine.subheading("1.1 Sub");
        assertThat(line.getType()).isEqualTo(ContentLine.LineType.SUBHEADING);
        assertThat(line.getText()).isEqualTo("1.1 Sub");
    }

    @Test
    void field_createsCorrectLine() {
        ContentLine line = ContentLine.field("Label", "Value");
        assertThat(line.getType()).isEqualTo(ContentLine.LineType.FIELD);
        assertThat(line.getText()).isEqualTo("Label");
        assertThat(line.getValue()).isEqualTo("Value");
    }

    @Test
    void body_createsCorrectLine() {
        ContentLine line = ContentLine.body("Body text");
        assertThat(line.getType()).isEqualTo(ContentLine.LineType.BODY);
        assertThat(line.getText()).isEqualTo("Body text");
    }

    @Test
    void small_createsCorrectLine() {
        ContentLine line = ContentLine.small("Small text");
        assertThat(line.getType()).isEqualTo(ContentLine.LineType.SMALL);
        assertThat(line.getText()).isEqualTo("Small text");
    }

    @Test
    void separator_createsCorrectLine() {
        ContentLine line = ContentLine.separator();
        assertThat(line.getType()).isEqualTo(ContentLine.LineType.SEPARATOR);
        assertThat(line.getText()).isNull();
        assertThat(line.getValue()).isNull();
    }

    @Test
    void spacer_createsCorrectLine() {
        ContentLine line = ContentLine.spacer();
        assertThat(line.getType()).isEqualTo(ContentLine.LineType.SPACER);
        assertThat(line.getText()).isNull();
    }

    @Test
    void mono_createsCorrectLine() {
        ContentLine line = ContentLine.mono("{ \"key\": \"value\" }");
        assertThat(line.getType()).isEqualTo(ContentLine.LineType.MONO);
        assertThat(line.getText()).isEqualTo("{ \"key\": \"value\" }");
    }
}
