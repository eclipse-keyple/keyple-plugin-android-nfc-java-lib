# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Changed
- Migrated the CI pipeline from Jenkins to GitHub Actions.

## [3.0.0] - 2025-02-10
### Breaking changes
- Replaced `AndroidNfcPluginFactoryProvider.getFactory()` by
  `AndroidNfcPluginFactoryProvider.provideFactory(config: AndroidNfcConfig)`.
- Moved `presenceCheckDelay`, `noPlateformSound`, and `skipNdefCheck` properties from
  `AndroidNfcReader` to `AndroidNfcConfig`.
- Removed methods `printTagId()`, `processIntent(Intent)` from `AndroidNfcReader`.
- Removed constant `READER_NAME` from `AndroidNfcReader` to `AndroidNfcConstants`.
- Removed constant `PLUGIN_NAME` from `AndroidNfcPlugin` to `AndroidNfcConstants`.
- Removed support for the `CardReader.isCardPresent()` method. This method is incompatible with the
  Android NFC model.
### Added
- Added `AndroidNfcConfig` to encapsulate plugin configuration.
- Added `AndroidNfcConstants` for plugin-related constants.
### Fixed
- Latency issue related to card removal.
### Changed
- Refactored `AndroidNfcPluginFactoryProvider` to provide factories with a configuration.
- Refactored `AndroidNfcSupportedProtocols` to align with the new model.
- Removed useless dependencies.
- Updated Gradle wrapper.
- Improved logging.
### Documentation
- Improved documentation to reflect the new event-driven model.

## [2.2.0] - 2024-04-12
### Changed
- Java source and target levels `1.6` -> `1.8`
- Kotlin version `1.4.20` -> `1.7.20`
### Upgraded
- Keyple Plugin API `2.2.0` -> `2.3.1`
- Keyple Util Lib `2.1.0` -> `2.4.0`
- Gradle `6.8.3` -> `7.6.4`
### Fixed
- Management of physical channel: the actual closing is now done by the card removal procedure. The associated timeout
  has been removed.
### Removed
- Dependency to logger implementation.

## [2.1.0] - 2023-11-13
:warning: **CAUTION**: this version requires to use at least version `2.3.2` of the
[Keyple Service Library](https://keyple.org/components-java/core/keyple-service-java-lib/)!
### Added
- Added project status badges on `README.md` file.
### Fixed
- CI: code coverage report when releasing.
- Handled `SecurityException` raised when closing the physical channel, to resolve a malfunction of Keyple Service's 
  internal state machine on recent versions of Android (API 12+).
### Upgraded
- Keyple Plugin API `2.0.0` -> `2.2.0`

## [2.0.1] - 2022-06-09
### Added
- "CHANGELOG.md" file (issue [eclipse-keyple/keyple#6]).
- CI: Forbid the publication of a version already released (issue [#5]).
### Fixed
- Removal of the unused Jacoco plugin for compiling Android applications that had an unwanted side effect when the application was launched (stacktrace with warnings).
### Upgraded
- "Keyple Util Library" to version `2.1.0` by removing the use of deprecated methods.

## [2.0.0] - 2021-10-06
This is the initial release.
It follows the extraction of Keyple 1.0 components contained in the `eclipse-keyple/keyple-java` repository to dedicated repositories.
It also brings many major API changes.

[unreleased]: https://github.com/eclipse-keyple/keyple-plugin-android-nfc-java-lib/compare/3.0.0...HEAD
[3.0.0]: https://github.com/eclipse-keyple/keyple-plugin-android-nfc-java-lib/compare/2.2.0...3.0.0
[2.2.0]: https://github.com/eclipse-keyple/keyple-plugin-android-nfc-java-lib/compare/2.1.0...2.2.0
[2.1.0]: https://github.com/eclipse-keyple/keyple-plugin-android-nfc-java-lib/compare/2.0.1...2.1.0
[2.0.1]: https://github.com/eclipse-keyple/keyple-plugin-android-nfc-java-lib/compare/2.0.0...2.0.1
[2.0.0]: https://github.com/eclipse-keyple/keyple-plugin-android-nfc-java-lib/releases/tag/2.0.0

[#5]: https://github.com/eclipse-keyple/keyple-plugin-android-nfc-java-lib/issues/5

[eclipse-keyple/keyple#6]: https://github.com/eclipse-keyple/keyple/issues/6