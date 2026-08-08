package com.izzy.kart

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import org.libsdl.app.SDLActivity

/**
 * The game itself — no on-screen touch controls; input comes from physical
 * gamepads and keyboards only.
 *
 * [LauncherActivity] guarantees mk64.o2r exists before this activity starts.
 */
class MainActivity : SDLActivity() {

    private var menuOpen = false
    private var modsButton: Button? = null

    private val handler = Handler(Looper.getMainLooper())
    private val menuWatcher = object : Runnable {
        override fun run() {
            syncMenuState()
            handler.postDelayed(this, MENU_POLL_MS)
        }
    }

    override fun getLibraries(): Array<String> = arrayOf("SDL2", "Spaghettify")

    private external fun isMenuOpen(): Boolean

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupModsButton()
    }

    override fun onResume() {
        super.onResume()
        syncMenuState()
        handler.removeCallbacks(menuWatcher)
        handler.postDelayed(menuWatcher, MENU_POLL_MS)
    }

    override fun onPause() {
        handler.removeCallbacks(menuWatcher)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(menuWatcher)
        super.onDestroy()
    }

    /**
     * Shows the Mods shortcut while the in-game menu is up. Managing mods is a
     * settings action, so it stays hidden during gameplay.
     */
    private fun syncMenuState() {
        val open = runCatching { isMenuOpen() }.getOrDefault(false)
        if (open == menuOpen) return

        menuOpen = open
        modsButton?.visibility = if (open) View.VISIBLE else View.GONE
    }

    private fun setupModsButton() {
        val button = Button(this).apply {
            text = getString(R.string.mods_button)
            visibility = View.GONE
            setOnClickListener { startActivity(Intent(this@MainActivity, ModsActivity::class.java)) }
        }
        modsButton = button

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            val margin = (16 * resources.displayMetrics.density).toInt()
            setMargins(margin, margin, margin, margin)
        }
        findViewById<FrameLayout>(android.R.id.content).addView(button, params)
    }

    /** Map the gamepad Select button to Escape so it toggles the ImGui menu. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BUTTON_SELECT) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> onNativeKeyDown(KeyEvent.KEYCODE_ESCAPE)
                KeyEvent.ACTION_UP -> onNativeKeyUp(KeyEvent.KEYCODE_ESCAPE)
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    // JNI stubs kept so a native lookup does not crash if libultraship calls them.
    @Suppress("unused")
    fun EnableTouchArea() {}

    @Suppress("unused")
    fun DisableTouchArea() {}

    private companion object {
        const val MENU_POLL_MS = 150L
    }
}
