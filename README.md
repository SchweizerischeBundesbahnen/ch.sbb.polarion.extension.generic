[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic&metric=bugs)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic&metric=coverage)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic)

# Generic extension of Polarion ALM

This is a Polarion extension which provides common part to other extensions reducing code duplication.

An extension which inherits from this generic extension will automatically get following functionality:

* An "about" page on administrative section of Polarion with basic information about this extension
* API to manipulate settings of this extension - to read, save settings and reverting them to default values as well as getting list of settings history revisions
* API to serialize/deserialize XML data (`JAXBUtils`)
* REST application and end points giving access to settings functionality described above as well as access to extension information and version
* OpenAPI specification endpoint at `/rest/api/openapi.json` providing machine-readable API documentation
* Swagger UI page listing information about REST API provided
* Some utility classes and methods to simplify development of new extensions
* Some test classes to simplify testing of new extensions using JUnit and Mockito

> [!IMPORTANT]
> Starting from version 8.0.0 only latest version of Polarion is supported.
> Right now it is Polarion 2512.

## How to use

To properly inherit from this generic extension and to take advantage of all mentioned above functionality
out of the box certain steps should be done, see below.

### pom.xml

Maven's `pom.xml` should contain following content:

* Reference to parent POM (don't forget to use proper version of it):

```xml
<parent>
    <groupId>ch.sbb.polarion.extensions</groupId>
    <artifactId>ch.sbb.polarion.extension.generic</artifactId>
    <version><!-- version goes here --></version>
</parent>
```

* Specify extension context, automatic module name, discover base package, web application name and extension-specific `Require-Bundle` entries in POM's properties:

```xml
<properties>
    <maven-jar-plugin.Extension-Context>pdf-exporter</maven-jar-plugin.Extension-Context>
    <maven-jar-plugin.Automatic-Module-Name>ch.sbb.polarion.extension.pdf_exporter</maven-jar-plugin.Automatic-Module-Name>
    <maven-jar-plugin.Discover-Base-Package>ch.sbb.polarion.extension.pdf_exporter</maven-jar-plugin.Discover-Base-Package>
    <maven-jar-plugin.Configuration-Properties-Prefix>ch.sbb.polarion.extension.pdf-exporter</maven-jar-plugin.Configuration-Properties-Prefix>
    <web.app.name>${maven-jar-plugin.Extension-Context}</web.app.name>

    <!-- Extension-specific Require-Bundle (in addition to common ones from parent POM):
        com.polarion.alm.wiki
        org.jsoup
    -->
    <maven-jar-plugin.Require-Bundle.extension>com.polarion.alm.wiki,org.jsoup</maven-jar-plugin.Require-Bundle.extension>
    <maven-jar-plugin.Require-Bundle>${maven-jar-plugin.Require-Bundle.common},${maven-jar-plugin.Require-Bundle.extension}</maven-jar-plugin.Require-Bundle>
</properties>
```

* Reference or extend following build plugins:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-clean-plugin</artifactId>
        </plugin>

        <plugin>
            <groupId>ch.sbb.maven.plugins</groupId>
            <artifactId>markdown2html-maven-plugin</artifactId>
        </plugin>

        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-dependency-plugin</artifactId>
        </plugin>

        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-jar-plugin</artifactId>
        </plugin>

        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
        </plugin>

        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
        </plugin>

        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-javadoc-plugin</artifactId>
        </plugin>

        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-source-plugin</artifactId>
        </plugin>

        <plugin>
            <groupId>io.swagger.core.v3</groupId>
            <artifactId>swagger-maven-plugin</artifactId>
            <configuration>
                <outputFormat>YAML</outputFormat>
                <resourcePackages>
                    <package>ch.sbb.polarion.extension.generic.rest.controller.info</package>
                    <package>ch.sbb.polarion.extension.generic.rest.controller.settings</package>
                    <package>ch.sbb.polarion.extension.generic.rest.model</package>
                    <package>ch.sbb.polarion.extension.pdf_exporter.rest.controller</package>
                    <package>ch.sbb.polarion.extension.pdf_exporter.rest.model</package>
                </resourcePackages>
            </configuration>
        </plugin>

    </plugins>
</build>
```

#### Optional: OpenAPI JSON formatting

To automatically format `docs/openapi.json` using pre-commit hooks, add the following plugin configuration:

```xml
<plugin>
    <groupId>io.github.grigoriev</groupId>
    <artifactId>pre-commit-run-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>format-openapi-json</id>
            <phase>process-classes</phase>
            <goals>
                <goal>run</goal>
            </goals>
            <configuration>
                <hooks>
                    <hook>pretty-format-openapi-json</hook>
                    <hook>mixed-line-ending-openapi-json</hook>
                </hooks>
                <files>
                    <file>docs/openapi.json</file>
                </files>
            </configuration>
        </execution>
    </executions>
</plugin>
```

The `process-classes` phase runs right after `compile` phase where `swagger-maven-plugin` generates the file.

This ensures consistent JSON formatting with proper key ordering (openapi, info, servers, paths, components) and line endings across all extensions.

### MANIFEST.MF

File `MANIFEST.MF` should be created in `src/main/resources/META-INF/MANIFEST.MF` with **only extension-specific** entries.

The following entries are **managed by the parent POM** via `<manifestEntries>` and must **NOT** be added to the static `MANIFEST.MF` file (they will be silently overridden if present):

| Entry | POM Property | Default Value |
|-------|-------------|---------------|
| `Require-Bundle` | `maven-jar-plugin.Require-Bundle` | 17 common bundles (see below) |
| `Support-Email` | `maven-jar-plugin.Support-Email` | `polarion-opensource@sbb.ch` |
| `Bundle-ActivationPolicy` | `maven-jar-plugin.Bundle-ActivationPolicy` | `lazy` |
| `Import-Package` | `maven-jar-plugin.Import-Package` + `maven-jar-plugin.Import-Package.required` | `org.osgi.framework` plus the always-appended `jakarta.annotation` pin |

Common `Require-Bundle` bundles defined in the parent POM (`maven-jar-plugin.Require-Bundle.common`):
`com.polarion.portal.tomcat`, `com.polarion.alm.ui`, `javax.inject`, `javax.annotation-api`, `org.glassfish.jersey`, `com.fasterxml.jackson.core`, `com.fasterxml.jackson.databind`, `com.fasterxml.jackson.annotations`, `com.fasterxml.jackson.module.jaxb.annotations`, `org.apache.commons.logging`, `slf4j.api`, `org.springframework.spring-core`, `org.springframework.spring-web`, `com.polarion.alm.tracker`, `com.polarion.platform.guice`

To add extension-specific bundles, set `maven-jar-plugin.Require-Bundle.extension` and override `maven-jar-plugin.Require-Bundle` in the extension's `pom.xml`:

```xml
<!-- Extension-specific Require-Bundle (in addition to common ones from parent POM):
    com.polarion.alm.wiki
    org.jsoup
-->
<maven-jar-plugin.Require-Bundle.extension>com.polarion.alm.wiki,org.jsoup</maven-jar-plugin.Require-Bundle.extension>
<maven-jar-plugin.Require-Bundle>${maven-jar-plugin.Require-Bundle.common},${maven-jar-plugin.Require-Bundle.extension}</maven-jar-plugin.Require-Bundle>
```

To override `Import-Package` (e.g. to add `org.osgi.util.tracker`), set `maven-jar-plugin.Import-Package` in the extension's `pom.xml`:

```xml
<maven-jar-plugin.Import-Package>org.osgi.framework,org.osgi.util.tracker</maven-jar-plugin.Import-Package>
```

The parent POM always appends `maven-jar-plugin.Import-Package.required` to the generated entry, so
overriding the property above cannot drop it. It currently pins `jakarta.annotation` to the
`jakarta.annotation-api` bundle: Tomcat exports the same package unversioned, and binding to Tomcat's
copy makes Jersey miss `@Priority` on request filters, which leaves the authentication and
authorization filters in a per-JVM-start order. Do not override this property.

The static `MANIFEST.MF` should only contain:

* `Bundle-Name` — extension display name:

```properties
Bundle-Name: PDF Exporter Extension for Polarion ALM
```

* `Bundle-Activator` — if the bundle has a form extension, registered either using custom `org.osgi.framework.BundleActivator` or `ch.sbb.polarion.extension.generic.GenericBundleActivator`:

```properties
Bundle-Activator: ch.sbb.polarion.extension.pdf_exporter.ExtensionBundleActivator
```

  Subclass `GenericBundleActivator` and return your form extensions from `getExtensions()`:

  ```java
  public class ExtensionBundleActivator extends GenericBundleActivator {
      @Override
      protected Map<String, IFormExtension> getExtensions() {
          return Map.of(MyFormExtension.ID, new MyFormExtension());
      }
  }
  ```

  > **Deferred registration (why it is not done inline in `start()`).**
  > Polarion's `FormExtensionsRegistry` is a lazily-initialized singleton: on its first
  > `getInstance()` it injects Polarion's own Guice-bound form extensions (`velocity_form`, `oslc`,
  > `execute-test`, `linkedResources`, workflow-signatures widget, `gitlab`, …). If that first touch
  > happens during OSGi bundle activation — before Polarion has built its global Guice injector —
  > `GuicePlatform.tryInjectMembers` injects nothing, the singleton is created **empty of all core
  > contributions**, and every Polarion-provided form extension is lost for the whole server
  > lifetime (typical symptom: *"Form extension 'velocity_form' was not found"* on a document/work
  > item form). To avoid poisoning the registry, `GenericBundleActivator` runs its startup on a daemon
  > thread that first waits for the global Guice injector to become available; Polarion then loads its
  > core extensions first and ours are added on top. The `onStart(BundleContext)` hook runs on that
  > same deferred thread (after the injector is ready, not synchronously during activation), so a
  > subclass hook may safely touch Polarion's platform. This is transparent to subclasses — just
  > implement `getExtensions()` and, if needed, override `onStart()`.

* `Export-Package` — if the extension exports packages for use by other bundles:

```properties
Export-Package: ch.sbb.polarion.extension.pdf_exporter,
 ch.sbb.polarion.extension.pdf_exporter.converter
```

* Any other extension-specific entries (e.g. `Guice-Modules`)

### Setting classes

If new extension should provide functionality to manipulate its settings, settings classes should be implemented extending
`GenericNamedSettings<T extends SettingsModel>`, eg:

```java
public class CssSettings extends GenericSettings<CssModel> {
    private static final String FEATURE_NAME = "css";

    public CssSettings() {
        super(FEATURE_NAME);
    }

    public CssSettings(SettingsService settingsService) {
        super(FEATURE_NAME, settingsService);
    }

    @Override
    public @NotNull CssModel defaultValues() {
        return CssModel.builder().css(ScopeUtils.getFileContent("default/dle-pdf-export.css")).build();
    }
}
```

...and settings model class from example above like this:

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CssModel extends SettingsModel {

    public static final String CSS = "CSS";

    private String css;

    @Override
    protected String serializeModelData() {
        return serializeEntry(CSS, css);
    }

    @Override
    protected void deserializeModelData(String serializedString) {
        css = deserializeEntry(CSS, serializedString);
    }
}
```

### Settings validation

The settings API includes built-in validation to ensure data integrity:

#### Duplicate setting names detection

If multiple settings files contain the same name (in the `-----BEGIN NAME-----` block), the API will return **HTTP 409 Conflict** error.
This can happen when settings files are manually created or imported via SVN.

**Example error response:**

```json
{
  "message": "Multiple settings files contain the same name: Default (68d0a695-9865-4612-a6ab-e6102bc8d1e1, 3ea1190b-adb9-4cda-8906-953030327958)"
}
```

The message includes the setting name and the file IDs (UUIDs) of the conflicting files.
If multiple duplicate names exist, they will all be listed:

```
Multiple settings files contain the same name: ConfigA (id1, id2), ConfigB (id3, id4, id5)
```

This validation is triggered on all settings operations: read, save, delete, list names, and list revisions.

### REST application

REST application class should inherit from `GenericRestApplication` of generic extension,
registering settings classes and extending classes of REST controller, web application filters and exception mappers:

```java
public class PdfExporterRestApplication extends GenericRestApplication {
    private final Logger logger = Logger.getLogger(PdfExporterRestApplication.class);

    public PdfExporterRestApplication() {
        logger.debug("Creating PDF-Exporter REST Application...");

        try {
            NamedSettingsRegistry.INSTANCE.register(
                    Arrays.asList(
                            new StylePackageSettings(),
                            new HeaderFooterSettings(),
                            new CssSettings(),
                            new LocalizationSettings(),
                            new CoverPageSettings(),
                            new FileNameTemplateSettings()
                    )
            );
        } catch (Exception e) {
            logger.error("Error during registration of named settings", e);
        }

        logger.debug("PDF-Exporter REST Application has been created");
    }
...
}
```

### REST architecture: `/internal` vs `/api`

Extensions built on top of this generic extension expose REST endpoints under two distinct URL spaces with **different security models**. Understanding the split is important - they are not interchangeable, and `@Secured` is intentionally applied to one but not the other.

| URL space | Intended caller | Authentication | Authorization                                                                                                                       | CSRF protection                                                                                                                                                                                                                                                                                                                                          |
|---|---|---|-------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `/rest/internal/*` | The extension's own UI, running inside an authenticated Polarion session | Servlet container (`<security-constraint>` in `web.xml` with `<role-name>user</role-name>`, FORM-based login against `PolarionRealm`); Polarion's `DoAsFilter` then propagates the session's `Subject` to the request thread | Real Polarion user from the session - operations run under that user's permissions | No server-side token check - `X-Polarion-REST-Token` is **not** validated on `/internal/*`. Defence relies on (a) the container session being required, (b) browser SameSite=Lax cookie default, which prevents cookie-bearing cross-site `<form>` POST/PUT/DELETE, and (c) the REST convention that state-changing methods are POST/PUT/DELETE, not GET |
| `/rest/api/*` | External clients. Mirrors Polarion's own public REST API authentication model (see `AccessTokenAuthenticator`) | `@Secured` filter (`AuthenticationFilter`), validating either `Authorization: Bearer <PAT>` or `X-Polarion-REST-Token` | Wrapped in `polarionService.callPrivileged(...)` - the PAT principal has no Polarion subject, so calls run with elevated privileges | Custom-header requirement is itself a CSRF defence per OWASP - browsers cannot attach custom headers to cookie-only cross-origin requests without a CORS preflight                                                                                                                                                                                       |

The controller pattern that implements this split is illustrated by `NamedSettingsInternalController` and `NamedSettingsApiController`:

* `NamedSettingsInternalController` - `@Path("/internal")`, **no** `@Secured`. Container-level auth already gates the endpoint.
* `NamedSettingsApiController extends NamedSettingsInternalController` - `@Path("/api")`, `@Secured`, each method overridden to wrap the inherited logic in `polarionService.callPrivileged(...)`.

Each consuming extension's `web.xml` must declare both constraints - the role-protected one for `/*` and an open one for `/rest/api/*` so the container does not interfere with `@Secured`:

```xml
<!-- Container-level auth: everything except /rest/api/* requires a Polarion session -->
<security-constraint>
    <web-resource-collection>
        <web-resource-name>All</web-resource-name>
        <url-pattern>/*</url-pattern>
    </web-resource-collection>
    <auth-constraint>
        <role-name>user</role-name>
    </auth-constraint>
</security-constraint>

<!-- /rest/api/* is opened at the container level - auth happens in @Secured (AuthenticationFilter) -->
<security-constraint>
    <web-resource-collection>
        <web-resource-name>All</web-resource-name>
        <url-pattern>/rest/api/*</url-pattern>
    </web-resource-collection>
    <auth-constraint/>
</security-constraint>

<!--
    Note on the empty <auth-constraint/>: per the Servlet spec this would deny all access, but
    Polarion's `PolarionRealm` (com.polarion.platform/.../PolarionRealm.java) overrides
    `hasResourcePermission()` to always return `true`, disabling container-level role enforcement.
    In this stack the empty constraint effectively means "open at the container level".
-->

<login-config>
    <auth-method>FORM</auth-method>
    <realm-name>PolarionRealm</realm-name>
    <form-login-config>
        <form-login-page>/login/login</form-login-page>
        <form-error-page>/login/error</form-error-page>
    </form-login-config>
</login-config>
```

Consequences of this design:

* **Do not add `@Secured` to `/internal/*` controllers.** It is redundant with the container's `<security-constraint>`, and it would break the extension's UI flow, which authenticates via the Polarion session, not via PAT.
* **Do not call `/internal/*` from external clients.** It is not a public API. External integrations must use `/rest/api/*`.

### UI servlet class

If new extension will contain UI parts/pages/artifacts, UI servlet class should be created extending `GenericUiServlet`
simply specifying servlet name in constructor:

```java
public class PdfExporterAdminUiServlet extends GenericUiServlet {

    @Serial
    private static final long serialVersionUID = -6337912330074718317L;

    public PdfExporterAdminUiServlet() {
        super("pdf-exporter-admin");
    }
}
```

### UI components (JavaScript / CSS)

The generic module ships shared, Polarion-matched front-end components so every extension's UI looks
and behaves the same. They are served by the extension's `GenericUiServlet` at
`/polarion/<extension>/ui/generic/{js,css}/…` and imported from there:

```js
import SearchableDropdown from '../ui/generic/js/modules/SearchableDropdown.js';
import { createSearchableSelect } from '../ui/generic/js/modules/searchableSelect.js';
```

Extension UIs are React apps built on the shared
[react-sbb-polarion](https://github.com/grigoriev/react-sbb-polarion) library (npm
`@grigoriev/react-sbb-polarion`), which vendors these modules and this CSS. Generic stays the
upstream source of truth: change a component here, then re-copy it into the library.

#### `SearchableDropdown` — the standard dropdown

`js/modules/SearchableDropdown.js` is the single dropdown component for all extensions. It renders a
Polarion-styled combobox with a searchable, never-clipped popup and supports single- and
multi-select. Prefer it over a raw `<select>` or any bespoke widget.

- **Element mode** — wrap an existing native `<select>` (the `<select>` stays the source of truth):

  ```js
  new SearchableDropdown({ element: document.getElementById('paper-size'), rememberSelection: false });
  ```

- **Build mode** — render into a `<div>` and populate programmatically (`addOption`, `empty`,
  `selectValue`, `selectMultipleValues`, `getSelectedValue`, `getSelectedText`, `containsOption`):

  ```js
  const dd = new SearchableDropdown({ selectContainer: document.getElementById('css-select'), label });
  dd.addOption('A4', 'A4');            // single-select defaults to the first option added
  dd.selectValue('A4');
  ```

Key options: `multiselect` (removable chips + in-list checkboxes; the popup stays open while
toggling), `searchable` (default `true`), `placeholder`, `allowEmpty` (default `false` — a
single-select then does **not** auto-select the first option; it stays unselected and shows the
`placeholder` until the user picks, e.g. `{ allowEmpty: true, placeholder: 'Select…' }`),
`clearable` (default `false` — adds a small `×` in the trigger that resets a single-select back to
its `placeholder`; pairs naturally with `allowEmpty`),
`rememberSelection` (cookie; usually pass `false`), `preserveOptionClasses` (mirror each `<option>`'s
CSS class onto the rendered option — e.g. the `parent` class renders a global-scope configuration
with an italic `global` marker).

**Editable (free-text) mode** — `editable: true` turns the single-select into a *creatable*
combobox: the trigger becomes a typeable field (the popup's separate search box is dropped — the
trigger itself filters), focus opens the suggestion list, and a value **not** in the list can be
committed (on Enter and on blur). Picking an option commits the option's `value` (the `label` is
shown only in the list). Because a free value cannot live in a `<select>`, editable mode wraps a
plain `<input>` (the already-supported non-`<select>` element mode) or build mode — the wrapped
`<input>` is kept in sync and receives `change`. Pass `inputFilter: (value) => value` to sanitise
what is typed (catches typing *and* paste), e.g. digits-only `v => v.replace(/\D/g, '')` or
Latin-only `v => v.replace(/[^A-Za-z]/g, '')`.

