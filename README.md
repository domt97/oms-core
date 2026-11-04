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

Releases are continuous: **the version in `gradle.properties` is the version `master` releases.**

1. In the PR that should produce a release, bump `version` (semver, below), e.g. `1.1.0` → `1.2.0`. The PR's
   `release-version` check says what merging will release, warns when code changes without a bump, and fails for a
   version that is already released or lower than the current one.
2. Merge into `master`. `publish.yml` runs: if there is no tag `v1.2.0` yet it tests, publishes `1.2.0` to GitHub
   Packages and then creates the tag `v1.2.0` with a GitHub Release (the tag comes last: a tag means fully released).
   Nothing to type; a merge without a bump releases nothing.
3. In each service that needs the change: bump `coreVersion=1.2.0` in a PR into `develop`.

If a Publish run fails, **re-run that run** (it keeps its commit). If the version is already completely on GitHub Packages
(only the tag step failed), the re-run just creates the missing tag, on the commit recorded in the POM (`oms.commit`).
Runs go one at a time and GitHub keeps only the newest waiting run: after quick merges a run can show "cancelled"; the
next run releases whatever version `master` then has, so if two merges bumped twice, the skipped number is simply never
released. A tag `vX.Y.Z` is only ever created by `publish.yml`; a released tag whose package is missing fails the run.
`publish.yml` runs only for `master`, also when started by hand. A published version can never be changed.

**Pre-release for a cross-repo change** (service PR needs an unmerged oms-core change): the repository owner tags the
oms-core branch `v1.2.0-rc.1` (`git tag v1.2.0-rc.1 && git push origin v1.2.0-rc.1`; `release.yml` publishes it), pins it
in the service PR to get CI feedback (the service's `core-pin` check stays red: an rc is never merged), then merges oms-core
with `version=1.2.0` (published by `publish.yml`) and pins that. An rc is published from unreviewed branch code and never
goes away; use it only for branches with an open PR. `release.yml` ignores rc tags pushed by anyone but the
repository owner and refuses an rc for a version that is already released.

`publish` refuses to run outside these workflows or for a non-release version.

### Versioning (semver)

| Bump | When |
|------|------|
| **MAJOR** | A public type, method or config key removed or renamed; a signature changed; behaviour services must adapt to (exception → status mapping, MDC keys, validation that can fail startup); Java, Spring Boot or BOM baseline bump (the AWS SDK BOM is published as a platform and can raise a service's SDK version) |
| **MINOR** | New API or config with safe defaults |
| **PATCH** | Fixes only, no API change |

oms-core has one line of development (`master`); there are no backport branches.

### A release that failed half-way

If the publish step failed after some files were uploaded, the version is broken and cannot be re-published (409);
`publish.yml` then fails and names the missing file instead of tagging it.
Delete that version in the repository's **Packages** page (allowed for public packages with few downloads), or bump
`version` to the next patch. Never move or re-use a tag. If a service build still says the version cannot be found after the release
is green, re-run it with `--refresh-dependencies` (Gradle may have cached the miss).

## Build

```bash
./gradlew test
./gradlew generatePomFileForMavenJavaPublication generateMetadataFileForMavenJavaPublication sourcesJar   # inspect build/publications
```
