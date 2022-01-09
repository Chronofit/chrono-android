package ca.chronofit.chrono

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ca.chronofit.chrono.util.AbstractFragment
import ca.chronofit.chrono.databinding.FragmentSwatchBinding

class StopwatchFragment : AbstractFragment() {

    override val binding: FragmentSwatchBinding by BindFragment(R.layout.fragment_swatch)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return binding.root
    }
}