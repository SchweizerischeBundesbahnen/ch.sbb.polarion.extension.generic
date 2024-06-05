# Changelog

## [4.9.2](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v4.9.1...v4.9.2) (2024-06-03)


### Bug Fixes

* exceptions are refactored not to use JAX-RS exceptions in services ([#20](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/20)) ([0e8e0df](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/0e8e0df77554bf947e0aaa06fe728a5f503b830c))

## [4.9.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v4.9.0...v4.9.1) (2024-05-29)


### Bug Fixes

* deprecation in GitHub actions for maven-release  ([#14](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/14)) ([dc2ee7d](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/dc2ee7d0bf06c71003e1e1300008387901de1398))
* exceptions are refactored not to use JAX-RS exceptions in services ([#18](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/18)) ([6c7de85](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/6c7de855b40cb72cdda677a98c5311a930079eda))

## [4.9.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v4.8.1...v4.9.0) (2024-05-28)


### Features

* GenericModule added. ([#9](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/9)) ([25ca190](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/25ca190e3361de90e1bc96b550929a3293132617))


### Bug Fixes

* add missing profiles in settings.xml ([#8](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/8)) ([295e052](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/295e052cbe6329368464514cf24fe7aec736b06c))
* use altDeploymentRepository instead of altReleaseDeploymentRepository ([#12](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/12)) ([314ce47](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/314ce4707628b0ece0e1f6f880a1c3e0dcf58348))

## 4.8.1 (2024-05-27)


### Miscellaneous Chores

* release 4.8.1 ([#4](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/4)) ([5fc3c1f](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/5fc3c1f52be9b43ed20c4e5a71f189d3dc995c89))


# Changelog before migration to conventional commits

| Version | Changes                                                                                                                                   |
|---------|-------------------------------------------------------------------------------------------------------------------------------------------|
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