Per-option **icons**: give a source `<option>` a `data-icon="…"` (element mode) or pass a third
argument to `addOption(value, text, icon)` (build mode); the icon is shown to the left of the label
in the list and on the closed single-select trigger. For an icon that sits on a coloured tile (e.g.
an entity/project icon on a dark background), add `data-icon-bg="#1a3a5c"` on the `<option>` or pass
it as the fourth argument, `addOption(value, text, icon, iconBg)`; the colour is applied behind the
icon in both the list and the trigger.

The popup is portalled into `document.body` so it is never clipped by an ancestor's `overflow`
(narrow side panels, scrollable modals). It auto-refreshes when its `<select>` options are
repopulated; it also
mirrors the wrapped `<select>`'s `disabled` state (setting `select.disabled` dims the control and
makes it non-interactive), its `<option disabled>` options (dimmed, non-selectable), and its `title`
tooltip. It exposes
full ARIA combobox/listbox semantics (`role`, `aria-expanded`, `aria-activedescendant`, per-option
`aria-selected`/`aria-disabled`, and an `aria-label` derived from the associated `<label>`) so screen
readers announce and navigate it like a native `<select>`. Call
`destroy()` to tear an instance down (removes the portal and container, disconnects observers, and
unbinds global listeners); re-wrapping the same `<select>` also disposes the previous instance
automatically, so a pane that re-initialises its dropdowns won't stack duplicates.

