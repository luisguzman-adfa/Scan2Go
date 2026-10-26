package org.appdevforall.scan2go.domain

/**
 * Recognizes the two Knowledge to Go (K2Go) specific QR payloads so Scan to Go can tell
 * the user what they are. Both are compact JSON that carries an "app" tag:
 *  - "iiab_sync"  : the Clone handshake (send a full system copy between devices).
 *  - "iiab_share" : the Content Exchange invite (share selected content).
 *
 * K2Go itself keys off the same "app" tag (its SyncHandshakeHelper and ShareTransport),
 * so we match on it and nothing else. Scan to Go never reads the ip, token or manifest:
 * it only names the code and can hand off to K2Go, so the tag is all it needs. Pure Kotlin
 * (no org.json, no Android), so it stays unit-testable off-device.
 */
object K2GoPayload {

    const val APP_TAG_CLONE = "iiab_sync"
    const val APP_TAG_CONTENT = "iiab_share"

    // The payloads are JSONObject.toString() output, so the tag reads as "app":"<value>".
    // Match the field anywhere in the object: key order is not guaranteed.
    private val APP_FIELD = Regex("\"app\"\\s*:\\s*\"($APP_TAG_CLONE|$APP_TAG_CONTENT)\"")

    /** The K2Go feature this payload belongs to, or null when it is not a K2Go code. */
    fun detect(raw: String): K2GoFeature? {
        val text = raw.trim()
        if (!text.startsWith("{")) return null
        return when (APP_FIELD.find(text)?.groupValues?.get(1)) {
            APP_TAG_CLONE -> K2GoFeature.CLONE
            APP_TAG_CONTENT -> K2GoFeature.CONTENT_EXCHANGE
            else -> null
        }
    }
}
