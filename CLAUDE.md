# oms-core — shared kernel of the OMS

Part of the OMS. System-wide map and rules: **`oms-workspace`**
(https://github.com/domt97/oms-workspace — `CLAUDE.md`, `architecture/`; versioning is ADR 0005).
Releasing, semver table and failed releases: **`README.md`**. This file is the short version for working in the code.

## Owns

A Java 17 library (`com.dotran.oms:oms-core`, Spring Boot 3.2.3 BOM) that every service depends on. No database, no
endpoints, no business rules of one context. Packages under `com.dotran.oms.core`:

| Package | Contains |
|---------|----------|
| `domain` (`id`, `page`) | `AggregateRoot`, `BaseDomain`, typed ids, paging types |
| `valueobject` | `Money`, `Address`, `PriceRange` |
| `annotation` | `@UseCase`, `@WebAdapter`, `@PersistenceAdapter`, `@JobAdapter` |
| `exception`, `rest` | `BusinessException` (optional machine-readable `code`, `withCode`), `NotFoundException`, `ValidationException`…; `GlobalExceptionHandler` (+ `errorResponse(...)` for service handlers), `ErrorResponse` (`code` only when set), request logging |
| `rest.client` | `RequestIdClientInterceptor` + `RequestIdClientConfig`: `X-Request-Id` on every injected `RestClient.Builder` in services that scan `com.dotran.oms` (off with `app.http-client.propagate-request-id=false`; third-party clients use `RestClient.builder()`) |
| `message` | `OutboxPayloadDecoder` (Debezium's double-encoded outbox payload; not a bean, services declare it) |
| `logging` | MDC: `MdcKeys`, `MdcProperties`, `MdcScope`, `MdcPropagation`, `LogSanitizer` |
| `cloud` | AWS config and S3 |
| `io` | Excel reading and the import template |
| `config`, `swagger`, `mapper`, `util` | `CoreConfiguration` (+ `application-core.yaml`), OpenAPI setup, `IdMapper`, small helpers |

## Before adding something here

A type belongs in oms-core only when **more than one context** needs the same thing with the same meaning (system
rule 6 in the workspace). Otherwise it stays in the owning service. Event DTOs never go here: each consumer keeps its
own copy in `infrastructure/message/dto` and the contract is the JSON Schema in `oms-workspace/architecture/contracts/`.

`domain` stays framework-free: no Spring, Hibernate or `jakarta.*` / `javax.persistence` API (Lombok and Jackson
annotations on typed ids are allowed). Spring
bridges go in `util` (e.g. `PageUtils.toPagedResult`, `toPageRequest`). `DomainHasNoFrameworkImportsTest` enforces it.

## Every change is a release decision

**The `version` in `gradle.properties` is what `master` releases.** A merge into `master` with a bumped `version`
publishes it to GitHub Packages and tags it (`publish.yml`); a published version can never be changed.

- Decide the bump from the semver table in `README.md`. Removing or renaming a public type, method or config key,
  changing a signature, the exception → status mapping, MDC keys, or the Java / Spring Boot / BOM baseline is **MAJOR**.
- A PR that changes code without a bump releases nothing; the `release-version` check says what merging will release.
- Services are not affected until they bump `coreVersion`, but a local service build uses `../oms-core` as it is on
  disk (composite build). So a local green build of a service does not prove it works with the pinned version.
- Docs-only changes (this file, `README.md`) need no bump.

## Rules for Claude

- Never create a tag or a GitHub Release, and never merge a PR in this repo: only the repository owner does.
- Bump `version` only when asked, and say in the PR what merging will release.
- Branch from `master`, PR into `master`. There is no `develop` and there are no backport branches.

## Tests and build

`./gradlew test` — plain JUnit, no Spring context needed for most of it. CI also generates the publication files (POM,
Gradle module metadata) as a dry run, so a broken publish configuration fails on the PR.
To check a change against a service before release: build that service locally with this repo as `../oms-core`.

## Pitfalls

- Anything `api` in `build.gradle` reaches every service's classpath; the AWS SDK BOM is published as a platform and can
  raise a service's SDK version.
- Config keys read at startup (`MdcProperties`, `SwaggerProperties`, cloud config) can stop a service from starting
  when their validation changes: treat that as a breaking change.
- `localstack/localstack.sh` here is not part of any setup step; local AWS resources are created by Terraform in the
  `development` repo (`docker/infra/local`).
