package ca.chronofit.chrono

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ca.chronofit.chrono.databinding.ItemCircuitBinding
import ca.chronofit.chrono.util.objects.CircuitObject

class CircuitAdapterItem(
    private val circuits: ArrayList<CircuitObject>
) : RecyclerView.Adapter<CircuitViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = CircuitViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_circuit, parent, false)
    )

    override fun onBindViewHolder(holder: CircuitViewHolder, position: Int) {
        holder.bind(circuits[position])
    }

    override fun getItemCount(): Int {
        return circuits.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateCircuits(newCircuits: List<CircuitObject>) {
        circuits.clear()
        circuits.addAll(newCircuits)
        notifyDataSetChanged()
    }

}

class CircuitViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val itemBinding: ItemCircuitBinding = view.bind()

    fun bind(circuit: CircuitObject) {
        val context = itemBinding.root.context

        itemBinding.circuitName.text = circuit.name
        itemBinding.numSets.text =
            context.resources.getQuantityString(R.plurals.sets, circuit.sets, circuit.sets)
        itemBinding.workTime.text = context.getString(R.string.circuit_work_time, circuit.work)
        itemBinding.restTime.text = context.getString(R.string.circuit_rest_time, circuit.rest)
    }
}