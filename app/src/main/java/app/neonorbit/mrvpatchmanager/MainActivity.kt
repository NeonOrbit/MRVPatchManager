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
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import app.neonorbit.mrvpatchmanager.databinding.AboutDialogBinding
import app.neonorbit.mrvpatchmanager.databinding.ActivityMainBinding
import app.neonorbit.mrvpatchmanager.remote.GithubService
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
        setupActionBarWithNavController(navController)
        theme.applyStyle(
            rikka.material.preference.R.style.ThemeOverlay_Rikka_Material3_Preference, true
        )
    }

    private fun setupActionBarWithNavController(navController: NavController) {
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
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.instruction -> {
                showInstructionDialog()
                true
            }
            R.id.tutorial -> {
                startActivity(Intent(
                    Intent.ACTION_VIEW, Uri.parse(AppConfigs.TUTORIAL_URL)
                ))
                true
            }
            R.id.update -> {
                GithubService.checkForUpdate(force = true)
                AppServices.showToast(R.string.checking_for_update, true)
                true
            }
            R.id.about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showInstructionDialog() {
        MaterialAlertDialogBuilder(this).setMessage(getString(R.string.instructions)).show()
    }

    private fun showAboutDialog() {
        AboutDialogBinding.inflate(LayoutInflater.from(this), null, false).apply {
            appTitle.text = getString(R.string.app_name)
            appVersion.text = getString(R.string.version_text, BuildConfig.VERSION_NAME)
            developerInfo.setLinkedText(
                R.string.developer_info_text, AppConfigs.DEVELOPER, AppConfigs.DEVELOPER_URL
            )
            helpForumInfo.setLinkedText(
                R.string.help_forum_info_text, AppConfigs.HELP_FORUM, AppConfigs.HELP_FORUM_URL
            )
            sourceCodeInfo.setLinkedText(
                R.string.source_code_info_text, AppConfigs.GITHUB_REPO, AppConfigs.GITHUB_REPO_URL
            )
        }.let {
            MaterialAlertDialogBuilder(this).setView(it.root).show()
            Glide.with(this).load(R.mipmap.ic_launcher).transform(RoundedCorners(50)).into(it.aboutIcon)
        }
    }
}
