# SECURITY.md

# Security Policy: edc-controlplane

## Supported Versions

| Version | Supported |
|---|---|
| 0.15.x (EDC 0.15.1) | ✅ Active |
| < 0.15.0 | ❌ No longer supported |

---

## Reporting a Vulnerability

Do **not** open a public GitHub issue for security vulnerabilities.

Report security issues to the DS4H security team at: **security@dataspace4health.eu** (or your organization's internal security contact).

Include:
- Description of the vulnerability
- Steps to reproduce
- Affected component(s) and version(s)
- Potential impact

You will receive an acknowledgement within 2 business days. Critical vulnerabilities will be patched in a hotfix release. Non-critical issues are scheduled for the next release cycle.

---

## Security Model

### Trust Boundaries

```
External (untrusted)                Internal (trusted)
────────────────────                ─────────────────
Counterpart CP (DSP :8092)     →    EDC DCP + PolicyEngine
Federated catalog crawlers     →    EDC DSP module
                                    │
                               ┌────▼─────────────────┐
                               │  PolicyEvaluationExt  │
                               │  (VC claim checks)    │
                               └────────────────────────┘
                                    │
                               ┌────▼─────────────────┐
                               │  TrustedIssuerRegistry│
                               │  (issuer allow-list)  │
                               └────────────────────────┘

Operator (authenticated)       →    Management API :9192
Data Plane (internal only)     →    Control API :8093
```

The DSP port (`:8092`) is the only port exposed to external counterparts. All other ports should be network-restricted to trusted internal networks.

### Authentication

| Interface | Authentication |
|---|---|
| DSP protocol (`:8092`) | DCP token (JWT with VC presentation) |
| Management API (`:9192`) | Token-based auth (EDC `auth-tokenbased`) |
| Control API (`:8093`) | Internal only — network-level restriction required |
| Health/observability (`:8181`) | Unauthenticated — restrict to cluster-internal |

### Authorization

Access control is enforced at policy evaluation time via Verifiable Credentials:

- **Catalog visibility**: `membershipStatus == "active"` is the minimum gate.
- **Contract negotiation**: Full attribute set evaluated — EU/EEA status, organization type, ownership type, roles, data protection role, jurisdiction, and supported protocols.
- **Transfer initiation**: Same constraints re-evaluated to ensure continuous compliance.
- **Prohibitions**: Participants matching prohibition constraints (e.g., `euEeaStatus == "third-country"`) are blocked even if all permission constraints pass.
- **Obligations**: `dacApproval` or `permitIssued` must be `"true"` where required by the policy.

---

## Vulnerability Remediation History

### Active Patches in This Release

| Advisory | Component | Severity | Patch Applied |
|---|---|---|---|
| GHSA-72hv-8253-57qq | `jackson-core` | HIGH | Pinned to `2.21.1` in Gradle resolution strategy |
| CVE-2026-1605 | Eclipse Jetty | HIGH | Pinned `org.eclipse.jetty:*` to `12.1.7` |
| CVE-2026-2332 | Eclipse Jetty | HIGH | Pinned `org.eclipse.jetty:*` to `12.1.7` |
| CVE-2026-45447 | OpenSSL | CRITICAL | `apt-get upgrade openssl libssl3` in Dockerfile |
| CVE-2025-68973 | GnuPG | MEDIUM | `apt-get upgrade gnupg*` in Dockerfile |

### Dependency Pinning Strategy

Security patches are applied via Gradle `resolutionStrategy.eachDependency` in `build.gradle.kts`:

```kotlin
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.fasterxml.jackson.core" && requested.name == "jackson-core") {
            useVersion("2.21.1")
            because("GHSA-72hv-8253-57qq: pin jackson-core to 2.21.1 (patched)")
        }
        if (requested.group.startsWith("org.eclipse.jetty")) {
            useVersion("12.1.7")
            because("CVE-2026-1605 and CVE-2026-2332: pin Jetty to 12.1.7 (patched)")
        }
    }
}
```

These pins **must be reviewed** on every EDC BOM upgrade. If the new BOM resolves patched versions, the pins can be removed.

---

## Container Security

### Non-Root Execution

The container runs as a non-root user:

```dockerfile
ARG APP_USER=appuser
ARG APP_UID=10100
RUN addgroup --system "$APP_USER" && adduser --uid "$APP_UID" ...
USER ${APP_UID}
```

The Helm chart enforces this via Kubernetes `SecurityContext`:

```yaml
security:
  runAsNonRoot: true
```

### Image Base

`eclipse-temurin:17-jre-jammy` — official Eclipse Foundation JRE image. OS packages are upgraded at build time to pick up the latest security patches.

### Secrets Management

- **Vault token**: Stored as a Kubernetes Secret (`edc-controlplane-vault`). For production, provision this secret externally — do not use the default `root` token from `values.yaml`.
- **Database credentials**: Stored as a Kubernetes Secret. Rotate on a defined schedule.
- **No secrets in source code**: Verified by the absence of hardcoded credentials; all secrets are injected via environment variables at runtime.

---

## Security Best Practices for Operators

### Production Checklist

- [ ] Replace the default Vault token (`root`) with a scoped, time-limited token
- [ ] Restrict Management API (`:9192`) and Control API (`:8093`) to internal network segments
- [ ] Do **not** set `edc.http.disable.tls=true` in any production environment
- [ ] Ensure all registered trusted issuers (`edc.iam.trustedissuer.*`) are reviewed and minimal
- [ ] Enable PostgreSQL TLS for database connections
- [ ] Rotate database credentials on a defined schedule
- [ ] Pin the Docker image by SHA digest in production Helm values, not just the tag
- [ ] Enable Kubernetes `readOnlyRootFilesystem` and `allowPrivilegeEscalation: false` in the pod security context
- [ ] Monitor the EDC CVE tracker and the Jetty/Jackson security advisories for new vulnerabilities

### TLS Bypass Warning

Setting `edc.http.disable.tls=true` creates an `OkHttpClient` that:
- Accepts **any** TLS certificate (including expired, self-signed, or invalid ones)
- Skips **hostname verification**

This makes all outbound HTTPS connections from the control plane vulnerable to man-in-the-middle attacks. The runtime logs a `WARNING` at startup when this flag is set. **Never deploy with this flag in production.**

---

## Dependency Scanning

Run dependency vulnerability scans before each release:

```bash
# Check for known CVEs in Gradle dependencies (requires OWASP dependency-check plugin)
./gradlew dependencyCheckAnalyze

# Or use the GitHub Dependabot alerts on the repository
```

Keep the `gradle/libs.versions.toml` lock file up to date and review Dependabot alerts promptly.
