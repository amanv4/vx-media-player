/*****************************************************************************
 * AboutFragment.kt
 *
 * Copyright © 2018 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 */

package tech.techyinc.vlc.gui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
import tech.techyinc.tools.setGone
import tech.techyinc.vlc.BuildConfig
import tech.techyinc.vlc.R
import tech.techyinc.vlc.gui.audio.AudioPagerAdapter
import tech.techyinc.vlc.gui.helpers.UiTools


private const val TAG = "VLC/AboutFragment"
private const val MODE_TOTAL = 3 // Number of audio browser modes

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
class AboutFragment : Fragment() {

    val SUPPORT_EMAIL : String = "support@techyinc.tech"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.supportActionBar?.title = "VX ${BuildConfig.VLC_VERSION_NAME}"

        requireActivity().findViewById<FloatingActionButton>(R.id.fab).setGone()
        val aboutMain = view.findViewById<NestedScrollView>(R.id.about_main)
        val webView = view.findViewById<WebView>(R.id.webview)
        val webView2 = view.findViewById<WebView>(R.id.webview2)
        val revision = getString(R.string.build_revision)

        val lists = arrayOf(aboutMain, webView, webView2)
        val titles = arrayOf(getString(R.string.about), getString(R.string.licence),getString(R.string.privacyPolicy))
        val viewPager = view.findViewById<ViewPager>(R.id.pager).apply {
            offscreenPageLimit = MODE_TOTAL - 1
            adapter = AudioPagerAdapter(lists as Array<View>, titles)
        }
        requireActivity().findViewById<TabLayout>(R.id.sliding_tabs).apply {
            visibility = View.VISIBLE
            setupWithViewPager(viewPager)
        }
        lifecycleScope.launch {
            UiTools.fillAboutView(view)
            webView.loadUrl("file:///android_asset/license.htm")
            webView2.loadUrl("file:///android_asset/privacyPolicy.htm")

            webView.webViewClient = object : WebViewClient() {

                override fun onPageFinished(view: WebView, url: String) {
                    if (url.startsWith("file:///android_asset")) {
                        // Inject CSS when page is done loading
                        injectCSS(webView, when (context?.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
                            Configuration.UI_MODE_NIGHT_YES -> {
                                "license_dark.css"
                            }
                            Configuration.UI_MODE_NIGHT_NO -> {
                                "license_light.css"
                            }
                            else -> {
                                "license_light.css"
                            }
                        })
                        injectCommitRevision(webView, revision)
                    }
                    super.onPageFinished(view, url)
                }

                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    if (url.contains("file://")) {
                        view.loadUrl(url)
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                        try {
                            startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            view.loadUrl(url)
                        }

                    }
                    return true
                }
            }
            webView2.webViewClient = object : WebViewClient() {

                override fun onPageFinished(view: WebView, url: String) {
                    if (url.startsWith("file:///android_asset")) {
                        // Inject CSS when page is done loading
                        injectCSS(webView2, when (context?.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
                            Configuration.UI_MODE_NIGHT_YES -> {
                                "license_dark.css"
                            }
                            Configuration.UI_MODE_NIGHT_NO -> {
                                "license_light.css"
                            }
                            else -> {
                                "license_light.css"
                            }
                        })
                        injectCommitRevision(webView2, revision)
                    }
                    super.onPageFinished(view, url)
                }

                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    if (url.contains("file://")) {
                        view.loadUrl(url)
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                        try {
                            startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            view.loadUrl(url)
                        }

                    }
                    return true
                }
        }
        }

        val feedbackLink = view.findViewById<TextView>(R.id.feedback_link)
        feedbackLink.setOnClickListener(){
            val subject : String = "Feedback: VX Media Player Version ${BuildConfig.VLC_VERSION_NAME}"
            val message : String = "VX Media Player Info:\nVersion Name: " + BuildConfig.VLC_VERSION_NAME + "\nVersion Code: " + BuildConfig.VLC_VERSION_CODE + "\nPackage Name: " + BuildConfig.APP_ID + "\nDevice Name: " + Build.DEVICE+ "\nModel Name: " + Build.MODEL+ "\nProduct Name: " + Build.PRODUCT
            val title : String = "Send Feedback"

            emailSender(SUPPORT_EMAIL, message, subject, title)

        }

        val policyLink = view.findViewById<TextView>(R.id.policy_link)
        policyLink.setOnClickListener(){
            val url : String = "https://www.techyinc.tech/vmp-privacy-policy/"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)

        }
    }

    fun emailSender(email : String, message: String, subject: String, title: String) {
        val send = Intent(Intent.ACTION_SEND)
        send.putExtra(Intent.EXTRA_EMAIL, email)
        send.putExtra(Intent.EXTRA_SUBJECT, subject)
        send.putExtra(Intent.EXTRA_TEXT, message)
        send.type = "message/rfc822"
        startActivity(Intent.createChooser(send, title))
    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun injectCSS(webView: WebView, cssAsset: String) {
        try {
            webView.settings.javaScriptEnabled = true
            val inputStream = requireActivity().assets.open(cssAsset)
            val buffer = ByteArray(inputStream.available())
            inputStream.read(buffer)
            inputStream.close()
            val encoded = Base64.encodeToString(buffer, Base64.NO_WRAP)
            webView.loadUrl("javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var style = document.createElement('style');" +
                    "style.type = 'text/css';" +
                    // Tell the browser to BASE64-decode the string into your script !!!
                    "style.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(style);" +
                    "})()")

            webView.settings.javaScriptEnabled = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun injectCommitRevision(webView: WebView, revision: String) {
        try {
            webView.settings.javaScriptEnabled = true

            webView.loadUrl("javascript:(function() {" +
                    "var link = document.getElementById('revision_link');" +
                    "var newLink = link.href.replace('!COMMITID!', '$revision');" +
                    "link.setAttribute('href', newLink);" +
                    "link.innerText = newLink;" +
                    "})()")

            webView.settings.javaScriptEnabled = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
