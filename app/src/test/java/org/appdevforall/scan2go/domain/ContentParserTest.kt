package org.appdevforall.scan2go.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentParserTest {

    @Test
    fun parsesUrl() {
        val c = ContentParser.parse("https://appdevforall.org")
        assertTrue(c is ScannedContent.Url)
        assertEquals("https://appdevforall.org", (c as ScannedContent.Url).uri)
    }

    @Test
    fun parsesWifi() {
        val c = ContentParser.parse("WIFI:T:WPA;S:MyNet;P:secret123;H:false;;")
        assertTrue(c is ScannedContent.Wifi)
        c as ScannedContent.Wifi
        assertEquals("MyNet", c.ssid)
        assertEquals("secret123", c.password)
        assertEquals("WPA", c.security)
    }

    @Test
    fun parsesOpenWifiWithoutPassword() {
        val c = ContentParser.parse("WIFI:T:nopass;S:Guest;;") as ScannedContent.Wifi
        assertEquals("Guest", c.ssid)
        assertEquals(null, c.password)
    }

    @Test
    fun parsesTel() {
        val c = ContentParser.parse("tel:+15551234567")
        assertTrue(c is ScannedContent.Phone)
        assertEquals("+15551234567", (c as ScannedContent.Phone).number)
    }

    @Test
    fun parsesGeo() {
        val c = ContentParser.parse("geo:37.786,-122.404?q=office")
        assertTrue(c is ScannedContent.Geo)
    }

    @Test
    fun fallsBackToText() {
        val c = ContentParser.parse("just some words")
        assertTrue(c is ScannedContent.Text)
        assertEquals("just some words", c.raw)
    }
}
