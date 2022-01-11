package ca.chronofit.chrono

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import ca.chronofit.chrono.databinding.ActivityMain2Binding

class MainActivity2 : AppCompatActivity() {

    private lateinit var bind: ActivityMain2Binding
    private lateinit var viewModel: MainViewModel

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        splashScreen()

        bind = DataBindingUtil.setContentView(this, R.layout.activity_main2)

        setupNavigation()
    }

    private fun splashScreen() {
        val splash = installSplashScreen()
        splash.setKeepVisibleCondition{
            viewModel.initialize()
        }
    }

    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        bind.navBar.setupWithNavController(navController)
    }
}