#### Shared control styling

Link the neutral, Polarion-matched sheets you need: `checkboxes.css`, `radios.css`, `inputs.css`,
`searchable-dropdown.css`, `tables.css`, `buttons.css` and `alerts.css`. React apps normally
get them through react-sbb-polarion's bundled `style.css` instead. The checkbox / radio / input rules
are intentionally **scoped** to the UI wrappers `.modal__container` (popups), `.standard-admin-page`
(admin pages) and `.form-wrapper` (document-properties side panels) so they never restyle Polarion's
own controls — put the matching wrapper class on your surface.

#### Design tokens & reuse in React SPAs (`control-tokens.css`)

The 2606 control look is defined once as CSS custom properties in `css/control-tokens.css` (border /
focus colors, control height, radius, the soft hover/active elevation shadows, checkbox images, radio
dot, combobox chevron, popup border, option hover tint, chips, typography, and the button tokens).
The class-based stylesheets above consume it via `var(--sbb-*)` — so restyling every native control
across the ecosystem is a one-file edit. Load it before any of them.

The tokens are declared on the shared UI scopes (`:root, .modal__container, .standard-admin-page,
.form-wrapper, .sbb-ui`), not only `:root`, so that on a page where several extensions load their own
bundled generic at **different versions** each extension's controls read the tokens from the closest
scoped ancestor — its own bundle's copy — rather than whichever `:root` loaded last. A consumer's UI
root must therefore carry one of those wrapper classes (`.sbb-ui` for the React SPAs).

