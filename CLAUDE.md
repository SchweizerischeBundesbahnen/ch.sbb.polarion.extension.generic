# CLAUDE.md

## Gotchas

- **This is the parent project for every Polarion extension in the org** (settings framework, REST base classes, OSGi helpers, etc.). Anything added or changed here is consumed by every downstream extension — treat all public APIs as if they have many callers, and break carefully. Conversely: when working on a downstream extension, look here first for any cross-cutting infrastructure before re-implementing it.
- **No OpenAPI spec**: generic does not ship an HTTP / OpenAPI surface. It exposes Java APIs (REST base classes, OSGi helpers) for other plugins to use, not endpoints. The `openapi-validation.yml` workflow and `redocly.yaml` config that exist in `open-source-polarion-java-repo-template` are intentionally **not present** here — do not "sync" them in. Downstream extensions that DO ship `docs/openapi.json` should adopt those template files in their own repos.
- **No GitHub Packages deploy**: unlike most plugins generated from `open-source-polarion-java-repo-template`, this repo intentionally does NOT publish to GitHub Packages. Releases go to Maven Central only; SNAPSHOTs are not published anywhere. The template's `deploy-github-packages` job (which actually deploys to GH Packages) is replaced here by the smaller `upload-release-assets` job (which only uploads release JARs to the corresponding GitHub Release on actual release builds). When syncing `maven-build.yml` from the template, do NOT add the GH-Packages-deploying step back, and do NOT rename `upload-release-assets` to `deploy-github-packages`. The job header comment in `maven-build.yml` ("Generic intentionally does NOT publish to GitHub Packages…") is the load-bearing marker; preserve it during template syncs.
- **Multi-module layout**: this repo is a multi-module Maven project. The actual extension code lives under `app/` (artifacts produced as `app/target/*.jar`), with the root `pom.xml` acting as parent POM. The template is single-module (`target/*.jar`) — when syncing `maven-build.yml` from the template, `app/target/` paths must be preserved.
- **Control-icon tokens are generated, not hand-written**: the icon tokens in `app/src/main/resources/css/control-tokens.css` are `url(inline:images/x.svg)` placeholders in source. The build (`npm run build:css` → `app/scripts/inline-svg-tokens.mjs`, wired into `frontend-maven-plugin` at `process-classes`) inlines each as base64 into `target/classes/css/control-tokens.css`. The `.svg` files under `app/src/main/resources/images/` are the single source of truth — **edit the `.svg` and rebuild; never hand-edit a base64 blob** (and don't add base64 back into the source CSS). Unit test `inlineSvgTokensTest.js` guards both invariants.
- **Maven Settings**: Builds require `.mvn/settings.xml` (JFrog, GitHub Packages, Sonatype credentials via env vars). CI passes it with `-s .mvn/settings.xml`. `.mvn/maven.config` auto-activates the Polarion version profile.
- **Polarion Dependencies**: You must extract dependencies from the Polarion installer using [polarion-artifacts-deployer](https://github.com/SchweizerischeBundesbahnen/polarion-artifacts-deployer) before the Maven build will work.
- **Local Polarion Installation**: Requires `POLARION_HOME` environment variable. Use the `install-to-local-polarion` Maven profile: `mvn clean install -P install-to-local-polarion`
- **After any code change**: Delete `<POLARION_HOME>/data/workspace/.config` before restarting Polarion or changes won't be picked up.
- **Remote Debugging**: Add to Polarion's `config.sh`: `JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"`
- **Logging**: Polarion logs: `<POLARION_HOME>/polarion/logs/main/*.log`
- **Branch conventions**: Conventional commits enforced by commitizen (pre-commit hook). Feature branches: `feature/<name>`, bug fixes: `fix/<name>`, LTS branches: `release-v*` (e.g., `release-v6`).
- **Pre-commit hooks block internal patterns**: some org-specific identifiers are treated as secrets. Run `pre-commit run -a` after implementation.

## Migration to RSP

Everything under `app/src/main/resources/{js,css,images}` is moving to **react-sbb-polarion** (RSP,
npm `@grigoriev/react-sbb-polarion`, repo `github.com/grigoriev/react-sbb-polarion`). RSP copies
files from here verbatim into its `src/generic/` and bundles them, so extensions get them from npm
instead of fetching them from this webapp. Every asset below eventually ends up owned by RSP and
deleted here. Migrate them one at a time; the table is the running state.

- **Do not add new shared UI assets here.** New components, styles and icons go straight to RSP.
- **Edit the original here, then re-copy into RSP.** RSP excludes `src/generic/**` from its own
  lint, format and coverage, and forbids hand-edits there. Those copies carry deliberate local
  patches (shadow-root portal via `getRootNode()`, `composedPath()` outside-clicks, `url(../images/…)`
  icon paths, a no-op `ensureSharedStyles`, Selawik `@font-face`), so a by-the-book re-copy silently
  deletes them. Check RSP's `.greptile/rules.md` before copying.
- **A file is removable from here only when nothing fetches it at runtime.** Downstream React apps
  pull some of these directly with `<link>` or `injectStyles(...)`, which no grep for an import
  finds. Verify against the extension repos, not against RSP.

### Already vendored in RSP, still fetched from here

| Asset | Fetched by |
|---|---|
| `js/modules/SearchableDropdown.js`, `searchableSelect.js`, `ensureSharedStyles.js`, `generic-build-info.js` | diff-tool's own `ui/src/components/SearchableSelect.jsx` dynamically imports `searchableSelect.js` at runtime |
| `css/control-tokens.css`, `checkboxes.css`, `radios.css`, `inputs.css`, `searchable-dropdown.css`, `buttons.css`, `alerts.css` | `ensureSharedStyles.js` injects all seven, so the diff-tool chain above pulls them; strictdoc-exporter's `starter.js` injects them too; diff-tool links two of them in `ui/*.html` |
| all 24 SVGs under `images/` | resolve `control-tokens.css`'s `url(inline:…)` placeholders at build time, so they move with it |

`css/tabs.css` completed this route: RSP owns it, nothing fetched it from here, so it was deleted.
That is the pattern to repeat for each row above.

### Not in RSP yet

| Asset | Note |
|---|---|
| `css/tables.css` | `.sbb-table` is used by api-extender, xml-repair and integrity-scanner, none of which links the sheet, so those tables render unstyled today. Moving it into RSP's bundle fixes that. |
| `css/configurations.css` | linked by diff-tool and excel-importer; belongs with RSP's `ConfigurationsPane` |
| `css/github-markdown-light.css` | linked by all 23 extensions |
| `css/micromodal.css` | injected by strictdoc-exporter's `starter.js`; overlaps RSP's `Modal.css` |
| `js/dle-toolbar-starter.js`, `css/dle-toolbar.css` | plain `<script>` loaded into the Polarion document editor by the pdf / docx / strictdoc exporters, outside any React app; probably stays here |
| `js/modules/BreadcrumbBridge.js` | deliberately fetched at runtime by RSP's `BreadcrumbInjector` and run in `window.top`, outside the React bundle; probably stays here |

### The npm toolchain is scaffolding for these assets

`app/package.json`, `app/src/test/js/` and the `frontend-maven-plugin` executions in `app/pom.xml`
exist only to test and build the files above. Nothing in the Java build needs them. Retire each part
as its subject leaves:

- `npm run build:css` and its two tests (`inlineSvgTokensTest.js`, `controlTokensScopeTest.js`) go
  with `control-tokens.css` and `images/`.
- The `generic-build-info.js` timestamp filtering in the **root** `pom.xml` goes with
  `ensureSharedStyles.js`.
- The whole node/npm setup, `app/package.json` and the JS test suite go once the last JS file does.

`js/modules/BreadcrumbBridge.js` and `js/dle-toolbar-starter.js` are what keep it alive, so the JS
tests outlive the asset migration unless those two move as well.
