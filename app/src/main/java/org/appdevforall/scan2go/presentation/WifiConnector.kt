package org.appdevforall.scan2go.presentation

import android.app.Activity
import android.content.Intent
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.annotation.RequiresApi
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.appdevforall.scan2go.R
import org.appdevforall.scan2go.domain.ScannedContent
import org.appdevforall.scan2go.util.Clipboard

/**
 * Wi-Fi join, tiered by OS version. One owner, no persisted state:
 *  - API 30+   : one-tap system dialog via Settings.ACTION_WIFI_ADD_NETWORKS. The
 *                caller launches [buildAddNetworksIntent] FOR A RESULT and reads
 *                [resultMessage], so an already-saved network (the system shows no
 *                dialog and returns ALREADY_EXISTS) still gives the user feedback.
 *  - API 24..29 (and WEP): [guide] shows credentials, copies the password, opens settings.
 *
 * This deliberately avoids the deprecated WifiConfiguration path (three incompatible
 * APIs across Android 7..14), which would mean three sources of truth for "am I connected".
 */
object WifiConnector {

    /** WEP is not supported by the add-networks API; it uses the guide path instead. */
    fun canSuggest(wifi: ScannedContent.Wifi): Boolean = when (wifi.security?.uppercase()) {
        null, "", "WPA", "WPA2", "WPA3", "SAE", "NOPASS" -> true
        else -> false
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun buildAddNetworksIntent(wifi: ScannedContent.Wifi): Intent {
        val builder = WifiNetworkSuggestion.Builder().setSsid(wifi.ssid)
        if (wifi.hidden) builder.setIsHiddenSsid(true)
        val pass = wifi.password
        if (!pass.isNullOrEmpty()) {
            when (wifi.security?.uppercase()) {
                "SAE", "WPA3" -> builder.setWpa3Passphrase(pass)
                else -> builder.setWpa2Passphrase(pass)
            }
        }
        return Intent(Settings.ACTION_WIFI_ADD_NETWORKS)
            .putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, arrayListOf(builder.build()))
    }

    /**
     * A string res to show after the add-networks dialog, or 0 to stay silent.
     * The system reports ALREADY_EXISTS (and shows no dialog) when the network is
     * already saved: that is why the one-tap button looked like it did nothing.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun resultMessage(result: ActivityResult): Int {
        if (result.resultCode != Activity.RESULT_OK) return 0
        val codes = result.data?.getIntegerArrayListExtra(Settings.EXTRA_WIFI_NETWORK_RESULT_LIST)
            ?: return 0
        return if (codes.contains(Settings.ADD_WIFI_RESULT_ALREADY_EXISTS)) R.string.wifi_already_saved else 0
    }

    fun guide(activity: Activity, wifi: ScannedContent.Wifi) {
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
