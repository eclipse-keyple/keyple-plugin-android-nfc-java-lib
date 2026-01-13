This document specifies the API dedicated to the integration of the Android NFC plugin in an Android
Keyple application.

The Android NFC plugin **requires** the use of the Keyple observation pattern at the reader level to
handle card insertion and removal events.  
**Unlike other reader implementations, Android NFC does not support continuous polling for card
presence (e.g., `isCardPresent()`).** Instead, it relies entirely on Android’s event-driven model.

To properly detect card presence and handle interactions, it is **mandatory** to cast [Reader]
objects as [ObservableReader] and implement the appropriate interfaces from the Keyple Service SPI
package.

Since NFC events are managed asynchronously by the Android system, not be possible** in this context. Any integration of the Android NFC plugin must therefore be designed
around this event-driven approach.

### Configuration & Instantiation

To create an instance of the plugin, use the [AndroidNfcPluginFactoryProvider] which requires an
[AndroidNfcConfig] object.

The configuration object allows you to customize the plugin behavior, including:
*   [AndroidNfcConfig.activity]: The Android Activity context (mandatory).
*   [AndroidNfcConfig.isPlatformSoundEnabled]: To enable/disable system sounds on tag discovery.
*   [AndroidNfcConfig.skipNdefCheck]: To optimize detection speed by skipping NDEF checks.
*   [AndroidNfcConfig.cardInsertionPollingInterval] & [AndroidNfcConfig.cardRemovalPollingInterval]:
    To fine-tune presence check timings.

```kotlin
val config = AndroidNfcConfig(activity = this)
val pluginFactory = AndroidNfcPluginFactoryProvider.provideFactory(config)
val plugin = pluginFactory.createPlugin()
```

### Storage Card Support

To support storage cards (such as **Mifare Classic**, **Mifare Ultralight**, etc.), the plugin relies
on the `keyple-plugin-storagecard-java-api`. An implementation of `ApduInterpreterFactory` must be
provided via the [AndroidNfcConfig.apduInterpreterFactory] property.

This factory is responsible for creating the interpreter that translates standard APDU commands into
specific Android NFC I/O operations (e.g. using `MifareClassic` or `MifareUltralight` Android tech
classes).

#### Key Management (Mifare Classic)

When using Mifare Classic cards, authentication requires keys. Since Keyple separates the "load key"
operation from the "authenticate" operation, the plugin emulates a reader's key memory:
*   **Volatile**: For session keys (cleared when the card is removed).
*   **Persistent**: For keys that must survive the card session (kept as long as the plugin is active).

For enhanced security, the application can implement the [KeyProvider] interface and register it
in [AndroidNfcConfig.keyProvider]. This mechanism allows the application to keep master keys in a
secure vault (e.g. Android Keystore, HSM, or encrypted file) and provide them to the plugin only
when strictly requested during an authentication operation.