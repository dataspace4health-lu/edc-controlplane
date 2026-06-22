---
description: "Guides Axet to generate comprehensive, well-structured documentation for existing code — READMEs, API docs, and inline comments."
author: "NTT Data Lux - Data & AI Team"
version: "1.0"
category: "Documentation"
tags: ["documentation", "readme", "api-docs", "comments"]
globs: ["*.*"]
---

# Create Documentation

When asked to document code, produce clear, accurate, and useful documentation that helps developers understand the codebase quickly.

## Documentation Approach

1. **Read Before Writing** — Analyze the code thoroughly before documenting. Understand the architecture, data flow, and key design decisions.

2. **Audience Awareness** — Write for the developer who will maintain or use this code next. Assume they're competent but unfamiliar with this specific codebase.

3. **Accuracy Over Completeness** — It's better to document fewer things correctly than everything superficially. Never guess — if something is unclear, say so.

## Documentation Files (Required)
1. `README.md`: Overview of the project, installation, usage, and links to other documentation.
2. `CONTRIBUTING.md`: Guidelines for contributing, coding standards, and PR processes.
3. `CHANGELOG.md`: Track changes between releases following semantic versioning.
4. `LICENSE`: Defines the open-source/commercial license for the repository.
5. `SECURITY.md`: Security policies, vulnerability reporting guidelines, and best practices.
6. `ARCHITECTURE.md`: High-level system architecture, design decisions, and dependencies.
7. `API.md`: API documentation (endpoints, purpose, authentication details, parameters, retrun values, errors, examples).
8. `DEPLOYMENT.md`: Deployment instructions, environment variables, and rollback steps.
9. `TROUBLESHOOTING.md`: Common issues and solutions for debugging errors.
10. `INFRASTRUCTURE.md`: Description of cloud services, infrastructure setup, and provisioning (Terraform, Ansible, Helm).

## Inline Comments

Add comments that explain **why**, not **what**:

- ✅ `// Retry up to 3 times because the upstream API is flaky during deploys`
- ❌ `// Set retries to 3`

Focus comments on:
- Non-obvious business logic or domain rules
- Workarounds with context on why they're needed
- Performance-critical sections explaining the optimization
- Complex algorithms with a brief explanation of the approach