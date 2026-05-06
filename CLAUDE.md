# CLAUDE.md

## Gotchas

- **This is the parent project for every Polarion extension in the org** (settings framework, REST base classes, OSGi helpers, etc.). Anything added or changed here is consumed by every downstream extension — treat all public APIs as if they have many callers, and break carefully. Conversely: when working on a downstream extension, look here first for any cross-cutting infrastructure before re-implementing it.
- **No OpenAPI spec**: generic does not ship an HTTP / OpenAPI surface. It exposes Java APIs (REST base classes, OSGi helpers) for other plugins to use, not endpoints. The `openapi-validation.yml` workflow and `redocly.yaml` config that exist in `open-source-polarion-java-repo-template` are intentionally **not present** here — do not "sync" them in. Downstream extensions that DO ship `docs/openapi.json` should adopt those template files in their own repos.
- **No GitHub Packages deploy**: unlike most plugins generated from `open-source-polarion-java-repo-template`, this repo intentionally does NOT deploy to GitHub Packages. Releases go to Maven Central only; SNAPSHOTs are not published anywhere. The `📦 Deploy to GitHub Packages` step is therefore omitted from `maven-build.yml` — do not "sync" it back from the template. The repo comment in `maven-build.yml` ("Releases and snapshots do not need to be deployed to GitHub packages") is the load-bearing marker; preserve it during template syncs.
- **Multi-module layout**: this repo is a multi-module Maven project. The actual extension code lives under `app/` (artifacts produced as `app/target/*.jar`), with the root `pom.xml` acting as parent POM. The template is single-module (`target/*.jar`) — when syncing `maven-build.yml` from the template, `app/target/` paths must be preserved.
- **Maven Settings**: Builds require `.mvn/settings.xml` (JFrog, GitHub Packages, Sonatype credentials via env vars). CI passes it with `-s .mvn/settings.xml`. `.mvn/maven.config` auto-activates the Polarion version profile.
- **Polarion Dependencies**: You must extract dependencies from the Polarion installer using [polarion-artifacts-deployer](https://github.com/SchweizerischeBundesbahnen/polarion-artifacts-deployer) before the Maven build will work.
- **Local Polarion Installation**: Requires `POLARION_HOME` environment variable. Use the `install-to-local-polarion` Maven profile: `mvn clean install -P install-to-local-polarion`
- **After any code change**: Delete `<POLARION_HOME>/data/workspace/.config` before restarting Polarion or changes won't be picked up.
- **Remote Debugging**: Add to Polarion's `config.sh`: `JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"`
- **Logging**: Polarion logs: `<POLARION_HOME>/polarion/logs/main/*.log`
- **Branch conventions**: Conventional commits enforced by commitizen (pre-commit hook). Feature branches: `feature/<name>`, bug fixes: `fix/<name>`, LTS branches: `release-v*` (e.g., `release-v6`).
- **Pre-commit hooks block internal patterns**: some org-specific identifiers are treated as secrets. Run `pre-commit run -a` after implementation.
