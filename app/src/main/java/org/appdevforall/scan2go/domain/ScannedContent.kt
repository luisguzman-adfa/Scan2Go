package org.appdevforall.scan2go.domain

/**
 * A QR payload after it is classified into a standard content type.
 * Pure Kotlin (JVM only): no Android import, so it is unit-testable off-device.
 * [raw] is the exact decoded string, used for copy and share.
 */
sealed interface ScannedContent {
    val raw: String

    data class Url(val uri: String, override val raw: String) : ScannedContent

    /** [security] is the ZXing network-encryption token: "WPA", "WEP", "SAE", or "nopass". */
    data class Wifi(
        val ssid: String,
        val password: String?,
        val security: String?,
        val hidden: Boolean,
        override val raw: String,
    ) : ScannedContent

    data class Phone(val number: String, override val raw: String) : ScannedContent

    data class Sms(val number: String, val body: String?, override val raw: String) : ScannedContent

    data class Email(
        val address: String,
        val subject: String?,
        val body: String?,
        override val raw: String,
    ) : ScannedContent

    data class Geo(
        val latitude: Double,
        val longitude: Double,
        val query: String?,
        override val raw: String,
    ) : ScannedContent

    /** Timestamps are epoch millis, or -1 when the payload omits them. */
    data class Event(
        val title: String?,
        val startMillis: Long,
        val endMillis: Long,
        val location: String?,
        val description: String?,
        override val raw: String,
    ) : ScannedContent

    data class Contact(
        val name: String?,
        val phones: List<String>,
        val emails: List<String>,
        override val raw: String,
    ) : ScannedContent

    /** A Knowledge to Go specific payload: Clone (full system copy) or Content Exchange
     *  (selected content). Scan to Go only names it and can hand off to K2Go; it never
     *  joins the transfer itself. */
    data class K2Go(val feature: K2GoFeature, override val raw: String) : ScannedContent

    data class Text(override val raw: String) : ScannedContent
}

/** Which Knowledge to Go feature a scanned K2Go payload belongs to. */
enum class K2GoFeature { CLONE, CONTENT_EXCHANGE }
