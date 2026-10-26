package org.appdevforall.scan2go.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class K2GoPayloadTest {

    @Test
    fun detectsCloneSyncPayload() {
        val raw = """{"ip":"192.168.49.1","port":1873,"user":"u","pass":"p","has_rootfs":true,"a":64,"app":"iiab_sync"}"""
        assertEquals(K2GoFeature.CLONE, K2GoPayload.detect(raw))
    }

    @Test
    fun detectsContentExchangeSharePayload() {
        val raw = """{"app":"iiab_share","addr":"192.168.49.1:8095","token":"abc-123_DEF","manifestUrl":"/v1/share/aa/manifest"}"""
        assertEquals(K2GoFeature.CONTENT_EXCHANGE, K2GoPayload.detect(raw))
    }

    @Test
    fun ignoresUnknownAppTag() {
        assertNull(K2GoPayload.detect("""{"app":"other_app","x":1}"""))
    }

    @Test
    fun ignoresNonJsonText() {
        assertNull(K2GoPayload.detect("iiab_sync"))
        assertNull(K2GoPayload.detect("https://example.org/iiab_share"))
    }

    @Test
    fun toleratesLeadingWhitespace() {
        assertEquals(K2GoFeature.CLONE, K2GoPayload.detect("  {\"app\": \"iiab_sync\"}"))
    }
}
