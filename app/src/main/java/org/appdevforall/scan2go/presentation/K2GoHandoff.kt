package org.appdevforall.scan2go.presentation

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.appdevforall.scan2go.R
import org.appdevforall.scan2go.domain.K2GoFeature

/**
 * Knowledge to Go (K2Go) hand-off. A K2Go Clone / Content Exchange QR is only useful inside
 * K2Go, so Scan to Go announces what the code is and, when K2Go is installed, offers to open
 * it. We do NOT deep-link into the exact screen: K2Go has no external entry point for these
 * payloads yet (a separate, coordinated K2Go change), so "Open" launches the app and the
 * message tells the user where to go. The install check uses package visibility granted by the
 * single <package> entry under <queries> in the manifest (least-privilege: not a permission,
 * not QUERY_ALL_PACKAGES).
 */
object K2GoHandoff {

    /** The one Knowledge to Go package: same applicationId for Clone and Content Exchange. */
    const val PACKAGE = "org.appdevforall.k2go"

    fun isInstalled(context: Context): Boolean =
        context.packageManager.getLaunchIntentForPackage(PACKAGE) != null

    /**
     * Announce the scanned K2Go code in a dialog the user dismisses on purpose, not a snackbar
     * that can vanish before it is read. Offers "Open" only when K2Go is present; otherwise it
     * tells the user how to get the app.
     */
    fun announce(activity: AppCompatActivity, feature: K2GoFeature) {
        val installed = isInstalled(activity)
        val message = when (feature) {
            K2GoFeature.CLONE ->
                if (installed) R.string.k2go_clone_installed else R.string.k2go_clone_missing
            K2GoFeature.CONTENT_EXCHANGE ->
                if (installed) R.string.k2go_content_installed else R.string.k2go_content_missing
        }
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.type_k2go)
            .setMessage(message)
        if (installed) {
            dialog.setPositiveButton(R.string.action_open) { _, _ -> launch(activity) }
                .setNegativeButton(R.string.cancel, null)
        } else {
            dialog.setPositiveButton(R.string.k2go_got_it, null)
        }
        dialog.show()
    }

    private fun launch(context: Context) {
        context.packageManager.getLaunchIntentForPackage(PACKAGE)?.let { context.startActivity(it) }
    }
}
