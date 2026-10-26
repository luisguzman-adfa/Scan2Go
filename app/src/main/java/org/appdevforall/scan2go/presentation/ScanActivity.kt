package org.appdevforall.scan2go.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import org.appdevforall.scan2go.R
import org.appdevforall.scan2go.databinding.ActivityScanBinding
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        actionRunner = ActionRunner(this, binding.root)

        binding.barcodeView.setStatusText("")
        binding.btnScanAgain.setOnClickListener { viewModel.scanAgain() }

        viewModel.state.observe(this) { render(it) }
    }

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
        binding.resultType.setText(typeLabel(content))
        binding.resultContent.text = contentSummary(content)
        binding.actionButtons.removeAllViews()
        for (action in ScanActions.forContent(content)) {
            val button = layoutInflater.inflate(
                R.layout.item_action_button, binding.actionButtons, false
            ) as MaterialButton
            button.setText(actionLabel(action))
            button.setOnClickListener { actionRunner.run(action, content) }
            binding.actionButtons.addView(button)
        }
        binding.resultCard.visibility = View.VISIBLE
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
        is ScannedContent.Text -> content.raw
    }
}
