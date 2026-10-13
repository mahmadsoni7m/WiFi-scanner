package com.wifiscanner.app

import com.wifiscanner.app.domain.IpUtils
import com.wifiscanner.app.domain.SecurityType
import com.wifiscanner.app.domain.SignalQuality
import com.wifiscanner.app.domain.Validators
import com.wifiscanner.app.domain.WifiBand
import com.wifiscanner.app.domain.WifiMath
import com.wifiscanner.app.network.ArpParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiMathTest {

    @Test
    fun classify_usesDocumentedThresholds() {
        assertEquals(SignalQuality.EXCELLENT, WifiMath.classify(-45))
        assertEquals(SignalQuality.GOOD, WifiMath.classify(-55))
        assertEquals(SignalQuality.FAIR, WifiMath.classify(-65))
        assertEquals(SignalQuality.WEAK, WifiMath.classify(-75))
        assertEquals(SignalQuality.POOR, WifiMath.classify(-90))
    }

    @Test
    fun percent_isClamped() {
        assertEquals(100, WifiMath.percent(-30))
        assertEquals(0, WifiMath.percent(-110))
        assertEquals(50, WifiMath.percent(-75))
    }

    @Test
    fun bandAndChannel_fromFrequency() {
        assertEquals(WifiBand.GHZ_2_4, WifiMath.bandOf(2437))
        assertEquals(6, WifiMath.channelOf(2437))
        assertEquals(14, WifiMath.channelOf(2484))
        assertEquals(WifiBand.GHZ_5, WifiMath.bandOf(5180))
        assertEquals(36, WifiMath.channelOf(5180))
        assertEquals(WifiBand.GHZ_6, WifiMath.bandOf(5955))
        assertEquals(1, WifiMath.channelOf(5955))
        assertEquals(WifiBand.UNKNOWN, WifiMath.bandOf(900))
        assertNull(WifiMath.channelOf(900))
    }

    @Test
    fun parseSecurity_recognisesCommonCapabilityStrings() {
        assertEquals(SecurityType.OPEN, WifiMath.parseSecurity("[ESS]"))
        assertEquals(SecurityType.WEP, WifiMath.parseSecurity("[WEP][ESS]"))
        assertEquals(SecurityType.WPA, WifiMath.parseSecurity("[WPA-PSK-TKIP][ESS]"))
        assertEquals(SecurityType.WPA2, WifiMath.parseSecurity("[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS]"))
        assertEquals(SecurityType.WPA3, WifiMath.parseSecurity("[RSN-SAE-CCMP][ESS]"))
        assertEquals(SecurityType.WPA2_WPA3, WifiMath.parseSecurity("[RSN-PSK-CCMP][RSN-SAE-CCMP][ESS]"))
        assertEquals(SecurityType.OWE, WifiMath.parseSecurity("[RSN-OWE-CCMP][ESS]"))
        assertEquals(SecurityType.ENTERPRISE, WifiMath.parseSecurity("[WPA2-EAP/SHA1-CCMP][ESS]"))
    }

    @Test
    fun validators() {
        assertTrue(Validators.isValidSsid("Home"))
        assertFalse(Validators.isValidSsid("  "))
        assertFalse(Validators.isValidSsid("x".repeat(33)))
        assertTrue(Validators.isValidPassphrase("12345678"))
        assertFalse(Validators.isValidPassphrase("1234567"))
        assertFalse(Validators.isValidPassphrase("x".repeat(64)))
        assertTrue(Validators.isValidHttpsUrl("https://speed.cloudflare.com"))
        assertFalse(Validators.isValidHttpsUrl("http://speed.cloudflare.com"))
        assertFalse(Validators.isValidHttpsUrl("https://"))
    }

    @Test
    fun ipUtils_hostRangeAndPrivateCheck() {
        val hosts = IpUtils.hostRange("192.168.1.20", 24)
        assertEquals(254, hosts.size)
        assertEquals("192.168.1.1", hosts.first())
        assertEquals("192.168.1.254", hosts.last())
        // prefixes shorter than /24 are limited to the device's own /24
        assertEquals(254, IpUtils.hostRange("10.1.2.3", 16).size)
        assertEquals(2, IpUtils.hostRange("192.168.1.5", 30).size)
        assertTrue(IpUtils.isPrivate("10.0.0.5"))
        assertTrue(IpUtils.isPrivate("172.20.1.1"))
        assertTrue(IpUtils.isPrivate("192.168.0.1"))
        assertFalse(IpUtils.isPrivate("8.8.8.8"))
        assertFalse(IpUtils.isPrivate("172.32.0.1"))
        assertNull(IpUtils.toLong("999.1.1.1"))
    }

    @Test
    fun arpParser_readsRowsAndSkipsIncompleteEntries() {
        val lines = listOf(
            "IP address       HW type     Flags       HW address            Mask     Device",
            "192.168.1.1      0x1         0x2         aa:bb:cc:dd:ee:ff     *        wlan0",
            "192.168.1.9      0x1         0x0         00:00:00:00:00:00     *        wlan0"
        )
        val table = ArpParser.parse(lines)
        assertEquals(mapOf("192.168.1.1" to "aa:bb:cc:dd:ee:ff"), table)
    }
}
