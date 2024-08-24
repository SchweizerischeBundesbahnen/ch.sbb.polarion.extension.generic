[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic&metric=bugs)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic&metric=coverage)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.generic)

# Generic extension of Polarion ALM

This is a Polarion extension which provides common part to other extensions reducing code duplication.

An extension which inherits from this generic extension will automatically get following functionality:

* An "about" page on administrative section of Polarion with basic information about this extension
* API to manipulate settings of this extension - to read, save settings and reverting them to default values as well as getting list of settings history revisions
* API to serialize/deserialize XML data (`JAXBUtils`)
* REST application and end points giving access to settings functionality described above as well as access to extension information and version
* Swagger UI page listing information about REST API provided

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

* Specify extension context, automatic module name and web application name in POM's properties:

```xml
<properties>
    <maven-jar-plugin.Extension-Context>pdf-exporter</maven-jar-plugin.Extension-Context>
    <maven-jar-plugin.Automatic-Module-Name>ch.sbb.polarion.extension.pdf_exporter</maven-jar-plugin.Automatic-Module-Name>
    <web.app.name>${maven-jar-plugin.Extension-Context}</web.app.name>
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
                    <package>ch.sbb.polarion.extension.generic.rest.controller</package>
                    <package>ch.sbb.polarion.extension.generic.rest.model</package>
                    <package>ch.sbb.polarion.extension.pdf.exporter.rest.controller</package>
                    <package>ch.sbb.polarion.extension.pdf.exporter.rest.model</package>
                </resourcePackages>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### MANIFEST.MF

File `MANIFEST.MF` should be created in `src/main/resources/META-INF/MANIFEST.MF` with following content:

* Property `Bundle-Name` should contain extension name, eg.

```properties
Bundle-Name: PDF Exporter Extension for Polarion ALM
```

* Property `Require-Bundle` should list all bundles from which this extension depends, eg.

```properties
Require-Bundle: com.polarion.portal.tomcat,
 com.polarion.alm.ui,
 com.polarion.platform.guice,
 com.polarion.alm.tracker,
 org.glassfish.jersey,
 com.fasterxml.jackson,
 com.fasterxml.jackson.jaxrs,
 io.swagger,
 org.apache.commons.logging,
 slf4j.api,
 org.springframework.spring-core,
 org.springframework.spring-web
```

* If Polarion's form extension is implemented, property `Guice-Modules` should specify a class which does this, eg.

```properties
Guice-Modules: ch.sbb.polarion.extension.pdf.exporter.PdfExporterModule
```

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
        CurrentExtensionConfiguration.getInstance().setExtensionConfiguration(PdfExporterExtensionConfiguration.getInstance());
    }
}
```
