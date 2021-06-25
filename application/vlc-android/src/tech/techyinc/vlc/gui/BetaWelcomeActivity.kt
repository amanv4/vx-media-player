package tech.techyinc.vlc.gui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import tech.techyinc.vlc.R
import tech.techyinc.vlc.databinding.ActivityBetaWelcomeBinding

class BetaWelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBetaWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_beta_welcome)
        binding.betaOkButton.setOnClickListener {
            finish()
        }
    }
}
