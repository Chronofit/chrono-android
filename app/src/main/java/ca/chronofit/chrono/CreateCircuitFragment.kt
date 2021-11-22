package ca.chronofit.chrono

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ca.chronofit.chrono.databinding.FragmentCreateCircuitBinding

class CreateCircuitFragment : Fragment() {

    private lateinit var bind: FragmentCreateCircuitBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_circuit, container, false)
    }

}