**Icon tokens are generated from `.svg` files at build time.** The 17 icon tokens
(checkbox states, combobox chevron / erase, info, revert, warning / error triangles, table +/−) live
only as `.svg` files under `images/` — the single source of truth. In the source stylesheet each is a
placeholder, `--sbb-x: url(inline:images/x.svg)`; the Maven build (`npm run build:css` →
`scripts/inline-svg-tokens.mjs`, wired into `frontend-maven-plugin` at `process-classes`) rewrites the
copy in `target/classes`, replacing each placeholder with the base64 of that `.svg`. The **shipped**
stylesheet is thus fully self-contained — no runtime icon requests (required for the React SPAs, which
link it directly and must render even when `/polarion/*` 404s). **To change an icon, edit its `.svg`
and rebuild — never hand-edit a base64 blob.**

This is also the **reuse path for extensions whose UI is not built from our class-based CSS** — the
React SPAs (Vite / Next), which render their own markup (custom components, native `<select>`,
Bootstrap classes) and cannot link `checkboxes.css` / `searchable-dropdown.css`. Instead they:

1. **Link the tokens at runtime** from the embedded `generic.app` (served by `GenericUiServlet`), and
   reference `var(--sbb-*)` in their own component CSS:

   ```html
   <link rel="stylesheet" href="/polarion/<ext>-app/ui/generic/css/control-tokens.css" />
   ```
   ```css
   .my-combo        { border: 1px solid var(--sbb-control-border, #c9c9c9); border-radius: var(--sbb-control-radius, 0); }
   .my-combo:hover  { box-shadow: var(--sbb-control-shadow-hover, 0 2px 6px 0 rgba(0, 0, 0, .2)); }
   ```

