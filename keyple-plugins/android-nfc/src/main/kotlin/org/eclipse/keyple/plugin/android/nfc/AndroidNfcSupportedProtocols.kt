package org.eclipse.keyple.plugin.android.nfc

public enum class AndroidNfcSupportedProtocols(val androidNfcTechIdentifier: String) {
    ISO_14443_4("android.nfc.tech.IsoDep"),
    MIFARE_ULTRA_LIGHT("android.nfc.tech.MifareUltralight"),
    MIFARE_CLASSIC("android.nfc.tech.MifareClassic")
}