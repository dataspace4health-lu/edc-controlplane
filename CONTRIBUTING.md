# CONTRIBUTING.md

# Contributing to edc-controlplane

## Getting Started

### Prerequisites

- JDK 17
- Docker (for image builds and integration testing)
- Git

### Clone and Build

```bash
git clone <repository-url>
cd edc-controlplane
./gradlew shadowJar
```

### Run Tests

```bash
# Unit tests for all subprojects
./gradlew test

# With aggregated HTML report
./gradlew test allTests
```

---

## Project Structure

```
edc-controlplane/
├── build.gradle.kts                         ← root build: aggregates all extensions
├── settings.gradle.kts                      ← subproject declarations
├── Dockerfile                               ← multi-stage container build
├── extensions/
│   ├── policy/                              ← credential-based policy evaluation
│   │   └── src/main/java/…/cp/policy/
│   ├── scope/                               ← request scope injection
│   │   └── src/main/java/…/cp/
│   ├── s3/                                  ← S3 DataAddress validation
│   │   └── src/main/java/…/cp/s3/
│   └── common/
│       ├── http/client/                     ← configurable OkHttpClient
│       └── decentralized-claims-issuers-configuration/  ← trusted issuer registration
├── config/checkstyle/checkstyle.xml
└── gradle/libs.versions.toml
```

---

## Coding Standards

### Style

Checkstyle is enforced on every build. Run it explicitly with:

```bash
./gradlew checkstyle
```

Reports are written to `build/reports/checkstyle/lint.html`. The build fails on any violation.

### Code File Convention

Add a relative path comment on the first line of every new file:

```java
// extensions/policy/src/main/java/com/nttdata/dataspace/cp/policy/MyClass.java
package com.nttdata.dataspace.cp.policy;
```

### Naming

- Extension classes: `*Extension` implementing `ServiceExtension`
- Evaluation functions: `*EvaluationFunction` implementing `AtomicConstraintRuleFunction`
- Constants classes: `*Constants` with a private constructor
- Tests: `*Test` in the same package as the class under test

---

## Adding a New Policy Constraint

The generic `CredentialClaimEvaluator` handles any `credentialSubject` top-level claim. No new class is needed for simple scalar or list checks.

**Steps**:

1. Open `extensions/policy/src/main/java/…/cp/policy/PolicyConstants.java`
2. Add the claim name constant:
   ```java
   public static final String MY_CLAIM = "myClaim";
   ```
3. Add the IRI constraint key:
   ```java
   public static final String MY_CLAIM_CONSTRAINT_KEY = EDC_NAMESPACE + MY_CLAIM;
   ```
4. Add the valid values set (or `Set.of()` for free-form):
   ```java
   public static final Set<String> VALID_MY_CLAIM_VALUES = Set.of("value-a", "value-b");
   ```
5. Open `PolicyEvaluationExtension.java` and register in the appropriate scope method(s):
   ```java
   bindToNegotiation(PolicyConstants.MY_CLAIM_CONSTRAINT_KEY,
           PolicyConstants.MY_CLAIM,
           PolicyConstants.VALID_MY_CLAIM_VALUES);

   bindToTransfer(PolicyConstants.MY_CLAIM_CONSTRAINT_KEY,
           PolicyConstants.MY_CLAIM,
           PolicyConstants.VALID_MY_CLAIM_VALUES);
   ```
6. Add a unit test in `PolicyEvaluationExtensionTest` verifying the constraint is registered in both scopes.

If the new policy requires cross-credential logic, time-based rules, or a non-standard credential structure, write a new `AtomicConstraintRuleFunction` implementation and register it directly via `policyEngine.registerFunction(...)`.

---

## Adding a New Extension Module

1. Create the directory:
   ```bash
   mkdir -p extensions/my-feature/src/main/java/com/nttdata/dataspace/cp/myfeature
   mkdir -p extensions/my-feature/src/main/resources/META-INF/services
   mkdir -p extensions/my-feature/src/test/java/com/nttdata/dataspace/cp/myfeature
   ```

2. Create `extensions/my-feature/build.gradle.kts` with only the dependencies this module needs.

3. Create the service registration file:
   ```
   extensions/my-feature/src/main/resources/META-INF/services/org.eclipse.edc.spi.system.ServiceExtension
   ```
   Contents (one class per line):
   ```
   com.nttdata.dataspace.cp.myfeature.MyFeatureExtension
   ```

4. Register in `settings.gradle.kts`:
   ```kotlin
   include("extensions:my-feature")
   findProject(":extensions:my-feature")?.name = "my-feature"
   ```

5. Add to root `build.gradle.kts` dependencies:
   ```kotlin
   implementation(project(":extensions:my-feature"))
   ```

---

## Testing Guidelines

Follow the test pyramid:

- **Unit tests (70%)** — test `initialize()`, evaluation functions, and validators in isolation using Mockito mocks
- **Integration tests (20%)** — test interactions with real EDC services where feasible
- **E2E tests (10%)** — exercise via `integration-edc` Helm deployment

### Test Quality Checklist

Before submitting a PR:

- [ ] New/modified code has corresponding unit tests
- [ ] Test names describe the scenario: `should_deny_when_membershipStatus_is_suspended`
- [ ] Each test follows Arrange-Act-Assert
- [ ] No hardcoded sleeps or ordering dependencies between tests
- [ ] `./gradlew test` passes without errors

### Coverage

JaCoCo reports are generated per subproject at `<subproject>/build/reports/jacoco/html/`. Target 80%+ line coverage for business logic in `policy` and `scope` extensions.

---

## Git Commit Convention

Use [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/):

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

Common types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`

Examples:
```
feat(policy): add organizationSize constraint to negotiation scope
fix(scope): prevent duplicate scope entries when default equals existing
docs: update ARCHITECTURE.md with S3 validator data flow
chore(deps): pin jackson-core to 2.21.1 for GHSA-72hv-8253-57qq
```

---

## Pull Request Process

1. Branch from `main` using a descriptive name: `feat/add-size-constraint` or `fix/scope-duplicate-entries`
2. Keep PRs focused — one logical change per PR
3. Ensure `./gradlew test checkstyle` passes locally before pushing
4. Update `CHANGELOG.md` under `[Unreleased]`
5. If the PR adds a new policy constraint, update `extensions/policy/README.md`

---

## Dependency Management

All dependency versions are declared in `gradle/libs.versions.toml`. When adding a new dependency:

1. Add the version to `[versions]` if not already present
2. Add the library alias to `[libraries]`
3. Reference via `libs.<alias>` in `build.gradle.kts`

Security patches applied via `resolutionStrategy.eachDependency` in the root `build.gradle.kts` — document the CVE in the comment.