2. **For real combobox parity**, wrap a native `<select>` with the vanilla `SearchableDropdown`,
   loaded via a runtime dynamic import (bundler-ignored so the build does not try to resolve the URL),
   and link `searchable-dropdown.css`. The `<select>` stays framework-controlled; the component
   mirrors the selection back and dispatches `change`. Use the shared **`searchableSelect.js`** factory
   (`createSearchableSelect`) rather than calling `new SearchableDropdown(...)` per app — it owns the
   defaults every combobox uses (`searchable`, `preserveOptionClasses`, no remembered selection). Derive
   the module URL from **this module's own served URL** (`import.meta.url`) so there is **no hardcoded
   `/<ext>-app/` segment** — the wrapper is then identical across every extension. (Derive from
   `import.meta.url`, not `location.pathname`: the module URL is always under `…/ui/…` and is stable
   regardless of the SPA's client-side route.)

   ```js
   const base = new URL('.', import.meta.url).href.replace(/\/ui\/.*$/, '/ui/generic/js/modules/');
   const { createSearchableSelect } = await import(/* webpackIgnore: true */ /* @vite-ignore */ base + 'searchableSelect.js');
   const sd = createSearchableSelect(selectEl, { allowEmpty: true, placeholder: 'Pick…' });
   // sd.selectValue(value) to sync from framework state; sd.destroy() on unmount.
   ```

   Server webapps that upgrade a fixed set of `<select>`s by id (the exporters) use the batch helper
   from the same module — `initSearchableDropdowns(ctx, singleIds, multiSelectId, options?)` — which
   wraps each via `createSearchableSelect` (so they share the same defaults) and forwards `options`
   (e.g. `{ allowEmpty: true }`) to every one.

   For an **editable / free-text** field (type a value or pick a filtered suggestion), the same module
   exports `createEditableSelect(inputEl, { inputFilter, items, placeholder })` — it wraps a text
   `<input>` as an editable dropdown (`editable: true`). It is the shared core of the React
   `SearchableInput` and the vanilla excel `ColumnInput`.

3. **Loading spinner**: reference `var(--sbb-spinner)` (the Polarion progress wheel) or add the
   `.sbb-spinner` class instead of hardcoding `/polarion/ria/images/progressWheel48.svg`, so the asset
   is swappable in one place (both are defined in `control-tokens.css`).

4. **Data tables**: give a results/list `<table>` the `sbb-table` class (`css/tables.css`) for the
   shared Polarion look — outer frame, tinted header, row
   separators and a hover tint, all driven by the `--sbb-table-*` tokens. Add `sbb-table--grid` for
   full spreadsheet cell borders or `sbb-table--compact` for denser rows; the extension keeps only its
   own column widths and row-state highlights. React SPAs link it at runtime like the other sheets:

   ```html
   <link rel="stylesheet" href="/polarion/<ext>-app/ui/generic/css/tables.css" />
   ```

`generic` is the upstream for these assets, but extensions no longer fetch the control CSS from here:
react-sbb-polarion copies it into its own bundle and ships it over npm. What still reads from
`/polarion/<ext>/ui/generic/` is a page that links a sheet directly, and `ensureSharedStyles.js` when
the vanilla `SearchableDropdown` is loaded at runtime. Always ship the literal fallback in the SPA's
`var()` calls so it still renders in a dev server running outside Polarion (where `/polarion/*` 404s).

#### Shared control CSS is self-provided by `SearchableDropdown` (versioned)

This applies to the **vanilla** runtime path only. A React surface gets the control CSS from
react-sbb-polarion's bundle, typically injected as a `<style>` inside its own shadow root, which a
page-level `<link>` could not reach in any case.

For the vanilla path the shared control CSS reaches a page as global `<link>`s. Historically that
only happened via an exporter's `scriptInjection.*Head` → `starter.js` → `injectStyles(...)`. That
path was **shared and optional**: the component classes are global, so one installed exporter styled
every surface — but if no exporter's `starter.js` ran (unconfigured `scriptInjection`, or the
injecting extension removed), a document-properties side panel rendered its controls **unstyled**
even though its JS had already wrapped them. (A panel cannot fix this from its own HTML fragment:
Polarion inserts it via `HtmlFragmentBuilder`, which honors an inline `<style>` but **not** external
`<link>`/`<script>` — which is also why panels bootstrap their JS through a `<link onload>` hook.)

