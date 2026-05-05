package com.kel4.notaly.auth

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.kel4.notaly.R

class SplashActivity : AppCompatActivity() {

    // ─── Views ───────────────────────────────────────────────
    private lateinit var ivLogo: ImageView
    private lateinit var tvAppName: TextView
    private lateinit var tvCursor: TextView
    private lateinit var tvTagline: TextView

    // ─── Sound ───────────────────────────────────────────────
    private var mediaPlayer: MediaPlayer? = null

    // ─── Handler ─────────────────────────────────────────────
    private val handler = Handler(Looper.getMainLooper())

    // ─── Konstanta ───────────────────────────────────────────
    companion object {
        private const val APP_NAME             = "notaly"
        private const val TYPEWRITER_DELAY_MS  = 100L    // jeda antar karakter
        private const val DELAY_LOGO           = 100L
        private const val DELAY_CURSOR_START   = 2000L
        private const val DELAY_TYPEWRITER     = 3000L
        private const val DELAY_TAGLINE        = 4000L   // tepat setelah typewriter
        private const val DELAY_SOUND          = 0L
        private const val DELAY_NAVIGATE       = 5000L
        private const val PREFS_NAME           = "UserPreferences"
        private const val KEY_IS_LOGGED_IN     = "IS_REG"
    }

    // ─── onCreate ────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        bindViews()
        startSplashSequence()

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Biarkan kosong.
                // Karena kosong, saat user menekan back, aplikasi tidak akan melakukan apa-apa.
            }
        })
        // -------------------------------------------------------

        bindViews()
        startSplashSequence()
    }

    private fun bindViews() {
        ivLogo    = findViewById(R.id.ivLogo)
        tvAppName = findViewById(R.id.tvAppName)
        tvCursor  = findViewById(R.id.tvCursor)
        tvTagline = findViewById(R.id.tvTagline)
    }

    // ─── Orkestrasi semua animasi ─────────────────────────────
    private fun startSplashSequence() {
        handler.postDelayed({ animateLogo() },       DELAY_LOGO)
        handler.postDelayed({ showCursor() },        DELAY_CURSOR_START)
        handler.postDelayed({ startTypewriter() },   DELAY_TYPEWRITER)
        handler.postDelayed({ showTagline() },       DELAY_TAGLINE)
        handler.postDelayed({ playSplashSound() },   DELAY_SOUND)
        handler.postDelayed({ navigateToNext() },    DELAY_NAVIGATE)
    }

    // ─── 1. Logo scale up ────────────────────────────────────
    private fun animateLogo() {
        ivLogo.scaleX = 1f
        ivLogo.scaleY = 1f
        ivLogo.alpha = 0f
        ivLogo.visibility = View.VISIBLE

        ivLogo.animate()
            .alpha(1f)
            .setDuration(1000)
            .withEndAction {
                // ivLogo.alpha = 1f
            }
            .start()
    }

    // ─── 2. Tampilkan cursor berkedip ─────────────────────────
    private fun showCursor() {
        val blinkAnim = AnimationUtils.loadAnimation(this, R.anim.anim_cursor_blink)
        tvCursor.visibility = View.VISIBLE
        tvCursor.alpha = 1f
        tvCursor.startAnimation(blinkAnim)
    }

    // ─── 3. Typewriter "notaly" ───────────────────────────────
    private fun startTypewriter() {
        tvAppName.text = ""
        var charIndex = 0

        val typeRunnable = object : Runnable {
            override fun run() {
                if (charIndex <= APP_NAME.length) {
                    tvAppName.text = APP_NAME.substring(0, charIndex)
                    charIndex++
                    if (charIndex <= APP_NAME.length) {
                        handler.postDelayed(this, TYPEWRITER_DELAY_MS)
                    } else {
                        hideCursor()
                    }
                }
            }
        }

        handler.post(typeRunnable)
    }

    // ─── Sembunyikan cursor setelah typewriter selesai ────────
    private fun hideCursor() {
        tvCursor.clearAnimation()
        tvCursor.animate()
            .alpha(0f)
            .setDuration(150)
            .start()
    }

    // ─── 4. Tagline fade in ───────────────────────────────────
    private fun showTagline() {
        val anim = AnimationUtils.loadAnimation(this, R.anim.anim_tagline_fadein)
        tvTagline.alpha = 0f
        tvTagline.visibility = View.VISIBLE
        tvTagline.startAnimation(anim)
        anim.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(a: android.view.animation.Animation?) {}
            override fun onAnimationRepeat(a: android.view.animation.Animation?) {}
            override fun onAnimationEnd(a: android.view.animation.Animation?) {
                tvTagline.alpha = 1f
            }
        })
    }

    // ─── 5. Sound effect ─────────────────────────────────────
    private fun playSplashSound() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.splash_sound)
            mediaPlayer?.apply {
                setVolume(0.55f, 0.55f)
                setOnCompletionListener { release() }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ─── 6. Navigasi ─────────────────────────────────────────
    private fun navigateToNext() {
        val destination = if (isUserLoggedIn()) {
            LoginActivity::class.java
        } else {
            MainActivity::class.java
        }

        startActivity(
            Intent(this, destination).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )

        overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
        finish()
    }

    // ─── Cek SharedPreferences ────────────────────────────────
    private fun isUserLoggedIn(): Boolean {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // ─── Cleanup ─────────────────────────────────────────────
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }
}
