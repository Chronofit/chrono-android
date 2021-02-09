package ca.chronofit.chrono.circuit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import ca.chronofit.chrono.R
import ca.chronofit.chrono.databinding.FragmentCircuitDashboardBinding
import ca.chronofit.chrono.util.BaseActivity
import ca.chronofit.chrono.util.adapters.CircuitViewAdapter
import ca.chronofit.chrono.util.constants.Constants
import ca.chronofit.chrono.util.constants.Events
import ca.chronofit.chrono.util.objects.CircuitObject
import ca.chronofit.chrono.util.objects.CircuitsObject
import ca.chronofit.chrono.util.objects.PreferenceManager
import ca.chronofit.chrono.util.objects.SettingsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.dialog_alert.view.*
import kotlinx.android.synthetic.main.fragment_dashboard_bottom_sheet.view.*

class CircuitDashboardFrag : Fragment() {
    private lateinit var bind: FragmentCircuitDashboardBinding
    private lateinit var recyclerView: RecyclerView

    private val settingsViewModel: SettingsViewModel by activityViewModels()

    private lateinit var remoteConfig: FirebaseRemoteConfig

    private var readyTime: Int = 5
    private var audioPrompts: Boolean = true
    private var lastRest: Boolean = true

    private var circuitsObject: CircuitsObject? = null
    private var selectedPosition: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bind = DataBindingUtil.inflate(
            inflater, R.layout.fragment_circuit_dashboard,
            container, false
        )
        PreferenceManager.with(activity as BaseActivity)

        remoteConfig = Firebase.remoteConfig

        recyclerView = bind.recyclerView
        loadData()
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        bind.addCircuit.setOnClickListener {
            FirebaseAnalytics.getInstance(requireContext())
                .logEvent(Events.CREATE_STARTED, Bundle())
            startActivityForResult(
                Intent(requireContext(), CircuitCreateActivity::class.java),
                Constants.DASH_TO_CREATE
            )
        }

        observeSettings()

        return bind.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                Constants.DASH_TO_CREATE -> {
                    // Circuit Added
                    loadData()
                    Toast.makeText(requireContext(), "Circuit added and saved!", Toast.LENGTH_SHORT)
                        .show()
                }
                Constants.DASH_TO_TIMER -> {
                    // Circuit Completed (Circuit Timer)
                    // Ideal spot to ask for a rating after a threshold of timers have been run
                    checkForReview()
                    Log.i("CircuitDashboardFrag", "Completed a circuit.")
                }
                Constants.DASH_TO_EDIT -> {
                    // Circuit Edited
                    loadData()
                    Toast.makeText(
                        requireContext(),
                        "Circuit edited and saved!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    @Suppress("NAME_SHADOWING")
    private fun checkForReview() {
        if ((PreferenceManager.get<Int>(Constants.NUM_COMPLETE) != null) && (PreferenceManager.get<Int>(
                Constants.NUM_COMPLETE
            )!! >= remoteConfig.getString(Constants.CONFIG_REVIEW_THRESHOLD).toInt())
        ) {
            val manager = ReviewManagerFactory.create(requireContext())
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { request ->
                if (request.isSuccessful) {
                    val reviewInfo = request.result
                    val flow = manager.launchReviewFlow(requireActivity(), reviewInfo)
                    flow.addOnCompleteListener {
                        Toast.makeText(
                            requireContext(),
                            "Thank you for the review. Your feedback is appreciated!",
                            Toast.LENGTH_SHORT
                        ).show()
                        FirebaseAnalytics.getInstance(requireContext())
                            .logEvent(Events.USER_REVIEWED, Bundle())
                    }
                } else {
                    Log.d("CircuitDashFrag", "Problem launching review flow")
                }
            }
        }
    }

    private fun circuitClicked(circuit: CircuitObject) {
        val jsonString = GsonBuilder().create().toJson(circuit)
        val intent = Intent(requireContext(), CircuitTimerActivity::class.java)
        intent.putExtra("circuitObject", jsonString)
        intent.putExtra("readyTime", readyTime)
        intent.putExtra("audioPrompts", audioPrompts)
        intent.putExtra("lastRest", lastRest)
        startActivityForResult(intent, Constants.DASH_TO_TIMER)
    }

    private val itemTouchHelperCallback = object : ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            return makeMovementFlags((ItemTouchHelper.UP or ItemTouchHelper.DOWN), 0)
        }

        override fun isLongPressDragEnabled(): Boolean {
            return true
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            itemMoved(viewHolder.adapterPosition, target.adapterPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            deleteCircuit(null, viewHolder.adapterPosition)
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                recyclerView.adapter!!.notifyDataSetChanged()
            }
            super.onSelectedChanged(viewHolder, actionState)
        }
    }

