# Package org.eclipse.keyple.plugin.android.nfc

This document is the specification of the API dedicated to the integration of the Android NFC plugin in a Android Keyple Application.

## Observability
The Android NFC Plugin implements the Keyple observation pattern at reader (card insertion and removal) level, in this case it is imperative to cast the
    [Reader] objects as [ObservableReader] and to implement the interfaces defined for this purpose in the Keyple Service SPI package.
However, the use of these observation features is optional; it is possible to operate in a static mode on the reader side.
