package com.nttdata.dataspace.cp.legalcontracting;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.MultiplicityConstraint;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.policy.model.Rule;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Assembles the ordered list of {@link ContentLine} objects that represent
 * the full text of a DS4H Contract Evidence PDF document.
 *
 * <p>This class contains no I/O; it only produces the logical content model.
 * Rendering to PDF is handled by {@link PdfPageWriter}.
 */
public class ContractContentBuilder {

    // ── HealthDCAT-AP / DCAT property keys ───────────────────────────────────
    static final String PROP_TITLE_LONG = "https://healthdcat-ap.github.io/healthdcat-ap.html#title";
    static final String PROP_TITLE_SHORT = "title";
    static final String PROP_DESCRIPTION_LONG = "https://purl.org/dc/terms/description";
    static final String PROP_DESCRIPTION_SHORT = "description";
    static final String PROP_PUBLISHER_LONG = "https://purl.org/dc/terms/publisher";
    static final String PROP_PUBLISHER_SHORT = "publisher";
    static final String PROP_FORMAT_LONG = "https://purl.org/dc/terms/format";
    static final String PROP_FORMAT_SHORT = "format";
    static final String PROP_CLASS_LONG = "https://healthdcat-ap.github.io/healthdcat-ap.html#healthCategory";
    static final String PROP_CLASS_SHORT = "classification";
    static final String PROP_VER_LONG = "https://www.w3.org/TR/vocab-dcat-2/#Property:resource_version";
    static final String PROP_VER_SHORT = "healthDcatVersion";
    static final String PROP_HASH_LONG = "https://healthdcat-ap.github.io/healthdcat-ap.html#checksum";
    static final String PROP_HASH_SHORT = "healthDcatHash";

    private static final Set<String> KNOWN_PROPS = Set.of(
            PROP_TITLE_LONG, PROP_TITLE_SHORT,
            PROP_DESCRIPTION_LONG, PROP_DESCRIPTION_SHORT,
            PROP_PUBLISHER_LONG, PROP_PUBLISHER_SHORT,
            PROP_FORMAT_LONG, PROP_FORMAT_SHORT,
            PROP_CLASS_LONG, PROP_CLASS_SHORT,
            PROP_VER_LONG, PROP_VER_SHORT,
            PROP_HASH_LONG, PROP_HASH_SHORT
    );

    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private ContractContentBuilder() {
    }

    /**
     * Builds the full list of content lines for the contract evidence document.
     */
    public static List<ContentLine> build(ContractNegotiation negotiation,
                                          ContractAgreement agreement,
                                          Asset asset,
                                          String odrlJson,
                                          String metaHash,
                                          String timestamp,
                                          String docHash) {
        List<ContentLine> lines = new ArrayList<>();
        Policy policy = agreement.getPolicy();

        buildHeader(lines, timestamp);
        buildSection1(lines, negotiation, agreement);
        buildSection2(lines, negotiation, agreement);
        buildSection3(lines, agreement, asset);
        buildSection4(lines, policy);
        buildSection5(lines, policy);
        buildSection6(lines, policy);
        buildSection7(lines, negotiation, agreement, odrlJson, metaHash, timestamp, docHash);

        return lines;
    }

    // ── Section builders ──────────────────────────────────────────────────────

    private static void buildHeader(List<ContentLine> lines, String timestamp) {
        lines.add(ContentLine.title("DATASPACE4HEALTH - Contract Evidence"));
        lines.add(ContentLine.subtitle("Finalized EDC Agreement - Human-Readable Technical Summary"));
        lines.add(ContentLine.spacer());
        lines.add(ContentLine.small("Generated: " + timestamp
                + "   |   Status: Finalized EDC Agreement"));
        lines.add(ContentLine.separator());
    }

    private static void buildSection1(List<ContentLine> lines,
                                       ContractNegotiation neg,
                                       ContractAgreement agr) {
        lines.add(ContentLine.heading("1. Agreement Identification"));
        lines.add(ContentLine.field("Agreement ID", agr.getId()));
        lines.add(ContentLine.field("Agreement Ref.", str(agr.getAgreementId())));
        lines.add(ContentLine.field("Agreement Finalized At",
                ISO_FMT.format(Instant.ofEpochSecond(agr.getContractSigningDate()))));
        lines.add(ContentLine.field("Negotiation ID", neg.getId()));
        lines.add(ContentLine.field("Negotiation State Timestamp",
                ISO_FMT.format(Instant.ofEpochMilli(neg.getStateTimestamp()))));
        lines.add(ContentLine.field("Negotiation Role", neg.getType().name()));
        lines.add(ContentLine.field("DSP Protocol", str(neg.getProtocol())));
        lines.add(ContentLine.field("Provider DID", str(agr.getProviderId())));
        lines.add(ContentLine.field("Consumer DID", str(agr.getConsumerId())));
        lines.add(ContentLine.separator());
    }

