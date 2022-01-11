package ca.chronofit.chrono

import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import ca.chronofit.chrono.databinding.FragmentDashboardBinding
import ca.chronofit.chrono.util.AbstractFragment

class DashboardFragment : AbstractFragment() {

    override val binding: FragmentDashboardBinding by BindFragment(R.layout.fragment_dashboard)
    lateinit var viewModel: DashboardViewModel
    private val circuitsAdapter = CircuitAdapterItem(arrayListOf())

    override fun setupView() {
        viewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)
        viewModel.refresh()

        binding.apply {
            recyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = circuitsAdapter
            }
            observeViewModel()
        }

        requireActivity().window!!.statusBarColor = ContextCompat.getColor(requireContext(), R.color.velvet)
    }

    private fun observeViewModel() {
        viewModel.circuits.observe(this, { circuits ->
            circuits?.let { circuitsAdapter.updateCircuits(it) }
        })

        viewModel.loading.observe(this, { isLoading ->
            isLoading?.let {
                binding.apply {
                    loading.visibility = if (it) View.VISIBLE else View.GONE
                    if (it) recyclerView.visibility = View.GONE
                }
            }
        })
    }
}