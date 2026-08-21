# CLAUDE.md

## Gotchas

- **This is the parent project for every Polarion extension in the org** (settings framework, REST base classes, OSGi helpers, etc.). Anything added or changed here is consumed by every downstream extension — treat all public APIs as if they have many callers, and break carefully. Conversely: when working on a downstream extension, look here first for any cross-cutting infrastructure before re-implementing it.
- **No OpenAPI spec**: generic does not ship an HTTP / OpenAPI surface. It exposes Java APIs (REST base classes, OSGi helpers) for other plugins to use, not endpoints. The `openapi-validation.yml` workflow and `redocly.yaml` config that exist in `open-source-polarion-java-repo-template` are intentionally **not present** here — do not "sync" them in. Downstream extensions that DO ship `docs/openapi.json` should adopt those template files in their own repos.
- **No GitHub Packages deploy**: unlike most plugins generated from `open-source-polarion-java-repo-template`, this repo intentionally does NOT publish to GitHub Packages. Releases go to Maven Central only; SNAPSHOTs are not published anywhere. The template's `deploy-github-packages` job (which actually deploys to GH Packages) is replaced here by the smaller `upload-release-assets` job (which only uploads release JARs to the corresponding GitHub Release on actual release builds). When syncing `maven-build.yml` from the template, do NOT add the GH-Packages-deploying step back, and do NOT rename `upload-release-assets` to `deploy-github-packages`. The job header comment in `maven-build.yml` ("Generic intentionally does NOT publish to GitHub Packages…") is the load-bearing marker; preserve it during template syncs.
- **Multi-module layout**: this repo is a multi-module Maven project. The actual extension code lives under `app/` (artifacts produced as `app/target/*.jar`), with the root `pom.xml` acting as parent POM. The template is single-module (`target/*.jar`) — when syncing `maven-build.yml` from the template, `app/target/` paths must be preserved.
- **Maven Settings**: Builds require `.mvn/settings.xml` (JFrog, GitHub Packages, Sonatype credentials via env vars). CI passes it with `-s .mvn/settings.xml`.
- **No Polarion version profile.** The platform properties (`polarion.version`, `jersey.version`, `maven.compiler.*`) are plain top-level `<properties>`, and the OSGi bundle artifactIds are literals in the dependencies that use them; the `polarion2606` profile and the `.mvn/maven.config` that activated it are gone. They used to hang on `<activeByDefault>`, which Maven switches off as soon as any other profile of this POM activates — the file-activated `ui-build-react-app` does that in every extension with a React `ui/`, and so does any `-D` flag profile — leaving the model unreadable for anything that did not pass `-P polarion2606`. Support for another platform belongs on a `release-v*` branch, which is where it already lives.
- **Maven profile and property names follow one scheme.** A profile is `<group>-<what it does>`, a property is `<group>.<subject>.<action>`, and both carry the same group, so `-Dui.tests.withoutDocker` activates `ui-tests-run-without-docker`. Two groups exist: `ui-` for the React app build and its tests, `local-` for what runs on a developer machine. A third, `release-`, is reserved for `gpg-sign` and `central-publishing` and stays unused until those are renamed, because 16 downstream workflows pass them by their current names. Do not name a profile after the tool it happens to call. The deprecated property spellings at the end of the properties block (`vite.*`, `jsTest*`, `generic.app.ui.webapp`) protect downstream **poms**, not command-line flags: an unknown `${...}` there resolves to its own literal text instead of failing, so a pom that reads or sets an old name would break with no error. They go in the next major.
- **Polarion Dependencies**: You must extract dependencies from the Polarion installer using [polarion-artifacts-deployer](https://github.com/SchweizerischeBundesbahnen/polarion-artifacts-deployer) before the Maven build will work.
- **Local Polarion Installation**: Requires `POLARION_HOME` environment variable. Use the `local-install-into-polarion` Maven profile: `mvn clean install -P local-install-into-polarion`
- **After any code change**: Delete `<POLARION_HOME>/data/workspace/.config` before restarting Polarion or changes won't be picked up.
- **Remote Debugging**: Add to Polarion's `config.sh`: `JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"`
- **Logging**: Polarion logs: `<POLARION_HOME>/polarion/logs/main/*.log`
- **Branch conventions**: Conventional commits enforced by commitizen (pre-commit hook). Feature branches: `feature/<name>`, bug fixes: `fix/<name>`, LTS branches: `release-v*` (e.g., `release-v6`).
- **Pre-commit hooks block internal patterns**: some org-specific identifiers are treated as secrets. Run `pre-commit run -a` after implementation.

## Migration to RSP

Done. Everything under `app/src/main/resources/{js,css,images}` moved to **react-sbb-polarion**
(RSP): the dropdown chain, the seven control stylesheets, the 24 SVG icons, `BreadcrumbBridge.js` and
`js/dle-toolbar-starter.js`. Extensions get all of it from npm (`@sbb-polarion/react-sbb-polarion`).
No `.js`, `.css` or image file is left in this repository - the only front-end that remains is the
Swagger UI page, whose inline script loads Swagger's own bundle from Polarion. RSP owns the moved
files outright; there is no original here to re-copy from.

- **Do not add shared UI assets here.** New components, styles and icons go straight to RSP.
- **The npm toolchain went with them.** `app/package.json`, `app/src/test/js/`, `app/scripts/` and the
  `frontend-maven-plugin` executions in `app/pom.xml` existed only to build and test those files, so
  the `app` module is a pure Java build now. The `frontend-maven-plugin.*` properties in the **root**
  `pom.xml` stay: they feed the `ui-build-react-app` profile that every extension with a `ui/package.json`
  inherits.
- **A file is removable from here only when nothing fetches it at runtime.** Downstream React apps
  pull assets with `<link>` or `injectStyles(...)`, which no grep for an import finds. Verify against
  the extension repos, not against RSP.

`css/github-markdown-light.css` went last. Its row here claimed all 23 extensions linked it, which no
longer held: markdown2html writes a bare HTML fragment, and RSP renders it - `About.tsx` and
`UserGuide.tsx` wrap it in `.markdown-body`, whose styling is bundled in RSP's `style.css` (its
`markdown.css` imports the vendored copy). No repository fetched the file from here.

Watch the trap that hid a dead file for a long time: a downstream `injectStyles(...)` or `<link>` does
**not** prove a sheet is live. `micromodal.css` was still fetched by an exporter's `starter.js` long
after that popup had moved to RSP's `Modal`, so the sheet matched no rendered markup at all. Check
that a sheet's selectors match markup something still renders, not just that a fetch exists.
