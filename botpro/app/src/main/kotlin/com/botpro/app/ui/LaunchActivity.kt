package com.botpro.app.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.botpro.app.R
import com.botpro.app.core.utils.AndroidUtilities

/**
 * Écran de lancement (Splash) de BotPro.
 * Correspond au `LaunchActivity` de Telegram,
 * simplifié pour BotPro (pas d'auth MTProto).
 */
class LaunchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialiser les utilitaires
        AndroidUtilities.init(this)

        // Transition vers l'écran principal après un court délai
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 800)
    }
}
