package ca.chronofit.chrono

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import ca.chronofit.chrono.databinding.FragmentSplashBinding
import ca.chronofit.chrono.util.AbstractFragment

class SplashFragment: AbstractFragment() {
    private lateinit var bind: FragmentSplashBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bind = DataBindingUtil.inflate(inflater, R.layout.fragment_splash, container, false)

        // Set the time out delay and launch main activity afterwards
        Handler(
            Looper.getMainLooper()
        ).postDelayed(
            {
//                navigate(SplashFragmentDirections)
            }, 1000
        )

        return bind.root
    }

}