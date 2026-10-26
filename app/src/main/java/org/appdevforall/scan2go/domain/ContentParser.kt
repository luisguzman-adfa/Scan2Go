package org.appdevforall.scan2go.domain

import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.client.result.AddressBookParsedResult
import com.google.zxing.client.result.CalendarParsedResult
import com.google.zxing.client.result.EmailAddressParsedResult
import com.google.zxing.client.result.GeoParsedResult
import com.google.zxing.client.result.ResultParser
import com.google.zxing.client.result.SMSParsedResult
import com.google.zxing.client.result.TelParsedResult
import com.google.zxing.client.result.URIParsedResult
import com.google.zxing.client.result.WifiParsedResult

/**
 * Classifies a raw decoded string into [ScannedContent].
 *
 * We do not hand-roll parsers. ZXing's ResultParser already understands every
 * standard payload (URL, WIFI, TEL, SMS, EMAIL, GEO, VEVENT, vCard/MeCard), so
 * this maps its typed result to our model and nothing more. ZXing core is a plain
 * JVM library, so this stays Android-free and unit-testable.
 * See: https://github.com/zxing/zxing (com.google.zxing.client.result)
 */
object ContentParser {

    fun parse(rawText: String): ScannedContent {
        val result = Result(rawText, null, null, BarcodeFormat.QR_CODE)
        return when (val parsed = ResultParser.parseResult(result)) {
            // getURI() is an all-caps acronym getter: Kotlin does not expose it as .uri,
            // so call the Java method directly.
            is URIParsedResult -> ScannedContent.Url(parsed.getURI(), rawText)

            is WifiParsedResult -> ScannedContent.Wifi(
                ssid = parsed.ssid.orEmpty(),
                password = parsed.password?.takeIf { it.isNotEmpty() },
                security = parsed.networkEncryption,
                hidden = parsed.isHidden,
                raw = rawText,
            )

            is TelParsedResult -> ScannedContent.Phone(parsed.number.orEmpty(), rawText)

            is SMSParsedResult -> ScannedContent.Sms(
                number = parsed.numbers?.firstOrNull().orEmpty(),
                body = parsed.body,
                raw = rawText,
            )

            is EmailAddressParsedResult -> ScannedContent.Email(
                address = parsed.tos?.firstOrNull().orEmpty(),
                subject = parsed.subject,
                body = parsed.body,
                raw = rawText,
            )

            is GeoParsedResult -> ScannedContent.Geo(
                latitude = parsed.latitude,
                longitude = parsed.longitude,
                query = parsed.query,
                raw = rawText,
            )

            is CalendarParsedResult -> ScannedContent.Event(
                title = parsed.summary,
                startMillis = parsed.startTimestamp,
                endMillis = parsed.endTimestamp,
                location = parsed.location,
                description = parsed.description,
                raw = rawText,
            )

            is AddressBookParsedResult -> ScannedContent.Contact(
                name = parsed.names?.firstOrNull(),
                phones = parsed.phoneNumbers?.toList().orEmpty(),
                emails = parsed.emails?.toList().orEmpty(),
                raw = rawText,
            )

            else -> ScannedContent.Text(rawText)
        }
    }
}
