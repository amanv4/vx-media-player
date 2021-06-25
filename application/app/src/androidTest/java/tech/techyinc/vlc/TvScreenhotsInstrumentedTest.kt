/*
 * ************************************************************************
 *  TvScreenhotsInstrumentedTest.kt
 * *************************************************************************
 * Copyright © 2020 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
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
 * **************************************************************************
 *
 *
 */

package tech.techyinc.vlc

import android.app.Activity
import android.content.Intent
import android.os.SystemClock
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.Direction
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.hamcrest.Matchers.allOf
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import tech.techyinc.resources.EXTRA_TARGET
import tech.techyinc.television.ui.MainTvActivity
import tech.techyinc.television.ui.audioplayer.AudioPlayerActivity
import tech.techyinc.television.ui.browser.VerticalGridActivity
import tech.techyinc.tools.Settings
import tech.techyinc.vlc.util.DpadHelper.pressBack
import tech.techyinc.vlc.util.DpadHelper.pressDPad
import tech.techyinc.vlc.util.DpadHelper.pressDPadCenter
import tech.techyinc.vlc.util.DpadHelper.pressHome
import tech.techyinc.vlc.util.DpadHelper.pressPip
import tech.techyinc.vlc.util.ScreenshotUtil
import tech.techyinc.vlc.util.UiUtils
import tech.techyinc.vlc.util.UiUtils.waitForActivity
import tech.techyinc.vlc.util.UiUtils.waitId
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule

//@RunWith(AndroidJUnit4::class)
class TvScreenhotsInstrumentedTest : BaseUITest() {
    @Rule
    @JvmField
    val activityTestRule = ActivityTestRule(MainTvActivity::class.java, true, false)

    @Rule
    @JvmField
    val demoModeRule = DemoModeRule()

    lateinit var activity: MainTvActivity

    companion object {
        @ClassRule
        @JvmField
        val localeTestRule = LocaleTestRule()
    }

    override fun beforeTest() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
        Settings.getInstance(context).edit().putBoolean("auto_rescan", false).putBoolean("audio_resume_card", false).commit()
        val intent = Intent().apply {
            putExtra(EXTRA_TARGET, R.id.nav_audio)
        }
        activityTestRule.launchActivity(intent)
        activity = activityTestRule.activity
    }

    fun getCurrentActivity(): Activity? {
        var currentActivity: Activity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync { run { currentActivity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).elementAtOrNull(0) } }
        return currentActivity
    }

    @ObsoleteCoroutinesApi
    @Test
    fun testTakeScreenshot() {
        SystemClock.sleep(1500)
        //Audio
        ScreenshotUtil.takeScreenshot(1, "tv_home")

        onView(allOf(withId(R.id.row_header), withText(activity.getString(R.string.audio)), withEffectiveVisibility(Visibility.VISIBLE))).perform(click())
        pressDPad(Direction.RIGHT, 4)
        pressDPadCenter()

        waitForActivity(VerticalGridActivity::class.java)
        waitId(tech.techyinc.television.R.id.list, 5000)

        getCurrentActivity()?.let { activity ->
            UiUtils.waitUntilLoaded { activity.findViewById(tech.techyinc.television.R.id.list) }
        } ?: throw IllegalStateException("Cannot find activity")

        ScreenshotUtil.takeScreenshot(4,"tv_audio_list")
        pressDPad(Direction.DOWN, 4)
        pressDPadCenter()
        waitForActivity(AudioPlayerActivity::class.java)

        waitId(tech.techyinc.television.R.id.album_cover, 5000)
        ScreenshotUtil.takeScreenshot(6,"tv_audio_player")
        pressBack()
        pressBack()
        pressDPad(Direction.DOWN, 2)
        pressDPadCenter()

        waitForActivity(VerticalGridActivity::class.java)
        waitId(tech.techyinc.television.R.id.list, 5000)
        getCurrentActivity()?.let { activity ->
            UiUtils.waitUntilLoaded { activity.findViewById(tech.techyinc.television.R.id.list) }
        }
        ScreenshotUtil.takeScreenshot(5,"tv_files")
        pressBack()
        pressDPad(Direction.LEFT)
        onView(allOf(withId(R.id.row_header), withText(activity.getString(R.string.video)), withEffectiveVisibility(Visibility.VISIBLE))).perform(click())
        pressDPadCenter()
        pressDPadCenter()
        waitForActivity(VerticalGridActivity::class.java)
        waitId(tech.techyinc.television.R.id.list, 5000)
        getCurrentActivity()?.let { activity ->
            UiUtils.waitUntilLoaded { activity.findViewById(tech.techyinc.television.R.id.list) }
        }
        ScreenshotUtil.takeScreenshot(2,"tv_video_list")
        pressDPad(Direction.DOWN, 3)
        pressDPadCenter()
        SystemClock.sleep(1500)
        pressDPad(Direction.DOWN)
        SystemClock.sleep(600)
        ScreenshotUtil.takeScreenshot(3,"tv_video_player")
        Espresso.pressBack()
        pressPip()
        pressHome()
        pressPip()
        SystemClock.sleep(1500)
        ScreenshotUtil.takeScreenshot(7,"tv_pip")
    }
}
