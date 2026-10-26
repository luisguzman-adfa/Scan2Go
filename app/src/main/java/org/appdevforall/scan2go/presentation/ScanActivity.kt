package org.appdevforall.scan2go.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import org.appdevforall.scan2go.R
import org.appdevforall.scan2go.databinding.ActivityScanBinding
import org.appdevforall.scan2go.domain.K2GoFeature
import org.appdevforall.scan2go.domain.ScanAction
import org.appdevforall.scan2go.domain.ScanActions
import org.appdevforall.scan2go.domain.ScannedContent
import org.appdevforall.scan2go.util.SnackbarDuration

/**
 * Single-shot scanner. One state owner (the ViewModel); the camera is paused while a
 * result is shown, so nothing decodes behind the result card. The camera is released
 * in onPause and reacquired in onResume, so there is no leak when returning to the app.
 */
class ScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBinding
    private val viewModel: ScanViewModel by viewModels()
    private lateinit var actionRunner: ActionRunner

    // The K2Go announcement dialog shows once per scanned code, not on every re-render
    // (rotation, tab re-select), so it does not reappear while the same result is on screen.
    private var k2goAnnouncedRaw: String? = null

    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            viewModel.onDecoded(result.text)
        }

        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>) = Unit
    }

    private val requestCamera = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startScanningIfReady() else showPermissionDenied()
    }

    // The Wi-Fi add-networks dialog returns ALREADY_EXISTS (and shows no dialog) when
    // the network is already saved; surface that instead of looking like nothing happened.
    private val wifiAddLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val message = WifiConnector.resultMessage(result)
            if (message != 0) snack(getString(message))
        }
    }

    private fun connectWifi(wifi: ScannedContent.Wifi) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && WifiConnector.canSuggest(wifi)) {
            wifiAddLauncher.launch(WifiConnector.buildAddNetworksIntent(wifi))
        } else {
            WifiConnector.guide(this, wifi)
        }
    }

    private fun snack(message: String) {
        Snackbar.make(binding.root, message, SnackbarDuration.millisForText(message)).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        actionRunner = ActionRunner(this, binding.root, ::connectWifi)

        binding.barcodeView.setStatusText("")
        binding.btnScanAgain.setOnClickListener { viewModel.scanAgain() }

        applyWindowInsets()
        viewModel.state.observe(this) { render(it) }
    }

    /**
     * Edge-to-edge is forced on Android 15+, so content draws under the system bars.
     * Keep the toolbar below the status bar and the bottom chrome clear of the
     * navigation bar, whatever the bar sizes or nav mode.
     */
    private fun applyWindowInsets() {
        val cardBase = dpToPx(12)
        val promptBase = dpToPx(48)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.updatePadding(top = bars.top)
            binding.resultCard.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = bars.bottom + cardBase
            }
            binding.promptText.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = bars.bottom + promptBase
            }
            insets
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    override fun onResume() {
        super.onResume()
        if (viewModel.state.value is ScanViewModel.State.Result) return
        if (hasCameraPermission()) startScanningIfReady()
        else requestCamera.launch(Manifest.permission.CAMERA)
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeView.pause()
    }

    private fun render(state: ScanViewModel.State) {
        when (state) {
            is ScanViewModel.State.Scanning -> {
                binding.resultCard.visibility = View.GONE
                binding.promptText.visibility = View.VISIBLE
                startScanningIfReady()
            }

            is ScanViewModel.State.Result -> {
                binding.promptText.visibility = View.GONE
                binding.barcodeView.pause()
                showResult(state.content)
            }
        }
    }

    private fun startScanningIfReady() {
        if (viewModel.state.value !is ScanViewModel.State.Scanning) return
        if (!hasCameraPermission()) return
        binding.barcodeView.resume()
        binding.barcodeView.decodeSingle(callback)
        applyTorch()
    }

    private fun showResult(content: ScannedContent) {
        binding.resultIcon.setImageResource(typeIcon(content))
        binding.resultType.setText(typeLabel(content))
        binding.resultContent.text = contentSummary(content)
        binding.actionButtons.removeAllViews()
        ScanActions.forContent(content).forEachIndexed { index, action ->
            // The first action is primary: a filled button. The rest are tonal.
            val layout = if (index == 0) R.layout.item_action_button_primary else R.layout.item_action_button
            val button = layoutInflater.inflate(layout, binding.actionButtons, false) as MaterialButton
            button.setText(actionLabel(action))
            button.setIconResource(actionIcon(action))
            button.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            button.setOnClickListener { actionRunner.run(action, content) }
            binding.actionButtons.addView(button)
        }
        binding.resultCard.visibility = View.VISIBLE

        // A K2Go code only works inside Knowledge to Go: announce what it is (and offer to
        // open the app when installed) in a dismissible dialog, not a transient snackbar.
        if (content is ScannedContent.K2Go && content.raw != k2goAnnouncedRaw) {
            k2goAnnouncedRaw = content.raw
            K2GoHandoff.announce(this, content.feature)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_scan, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_torch) {
            val on = !viewModel.torchOn
            viewModel.setTorch(on)
            applyTorch()
            item.setTitle(if (on) R.string.torch_off else R.string.torch_on)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun applyTorch() {
        if (viewModel.torchOn) binding.barcodeView.setTorchOn() else binding.barcodeView.setTorchOff()
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun showPermissionDenied() {
        val message = getString(R.string.camera_permission_denied)
        Snackbar.make(binding.root, message, SnackbarDuration.millisForText(message)).show()
    }

    private fun typeLabel(content: ScannedContent): Int = when (content) {
        is ScannedContent.Url -> R.string.type_url
        is ScannedContent.Wifi -> R.string.type_wifi
        is ScannedContent.Phone -> R.string.type_phone
        is ScannedContent.Sms -> R.string.type_sms
        is ScannedContent.Email -> R.string.type_email
        is ScannedContent.Geo -> R.string.type_geo
        is ScannedContent.Event -> R.string.type_event
        is ScannedContent.Contact -> R.string.type_contact
        is ScannedContent.K2Go -> R.string.type_k2go
        is ScannedContent.Text -> R.string.type_text
    }

    private fun actionLabel(action: ScanAction): Int = when (action) {
        ScanAction.OPEN -> R.string.action_open
        ScanAction.COPY -> R.string.action_copy
        ScanAction.SHARE -> R.string.action_share
        ScanAction.WEB_SEARCH -> R.string.action_web_search
        ScanAction.WIFI_CONNECT -> R.string.action_wifi_connect
        ScanAction.WIFI_COPY_PASSWORD -> R.string.action_wifi_copy_password
        ScanAction.DIAL -> R.string.action_dial
        ScanAction.SMS -> R.string.action_sms
        ScanAction.EMAIL -> R.string.action_email
        ScanAction.MAP -> R.string.action_map
        ScanAction.ADD_EVENT -> R.string.action_add_event
        ScanAction.ADD_CONTACT -> R.string.action_add_contact
    }

    private fun typeIcon(content: ScannedContent): Int = when (content) {
        is ScannedContent.Url -> R.drawable.ic_link
        is ScannedContent.Wifi -> R.drawable.ic_wifi
        is ScannedContent.Phone -> R.drawable.ic_call
        is ScannedContent.Sms -> R.drawable.ic_sms
        is ScannedContent.Email -> R.drawable.ic_email
        is ScannedContent.Geo -> R.drawable.ic_place
        is ScannedContent.Event -> R.drawable.ic_event
        is ScannedContent.Contact -> R.drawable.ic_person
        is ScannedContent.K2Go -> R.drawable.ic_k2go
        is ScannedContent.Text -> R.drawable.ic_text
    }

    private fun actionIcon(action: ScanAction): Int = when (action) {
        ScanAction.OPEN -> R.drawable.ic_open
        ScanAction.COPY -> R.drawable.ic_copy
        ScanAction.SHARE -> R.drawable.ic_share
        ScanAction.WEB_SEARCH -> R.drawable.ic_search
        ScanAction.WIFI_CONNECT -> R.drawable.ic_wifi
        ScanAction.WIFI_COPY_PASSWORD -> R.drawable.ic_key
        ScanAction.DIAL -> R.drawable.ic_call
        ScanAction.SMS -> R.drawable.ic_sms
        ScanAction.EMAIL -> R.drawable.ic_email
        ScanAction.MAP -> R.drawable.ic_place
        ScanAction.ADD_EVENT -> R.drawable.ic_event
        ScanAction.ADD_CONTACT -> R.drawable.ic_person_add
    }

    private fun contentSummary(content: ScannedContent): String = when (content) {
        is ScannedContent.Url -> content.uri
        is ScannedContent.Wifi -> buildString {
            append(content.ssid)
            content.security?.let { append("  (").append(it).append(")") }
        }
        is ScannedContent.Phone -> content.number
        is ScannedContent.Sms -> listOfNotNull(content.number, content.body).joinToString("\n")
        is ScannedContent.Email -> content.address
        is ScannedContent.Geo ->
            listOfNotNull("${content.latitude}, ${content.longitude}", content.query).joinToString("\n")
        is ScannedContent.Event ->
            listOfNotNull(content.title, content.location).joinToString("\n").ifEmpty { content.raw }
        is ScannedContent.Contact -> buildString {
            content.name?.let { appendLine(it) }
            content.phones.forEach { appendLine(it) }
            content.emails.forEach { appendLine(it) }
        }.trim().ifEmpty { content.raw }
        is ScannedContent.K2Go -> getString(
            when (content.feature) {
                K2GoFeature.CLONE -> R.string.k2go_clone_summary
                K2GoFeature.CONTENT_EXCHANGE -> R.string.k2go_content_summary
            }
        )
        is ScannedContent.Text -> content.raw
    }
}
