package ca.chronofit.chrono

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ca.chronofit.chrono.util.objects.CircuitObject

class DashboardViewModel : ViewModel() {

    val circuits = MutableLiveData<List<CircuitObject>>()
    val loading = MutableLiveData<Boolean>()

    fun refresh() {
        fetchCircuits()
    }

    private fun fetchCircuits() {
        val mockData = listOf(
            mockCircuit("Circuit 1"),
            mockCircuit("Circuit 2"),
            mockCircuit("Circuit 3"),
            mockCircuit("Circuit 4"),
            mockCircuit("Circuit 5"),
            mockCircuit("Circuit 6"),
            mockCircuit("Circuit 7")
        )
        circuits.value = mockData
        loading.value = false
    }

    private fun mockCircuit(circuitName: String): CircuitObject {
        return CircuitObject(
            name = circuitName,
            sets = 2,
            work = 10,
            rest = 10,
            count = 0,
            iconId = -1,
        )
    }
}