package ca.chronofit.chrono.util

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment

abstract class AbstractFragment : Fragment() {

    protected abstract val binding: ViewDataBinding
    protected abstract val fragmentViewModel: AbstractViewModel

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
}