So the component provides its own styling: the `SearchableDropdown` constructor calls
`ensureSharedStyles()` (`js/modules/ensureSharedStyles.js`), which injects the shared CSS
(`checkboxes` / `radios` / `inputs` / `searchable-dropdown`) as `<link>`s into `<head>`. Any surface
that renders a dropdown — side panel, export popup, admin page — is therefore styled on its own, with
**no per-extension wiring**: an extension just consumes the generic version that ships it. This is
the runtime path; a React app gets the same CSS bundled from react-sbb-polarion instead, and
RSP's vendored `ensureSharedStyles` is a deliberate no-op there.

**Versioning.** Each injected `<link>` carries `data-generic-version` = the generic bundle build
timestamp (baked into `generic-build-info.js` via `resources-filtered`). The `<link>` ids match the
ones `injectStyles` uses, so the two never duplicate. On a clash `ensureSharedStyles` keeps the copy
with the **higher** version and treats an unversioned copy (e.g. from an older `starter.js`) as the
lowest — so across a page assembled from extensions on **different generic versions**, the newest
generic's CSS always wins, deterministically and regardless of load order. `starter.js` needs no
change: its unversioned injection is simply superseded once a versioned copy is present.

#### Configuration pane styling

`css/configurations.css` carries the look of the admin "choose a configuration" pane. The pane
itself is a React component in react-sbb-polarion (`ConfigurationsPane`); generic ships only the
stylesheet, served at `/polarion/<ext>/ui/generic/css/configurations.css`.

#### Modal dialogs

