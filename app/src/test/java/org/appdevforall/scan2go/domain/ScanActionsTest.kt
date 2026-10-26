package org.appdevforall.scan2go.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanActionsTest {

    @Test
    fun urlOffersOpenCopyShare() {
        val actions = ScanActions.forContent(ScannedContent.Url("https://x.org", "https://x.org"))
        assertEquals(listOf(ScanAction.OPEN, ScanAction.COPY, ScanAction.SHARE), actions)
    }

    @Test
    fun wifiWithPasswordOffersCopyPassword() {
        val wifi = ScannedContent.Wifi("Net", "pw", "WPA", false, "raw")
        assertTrue(ScanAction.WIFI_COPY_PASSWORD in ScanActions.forContent(wifi))
    }

    @Test
    fun openWifiHidesCopyPassword() {
        val open = ScannedContent.Wifi("Guest", null, "nopass", false, "raw")
        val actions = ScanActions.forContent(open)
        assertTrue(ScanAction.WIFI_CONNECT in actions)
        assertFalse(ScanAction.WIFI_COPY_PASSWORD in actions)
    }

    @Test
    fun textOffersWebSearch() {
        assertTrue(ScanAction.WEB_SEARCH in ScanActions.forContent(ScannedContent.Text("hello")))
    }
}
