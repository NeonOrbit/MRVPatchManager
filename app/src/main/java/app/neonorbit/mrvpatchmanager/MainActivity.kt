package app.neonorbit.mrvpatchmanager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import app.neonorbit.mrvpatchmanager.databinding.AboutDialogBinding
import app.neonorbit.mrvpatchmanager.databinding.ActivityMainBinding
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(findViewById(R.id.toolbar))

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val navController = (navHost as NavHostFragment).navController
        binding.navView.setupWithNavController(navController)

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_home, R.id.navigation_patched, R.id.navigation_settings)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_home) {
                supportActionBar?.title = null
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.setHomeAsUpIndicator(R.drawable.home_top_icon)
            }
        }

        theme.applyStyle(
            rikka.material.preference.R.style.ThemeOverlay_Rikka_Material3_Preference, false
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.about, android.R.id.home -> {
                showAboutDialog()
                true
            }
            R.id.instruction -> {
                showInstructionDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showInstructionDialog() {
        MaterialAlertDialogBuilder(this).setMessage(
            getString(R.string.full_instructions)
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
        Glide.with(this).load(R.mipmap.ic_launcher).circleCrop().into(adb.aboutIcon)
    }
}
