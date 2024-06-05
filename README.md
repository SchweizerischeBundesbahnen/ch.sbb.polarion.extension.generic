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
