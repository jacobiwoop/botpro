package com.botpro.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.botpro.app.R

/**
 * Écran des paramètres de BotPro.
 * Inspiré de SettingsActivity de Telegram, adapté pour configurer
 * les thèmes, les bots par défaut et les options de réseau.
 */
class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.toolbar_title)?.text = getString(R.string.settings_title)
        view.findViewById<TextView>(R.id.text_version_value)?.text = "1.0.0 (BotPro Pure Kotlin)"
    }
}