    private static void buildSection2(List<ContentLine> lines,
                                       ContractNegotiation neg,
                                       ContractAgreement agr) {
        lines.add(ContentLine.heading("2. Parties"));
        lines.add(ContentLine.subheading("2.1 Provider (Data Holder)"));
        lines.add(ContentLine.field("Participant DID", str(agr.getProviderId())));
        lines.add(ContentLine.field("DSP Callback Address", str(neg.getCounterPartyAddress())));
        lines.add(ContentLine.subheading("2.2 Consumer (Data Requester)"));
        lines.add(ContentLine.field("Participant DID", str(agr.getConsumerId())));
        lines.add(ContentLine.field("Counterparty ID", str(neg.getCounterPartyId())));
        lines.add(ContentLine.subheading("2.3 Identity Verification"));
        lines.add(ContentLine.field("VC Issuer (counterparty DID)", str(neg.getCounterPartyId())));
        lines.add(ContentLine.body("  Identity verified via Decentralised Claims Protocol (DCP)"));
        lines.add(ContentLine.body("  through the IdentityHub service. Verifiable Credentials were"));
        lines.add(ContentLine.body("  presented and evaluated during negotiation."));
        lines.add(ContentLine.separator());
    }

    private static void buildSection3(List<ContentLine> lines,
                                       ContractAgreement agr,
                                       Asset asset) {
        lines.add(ContentLine.heading("3. Dataset Schedule"));
        lines.add(ContentLine.field("EDC Asset ID", str(agr.getAssetId())));
        if (asset != null) {
            String title = assetProp(asset, PROP_TITLE_LONG, PROP_TITLE_SHORT);
            String description = assetProp(asset, PROP_DESCRIPTION_LONG, PROP_DESCRIPTION_SHORT);
            String publisher = assetProp(asset, PROP_PUBLISHER_LONG, PROP_PUBLISHER_SHORT);
            String format = assetProp(asset, PROP_FORMAT_LONG, PROP_FORMAT_SHORT);
            String classification = assetProp(asset, PROP_CLASS_LONG, PROP_CLASS_SHORT);
            String version = assetProp(asset, PROP_VER_LONG, PROP_VER_SHORT);
            String hash = assetProp(asset, PROP_HASH_LONG, PROP_HASH_SHORT);

            lines.add(ContentLine.field("Dataset Title", title));
            lines.add(ContentLine.field("Description", description));
            lines.add(ContentLine.field("Publisher / Data Holder", publisher));
            if (!"N/A".equals(format)) {
                lines.add(ContentLine.field("Format", format));
            }
            if (!"N/A".equals(classification)) {
                lines.add(ContentLine.field("Classification / Health Category", classification));
            }
            if (!"N/A".equals(version)) {
                lines.add(ContentLine.field("HealthDCAT-AP Metadata Version", version));
            }
            if (!"N/A".equals(hash)) {
                lines.add(ContentLine.field("HealthDCAT-AP Metadata Hash", hash));
            }
            for (Map.Entry<String, Object> e : asset.getProperties().entrySet()) {
                if (e.getValue() != null && !isKnownProp(e.getKey())) {
                    lines.add(ContentLine.field("  " + abbrev(e.getKey()),
                            Objects.toString(e.getValue())));
                }
            }
        } else {
            lines.add(ContentLine.body("  Asset metadata is not available for this agreement."));
        }
        lines.add(ContentLine.separator());
    }

    private static void buildSection4(List<ContentLine> lines, Policy policy) {
        lines.add(ContentLine.heading("4. Permissions"));
        if (policy != null && policy.getPermissions() != null && !policy.getPermissions().isEmpty()) {
            int i = 1;
            for (Permission p : policy.getPermissions()) {
                lines.add(ContentLine.subheading("Permission " + i++));
                addRuleLines(lines, p);
            }
        } else {
            lines.add(ContentLine.body("  No permissions defined in this policy."));
        }
        lines.add(ContentLine.separator());
    }

