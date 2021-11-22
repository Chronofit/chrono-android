package ca.chronofit.chrono.util

import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.Navigator
import androidx.navigation.fragment.findNavController

abstract class AbstractFragment : Fragment() {

    protected fun navigate(navDirections: NavDirections, extras: Navigator.Extras? = null) {
        if (extras != null) {
            findNavController().navigate(navDirections, extras)
        } else {
            findNavController().navigate(navDirections)
        }
    }
}