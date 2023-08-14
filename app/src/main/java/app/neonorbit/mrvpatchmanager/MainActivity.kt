package app.neonorbit.mrvpatchmanager

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import app.neonorbit.mrvpatchmanager.databinding.AboutDialogBinding
import app.neonorbit.mrvpatchmanager.databinding.ActivityMainBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        this.installSplashScreen()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater).also {
            setContentView(it.root)
            setSupportActionBar(it.toolbar)
        }

        val navController = supportFragmentManager.findFragmentById(R.id.nav_host_fragment).let {
            (it as NavHostFragment).navController
        }
        NavigationUI.setupWithNavController(binding.navView, navController)

        binding.icon.setOnClickListener { showAboutDialog() }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_home) {
                binding.icon.isClickable = true
                binding.title.animate().setDuration(300).alpha(0.0f)
                binding.icon.animate().setDuration(300).alpha(1.0f)
            } else {
                binding.icon.isClickable = false
                binding.title.text = destination.label
                binding.icon.animate().setDuration(300).alpha(0.0f)
                binding.title.animate().setDuration(300).alpha(1.0f)
            }
            if (!binding.title.isVisible) binding.title.isVisible = true
        }
        theme.applyStyle(
            rikka.material.preference.R.style.ThemeOverlay_Rikka_Material3_Preference, true
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.about -> {
                showAboutDialog()
                true
            }
            R.id.instruction -> {
                showInstructionDialog()
                true
            }
            R.id.tutorial -> {
                startActivity(Intent(
                    Intent.ACTION_VIEW, Uri.parse(AppConfig.TUTORIAL_URL)
                ))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showInstructionDialog() {
        MaterialAlertDialogBuilder(this).setMessage(
            getString(R.string.instructions)
        ).show()
    }

    private fun showAboutDialog() {
        val adb = AboutDialogBinding.inflate(LayoutInflater.from(this), null, false)
        adb.appTitle.text = getString(R.string.app_name)
        adb.appVersion.text = getString(R.string.version_text, BuildConfig.VERSION_NAME)
        adb.developerInfo.setLinkedText(
            R.string.developer_info_text, AppConfig.DEVELOPER, AppConfig.DEVELOPER_URL
        )
        adb.helpForumInfo.setLinkedText(
            R.string.help_forum_info_text, AppConfig.HELP_FORUM, AppConfig.HELP_FORUM_URL
        )
        adb.sourceCodeInfo.setLinkedText(
            R.string.source_code_info_text, AppConfig.GITHUB_REPO, AppConfig.GITHUB_REPO_URL
        )
        MaterialAlertDialogBuilder(this).setView(adb.root).show()
        Glide.with(this).load(R.mipmap.ic_launcher).transform(RoundedCorners(50)).into(adb.aboutIcon)
    }
}
