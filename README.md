# Generic extension of Polarion ALM

This is a Polarion extension which provides common part to other extensions reducing code duplication.

An extension which inherits from this generic extension will automatically get following functionality:

* An "about" page on administrative section of Polarion with basic information about this extension
* API to manipulate settings of this extension - to read, save settings and reverting them to default values
  as well as getting list of settings history revisions
* API to serialize/deserialize XML data (`JAXBUtils`)
* REST application and end points giving access to settings functionality described above as well as access
  to extension information and version
* Swagger UI page listing information about REST API provided

## Ho to use

To properly inherit from this generic extension and to take advantage of all mentioned above functionality
out of the box certain steps should be done, see below.

### pom.xml

Maven's `pom.xml` should contain following content:

* Reference to parent POM (don't forget to use proper version of it):

```xml
<parent>
  <groupId>ch.sbb.polarion.extensions</groupId>
  <artifactId>ch.sbb.polarion.extension.generic.parent-pom</artifactId>
  <version>1.1.5</version>
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

### Rest application

Rest application class should inherit from `GenericRestApplication` of generic extension,
registering settings classes and extending classes of REST controller, web application filters and exception mappers:

```java
public class PdfExporterRestApplication extends GenericRestApplication {

    public PdfExporterRestApplication() {
        SettingsRegistry.INSTANCE.register(Arrays.asList(new CssSettings(), new HeaderFooterSettings(), new LocalizationSettings()));
    }

    @Override
    @NotNull
    protected Set<Class<?>> getControllerClasses() {
        final Set<Class<?>> controllerClasses = super.getControllerClasses();
        controllerClasses.addAll(Set.of(
            ApiController.class,
            InternalController.class
        ));
        return controllerClasses;
    }

    // potentially override also methods
    //   protected @NotNull Set<Class<?>> getExceptionMappers()
    // and
    //   protected @NotNull Set<Class<?>> getFilters()
}
```

### UI servlet class

If new extension will contain UI parts/pages/artifacts, UI servlet class should be created extending `GenericUiServlet`
simply specifying servlet name in constructor:

```java
public class PdfAdminUiServlet extends GenericUiServlet {
    public PdfAdminUiServlet() {
        super("pdf-exporter-admin");
    }
}
```

## Changelog

| Version | Changes                                                                                                                                   |
|---------|-------------------------------------------------------------------------------------------------------------------------------------------|
| v4.8.1  | SonarQube fixes                                                                                                                           |
| v4.8.0  | Polarion 2404 is supported                                                                                                                |
| v4.7.0  | * Added/Improved logging.<br/>* Display baseline in the revisions table                                                                   |
| v4.6.0  | Utility class HtmlUtils is introduced                                                                                                     |
| v4.5.0  | * User-friendly message about deactivated configuration properties on the Swagger UI page.<br/> * Logging in ExceptionMappers             |
| v4.4.0  | About page refactored and support email added                                                                                             |
| v4.3.0  | About page supports configuration help and icon                                                                                           |
| v4.2.5  | dummy delivery-sbb and delivery-external maven profiles added                                                                             |
| v4.2.4  | Methods for getting entities by revision now properly return objects for already deleted entities                                         |
| v4.2.3  | Fix saving default settings in read only transaction                                                                                      |
| v4.2.2  | Extended LogoutFilter with async skip request property                                                                                    |
| v4.2.1  | Fix exception during save operation in nested transaction                                                                                 |
| v4.2.0  | Added methods for getting fields values<br/> Proper users/assignee fields processing                                                      |
| v4.1.0  | CORS filter for REST application implemented                                                                                              |
| v4.0.7  | Made timed hiding of alert messages optional                                                                                              |
| v4.0.6  | Update maven dependencies                                                                                                                 |
| v4.0.5  | * Fixed scope agnostic controller<br/> * Added display config properties in "about page"                                                  |
| v4.0.4  | Added ability to set nulls to fields<br/> * Multi-value enum support                                                                      |
| v4.0.3  | Added boolean field converter                                                                                                             |
| v4.0.2  | Changed CSS tweaks                                                                                                                        |
| v4.0.1  | Removed 8px margin from code-input                                                                                                        |
| v4.0.0  | * Upgrade code-input component to v2.1.0<br/> * Reworking and polishing JS code<br/> * Refactoring                                        |
| v3.0.7  | Converters generic types improvement                                                                                                      |
| v3.0.6  | Moved generic methods/converters for settings fields                                                                                      |
| v3.0.5  | Fix for default-settings                                                                                                                  |
| v3.0.4  | Changed configuration pre-delete callback                                                                                                 |
| v3.0.3  | Added configuration delete callback into JS component                                                                                     |
| v3.0.2  | Added API to save directly byte array                                                                                                     |
| v3.0.1  | Refactored polarion 2310 profile                                                                                                          |
| v3.0.0  | * Settings id/name usage improvement<br/> * Fixed messages clearing<br/> * Refactoring                                                    |
| v2.2.0  | * Fixed revisions list in case when setting renamed<br/> * Fixed UI of custom select<br/> * Refactoring                                   |
| v2.1.1  | * Fixed reading setting and revert to revision urls<br/> * Unified reading revisions calls                                                |
| v2.1.0  | * Extended custom select element<br/> * Added function to check if it contains certain option                                             |
| v2.0.0  | * Changed polarion version to 2310<br/> * Update maven dependencies<br/> * Refactoring                                                    |
| v1.1.20 | Extend functionality                                                                                                                      |
| v1.1.19 | * Added custom select<br/> * Added getters for PolarionService                                                                            |
| v1.1.18 | * Refactoring<br/> * Update maven dependencies                                                                                            |
| v1.1.17 | * Added GenericSettings afterSave action call<br/> * Update log4j.version to v2.21.0                                                      |
| v1.1.16 | * PolarionService has been introduced<br/> * Added unit tests for PolarionService                                                         |
| v1.1.15 | Added settings move logic                                                                                                                 |
| v1.1.14 | Refactoring                                                                                                                               |
| v1.1.13 | Added SBB extension config using polarion.properties                                                                                      |
| v1.1.12 | Added utils of getting system parameters                                                                                                  |
| v1.1.11 | * Added generic method for deserialization<br/> * Added unit tests for serialization and deserialization<br/> * Update maven dependencies |
| v1.1.10 | * Fixed maven dependencies versions<br/> * Fixed CSS                                                                                      |
| v1.1.9  | Added jobs logger                                                                                                                         |
| v1.1.8  | Added API to delete location from repository                                                                                              |
| v1.1.7  | * Added CRUD for cookies<br/> * Added validate projectId for context before using<br/> * Added font for option                            |
| v1.1.6  | * Added Readme<br/> * Refactoring settings                                                                                                |
| v1.1.5  | * Added NotAuthorizedExceptionMapper<br/> * Refactoring settings                                                                          |
| v1.1.4  | * Added OpenAPI annotations<br/> * Added settings-related java classes<br/> * Added generic settings controllers                          |
| v1.1.3  | * Multiple web apps fix<br/> * Bundle symbolic name                                                                                       |
| v1.1.2  | Sonarqube fixes                                                                                                                           |
| v1.1.1  | Hide context from swagger                                                                                                                 |
| v1.1.0  | Context has been introduced                                                                                                               |
| v1.0.0  | Initial release                                                                                                                           |