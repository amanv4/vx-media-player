package tech.techyinc.television.util

import android.app.Activity
import android.content.Intent
import android.view.View
import com.google.android.material.snackbar.Snackbar
import tech.techyinc.television.ui.browser.REQUEST_CODE_NO_CONNECTION
import tech.techyinc.television.ui.dialogs.ConfirmationTvActivity
import tech.techyinc.resources.util.NoConnectivityException
import tech.techyinc.tools.Settings
import tech.techyinc.vlc.R


fun Activity.manageHttpException(e: Exception) {
    when (e) {
        is NoConnectivityException -> {
            if (Settings.showTvUi) {
                val intent = Intent(this, ConfirmationTvActivity::class.java)
                intent.putExtra(ConfirmationTvActivity.CONFIRMATION_DIALOG_TITLE, getString(R.string.no_internet_connection))
                intent.putExtra(ConfirmationTvActivity.CONFIRMATION_DIALOG_TEXT, getString(R.string.open_network_settings))
                startActivityForResult(intent, REQUEST_CODE_NO_CONNECTION)
            } else {
                Snackbar.make(findViewById<View>(android.R.id.content), R.string.no_internet_connection, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}