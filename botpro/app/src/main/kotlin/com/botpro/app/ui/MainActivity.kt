package com.botpro.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.botpro.app.R
import com.botpro.app.ui.botlist.BotListFragment

/**
 * Activité principale de BotPro affichant l'écran de liste des conversations.
 * Conforme à Telegram : plein écran avec Action Bar et FAB, sans barre d'onglets basse.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            loadFragment(BotListFragment())
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
