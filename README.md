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

    <!-- Extension-specific bundles (in addition to common ones from parent POM).
         Must start with comma. Leave empty or omit if only common bundles are needed. -->
    <maven-jar-plugin.Require-Bundle.extension>,com.polarion.alm.tracker,com.polarion.platform.guice,com.polarion.alm.wiki,org.jsoup,org.springframework.spring-core,org.springframework.spring-web</maven-jar-plugin.Require-Bundle.extension>
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
| `Require-Bundle` | `maven-jar-plugin.Require-Bundle.common` + `.extension` | 11 common bundles (see below) |
| `Support-Email` | `maven-jar-plugin.Support-Email` | `polarion-opensource@sbb.ch` |
| `Bundle-ActivationPolicy` | `maven-jar-plugin.Bundle-ActivationPolicy` | `lazy` |
| `Import-Package` | `maven-jar-plugin.Import-Package` | `org.osgi.framework` |

Common `Require-Bundle` bundles defined in the parent POM (`maven-jar-plugin.Require-Bundle.common`):
`com.polarion.portal.tomcat`, `com.polarion.alm.ui`, `javax.inject`, `javax.annotation-api`, `org.glassfish.jersey`, `com.fasterxml.jackson.core`, `com.fasterxml.jackson.databind`, `com.fasterxml.jackson.annotations`, `com.fasterxml.jackson.module.jaxb.annotations`, `org.apache.commons.logging`, `slf4j.api`

To add extension-specific bundles, override `maven-jar-plugin.Require-Bundle.extension` in the extension's `pom.xml` (value must start with `,`):

```xml
<maven-jar-plugin.Require-Bundle.extension>,com.polarion.alm.tracker,com.polarion.platform.guice,org.springframework.spring-core,org.springframework.spring-web</maven-jar-plugin.Require-Bundle.extension>
```

To override `Import-Package` (e.g. to add `org.eclipse.core.runtime`), set `maven-jar-plugin.Import-Package` in the extension's `pom.xml`:

```xml
<maven-jar-plugin.Import-Package>org.osgi.framework,org.eclipse.core.runtime</maven-jar-plugin.Import-Package>
```

The static `MANIFEST.MF` should only contain:

* `Bundle-Name` — extension display name:

```properties
Bundle-Name: PDF Exporter Extension for Polarion ALM
```

* `Bundle-Activator` — if the bundle has a form extension, registered either using custom `org.osgi.framework.BundleActivator` or `ch.sbb.polarion.extension.generic.GenericBundleActivator`:

```properties
Bundle-Activator: ch.sbb.polarion.extension.pdf_exporter.ExtensionBundleActivator
```

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