    private static void buildSection5(List<ContentLine> lines, Policy policy) {
        lines.add(ContentLine.heading("5. Prohibitions"));
        if (policy != null && policy.getProhibitions() != null && !policy.getProhibitions().isEmpty()) {
            int i = 1;
            for (Prohibition p : policy.getProhibitions()) {
                lines.add(ContentLine.subheading("Prohibition " + i++));
                addRuleLines(lines, p);
            }
        } else {
            lines.add(ContentLine.body("  No prohibitions defined in this policy."));
        }
        lines.add(ContentLine.separator());
    }

    private static void buildSection6(List<ContentLine> lines, Policy policy) {
        lines.add(ContentLine.heading("6. Obligations"));
        if (policy != null && policy.getObligations() != null && !policy.getObligations().isEmpty()) {
            int i = 1;
            for (Duty d : policy.getObligations()) {
                lines.add(ContentLine.subheading("Obligation " + i++));
                addRuleLines(lines, d);
            }
        } else {
            lines.add(ContentLine.body("  No obligations defined in this policy."));
        }
        lines.add(ContentLine.separator());
    }

    private static void buildSection7(List<ContentLine> lines,
                                       ContractNegotiation neg,
                                       ContractAgreement agr,
                                       String odrlJson,
                                       String metaHash,
                                       String timestamp,
                                       String docHash) {
        lines.add(ContentLine.heading("7. Technical Annex"));
        lines.add(ContentLine.subheading("7.1 Agreement Summary"));
        lines.add(ContentLine.field("Provider DID", str(agr.getProviderId())));
        lines.add(ContentLine.field("Consumer DID", str(agr.getConsumerId())));
        lines.add(ContentLine.field("DSP Protocol", str(neg.getProtocol())));
        lines.add(ContentLine.field("Correlation ID", str(neg.getCorrelationId())));
        lines.add(ContentLine.subheading("7.2 Document Integrity"));
        lines.add(ContentLine.field("Document Generation Timestamp", timestamp));
        lines.add(ContentLine.field("Document Hash (SHA-256)", docHash));
        lines.add(ContentLine.field("Policy/Metadata Hash (SHA-256)", metaHash));
        lines.add(ContentLine.subheading("7.3 ODRL Policy JSON-LD (Exact)"));
        lines.add(ContentLine.body("The following is the exact ODRL policy attached to this agreement:"));
        lines.add(ContentLine.spacer());
        for (String odrlLine : odrlJson.split("\n", -1)) {
            lines.add(ContentLine.mono(odrlLine));
        }
        lines.add(ContentLine.separator());
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    static String str(Object o) {
        return o != null ? o.toString() : "N/A";
    }

    static String assetProp(Asset asset, String longKey, String shortKey) {
        Object v = asset.getProperty(longKey);
        if (v == null) {
            v = asset.getProperty(shortKey);
        }
        if (v == null) {
            v = asset.getProperty("https://w3id.org/edc/v0.0.1/ns/" + shortKey);
        }
        return v != null ? v.toString() : "N/A";
    }

    static boolean isKnownProp(String key) {
        return KNOWN_PROPS.contains(key)
                || key.endsWith(":id")
                || key.endsWith("/id")
                || key.equals("https://w3id.org/edc/v0.0.1/ns/id");
    }

    static String abbrev(String key) {
        int slash = key.lastIndexOf('/');
        int hash = key.lastIndexOf('#');
        int pos = Math.max(slash, hash);
        return pos >= 0 && pos < key.length() - 1 ? key.substring(pos + 1) : key;
    }

    /**
     * Renders a policy rule (permission, prohibition or obligation) as human-readable lines.
     * Strips IRI prefixes from action types and constraint operands for readability.
     */
    static void addRuleLines(List<ContentLine> lines, Rule rule) {
        Action action = rule.getAction();
        if (action != null) {
            lines.add(ContentLine.field("    Action", abbrevIri(str(action.getType()))));
        }
        List<Constraint> constraints = rule.getConstraints();
        if (constraints != null && !constraints.isEmpty()) {
            for (Constraint c : constraints) {
                renderConstraintLines(lines, c, "    ");
            }
        }
        if (rule instanceof Permission) {
            List<Duty> duties = ((Permission) rule).getDuties();
            if (duties != null && !duties.isEmpty()) {
                lines.add(ContentLine.body("    Pre-condition Duties:"));
                for (Duty d : duties) {
                    Action da = d.getAction();
                    lines.add(ContentLine.body("      " + abbrevIri(da != null ? str(da.getType()) : "N/A")));
                }
            }
        }
        if (rule instanceof Duty) {
            List<Duty> consequences = ((Duty) rule).getConsequences();
            if (consequences != null && !consequences.isEmpty()) {
                lines.add(ContentLine.body("    Consequences:"));
                for (Duty c : consequences) {
                    Action ca = c.getAction();
                    lines.add(ContentLine.body("      " + abbrevIri(ca != null ? str(ca.getType()) : "N/A")));
                }
            }
        }
    }

    /**
     * Recursively renders a constraint as one or more field/body lines.
     * AtomicConstraints are shown as "Label = Value".
     * MultiplicityConstraints (And/Or/Xone) are expanded inline.
     */
    private static void renderConstraintLines(List<ContentLine> lines, Constraint c, String indent) {
        if (c instanceof AtomicConstraint) {
            AtomicConstraint ac = (AtomicConstraint) c;
            String rawLeft = ac.getLeftExpression() instanceof LiteralExpression
                    ? abbrevIri(((LiteralExpression) ac.getLeftExpression()).getValue().toString()) : "?";
            String op = ac.getOperator() != null ? operatorSymbol(ac.getOperator().name()) : "=";
            String right = ac.getRightExpression() instanceof LiteralExpression
                    ? ((LiteralExpression) ac.getRightExpression()).getValue().toString() : "?";
            String label = camelToTitle(rawLeft);
            lines.add(ContentLine.field(indent + label, op + " " + right));
        } else if (c instanceof MultiplicityConstraint) {
            MultiplicityConstraint mc = (MultiplicityConstraint) c;
            String kind = mc.getClass().getSimpleName()
                    .replace("AndConstraint", "ALL of")
                    .replace("OrConstraint", "ANY of")
                    .replace("XoneConstraint", "ONE of");
            lines.add(ContentLine.body(indent + kind + ":"));
            for (Constraint inner : mc.getConstraints()) {
                renderConstraintLines(lines, inner, indent + "  ");
            }
        } else {
            lines.add(ContentLine.body(indent + c.getClass().getSimpleName()));
        }
    }

    /** Converts an ODRL operator name to a human-readable symbol. */
    private static String operatorSymbol(String op) {
        switch (op) {
            case "EQ":  return "=";
            case "NEQ": return "!=";
            case "GT":  return ">";
            case "GEQ": return ">=";
            case "LT":  return "<";
            case "LEQ": return "<=";
            case "IN":  return "in";
            case "HAS_PART": return "has part";
            case "IS_PART_OF": return "is part of";
            case "IS_ANY_OF": return "is any of";
            case "IS_ALL_OF": return "is all of";
            case "IS_NONE_OF": return "is none of";
            default:    return op;
        }
    }

    /**
     * Converts a camelCase or PascalCase token (after IRI prefix stripping)
     * into a human-readable Title Case label.
     * e.g. "membershipStatus" → "Membership Status"
     *      "euEeaStatus" → "EU EEA Status"
     */
    static String camelToTitle(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (i == 0) {
                sb.append(Character.toUpperCase(ch));
            } else if (Character.isUpperCase(ch)) {
                sb.append(' ');
                sb.append(ch);
            } else {
                sb.append(ch);
            }
        }
        // Capitalize common acronyms
        return sb.toString()
                .replace("Eu ", "EU ")
                .replace("Eea ", "EEA ")
                .replace("Dac ", "DAC ")
                .replace("Did ", "DID ")
                .replace("Dsp ", "DSP ")
                .replace("Vc ", "VC ");
    }

    /** Strips common IRI prefixes to produce a short human-readable token. */
    static String abbrevIri(String iri) {
        if (iri == null) {
            return "N/A";
        }
        // Strip well-known prefixes
        String[] prefixes = {
            "https://w3id.org/edc/v0.0.1/ns/",
            "http://www.w3.org/ns/odrl/2/",
            "https://www.w3.org/ns/odrl/2/",
            "http://purl.org/dc/terms/",
            "https://purl.org/dc/terms/",
            "odrl:",
            "edc:",
            "dct:"
        };
        for (String prefix : prefixes) {
            if (iri.startsWith(prefix)) {
                return iri.substring(prefix.length());
            }
        }
        // Fall back to last path segment
        return abbrev(iri);
    }
}
