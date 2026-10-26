package org.appdevforall.scan2go.presentation

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.appdevforall.scan2go.R
import org.appdevforall.scan2go.domain.ScanAction
import org.appdevforall.scan2go.domain.ScannedContent
import org.appdevforall.scan2go.util.Clipboard
import org.appdevforall.scan2go.util.SnackbarDuration

/** Turns a domain [ScanAction] into an Android effect (Intent, clipboard, dialog). */
class ActionRunner(
    private val activity: AppCompatActivity,
    private val rootView: View,
    private val onWifiConnect: (ScannedContent.Wifi) -> Unit,
) {

    fun run(action: ScanAction, content: ScannedContent) {
        when (action) {
            ScanAction.OPEN -> confirmThenOpen((content as ScannedContent.Url).uri)

            ScanAction.COPY -> copy(content.raw)

            ScanAction.SHARE -> startChooser(
                Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, content.raw)
            )

            ScanAction.WEB_SEARCH -> start(
                Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, content.raw)
            )

            ScanAction.WIFI_CONNECT -> onWifiConnect(content as ScannedContent.Wifi)

            ScanAction.WIFI_COPY_PASSWORD ->
                copy((content as ScannedContent.Wifi).password.orEmpty(), label = "wifi-password")

            ScanAction.DIAL -> start(
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + (content as ScannedContent.Phone).number))
            )

            ScanAction.SMS -> {
                val sms = content as ScannedContent.Sms
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + sms.number))
                if (!sms.body.isNullOrEmpty()) intent.putExtra("sms_body", sms.body)
                start(intent)
            }

            ScanAction.EMAIL -> {
                val email = content as ScannedContent.Email
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email.address))
                if (!email.subject.isNullOrEmpty()) intent.putExtra(Intent.EXTRA_SUBJECT, email.subject)
                if (!email.body.isNullOrEmpty()) intent.putExtra(Intent.EXTRA_TEXT, email.body)
                start(intent)
            }

            ScanAction.MAP -> {
                val geo = content as ScannedContent.Geo
                val query = geo.query?.let { "?q=" + Uri.encode(it) }.orEmpty()
                start(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${geo.latitude},${geo.longitude}$query")))
            }

            ScanAction.ADD_EVENT -> addEvent(content as ScannedContent.Event)

            ScanAction.ADD_CONTACT -> addContact(content)
        }
    }

    private fun confirmThenOpen(url: String) {
        // Never auto-open a scanned link: show the destination and let the user confirm.
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.open_link_title)
            .setMessage(url)
            .setPositiveButton(R.string.action_open) { _, _ ->
                start(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun addEvent(event: ScannedContent.Event) {
        val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
        event.title?.let { intent.putExtra(CalendarContract.Events.TITLE, it) }
        event.location?.let { intent.putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
        event.description?.let { intent.putExtra(CalendarContract.Events.DESCRIPTION, it) }
        if (event.startMillis > 0) intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startMillis)
        if (event.endMillis > 0) intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endMillis)
        start(intent)
    }

    private fun addContact(content: ScannedContent) {
        val intent = Intent(Intent.ACTION_INSERT).setType(ContactsContract.Contacts.CONTENT_TYPE)
        when (content) {
            is ScannedContent.Phone ->
                intent.putExtra(ContactsContract.Intents.Insert.PHONE, content.number)
            is ScannedContent.Contact -> {
                content.name?.let { intent.putExtra(ContactsContract.Intents.Insert.NAME, it) }
                content.phones.firstOrNull()?.let { intent.putExtra(ContactsContract.Intents.Insert.PHONE, it) }
                content.emails.firstOrNull()?.let { intent.putExtra(ContactsContract.Intents.Insert.EMAIL, it) }
            }
            else -> Unit
        }
        start(intent)
    }

    private fun copy(text: String, label: String = "scan2go") {
        Clipboard.copy(activity, label, text)
        toast(activity.getString(R.string.copied))
    }

    private fun start(intent: Intent) {
        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(activity.getString(R.string.no_app_to_handle))
        }
    }

    private fun startChooser(intent: Intent) = start(Intent.createChooser(intent, null))

    private fun toast(message: String) {
        Snackbar.make(rootView, message, SnackbarDuration.millisForText(message)).show()
    }
}
