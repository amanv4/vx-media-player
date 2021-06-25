package tech.techyinc.vlc.gui

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.BaseContextWrappingDelegate
import tech.techyinc.resources.AppContextProvider
import tech.techyinc.tools.KeyHelper
import tech.techyinc.tools.Settings
import tech.techyinc.tools.getContextWithLocale
import tech.techyinc.tools.setGone
import tech.techyinc.vlc.R
import tech.techyinc.vlc.gui.helpers.UiTools
import tech.techyinc.vlc.gui.helpers.applyTheme

abstract class BaseActivity : AppCompatActivity() {

    private var currentNightMode: Int = 0
    private var startColor: Int = 0
    lateinit var settings: SharedPreferences

    open val displayTitle = false
    open fun forcedTheme():Int? = null
    abstract fun getSnackAnchorView(): View?
    private var baseContextWrappingDelegate: AppCompatDelegate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        settings = Settings.getInstance(this)
        /* Theme must be applied before super.onCreate */
        applyTheme()
        super.onCreate(savedInstanceState)
        if (UiTools.currentNightMode != resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            UiTools.invalidateBitmaps()
            UiTools.currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (displayTitle) {
            findViewById<View>(R.id.toolbar_icon).setGone()
            findViewById<View>(R.id.toolbar_vlc_title).setGone()
        }
    }

    override fun getDelegate() = baseContextWrappingDelegate
            ?: BaseContextWrappingDelegate(super.getDelegate()).apply { baseContextWrappingDelegate = this }

    override fun createConfigurationContext(overrideConfiguration: Configuration) = super.createConfigurationContext(overrideConfiguration).getContextWithLocale(AppContextProvider.locale)

    override fun getApplicationContext(): Context {
        return super.getApplicationContext().getContextWithLocale(AppContextProvider.locale)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        KeyHelper.manageModifiers(event)
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        KeyHelper.manageModifiers(event)
        return super.onKeyUp(keyCode, event)
    }

    override fun onSupportActionModeStarted(mode: androidx.appcompat.view.ActionMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startColor = window.statusBarColor
            val typedValue = TypedValue()
            theme.resolveAttribute(R.attr.actionModeBackground, typedValue, true)
            window.statusBarColor = typedValue.data
        }
        super.onSupportActionModeStarted(mode)
    }

    override fun onSupportActionModeFinished(mode: androidx.appcompat.view.ActionMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) window.statusBarColor = startColor
        super.onSupportActionModeFinished(mode)
    }
}
