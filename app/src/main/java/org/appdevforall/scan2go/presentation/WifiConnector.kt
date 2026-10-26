package org.appdevforall.scan2go.presentation

import android.app.Activity
import android.content.Intent
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.appdevforall.scan2go.R
import org.appdevforall.scan2go.domain.ScannedContent
import org.appdevforall.scan2go.util.Clipboard

/**
 * Connects to a scanned Wi-Fi network, or guides the user when the OS cannot do it
 * for us. One owner, two branches, no persisted state:
 *  - API 30+   : one-tap system dialog via Settings.ACTION_WIFI_ADD_NETWORKS.
 *  - API 24..29: show credentials, copy the password, open Wi-Fi settings.
 *
 * This deliberately avoids the deprecated WifiConfiguration path (three incompatible
 * APIs across Android 7..14). That path would mean three sources of truth for
 * "am I connected", which is the duplicated-state trap we are avoiding by design.
 */
object WifiConnector {

    fun connectOrGuide(activity: Activity, wifi: ScannedContent.Wifi) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && canSuggest(wifi)) {
            addNetwork(activity, wifi)
        } else {
            guide(activity, wifi)
        }
    }

    /** WEP is not supported by the suggestion API; it falls back to the guide path. */
    private fun canSuggest(wifi: ScannedContent.Wifi): Boolean = when (wifi.security?.uppercase()) {
        null, "", "WPA", "WPA2", "WPA3", "SAE", "NOPASS" -> true
        else -> false
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun addNetwork(activity: Activity, wifi: ScannedContent.Wifi) {
        val builder = WifiNetworkSuggestion.Builder().setSsid(wifi.ssid)
        if (wifi.hidden) builder.setIsHiddenSsid(true)
        val pass = wifi.password
        if (!pass.isNullOrEmpty()) {
            when (wifi.security?.uppercase()) {
                "SAE", "WPA3" -> builder.setWpa3Passphrase(pass)
                else -> builder.setWpa2Passphrase(pass)
            }
        }
        val suggestions = arrayListOf(builder.build())
        val intent = Intent(Settings.ACTION_WIFI_ADD_NETWORKS)
            .putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, suggestions)
        activity.startActivity(intent)
    }

    private fun guide(activity: Activity, wifi: ScannedContent.Wifi) {
        val creds = activity.getString(
            R.string.wifi_credentials,
            wifi.ssid,
            wifi.password ?: activity.getString(R.string.wifi_no_password),
        )
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.type_wifi)
            .setMessage(creds)
            .setPositiveButton(R.string.wifi_open_settings) { _, _ -> openWifiSettings(activity) }
            .apply {
                if (!wifi.password.isNullOrEmpty()) {
                    setNeutralButton(R.string.action_wifi_copy_password) { _, _ ->
                        Clipboard.copy(activity, "wifi-password", wifi.password)
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openWifiSettings(activity: Activity) {
        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Settings.Panel.ACTION_WIFI
        } else {
            Settings.ACTION_WIFI_SETTINGS
        }
        activity.startActivity(Intent(action))
    }
}
