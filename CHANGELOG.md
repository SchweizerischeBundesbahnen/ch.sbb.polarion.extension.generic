# Changelog

## [10.0.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v9.1.1...v10.0.0) (2025-06-02)


### ⚠ BREAKING CHANGES

* **deps:** update Maven distribution management to use central repository ([#288](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/288))

### Miscellaneous Chores

* **deps:** update Maven distribution management to use central repository ([#288](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/288)) ([d33ec18](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/d33ec1857183dd449965aa9f134bfada6167dc27))

## [9.1.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v9.1.0...v9.1.1) (2025-05-22)


### Bug Fixes

* fix transaction mocks for TransactionalExecutor ([#282](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/282)) ([fb40581](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/fb40581234b07498dff98b7efb97cee3e1147cd7))
* fixed downloading blob file inside iframe ([#284](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/284)) ([e5ba3e5](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/e5ba3e53d7a2f86d79c23d274f90d8eee9655ead)), closes [#281](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/281)

## [9.1.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v9.0.1...v9.1.0) (2025-05-19)


### Features

* Introduced new annotation based property mapping scanner ([#273](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/273)) ([8ca0cf3](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/8ca0cf3718a7f43f26e7cc90cd12da881c6eca10))
* Introduced new annotation based property mapping scanner that uses annotations to detect value, defaultValue, and description getter methods in extensions. The old method naming convention is still supported, but now treated with lower priority. ([8ca0cf3](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/8ca0cf3718a7f43f26e7cc90cd12da881c6eca10)), closes [#266](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/266)
* Provide icon URLs for enum options ([#275](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/275)) ([bf7eb72](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/bf7eb72b00aab6ff1a2cf8b81958b9ac05735a15))

## [9.0.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v9.0.0...v9.0.1) (2025-05-08)


### Bug Fixes

* unable to initialize extension without declared ExtensionBundleA… ([#268](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/268)) ([791c0d3](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/791c0d3304399c4acf2917775872352267618645))
* unable to initialize extension without declared ExtensionBundleActivator ([791c0d3](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/791c0d3304399c4acf2917775872352267618645)), closes [#267](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/267)

## [9.0.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v8.1.1...v9.0.0) (2025-05-06)


### ⚠ BREAKING CHANGES

* change the way of form-extensions registering ([#262](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/262))

### Features

* change the way of form-extensions registering ([#262](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/262)) ([d6b4345](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/d6b4345ea00bdc11a0bf2e205c88a8803297a878)), closes [#261](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/261)

## [8.1.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v8.1.0...v8.1.1) (2025-04-15)


### Bug Fixes

* **deps:** update dependency com.google.re2j:re2j to v1.8 ([#250](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/250)) ([1656844](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/1656844a01b82e019e8f7abd0a1066c45bf104ad))
* Link color to simulate Polarion style ([#247](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/247)) ([3eb9155](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/3eb9155403c09ef2661283368859d52fb86c3666))

## [8.1.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v8.0.0...v8.1.0) (2025-03-04)


### Features

* Add Polarion status provider and maven resource filtering  ([#241](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/241)) ([17808c2](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/17808c28e1e1f8f1e5e23cc6d1d3c0e1474bdaa7))
* JS modules alternatives for currently existing JS entities ([#245](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/245)) ([6f7a17b](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/6f7a17b51c29916cbdb65cfd562ef494bdcccf7a))

## [8.0.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v7.7.0...v8.0.0) (2024-12-19)


### ⚠ BREAKING CHANGES

* polarion2410 support ([#211](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/211))

### Features

* extensions for unit tests ([#231](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/231)) ([66cbed7](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/66cbed71e8fec42abf65734399bf14d2d0303138)), closes [#220](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/220)
* polarion2410 support ([#211](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/211)) ([e3d2617](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/e3d2617f23f1b23cd59e3a5cad02bee19636cc15)), closes [#206](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/206)
* README.md updated ([#234](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/234)) ([6dea086](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/6dea0861dc15c41c6ab541ec1107aae41173fca6)), closes [#232](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/232)
* splitting REST API controllers to different packages ([91591af](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/91591af0570ed318a1260d8b7e892e5f99f1787c))
* splitting REST API controllers to different packages ([#233](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/233)) ([91591af](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/91591af0570ed318a1260d8b7e892e5f99f1787c)), closes [#232](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/232)

## [7.7.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v7.6.0...v7.7.0) (2024-10-31)


### Features

* javax.transaction-api for test scope ([#213](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/213)) ([25930c2](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/25930c22d32b02e089ddfea188a7969300f10bec))
* polarion2404 profile as default using .mvn/maven.config ([#208](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/208)) ([3e99966](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/3e9996654cef68319478be0254b562dae7bf6560)), closes [#207](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/207)
* polarion2404 profile as default using .mvn/maven.config ([#210](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/210)) ([5bc9f73](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/5bc9f732d91427cc94a17f9e14565369a898419d)), closes [#207](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/207)


### Bug Fixes

* proper error message on attempt to set single value into multi-v… ([#214](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/214)) ([e18543d](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/e18543dadd27469775e3e16fed3da2b7a91bf796))
* proper error message on attempt to set single value into multi-value field and vice-versa ([e18543d](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/e18543dadd27469775e3e16fed3da2b7a91bf796))

## [7.6.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v7.5.0...v7.6.0) (2024-10-25)


### Features

* polarion 2410 support ([#203](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/203)) ([64f8a06](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/64f8a064411d2f0aa824ee06052766e26e694409)), closes [#202](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/202)
* removing sonarcloud badges from about page of extensions ([#200](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/200)) ([80f3f85](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/80f3f859c041c2859bafb225abebbf17975100ff)), closes [#199](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/199)

## [7.5.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v7.4.1...v7.5.0) (2024-10-22)


### Features

* "default" and "description" columns added to configuration properties table in About page ([#191](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/191)) ([94daaba](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/94daaba55689f3265037d7e21a5d8a15fc392240)), closes [#190](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/190)
* default value for "debug" configuration option ([#194](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/194)) ([8de1d51](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/8de1d5164b9296655f2195d79ef612c5956d0ed1)), closes [#190](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/190)
* getting configured but obsolete/non-valid options for extension ([#196](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/196)) ([79e1f9c](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/79e1f9c99a95d499e302414bd0be4b0aa26a7073)), closes [#190](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/190)
* refactoring after adding "default" and "description" columns ([#193](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/193)) ([45a0ab1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/45a0ab1d55d66db05441d828edd9346abbb29815)), closes [#190](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/190)


### Bug Fixes

* regexp changed to catch all urls and u/e-numbers ([#189](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/189)) ([aff0166](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/aff0166487bc2c6b8519f92be92623f8feded347))

## [7.4.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v7.4.0...v7.4.1) (2024-10-15)


### Bug Fixes

* fields data ignored on new style package creation ([#184](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/184)) ([f934104](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/f934104c3d9655c51908c62df7cababab1a8093d))
* wrong configuration properties if no Configuration-Properties-Prefix provided in manifest ([#187](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/187)) ([473af1a](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/473af1a8dd7bcccf0415206e8c762a83ef127f53)), closes [#186](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/186)

## [7.4.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v7.3.1...v7.4.0) (2024-10-08)


### Features

* current context for test classes ([#180](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/180)) ([244de95](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/244de95898488926f2171478b366213446dcd6e3))

## [7.3.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v7.3.0...v7.3.1) (2024-10-02)


### Bug Fixes

* options mapping refactoring ([#176](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/176)) ([243f4f9](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/243f4f937ef2f347b0f0d1f86f6c01fbd1b21572))
* sonarqube code smells ([#178](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/178)) ([cc0cfc8](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/cc0cfc81cf3facc26e62fb901fc5d2187f9e1591))

## [7.3.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v7.2.0...v7.3.0) (2024-10-02)


### Features

* extension context should not be null or blank ([#173](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/173)) ([ae54c17](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/ae54c17afa589d6a7a7004f96f30e648d1001a72))

## [7.2.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v7.1.0...v7.2.0) (2024-09-30)


### Features

* Configuration-Properties-Prefix added to manifest ([#164](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/164)) ([d2eec01](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/d2eec018359284c72cea77e8e6816c0e638d9d61)), closes [#163](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/163)


### Bug Fixes

* SonarQube code smells ([#170](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/170)) ([837a95a](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/837a95ad293861368472594aedc1287a806b2aba))

## [7.1.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v7.0.2...v7.1.0) (2024-09-09)


### Features

* deserialize methods with defaultValue parameter ([#159](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/159)) ([91e4c61](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/91e4c615c8cbd0ab767618d32368eca3e2df51fa)), closes [#158](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/158)
* deserialize methods with defaultValue parameter ([#160](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/160)) ([3c8f10f](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/3c8f10fecc75126abbb33d9b7839af9c291aed9b))
* Extended REST APi annotation ([#155](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/155)) ([2cd1808](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/2cd1808e4a6191bef17d8ce2e40e207f8d3552a5)), closes [#154](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/154)

## [7.0.2](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v7.0.1...v7.0.2) (2024-09-02)


### Bug Fixes

* exclude extra mappers registration in JacksonFeature ([#146](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/146)) ([c681097](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/c6810979201adaee82061d7a6a44b59c3efbf7d3)), closes [#173](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/173)
* processing URLs in About page ([#151](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/151)) ([9ba2966](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/9ba2966b26d801e0241e66cc0576b98a9cc97b4d))
* Sort rows in Extension configuration status table by configuration name ([#150](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/150)) ([75550d4](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/75550d45b85924bc9675f523698b1127e60fa536)), closes [#148](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/148)

## [7.0.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v7.0.0...v7.0.1) (2024-09-02)


### Bug Fixes

* use separate attribute for XSRF token in LogoutFilter ([#143](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/143)) ([37e3364](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/37e33644b2aa0a5f708fcc0c5ebc736aa2faf57a))

## [7.0.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v6.7.0...v7.0.0) (2024-09-02)


### ⚠ BREAKING CHANGES

* additional block for internal extension information on the abo… ([#139](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/139))

### Features

* additional block for internal extension information on the abo… ([#139](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/139)) ([d27c5cd](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/d27c5cd4840395463e29f5aeb06b9121b4896b50)), closes [#138](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/138)


### Bug Fixes

* jacoco "can't add different class with same name" issue introduced in v6.7.0 ([#141](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/141)) ([c46bb87](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/c46bb87aee8b4743abe58e5605f6b8c60d76eaf5))

## [6.7.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/compare/v6.6.3...v6.7.0) (2024-08-26)


### Features

* sonarcloud with coverage support added ([#125](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/125)) ([7f50d15](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/7f50d15e1f503c3fe62e272dfef4239f8c5b5d1f))


### Bug Fixes

* sonar configuration ([#135](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/135)) ([d783261](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/d783261add5de1cce254a64cd9739e62d106883e))
* sonarqube quality issues ([#134](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/issues/134)) ([f7873d3](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic/commit/f7873d30abedb71ba213540057aa44245f8fd8a2))

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