Generic ships no dialog CSS. Use react-sbb-polarion's `Modal`, which renders a `<dialog>` styled by
its own bundled `Modal.css` (`.rsp-modal*` classes). The [Micromodal](https://micromodal.vercel.app/)
library and the `micromodal.css` that styled its `.modal__*` markup were both removed in 16.0.0.

`.modal__container` survives only as one of the wrapper scopes on which `control-tokens.css` declares
the `--sbb-*` custom properties, alongside `.standard-admin-page`, `.form-wrapper` and `.sbb-ui`. It
no longer carries any dialog styling of its own.

#### `BreadcrumbBridge` — app-header breadcrumb for topic extensions

`js/modules/BreadcrumbBridge.js` replaces the GWT app-header breadcrumb
(`.polarion-ApplicationHeader-breadcrumb`) — which shows a generic "home" for an extension topic —
with the extension's own name/icon while the extension's URL is active. It mirrors Polarion's own
breadcrumb shape:

- a **root** topic → `[icon] Title` (30px icon)
- a **sub** topic → `Parent › [small icon] Title` (17px icon)

Polarion renders that breadcrumb in the shell window, but an extension topic runs in a frame, so the
topic page injects this **classic** script into the shell (it needs `document.currentScript` to read
its config, so it is deliberately *not* an ES module). It is the single shared breadcrumb-bridge
implementation for every extension. It **never** activates on Polarion's own Administration pages
(`#/administration/…`), which render their breadcrumb correctly.

Inject it into the shell and configure it via `data-*` attributes (auto-install), or — once loaded —
call `install()` directly so a sub-topic can re-label without re-loading the module:

```js
// From the topic page (React effect in the SPA):
const shell = window.top;
const cfg = { marker: '<ext>', title: 'My Extension',
              // parent: 'My Extension',                 // set for a sub-topic → "Parent › title"
              icon: '/polarion/<ext>-admin/ui/images/menu/30x30/_parent.svg' };
if (shell.SbbBreadcrumbBridge) {
  shell.SbbBreadcrumbBridge.install(cfg);                // already loaded → install/update in place
} else {
  const doc = shell.document;
  const s = doc.createElement('script');
  s.id = 'sbb-breadcrumb-bridge-loader';
  s.src = '/polarion/<ext>/ui/generic/js/modules/BreadcrumbBridge.js';
  s.dataset.marker = cfg.marker;
  s.dataset.title = cfg.title;
  if (cfg.parent) s.dataset.parent = cfg.parent;
  if (cfg.icon) s.dataset.icon = cfg.icon;
  doc.head.appendChild(s);
}
```

Re-installing with the same `marker` **updates** the title/parent/icon (so navigating between an
extension's sub-topics re-labels the breadcrumb). It hides the real breadcrumb with a `!important`
stylesheet rule (surviving GWT re-renders) and keeps a `MutationObserver` connected, so it is safe to
inject early. `install(...)` returns an `{ sync, update, destroy }` handle.

#### Checkboxes (inline-SVG control tokens)

Native `<input type="checkbox">` is restyled to match Polarion 2606's own checkbox look — border,
gradient fill, and a heavy check mark (traced from Arial Unicode's U+2714, tuned to Polarion's
raster) — across all nine states: **unchecked / checked / indeterminate**, each in **normal /
keyboard-focus / read-only** variants. Focus uses Polarion's inverted-gradient box; read-only uses
the greyed border and glyph.

The visuals live in two files:

- **`css/control-tokens.css`** — nine `--sbb-checkbox-*` custom properties (e.g.
  `--sbb-checkbox-checked`, `--sbb-checkbox-indeterminate-focus`).
- **`css/checkboxes.css`** — the rules, scoped to our own wrappers (`.modal__container`,
  `.standard-admin-page`, `.form-wrapper`) so injecting it into a Polarion page never restyles
  Polarion's own checkboxes. It carries no literal fallbacks: it consumes the tokens through
  `var(--sbb-checkbox-*)`, and every load path puts `control-tokens.css` alongside it
  (`ensureSharedStyles` injects that one first).

**Why the images are inlined as base64 `data:` URIs (not linked `.svg`/`.png` files).**
A relative `url()` inside a CSS *custom property* resolves against the stylesheet that **uses** the
variable, not the one that **declares** it. So when a consumer references `var(--sbb-checkbox-…)`
from its own bundled CSS (a Vite/Next SPA served from `…/assets/…`), a relative image path resolves
against that bundle and **404s**; an absolute `/polarion/<ext>/…` path avoids the 404 but hard-codes
the per-consumer mount. A `data:` URI is self-contained — no URL resolution happens — so the same
token renders identically in generic's own CSS, in an injected side panel, and in any SPA bundle.
(Polarion pages set no CSP that blocks `data:` in CSS backgrounds.)

The SVG sources under **`images/checkbox/*.svg`** are the single source of truth. The source
stylesheet holds only `url(inline:images/checkbox/x.svg)` placeholders; the Maven build resolves each
one to base64 in `target/classes`. **To change the look, edit the `.svg` and rebuild.** Never
hand-edit a base64 blob and never write one back into the source CSS: `inlineSvgTokensTest.js` fails
the build if you do.

**Injected / admin / popup context** — link `checkboxes.css`
and use a native checkbox inside one of the scoped wrappers; `:indeterminate`, `:disabled` and
`:focus-visible` are handled automatically:

```html
<link rel="stylesheet" href="/polarion/<ext>/ui/generic/css/checkboxes.css">
<div class="form-wrapper"><label><input type="checkbox"> Include unreferenced</label></div>
```

**React / SPA** — link `control-tokens.css` once, then style the checkbox from the tokens, scoped to
your own root (e.g. `.app`) so Polarion's own checkboxes are untouched:

```css
.app input[type="checkbox"] {
  appearance: none; -webkit-appearance: none;
  width: var(--sbb-toggle-size, 15px); height: var(--sbb-toggle-size, 15px);
  background: var(--sbb-checkbox-unchecked) no-repeat center / contain;
}
.app input[type="checkbox"]:checked            { background-image: var(--sbb-checkbox-checked); }
.app input[type="checkbox"]:indeterminate      { background-image: var(--sbb-checkbox-indeterminate); }
.app input[type="checkbox"]:disabled           { background-image: var(--sbb-checkbox-unchecked-readonly); }
.app input[type="checkbox"]:disabled:checked   { background-image: var(--sbb-checkbox-checked-readonly); }
.app input[type="checkbox"]:focus-visible      { outline: none; background-image: var(--sbb-checkbox-unchecked-focus); }
.app input[type="checkbox"]:focus-visible:checked { background-image: var(--sbb-checkbox-checked-focus); }
```

Because these are inline data URIs, no relative or absolute `url()` path is ever needed — consumers
can drop the old per-mount work-arounds for the indeterminate image. The **indeterminate** state has
no HTML attribute, so set it from JS (e.g. a React ref):
`ref={(el) => { if (el) el.indeterminate = someButNotAll; }}`.

### Custom extension configuration

In order to register additional configuration properties a subclass of `ExtensionConfiguration` must be marked with the `@Discoverable`:

```java
@Discoverable
public class PdfExporterExtensionConfiguration extends ExtensionConfiguration {
    @Override
    public @NotNull List<String> getSupportedProperties() {
        List<String> supportedProperties = new ArrayList<>(super.getSupportedProperties());
        supportedProperties.add("weasyprint.service");
        ...
        return supportedProperties;
    }
    ...
}
```

### Document (DLE) editor toolbar button

To add a button to Polarion's document editor toolbar (via the `scriptInjection.dleEditorHead`
configuration property), reuse the shared engine `dle-toolbar-starter.js` shipped by generic instead
of writing the injection logic in every extension. The engine knows the toolbar DOM selectors and
**re-injects the button automatically** whenever Polarion (GWT) re-renders the toolbar — e.g. after the
user clicks *Save* — so the button does not disappear (a one-time injection otherwise would).

The engine is served to each extension at `/polarion/<extension>/ui/generic/js/dle-toolbar-starter.js`
and exposes `window.GenericDleToolbarStarter.create({ markerId, alternateHtml, defaultHtml, target,
order, permissionCheckUrl, permissionCheck })`, which returns `{ injectToolbar, setDisabled, destroy }`.
The same engine also drives the **Live Report** (Rich Page) toolbar — pass `target: 'richPagePreview'`
(see below); everything here, including disabling, works for both toolbars. It also injects the shared
toolbar-button stylesheet (`css/dle-toolbar.css`) itself, so you don't ship those `.dleToolBar*` rules.

Add a thin `starter.js` to your extension's webapp that supplies only the extension-specific parts
(button markup, a unique `markerId`, any css/js the button needs) and bootstraps the engine:

```js
(function () {
    const ts = `?timestamp=${Date.now()}`;
    const TOOLBAR_HTML = `<table class="dleToolBarTable">...your button...</table>`;           // standalone variant
    const ALTERNATE_TOOLBAR_HTML = `<table class="dleToolBarTable">...your button...</table>`;  // variant for inside the toolbar row

    // Expose the global immediately and queue calls until the engine has loaded.
    let starter = null, order;
    const pending = [];
    window.MyExtensionStarter = {
        injectToolbar: (params) => {
            // Capture this button's position among all toolbar buttons: this stub runs synchronously
            // as dleEditorHead executes, so the shared counter reflects config order (see "Ordering").
            if (order === undefined) {
                const seq = top.__genericDleToolbarSeq || (top.__genericDleToolbarSeq = { n: 0 });
                order = seq.n++;
            }
            starter ? starter.injectToolbar(params) : pending.push(params);
        }
    };

    const engine = document.createElement('script');
    engine.src = `/polarion/my-extension/ui/generic/js/dle-toolbar-starter.js${ts}`;
    engine.onload = () => {
        const generic = window.GenericDleToolbarStarter;
        generic.injectStyles("my-extension-styles", `/polarion/my-extension/css/my-extension.css${ts}`);
        // ...inject any other css/js the button needs...
        starter = generic.create({
            markerId: 'my-extension-toolbar-injected',   // unique id set on the injected element
            alternateHtml: ALTERNATE_TOOLBAR_HTML,
            defaultHtml: TOOLBAR_HTML,
            order                                        // stable left-to-right order (see "Ordering")
        });
        pending.forEach(p => starter.injectToolbar(p));
        pending.length = 0;
    };
    document.head.appendChild(engine);
})();
```

Then point the document editor at it via the Polarion configuration property (the bootstrap loads the
engine and queues the call, so this config is the same as without the shared engine):

```properties
scriptInjection.dleEditorHead=<script src="/polarion/my-extension/js/starter.js"></script><script>MyExtensionStarter.injectToolbar({alternate: true});</script>
```

`injectToolbar({ alternate: true })` inserts the button into the editor toolbar row; calling it without
`alternate` renders the standalone `defaultHtml` variant above the rich-text area. The engine is
idempotent (guarded by `markerId`) and sets up one self-healing observer per extension.

#### Ordering multiple buttons

When several extensions each add a button (separate `dleEditorHead` entries), the engine keeps them in a
stable left-to-right order across re-renders. The order is **not configured by hand** — it is the
position of the `injectToolbar` call in `dleEditorHead`:

- The inline `<script>…injectToolbar(...)</script>` tags in `dleEditorHead` execute in document order, so
  each thin starter's `injectToolbar` stub runs synchronously in that order.
- On its first call the stub reads (and increments) a shared `top.__genericDleToolbarSeq` counter and
  passes that value as `order` to `create({ order })`.
- On every (re-)injection the engine inserts the button before the first already-present button whose
  `order` is higher, otherwise before the toolbar spacer cell. The marker→order map lives on
  `top.__genericDleToolbarOrder`, so placement is independent of which extension's self-healing observer
  happens to re-fire first after a toolbar re-render.

To change the order, reorder the `injectToolbar` lines in `dleEditorHead`. Note that determinism requires
**distinct** `order` values: buttons that share the same `order` (including any caller that omits it — it
defaults to `0`) tie-break by observer-fire order, i.e. non-deterministically, exactly as before this
mechanism existed.

#### Disabling the button (permission-gated actions)

When the button's action isn't always available — e.g. the user isn't permitted to export — inject it
**disabled** instead of clickable. The disabled state is kept by the engine and re-applied on every
self-healing re-injection (so it survives toolbar re-renders), and the click is blocked via a
`pointer-events: none` class regardless of the `onclick` baked into your button markup. This works the
same for the Live Doc (`dleEditor`) and Live Report (`richPagePreview`) toolbars.

There are two ways to use it.

**1. Let the engine run a permission check.** Pass either a URL or a function to `create(...)`. The
engine treats the result as a plain boolean; **any context the check needs (project, document, …) is
the extension's to supply** — the engine doesn't parse the page or know about projects. The extension
knows the context (e.g. from its own `ExportContext`) and bakes it into the URL or the function:

```js
// The engine GETs the URL and expects JSON { "permitted": boolean }.
// permitted !== true, a non-OK HTTP status, or an error → the button stays disabled (fail-closed).
// Build the URL with whatever the check is scoped to — here a project-level permission:
window.GenericDleToolbarStarter.create({
    markerId: 'my-extension-toolbar-injected',
    alternateHtml: ALTERNATE_TOOLBAR_HTML,
    defaultHtml: TOOLBAR_HTML,
    permissionCheckUrl: `/polarion/my-extension/rest/internal/permissions/export?projectId=${ctx.getProjectId()}`
}).injectToolbar({ alternate: true });

// ...or run the check yourself (custom headers, your own REST wrapper, extra context):
window.GenericDleToolbarStarter.create({
    markerId: 'my-extension-toolbar-injected',
    alternateHtml: ALTERNATE_TOOLBAR_HTML,
    defaultHtml: TOOLBAR_HTML,
    permissionCheck: () => ctx.callAsync({
        method: 'GET',
        url: `/polarion/my-extension/rest/internal/permissions/export?projectId=${ctx.getProjectId()}`
    }).then(r => r.response.permitted)
}).injectToolbar({ alternate: true });
```

While the check is pending the button is shown **disabled**, so it never flickers enabled→disabled;
it becomes clickable only once the check confirms access. `permissionCheck` (a function returning
`boolean | Promise<boolean>`) takes precedence over `permissionCheckUrl` if both are given. If neither
is configured the button is enabled, as before.

**2. Toggle it yourself.** `create(...)` returns a `setDisabled(bool)` method, and `injectToolbar`
accepts a `disabled` flag — use these if you resolve the permission on your own schedule:

```js
const starter = window.GenericDleToolbarStarter.create({ markerId: '…', alternateHtml: ALTERNATE_TOOLBAR_HTML, defaultHtml: TOOLBAR_HTML });
starter.injectToolbar({ alternate: true, disabled: true });   // start disabled
myPermissionCheck().then(allowed => starter.setDisabled(!allowed));   // enable/disable later
```

The backend endpoint (or whatever decides *whether* the action is permitted) is owned by your
extension; the engine only renders and preserves the disabled look. The disabled styling lives in the
shared `css/dle-toolbar.css` as `.dleToolBarDisabled`.
