package org.appdevforall.scan2go.domain

/** An action the user can take on a scanned item. The UI renders one button per action. */
enum class ScanAction {
    OPEN,
    COPY,
    SHARE,
    WEB_SEARCH,
    WIFI_CONNECT,
    WIFI_COPY_PASSWORD,
    DIAL,
    SMS,
    EMAIL,
    MAP,
    ADD_EVENT,
    ADD_CONTACT,
}

/**
 * The action matrix: which actions each content type offers. This is a business
 * rule, so it lives in the domain (not the UI) and is unit-tested. The UI only
 * turns each [ScanAction] into an Android Intent.
 */
object ScanActions {
    fun forContent(content: ScannedContent): List<ScanAction> = when (content) {
        is ScannedContent.Url -> listOf(ScanAction.OPEN, ScanAction.COPY, ScanAction.SHARE)

        is ScannedContent.Wifi -> buildList {
            add(ScanAction.WIFI_CONNECT)
            if (!content.password.isNullOrEmpty()) add(ScanAction.WIFI_COPY_PASSWORD)
            add(ScanAction.COPY)
        }

        is ScannedContent.Phone -> listOf(ScanAction.DIAL, ScanAction.ADD_CONTACT, ScanAction.COPY)
        is ScannedContent.Sms -> listOf(ScanAction.SMS, ScanAction.COPY)
        is ScannedContent.Email -> listOf(ScanAction.EMAIL, ScanAction.COPY)
        is ScannedContent.Geo -> listOf(ScanAction.MAP, ScanAction.COPY)
        is ScannedContent.Event -> listOf(ScanAction.ADD_EVENT, ScanAction.COPY)
        is ScannedContent.Contact -> listOf(ScanAction.ADD_CONTACT, ScanAction.COPY, ScanAction.SHARE)
        // K2Go payloads are not actionable by a generic reader (no URL to open, no number
        // to dial). Scan to Go copies/shares the raw code; the open hand-off is a dialog.
        is ScannedContent.K2Go -> listOf(ScanAction.COPY, ScanAction.SHARE)

        is ScannedContent.Text -> listOf(ScanAction.COPY, ScanAction.SHARE, ScanAction.WEB_SEARCH)
    }
}
