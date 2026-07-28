package com.nttdata.dataspace.cp.legalcontracting;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ContractContentBuilder}.
 */
class ContractContentBuilderTest {

    // ── abbrevIri ─────────────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
        "https://w3id.org/edc/v0.0.1/ns/membershipStatus,membershipStatus",
        "http://www.w3.org/ns/odrl/2/use,use",
        "https://www.w3.org/ns/odrl/2/use,use",
        "http://purl.org/dc/terms/format,format",
        "https://purl.org/dc/terms/format,format",
        "odrl:use,use",
        "edc:type,type",
        "dct:format,format",
        "https://example.com/path/token,token",
        "plain,plain",
    })
    void abbrevIri_stripsKnownPrefixes(String input, String expected) {
        assertThat(ContractContentBuilder.abbrevIri(input)).isEqualTo(expected);
    }

    @Test
    void abbrevIri_null_returnsNA() {
        assertThat(ContractContentBuilder.abbrevIri(null)).isEqualTo("N/A");
    }

    @Test
    void abbrevIri_unknownIri_returnsLastSegment() {
        assertThat(ContractContentBuilder.abbrevIri("https://custom.org/ns/foo/bar")).isEqualTo("bar");
    }

    // ── camelToTitle ──────────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
        "membershipStatus,Membership Status",
        "organizationType,Organization Type",
        "dacApproval,DAC Approval",
        "euEeaStatus,EU EEA Status",
        "permitIssued,Permit Issued",
        "jurisdictionCountry,Jurisdiction Country",
        "dataProtectionRole,Data Protection Role",
        "supportedProtocols,Supported Protocols",
    })
    void camelToTitle_convertsCorrectly(String input, String expected) {
        assertThat(ContractContentBuilder.camelToTitle(input)).isEqualTo(expected);
    }

    @Test
    void camelToTitle_null_returnsNull() {
        assertThat(ContractContentBuilder.camelToTitle(null)).isNull();
    }

    @Test
    void camelToTitle_emptyString_returnsEmpty() {
        assertThat(ContractContentBuilder.camelToTitle("")).isEmpty();
    }

    // ── abbrev ────────────────────────────────────────────────────────────────

    @Test
    void abbrev_pathKey_returnsLastSegment() {
        assertThat(ContractContentBuilder.abbrev("https://example.com/ns/myProp")).isEqualTo("myProp");
    }

    @Test
    void abbrev_hashKey_returnsLocalPart() {
        assertThat(ContractContentBuilder.abbrev("https://example.com/ns#myProp")).isEqualTo("myProp");
    }

    @Test
    void abbrev_plainKey_returnsAsIs() {
        assertThat(ContractContentBuilder.abbrev("myProp")).isEqualTo("myProp");
    }

    @Test
    void abbrev_keyEndingWithSlash_returnsFullKey() {
        String key = "https://example.com/";
        assertThat(ContractContentBuilder.abbrev(key)).isEqualTo(key);
    }

    // ── str ───────────────────────────────────────────────────────────────────

    @Test
    void str_null_returnsNA() {
        assertThat(ContractContentBuilder.str(null)).isEqualTo("N/A");
    }

    @Test
    void str_nonNull_returnsToString() {
        assertThat(ContractContentBuilder.str("hello")).isEqualTo("hello");
        assertThat(ContractContentBuilder.str(42)).isEqualTo("42");
    }

    // ── isKnownProp ───────────────────────────────────────────────────────────

    @Test
    void isKnownProp_knownLongKey_returnsTrue() {
        assertThat(ContractContentBuilder.isKnownProp(ContractContentBuilder.PROP_TITLE_LONG)).isTrue();
        assertThat(ContractContentBuilder.isKnownProp(ContractContentBuilder.PROP_DESCRIPTION_LONG)).isTrue();
        assertThat(ContractContentBuilder.isKnownProp(ContractContentBuilder.PROP_PUBLISHER_LONG)).isTrue();
    }

    @Test
    void isKnownProp_knownShortKey_returnsTrue() {
        assertThat(ContractContentBuilder.isKnownProp(ContractContentBuilder.PROP_TITLE_SHORT)).isTrue();
        assertThat(ContractContentBuilder.isKnownProp(ContractContentBuilder.PROP_FORMAT_SHORT)).isTrue();
    }

    @Test
    void isKnownProp_idSuffixes_returnsTrue() {
        assertThat(ContractContentBuilder.isKnownProp("https://w3id.org/edc/v0.0.1/ns/id")).isTrue();
        assertThat(ContractContentBuilder.isKnownProp("edc:id")).isTrue();
        assertThat(ContractContentBuilder.isKnownProp("some/id")).isTrue();
    }

    @Test
    void isKnownProp_unknownKey_returnsFalse() {
        assertThat(ContractContentBuilder.isKnownProp("https://custom.org/unknown")).isFalse();
    }

    // ── assetProp ─────────────────────────────────────────────────────────────

    @Test
    void assetProp_longKeyPresent_returnsValue() {
        Asset asset = Asset.Builder.newInstance().id("a1")
                .property(ContractContentBuilder.PROP_TITLE_LONG, "Long Title")
                .build();
        assertThat(ContractContentBuilder.assetProp(asset, ContractContentBuilder.PROP_TITLE_LONG,
                ContractContentBuilder.PROP_TITLE_SHORT)).isEqualTo("Long Title");
    }

    @Test
    void assetProp_onlyShortKeyPresent_returnsValue() {
        Asset asset = Asset.Builder.newInstance().id("a1")
                .property("title", "Short Title")
                .build();
        assertThat(ContractContentBuilder.assetProp(asset, ContractContentBuilder.PROP_TITLE_LONG,
                ContractContentBuilder.PROP_TITLE_SHORT)).isEqualTo("Short Title");
    }

    @Test
    void assetProp_onlyEdcNsKey_returnsValue() {
        Asset asset = Asset.Builder.newInstance().id("a1")
                .property("https://w3id.org/edc/v0.0.1/ns/title", "EDC Title")
                .build();
        assertThat(ContractContentBuilder.assetProp(asset, ContractContentBuilder.PROP_TITLE_LONG,
                ContractContentBuilder.PROP_TITLE_SHORT)).isEqualTo("EDC Title");
    }

    @Test
    void assetProp_noKeyPresent_returnsNA() {
        Asset asset = Asset.Builder.newInstance().id("a1").build();
        assertThat(ContractContentBuilder.assetProp(asset, ContractContentBuilder.PROP_TITLE_LONG,
                ContractContentBuilder.PROP_TITLE_SHORT)).isEqualTo("N/A");
    }

    // ── addRuleLines ─────────────────────────────────────────────────────────

    @Test
    void addRuleLines_atomicConstraint_rendersAsField() {
        AtomicConstraint constraint = AtomicConstraint.Builder.newInstance()
                .leftExpression(new LiteralExpression("membershipStatus"))
                .operator(Operator.EQ)
                .rightExpression(new LiteralExpression("active"))
                .build();
        Permission rule = Permission.Builder.newInstance()
                .action(Action.Builder.newInstance().type("odrl:use").build())
                .constraint(constraint)
                .build();

        List<ContentLine> lines = new java.util.ArrayList<>();
        ContractContentBuilder.addRuleLines(lines, rule);

        String text = linesToText(lines);
        assertThat(text).contains("use").contains("Membership Status").contains("active");
    }

    @Test
    void addRuleLines_andConstraint_expandsInnerConstraints() {
        AtomicConstraint inner = AtomicConstraint.Builder.newInstance()
                .leftExpression(new LiteralExpression("organizationType"))
                .operator(Operator.EQ)
                .rightExpression(new LiteralExpression("hospital"))
                .build();
        AndConstraint and = AndConstraint.Builder.newInstance().constraint(inner).build();
        Permission rule = Permission.Builder.newInstance()
                .action(Action.Builder.newInstance().type("use").build())
                .constraint(and)
                .build();

        List<ContentLine> lines = new java.util.ArrayList<>();
        ContractContentBuilder.addRuleLines(lines, rule);

        String text = linesToText(lines);
        assertThat(text).contains("ALL of").contains("Organization Type").contains("hospital");
    }

    @Test
    void addRuleLines_permissionWithDuties_rendersDuties() {
        Duty duty = Duty.Builder.newInstance()
                .action(Action.Builder.newInstance().type("report").build())
                .build();
        Permission rule = Permission.Builder.newInstance()
                .action(Action.Builder.newInstance().type("use").build())
                .duty(duty)
                .build();

        List<ContentLine> lines = new java.util.ArrayList<>();
        ContractContentBuilder.addRuleLines(lines, rule);

        String text = linesToText(lines);
        assertThat(text).contains("Pre-condition Duties").contains("report");
    }

    @Test
    void addRuleLines_dutyWithConsequences_rendersConsequences() {
        Duty consequence = Duty.Builder.newInstance()
                .action(Action.Builder.newInstance().type("notify").build())
                .build();
        Duty rule = Duty.Builder.newInstance()
                .action(Action.Builder.newInstance().type("delete").build())
                .consequence(consequence)
                .build();

        List<ContentLine> lines = new java.util.ArrayList<>();
        ContractContentBuilder.addRuleLines(lines, rule);

        String text = linesToText(lines);
        assertThat(text).contains("Consequences").contains("notify");
    }

    @Test
    void addRuleLines_noConstraints_showsOnlyAction() {
        Permission rule = Permission.Builder.newInstance()
                .action(Action.Builder.newInstance().type("use").build())
                .build();

        List<ContentLine> lines = new java.util.ArrayList<>();
        ContractContentBuilder.addRuleLines(lines, rule);

        assertThat(lines).isNotEmpty();
        assertThat(linesToText(lines)).contains("use");
    }

    // ── operatorSymbol (tested indirectly via addRuleLines) ───────────────────

    @ParameterizedTest
    @CsvSource({
        "EQ,=", "NEQ,!=", "GT,>", "GEQ,>=", "LT,<", "LEQ,<=",
        "IN,in", "HAS_PART,has part", "IS_PART_OF,is part of",
        "IS_ANY_OF,is any of", "IS_ALL_OF,is all of", "IS_NONE_OF,is none of",
        "CUSTOM,CUSTOM",
    })
    void operatorSymbol_viaAtomicConstraint(String opName, String expectedSymbol) {
        Operator op = safeOperator(opName);
        if (op == null) {
            return; // skip if operator not available in this EDC version
        }
        AtomicConstraint constraint = AtomicConstraint.Builder.newInstance()
                .leftExpression(new LiteralExpression("x"))
                .operator(op)
                .rightExpression(new LiteralExpression("y"))
                .build();
        Permission rule = Permission.Builder.newInstance()
                .action(Action.Builder.newInstance().type("use").build())
                .constraint(constraint)
                .build();
        List<ContentLine> lines = new java.util.ArrayList<>();
        ContractContentBuilder.addRuleLines(lines, rule);
        assertThat(linesToText(lines)).contains(expectedSymbol + " y");
    }

    // ── build() sections with prohibitions and obligations ────────────────────

    @Test
    void build_withProhibitions_rendersSection5() {
        AtomicConstraint c = buildAtomicConstraint("euEeaStatus", Operator.EQ, "outside");
        Prohibition prohibition = Prohibition.Builder.newInstance()
                .action(Action.Builder.newInstance().type("use").build())
                .constraint(c)
                .build();
        Policy policy = Policy.Builder.newInstance().prohibition(prohibition).build();
        ContractAgreement agreement = buildAgreement("agr-p", policy);
        ContractNegotiation negotiation = buildNegotiation("agr-p");

        List<ContentLine> lines = ContractContentBuilder.build(
                negotiation, agreement, null, "{}", "metaHash", "2026-01-01T00:00:00Z", "docHash");

        String text = linesToText(lines);
        assertThat(text).contains("5. Prohibitions").contains("EU EEA Status").contains("outside");
        assertThat(text).contains("No obligations");
    }

    @Test
    void build_withObligations_rendersSection6() {
        Duty obligation = Duty.Builder.newInstance()
                .action(Action.Builder.newInstance().type("report").build())
                .build();
        Policy policy = Policy.Builder.newInstance().duty(obligation).build();
        ContractAgreement agreement = buildAgreement("agr-o", policy);
        ContractNegotiation negotiation = buildNegotiation("agr-o");

        List<ContentLine> lines = ContractContentBuilder.build(
                negotiation, agreement, null, "{}", "metaHash", "2026-01-01T00:00:00Z", "docHash");

        String text = linesToText(lines);
        assertThat(text).contains("6. Obligations").contains("report");
        assertThat(text).contains("No prohibitions");
    }

    @Test
    void build_withAsset_rendersSection3WithOptionalFields() {
        Policy policy = Policy.Builder.newInstance().build();
        ContractAgreement agreement = buildAgreement("agr-a", policy);
        ContractNegotiation negotiation = buildNegotiation("agr-a");
        Asset asset = Asset.Builder.newInstance().id("asset-1")
                .property("title", "Health Dataset")
                .property("description", "A test dataset")
                .property("publisher", "Hospital A")
                .property("format", "CSV")
                .property("classification", "PSEUDONYMISED")
                .property("healthDcatVersion", "1.0")
                .property("healthDcatHash", "abc123")
                .property("customProp", "customValue")
                .build();

        List<ContentLine> lines = ContractContentBuilder.build(
                negotiation, agreement, asset, "{}", "metaHash", "2026-01-01T00:00:00Z", "docHash");

        String text = linesToText(lines);
        assertThat(text).contains("Health Dataset").contains("Hospital A").contains("CSV")
                .contains("PSEUDONYMISED").contains("1.0").contains("abc123").contains("customValue");
    }

    @Test
    void build_nullAsset_showsFallbackMessage() {
        Policy policy = Policy.Builder.newInstance().build();
        ContractAgreement agreement = buildAgreement("agr-null", policy);
        ContractNegotiation negotiation = buildNegotiation("agr-null");

        List<ContentLine> lines = ContractContentBuilder.build(
                negotiation, agreement, null, "{}", "hash", "2026-01-01T00:00:00Z", "dh");

        assertThat(linesToText(lines)).contains("Asset metadata is not available");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String linesToText(List<ContentLine> lines) {
        StringBuilder sb = new StringBuilder();
        for (ContentLine l : lines) {
            if (l.getText() != null) sb.append(l.getText()).append('\n');
            if (l.getValue() != null) sb.append(l.getValue()).append('\n');
        }
        return sb.toString();
    }

    private static AtomicConstraint buildAtomicConstraint(String left, Operator op, String right) {
        return AtomicConstraint.Builder.newInstance()
                .leftExpression(new LiteralExpression(left))
                .operator(op)
                .rightExpression(new LiteralExpression(right))
                .build();
    }

    private static ContractAgreement buildAgreement(String agreementId, Policy policy) {
        return ContractAgreement.Builder.newInstance()
                .id(agreementId)
                .providerId("did:web:provider")
                .consumerId("did:web:consumer")
                .assetId("asset-1")
                .contractSigningDate(Instant.now().getEpochSecond())
                .policy(policy)
                .build();
    }

    private static ContractNegotiation buildNegotiation(String agreementId) {
        return ContractNegotiation.Builder.newInstance()
                .id("neg-" + agreementId)
                .counterPartyId("did:web:consumer")
                .counterPartyAddress("https://consumer/dsp")
                .protocol("dataspace-protocol-http")
                .contractAgreement(buildAgreement(agreementId, Policy.Builder.newInstance().build()))
                .build();
    }

    private static Operator safeOperator(String name) {
        try {
            return Operator.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
