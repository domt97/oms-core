# oms-core

Shared kernel of the OMS: typed ids, `AggregateRoot`, `Money`/`Address`, exceptions and `GlobalExceptionHandler`,
`@UseCase`/`@WebAdapter`/`@PersistenceAdapter`, MDC logging (`com.dotran.oms.core.logging`), S3, Excel import template.
Architecture and rules: [oms-workspace](https://github.com/domt97/oms-workspace) (ADR 0005 for versioning).

## How services use it

- **CI and releases** use the version pinned in each service's `gradle.properties` (`coreVersion`), downloaded from
  GitHub Packages (`com.dotran.oms:oms-core`). A service picks up an oms-core change only when it bumps `coreVersion`.
- **Locally**, when `../oms-core` exists next to a service (the `oms-workspace/repos` layout), Gradle uses it as a
  composite build instead and `coreVersion` is ignored; the build log then says "oms-core: using the local ../oms-core".
  To build exactly what CI builds, move `../oms-core` aside (and have a token, below).
- Building a service **without** `../oms-core` needs a GitHub token to read the package (GitHub Packages Maven always
  requires one): a classic personal access token with only `read:packages`, in `~/.gradle/gradle.properties`:
  ```properties
  gpr.user=<github user>
  gpr.key=<token>
  ```
  Set both (otherwise a `GITHUB_TOKEN` environment variable may be used and rejected). Never commit it.

## Releasing

Only the repository owner creates tags; a published version can never be changed.

1. Merge the change into `master` (CI green).
2. Tag the commit on `master` and push the tag: `git tag v1.2.0 && git push origin v1.2.0`.
3. `release.yml` checks the tag, runs the tests and publishes `1.2.0`. Wait until it is green.
4. In each service that needs the change: bump `coreVersion=1.2.0` in a PR into `develop`.

**Pre-release for a cross-repo change** (service PR needs an unmerged oms-core change): tag the oms-core branch
`v1.2.0-rc.1`, pin it in the service PR to get CI feedback (the service's `core-pin` check stays red: an rc is never
merged), then release `v1.2.0` from `master` and pin that. An rc is published from unreviewed branch code and never
goes away; use it only for branches with an open PR.

`publish` refuses to run outside `release.yml` or for a non-release version.

### Versioning (semver)

| Bump | When |
|------|------|
| **MAJOR** | A public type, method or config key removed or renamed; a signature changed; behaviour services must adapt to (exception → status mapping, MDC keys, validation that can fail startup); Java, Spring Boot or BOM baseline bump (the AWS SDK BOM is published as a platform and can raise a service's SDK version) |
| **MINOR** | New API or config with safe defaults |
| **PATCH** | Fixes only, no API change |

oms-core has one line of development (`master`); there are no backport branches.

### A release that failed half-way

If the publish step failed after some files were uploaded, the version is broken and cannot be re-published (409).
Delete that version in the repository's **Packages** page (allowed for public packages with few downloads), or tag the
next patch. Never move or re-use a tag. If a service build still says the version cannot be found after the release
is green, re-run it with `--refresh-dependencies` (Gradle may have cached the miss).

## Build

```bash
./gradlew test
./gradlew generatePomFileForMavenJavaPublication generateMetadataFileForMavenJavaPublication sourcesJar   # inspect build/publications
```
