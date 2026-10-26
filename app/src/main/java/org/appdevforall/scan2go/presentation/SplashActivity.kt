package org.appdevforall.scan2go.presentation

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.appdevforall.scan2go.R

/**
 * Branded intro: the logo fades in, the "Scan to Go" wordmark rises and fades in,
 * then the whole screen fades to the base color and routes to the scanner.
 * Mirrors the K2Go splash motion.
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Logo: simple fade-in.
        val logo = findViewById<ImageView>(R.id.logo)
        logo.alpha = 0f
        logo.animate().alpha(1f).setStartDelay(220L).setDuration(700L).start()

        // Wordmark: rises and fades in after the logo.
        val word = findViewById<TextView>(R.id.word)
        val dy = 16f * resources.displayMetrics.density
        word.alpha = 0f
        word.translationY = dy
        word.animate().alpha(1f).translationY(0f).setStartDelay(650L).setDuration(600L).start()

        // Exit: fade the whole screen to the base color, then route to the scanner.
        val fade = findViewById<View>(R.id.fade)
        fade.alpha = 0f
        Handler(Looper.getMainLooper()).postDelayed({
            fade.animate().alpha(1f).setDuration(EXIT_FADE_MS).withEndAction {
                startActivity(Intent(this, ScanActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }.start()
        }, EXIT_AT_MS)
    }

    private companion object {
        const val EXIT_AT_MS = 2200L
        const val EXIT_FADE_MS = 400L
    }
}
