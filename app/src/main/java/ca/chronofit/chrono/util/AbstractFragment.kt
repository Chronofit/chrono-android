package ca.chronofit.chrono.util

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.Navigator
import androidx.navigation.fragment.findNavController

abstract class AbstractFragment : Fragment() {

    protected abstract val binding: ViewDataBinding

    protected open fun setupView() {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = binding.root
        setupView()

        return view
    }

    protected fun navigate(navDirections: NavDirections, extras: Navigator.Extras? = null) {
        if (extras != null) {
            findNavController().navigate(navDirections, extras)
        } else {
            findNavController().navigate(navDirections)
        }
    }
}