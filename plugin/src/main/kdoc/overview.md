This document specifies the API dedicated to the integration of the Android NFC plugin in an Android
Keyple application.

The Android NFC plugin **requires** the use of the Keyple observation pattern at the reader level to
handle card insertion and removal events.  
**Unlike other reader implementations, Android NFC does not support continuous polling for card
presence (e.g., `isCardPresent()`).** Instead, it relies entirely on Android’s event-driven model.

To properly detect card presence and handle interactions, it is **mandatory** to cast [Reader]
objects as [ObservableReader] and implement the appropriate interfaces from the Keyple Service SPI
package.

Since NFC events are managed asynchronously by the Android system, **working without observation is
not possible** in this context. Any integration of the Android NFC plugin must therefore be designed
around this event-driven approach.