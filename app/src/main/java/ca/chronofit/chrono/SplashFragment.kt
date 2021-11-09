package ca.chronofit.chrono

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import ca.chronofit.chrono.databinding.FragmentSplashBinding

class SplashFragment: Fragment() {
    private lateinit var bind: FragmentSplashBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bind = DataBindingUtil.inflate(inflater, R.layout.fragment_splash, container, false)

        return bind.root
    }

}