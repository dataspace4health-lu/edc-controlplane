# INFRASTRUCTURE.md

# Infrastructure: edc-controlplane

## Overview

The `edc-controlplane` is deployed on Kubernetes via the Helm chart at `helm-edc-connector/components/controlplane`. It is one component of the broader `helm-edc-connector` parent chart that deploys a complete EDC participant instance.

---

## Helm Chart

**Chart location:** `helm-edc-connector/components/controlplane`

**Parent chart:** `helm-edc-connector` (deploys control plane + data plane + IdentityHub + dashboard together)

### Install as part of a full participant (recommended)

```bash
cd helm-edc-connector
make build
make install NAMESPACE=edc-acme override_values="-f participant-values.yaml"
```

### Install standalone (control plane only)

```bash
helm upgrade --install edc-controlplane ./helm-edc-connector/components/controlplane \
  --namespace edc-acme \
  --create-namespace \
  -f my-values.yaml
```

---

## Kubernetes Resources

The chart creates the following resources per release:

| Resource | Description |
|---|---|
| `Deployment` | Runs the control plane pod (1 replica by default) |
| `Service` | Exposes ports 8181, 9192, 8092, 8093, 8194 within the cluster |
| `Ingress` | Routes external traffic to the correct ports |
| `ConfigMap` | Mounts EDC configuration properties |
| `Secret` (optional) | Vault token secret — created by the chart when `createSecret: true` |
| `HorizontalPodAutoscaler` | Optional — disabled by default |

### Container

| Setting | Value |
|---|---|
| Base image | `eclipse-temurin:17-jre` |
| Run as user | UID 10100 (non-root) |
| Exposed ports | 8181, 9192, 8092, 8093, 8194 |

---

## Dependencies

### PostgreSQL

The chart includes PostgreSQL as an optional subchart.

| Mode | Configuration |
|---|---|
| Bundled (default) | `postgresql.enabled: true` — deploys a PostgreSQL pod with a PersistentVolume |
| External | `postgresql.enabled: false` — provide JDBC URL, user, and password via `extraVars` |

Default bundled database settings:

```yaml
postgresql:
  enabled: true
  auth:
    username: edc_user
    password: "<password>"
    database: edc_connector_db
    postgresPassword: "<postgres-password>"
  primary:
    persistence:
      enabled: true
      size: 5Gi
```

### HashiCorp Vault

The chart supports both bundled and external Vault.

| Mode | Configuration |
|---|---|
| Bundled dev Vault | `vault.enabled: true` — for local/integration use only |
| External Vault | `vault.enabled: false` — configure `config.vault.url` and `config.vault.tokenSecretName` |

For production, always use an external Vault with a pre-created Kubernetes secret:

```bash
kubectl create secret generic edc-vault \
  --from-literal=token=<vault-token> \
  -n edc-acme
```

```yaml
config:
  vault:
    url: "https://vault.example.com"
    createSecret: false
    tokenSecretName: "edc-vault"
    tokenSecretKey: "token"
```

---

## Ingress

The chart creates an Ingress resource when `ingress.enabled: true`.

```yaml
ingress:
  enabled: true
  hosts:
    - host: edc-acme.dataspace4health.local
      paths:
        - path: /edc/cp/base/api
          port: 8181
          pathType: Prefix
        - path: /edc/cp/management/api
          port: 9192
          pathType: Prefix
        - path: /edc/cp/dsp/api
          port: 8092
          pathType: Prefix
        - path: /edc/cp/control/api
          port: 8093
          pathType: Prefix
        - path: /edc/cp/catalog/api
          port: 8194
          pathType: Prefix
  tls:
    - secretName: edc-acme-tls
      hosts:
        - edc-acme.dataspace4health.local
```

The `config.dsp.callbackAddress` must match the externally reachable DSP URL.

---

## Health and Readiness

The Kubernetes readiness probe calls:

```
GET http://<pod-ip>:8181/edc/cp/base/api/check/health
```

The pod is marked ready only when all health checks pass (including Vault connectivity). If Vault is unavailable at startup, the pod stays unready and Kubernetes retries.

---

## Autoscaling

HPA is disabled by default. Multiple replicas are safe because all state is persisted in PostgreSQL.

```yaml
autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 3
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 70
```

---

## SQL Schema

SQL schema auto-creation is enabled by default (`config.sqlSchemaAutocreate: true`). EDC runs DDL statements on startup to create missing tables. Set to `false` in production once the schema is stable and manage migrations explicitly.

---

## Upgrade

```bash
helm upgrade edc-controlplane ./helm-edc-connector/components/controlplane \
  --namespace edc-acme \
  -f my-values.yaml
```

See [DEPLOYMENT.md](DEPLOYMENT.md) for rollback steps and upgrade notes, including the EDC version upgrade path for removing the local trusted issuer extension.

---

## Key Chart Values Reference

| Value | Description |
|---|---|
| `image.repository` | Container registry |
| `image.name` | Image name (`edc-controlplane`) |
| `image.tag` | Image tag |
| `config.participantId` | Participant DID |
| `config.issuerId` | Issuer DID (should match `participantId`) |
| `config.dsp.callbackAddress` | Externally reachable DSP URL |
| `config.sts.oauthTokenUrl` | STS token endpoint (IdentityHub) |
| `config.sts.oauthClientId` | STS client ID (participant DID) |
| `config.sts.oauthClientSecretAlias` | Vault alias for STS client secret |
| `config.sts.publicKeyId` | Public key ID for token verification |
| `config.sts.privateKeyAlias` | Vault alias for private key |
| `config.vault.url` | Vault server URL |
| `config.vault.tokenSecretName` | Kubernetes secret name holding the Vault token |
| `config.iam.trustedissuer` | Map of trusted VC issuers |
| `config.scope.catalog` | Default scope injected into catalog requests |
| `config.scope.contract` | Default scope injected into negotiation requests |
| `config.scope.transfer` | Default scope injected into transfer requests |
| `ingress.enabled` | Enable Ingress resource |
| `ingress.hosts` | Hostname and path routing |
| `postgresql.enabled` | Deploy bundled PostgreSQL |
| `vault.enabled` | Deploy bundled dev Vault |
