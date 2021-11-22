package ca.chronofit.chrono

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import ca.chronofit.chrono.util.AbstractFragment
import ca.chronofit.chrono.databinding.FragmentSwatchBinding

class StopwatchFragment: AbstractFragment() {

    private lateinit var bind: FragmentSwatchBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bind = DataBindingUtil.inflate(inflater, R.layout.fragment_swatch, container, false)

        return bind.root
    }

}