    private fun itemMoved(current: Int, target: Int) {
        recyclerView.adapter!!.notifyItemMoved(current, target)

        // Update Model
        val circuit = circuitsObject!!.circuits!![current]
        circuitsObject!!.circuits!!.removeAt(current)
        circuitsObject!!.circuits!!.add(target, circuit)

        // Save updated list in local storage
        PreferenceManager.put(circuitsObject, Constants.CIRCUITS)
    }

    @SuppressLint("InflateParams")
    private fun showMoreMenu(position: Int) {
        selectedPosition = position

        // Roll out the bottom sheet
        val modalSheetView = layoutInflater.inflate(R.layout.fragment_dashboard_bottom_sheet, null)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(modalSheetView)

        modalSheetView.delete_layout.setOnClickListener {
            deleteCircuit(dialog, position)
        }

        modalSheetView.edit_layout.setOnClickListener {
            val intent = Intent(requireContext(), CircuitCreateActivity::class.java)
            intent.putExtra("isEdit", true)
            intent.putExtra("circuitPosition", position)
            dialog.dismiss()
            startActivityForResult(intent, Constants.DASH_TO_EDIT)
        }

        modalSheetView.share_layout.setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(
                    Intent.EXTRA_TEXT,
                    circuitsObject!!.circuits?.get(position)!!.shareString()
                )
                type = "text/plain"
            }
            startActivity(Intent.createChooser(sendIntent, null))
        }
        dialog.show()
    }

    @SuppressLint("SetTextI18n")
    private fun deleteCircuit(dialog: BottomSheetDialog?, position: Int) {
        Log.i("arrange", position.toString())
        val builder =
            MaterialAlertDialogBuilder(requireContext(), R.style.CustomMaterialDialog).create()
        val dialogView = View.inflate(requireContext(), R.layout.dialog_alert, null)

        // Set Dialog Views
        dialogView.dialog_title.text =
            "Delete " + circuitsObject?.circuits!![position].name
        dialogView.dialog_subtitle.text = getString(R.string.delete_circuit_subtitle)
        dialogView.confirm.text = getString(R.string.delete)
        dialogView.cancel.text = getString(R.string.cancel)

        // Button Logic
        dialogView.cancel.setOnClickListener {
            builder.dismiss()
        }

        dialogView.confirm.setOnClickListener {
            // Dismiss popups
            builder.dismiss()
            dialog!!.dismiss()

            // Remove from model and recyclerview
            circuitsObject?.circuits?.remove(circuitsObject?.circuits!![position])
            recyclerView.adapter?.notifyItemRemoved(position)
            recyclerView.adapter?.notifyItemRangeChanged(position, circuitsObject?.circuits!!.size)

            if (recyclerView.adapter?.itemCount!! == 0) {
                loadEmptyUI()
            }

            // Save updated list in local storage
            PreferenceManager.put(circuitsObject, Constants.CIRCUITS)
        }

        // Display the Dialog
        builder.setView(dialogView)
        builder.show()
    }

    private fun observeSettings() {
        // Retrieve Settings if they exist
        if (PreferenceManager.get<Int>(Constants.GET_READY_SETTING) != null) {
            readyTime = PreferenceManager.get<Int>(Constants.GET_READY_SETTING)!!
        }

        if (PreferenceManager.get<Boolean>(Constants.AUDIO_SETTING) != null) {
            audioPrompts = PreferenceManager.get<Boolean>(Constants.AUDIO_SETTING)!!
        }

        if (PreferenceManager.get<Boolean>(Constants.LAST_REST_SETTING) != null) {
            lastRest = PreferenceManager.get<Boolean>(Constants.LAST_REST_SETTING)!!
        }

        // Observe Settings
        settingsViewModel.getReadyTime.observe(viewLifecycleOwner, { _readyTime ->
            readyTime = (_readyTime.substring(0, _readyTime.length - 1)).toInt()
        })

        settingsViewModel.audioPrompts.observe(viewLifecycleOwner, { prompts ->
            audioPrompts = prompts
        })

        settingsViewModel.lastRest.observe(viewLifecycleOwner, { rest ->
            lastRest = rest
        })
    }

    private fun loadData() {
        circuitsObject = PreferenceManager.get<CircuitsObject>(Constants.CIRCUITS)

        if (circuitsObject != null && circuitsObject?.circuits!!.size > 0) {
            bind.recyclerView.visibility = View.VISIBLE
            bind.emptyLayout.visibility = View.GONE

            recyclerView.adapter = CircuitViewAdapter(
                circuitsObject?.circuits!!,
                { circuitObject: CircuitObject -> circuitClicked(circuitObject) },
                { position: Int -> showMoreMenu(position) }, requireContext()
            )
        } else {
            loadEmptyUI()
        }
    }

    private fun loadEmptyUI() {
        bind.recyclerView.visibility = View.GONE
        bind.emptyLayout.visibility = View.VISIBLE
    }
}