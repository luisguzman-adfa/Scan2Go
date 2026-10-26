package org.appdevforall.scan2go.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/** One place that copies text to the clipboard, so no two call sites re-implement it. */
object Clipboard {
    fun copy(context: Context, label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}
