# Changelog

## [6.6.3](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v6.6.2...v6.6.3) (2024-08-22)


### Bug Fixes

* show revision in the exception message if it was provided as a p… ([#128](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/128)) ([107e96e](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/107e96e91a80ccbe96b971b358cb6ad524ef8fcb))

## [6.6.2](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v6.6.1...v6.6.2) (2024-08-09)


### Bug Fixes

* upload tests jar to maven central and github packages ([#121](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/121)) ([89dd429](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/89dd429717318ce9d7de1ad288ae46d0cf74b078))

## [6.6.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v6.6.0...v6.6.1) (2024-08-07)


### Bug Fixes

* update markdown2html maven plugin to v1.3.0 to generate heading IDs ([#116](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/116)) ([2b195ff](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/2b195ffabe5d8b423ddf9a33633202b1c0570228))

## [6.6.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v6.5.2...v6.6.0) (2024-08-07)


### Features

* jersey-client added as test dependency ([#113](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/113)) ([d0c48eb](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/d0c48eb9cbcfc1394c1b18a5ca04466facb7edee)), closes [#112](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/112)

## [6.5.2](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v6.5.1...v6.5.2) (2024-08-01)


### Bug Fixes

* JavaScript error 'getStringIfTextResponse not a function' fixed ([#108](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/108)) ([046195d](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/046195d70ac95826066d205f1e17c906425d5bee))

## [6.5.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v6.5.0...v6.5.1) (2024-07-31)


### Bug Fixes

* cannot insert csv with spaces next to comma into multi-enum field ([#105](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/105)) ([426fd74](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/426fd74a1ca09b865370191f8f19815a0e1c97a1)), closes [#104](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/104)

## [6.5.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v6.4.0...v6.5.0) (2024-07-30)


### Features

* swagger-maven-plugin added for OpenAPl spec generation ([#101](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/101)) ([e907109](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/e9071096c20d85e3c7cb8dd2cb3271efe7120e48)), closes [#100](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/100)


### Bug Fixes

* check polarion configuration whether XSRF Token is enabled ([#98](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/98)) ([6549a95](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/6549a95a4e79632e787c2138f9a803556ab31aea)), closes [#93](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/93)

## [6.4.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v6.3.0...v6.4.0) (2024-07-24)


### Features

* allowed resource file types set expanded (woff/woff2/ico) ([#91](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/91)) ([47de7c7](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/47de7c7bb8b4eb65cb62c1c729031d1c7037fb7d)), closes [#90](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/90)
* exclude "## Build", "## Installation to Polarion" and "## Changelog" sections from About page ([#96](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/96)) ([2822842](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/2822842ceee56aca0b7a107b3176605a552fcafb)), closes [#94](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/94)
* XSRF Token support ([#95](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/95)) ([f7df6f3](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/f7df6f347786b2c9571fb097809b4acdc9cbb3aa)), closes [#93](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/93)

## [6.3.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v6.2.0...v6.3.0) (2024-07-16)


### Features

* by default markdown2html-maven-plugin stops building on error ([#87](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/87)) ([9c57c49](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/9c57c499fc4b3102973df31f7fdcfa2f13168cef))

## [6.2.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v6.1.0...v6.2.0) (2024-07-10)


### Features

* use markdown2html-maven-plugin for making generation of about.html based on README.md cross-platform ([#82](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/82)) ([e78475b](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/e78475b8ac112ecc3206d2c1dc0fa6770d3a7c4c)), closes [#81](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/81)


### Documentation

* README.md updated according to the latest changes ([#84](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/84)) ([09042ba](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/09042baa02d22a373bed8be2b1bc7b1e8f2857d4)), closes [#81](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/81)

## [6.1.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v6.0.3...v6.1.0) (2024-07-09)


### Features

* default about page changed to display help, if help is was not included link to README.md will be displayed ([#79](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/79)) ([030fc3f](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/030fc3fd49c3d2dfe9129b26c81f1891355483af))


### Bug Fixes

* Changed logic for exception handling ([#76](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/76)) ([9b17f32](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/9b17f32163bc8766faedfb5c5e0a7e190a755192))

## [6.0.3](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v6.0.2...v6.0.3) (2024-07-05)


### Miscellaneous Chores

* release 6.0.3 ([#73](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/73)) ([35a441b](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/35a441bd046c6295c23c33b32a32e9db3d94607d))

## [6.0.2](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v6.0.1...v6.0.2) (2024-07-03)


### Bug Fixes

* Changed exception mapping logic. ([#69](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/69)) ([a5290cd](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/a5290cd310bc146acbf0fc8ad7ced6569c39cce7))

## [6.0.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v6.0.0...v6.0.1) (2024-07-01)


### Bug Fixes

* mvn central deployment ([#58](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/58)) ([b187ed0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/b187ed08debd6da4f4e936a5b0d216b93dc87f5b))

## [6.0.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v5.0.1...v6.0.0) (2024-07-01)


### ⚠ BREAKING CHANGES

* maven central deployment ([#55](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/55))

### Features

* maven central deployment ([#55](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/55)) ([a891203](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/a89120341a0cab3906c18a95dd452d75af0dcf9c))

## [5.0.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v5.0.0...v5.0.1) (2024-06-29)


### Bug Fixes

* Fixed loading named settings ([#50](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/50)) ([23a96ed](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/23a96edebb890914507642e3e7f3403daf1963eb)), closes [#51](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/51)

## [5.0.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v4.11.0...v5.0.0) (2024-06-26)


### ⚠ BREAKING CHANGES

* Configured jax-rs controllers, filters and exception-mappers a… ([#44](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/44))

### Features

* Configured jax-rs controllers, filters and exception-mappers a… ([#44](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/44)) ([382c97d](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/382c97d121c06eed7961c51fbf06ffc3a885406a))

## [4.11.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v4.10.0...v4.11.0) (2024-06-24)


### Features

* maven profile for auto generation of about page based on readme.md added ([#40](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/40)) ([11c8baf](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/11c8baf4d9e23cff3ae6c4f73be2dc2bb24dd132))


### Bug Fixes

* security alerts fix ([#30](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/30)) ([f75403a](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/f75403af7435251324532ddd94afa0c5fda9b70b))

## [4.10.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v4.9.2...v4.10.0) (2024-06-05)


### Features

* do not return default settings if requested name does not exist  ([#25](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/25)) ([defe5f0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/defe5f098a95b5c319454d0a5717560f5cbfc0ea))
* do not return default settings if requested name is not exist ([#23](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/23)) ([18905bb](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/18905bbd6b40e46c7e797797c39de2a34763124b))


### Documentation

* changelog history moved from README.md to CHANGELOG.md ([#27](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/27)) ([a635528](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/a635528852f77c4eb91887fb12816b60c8186004))

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
