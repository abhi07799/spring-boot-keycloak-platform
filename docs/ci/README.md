# Continuous Integration

The repository runs a Maven verification workflow on pull requests targeting `develop` or `main`, and on pushes to either branch.

## What the workflow checks

- Checks out the exact commit under review.
- Installs Eclipse Temurin JDK 21.
- Restores/caches Maven dependencies using the setup-java Maven cache.
- Runs `mvn --batch-mode --no-transfer-progress clean verify`, which compiles the project, runs the test suite, and executes Maven verification lifecycle checks.
- Fails the job if compilation, tests, or verification fails.

The workflow can also be started manually with **Actions → Maven CI → Run workflow**.

## Required status check

For merge protection, configure GitHub branch protection or a ruleset separately for `develop` and `main`. After the first successful workflow run, require the **Java 21 / Maven Verify** job before merging. This repository workflow does not itself change branch-protection settings.

## Scope and limitations

This CI job validates the Maven project without requiring Docker, PostgreSQL, Keycloak, or SMTP services. It does not prove live identity-provider integration or email delivery. Those remain environment-level integration checks and should be covered by a separate integration-test environment when introduced.

The workflow uses GitHub-hosted Ubuntu runners and read-only repository contents permission. It does not require application secrets.
