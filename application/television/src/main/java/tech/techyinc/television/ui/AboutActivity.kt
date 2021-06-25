package tech.techyinc.television.ui

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import tech.techyinc.resources.util.applyOverscanMargin
import tech.techyinc.television.R

import tech.techyinc.vlc.gui.helpers.UiTools

class AboutActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about_main)
        UiTools.fillAboutView(window.decorView.rootView)
        applyOverscanMargin(this)
        this.registerTimeView(findViewById(R.id.tv_time))
    }
}
