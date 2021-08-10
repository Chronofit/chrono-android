package ca.chronofit.chrono.circuit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
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
import ca.chronofit.chrono.MainActivity
import ca.chronofit.chrono.R
import ca.chronofit.chrono.databinding.DialogAlertBinding
import ca.chronofit.chrono.databinding.FragmentCircuitDashboardBinding
import ca.chronofit.chrono.databinding.FragmentDashboardBottomSheetBinding
import ca.chronofit.chrono.util.BaseActivity
import ca.chronofit.chrono.util.adapters.CircuitItemAdapter
import ca.chronofit.chrono.util.constants.Constants
import ca.chronofit.chrono.util.constants.Events
import ca.chronofit.chrono.util.objects.CircuitObject
import ca.chronofit.chrono.util.objects.CircuitsObject
import ca.chronofit.chrono.util.objects.PreferenceManager
import ca.chronofit.chrono.util.objects.SettingsViewModel
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.dynamiclinks.ktx.androidParameters
import com.google.firebase.dynamiclinks.ktx.dynamicLink
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.gson.GsonBuilder
import org.json.JSONObject

class CircuitDashboardFrag : Fragment() {
    private lateinit var bind: FragmentCircuitDashboardBinding
    private lateinit var recyclerView: RecyclerView
    private val settingsViewModel: SettingsViewModel by activityViewModels()
    private lateinit var remoteConfig: FirebaseRemoteConfig
    private var readyTime: Int = 5
    private var audioPrompts: Boolean = true
    private var lastRest: Boolean = true
    private var soundEffect: String = Constants.SOUND_LONG_WHISTLE
    private lateinit var circuitsObject: CircuitsObject
    private var selectedPosition: Int = 0
    private var bounceFab: YoYo.YoYoString? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bind = DataBindingUtil.inflate(
            inflater, R.layout.fragment_circuit_dashboard,
            container, false
        )
        recyclerView = bind.recyclerView

        PreferenceManager.with(activity as BaseActivity)
        remoteConfig = Firebase.remoteConfig
        observeSettings()
        loadData()

        bind.sortChips.apply {
            setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.chip_alphabetical -> {
                        circuitsObject.circuits.sortBy { circuit -> circuit.name }
                        PreferenceManager.put(Constants.ALPHABETICAL, Constants.SORT_PREFERENCE)
                        animateChange()
                    }
                    R.id.chip_date -> {
                        circuitsObject.circuits.sortByDescending { circuit -> circuit.date }
                        PreferenceManager.put(Constants.RECENTLY_ADDED, Constants.SORT_PREFERENCE)
                        animateChange()
                    }
                    R.id.chip_popular -> {
                        circuitsObject.circuits.sortByDescending { circuit -> circuit.count }
                        PreferenceManager.put(Constants.POPULARITY, Constants.SORT_PREFERENCE)
                        animateChange()
                    }
                }
            }
        }

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

        return bind.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                Constants.DASH_TO_CREATE -> {
                    loadData()
                    Toast.makeText(requireContext(), "Circuit added and saved!", Toast.LENGTH_SHORT)
                        .show()
                }
                Constants.DASH_TO_TIMER -> {
                    checkForReview()
                }
                Constants.DASH_TO_EDIT -> {
                    loadData()
                    Toast.makeText(
                        requireContext(),
                        "Circuit edited and saved!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        if (PreferenceManager.get(Constants.SORT_PREFERENCE) == Constants.POPULARITY) {
            circuitsObject.circuits.sortByDescending { it.count }
            PreferenceManager.put(Constants.POPULARITY, Constants.SORT_PREFERENCE)
            animateChange()
        }
    }

    fun startCircuitCreateActivity(name: String, sets: String, work: String, rest: String) {
        val startIntent = Intent(requireContext(), CircuitCreateActivity::class.java)
        startIntent.putExtra(getString(R.string.is_circuit_shared), true)
        startIntent.putExtra(getString(R.string.circuit_name), name)
        startIntent.putExtra(getString(R.string.sets), sets)
        startIntent.putExtra(getString(R.string.work), work)
        startIntent.putExtra(getString(R.string.rest), rest)
        startActivityForResult(
            startIntent,
            Constants.DASH_TO_CREATE
        )
    }


    @Suppress("NAME_SHADOWING")
    private fun checkForReview() {
        if ((PreferenceManager.get<Int>(Constants.NUM_COMPLETE) != null) && (PreferenceManager.get<Boolean>(
                Constants.REVIEW_PROMPT_COMPLETE
            ) != null) && (!PreferenceManager.get<Boolean>(Constants.REVIEW_PROMPT_COMPLETE)!!) && (PreferenceManager.get<Int>(
                Constants.NUM_COMPLETE
            )!! >= remoteConfig.getString(Constants.CONFIG_REVIEW_THRESHOLD).toInt())
        ) {
            val props = JSONObject()
            props.put("source", "CircuitDashboardActivity")
            (activity as MainActivity).mixpanel.track("Review pop-up presented", props)

            val manager = ReviewManagerFactory.create(requireContext())
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { request ->
                if (request.isSuccessful) {
                    val props = JSONObject()
                    val reviewInfo = request.result
                    val flow = manager.launchReviewFlow(requireActivity(), reviewInfo)
                    flow.addOnCompleteListener {
                        props.put("source", "CircuitDashboardActivity")
                        (activity as MainActivity).mixpanel.track("App reviewed", props)

                        Toast.makeText(
                            requireContext(),
                            "Thank you for the review. Your feedback is appreciated!",
                            Toast.LENGTH_SHORT
                        ).show()

                        PreferenceManager.put(true, Constants.REVIEW_PROMPT_COMPLETE)
                    }
                } else {
                    Log.e("CircuitDashFrag", "Problem launching review flow")
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
        intent.putExtra("soundEffect", soundEffect)

        val index = circuitsObject.circuits.indexOf(circuit)
        val currCount = circuitsObject.circuits[index].count
        if (currCount != null) {
            circuitsObject.circuits[index].count = currCount + 1
        } else {
            circuitsObject.circuits[index].count = 0
        }

        PreferenceManager.put(circuitsObject, Constants.CIRCUITS)
        recyclerView.adapter?.notifyDataSetChanged()

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

        if (current != target) {
            bind.sortChips.clearCheck()
        }

        // Update Model
        val circuit = circuitsObject.circuits[current]
        circuitsObject.circuits.removeAt(current)
        circuitsObject.circuits.add(target, circuit)

        // Save updated list in local storage
        PreferenceManager.put(circuitsObject, Constants.CIRCUITS)
    }

    private fun animateChange() {
        if (circuitsObject.circuits.size != 0) {
            recyclerView.adapter!!.notifyDataSetChanged()
            PreferenceManager.put(circuitsObject, Constants.CIRCUITS)
        }
    }

    @SuppressLint("InflateParams")
    private fun showMoreMenu(position: Int) {
        selectedPosition = position

        val bottomSheetFrag = BottomSheetDialog(requireContext())
        val fragBinding =
            FragmentDashboardBottomSheetBinding.inflate(LayoutInflater.from(requireContext()))

        fragBinding.deleteLayout.setOnClickListener {
            deleteCircuit(bottomSheetFrag, position)
        }

        fragBinding.editLayout.setOnClickListener {
            val intent = Intent(requireContext(), CircuitCreateActivity::class.java)
            intent.putExtra("isEdit", true)
            intent.putExtra("circuitPosition", position)
            bottomSheetFrag.dismiss()
            startActivityForResult(intent, Constants.DASH_TO_EDIT)
        }

        fragBinding.shareLayout.setOnClickListener {
            val url = createLink(position)
            val props = JSONObject()
            props.put("source", "CircuitDashboardActivity")
            props.put("name", circuitsObject.circuits[position].name)
            props.put("sets", circuitsObject.circuits[position].sets)
            props.put("work", circuitsObject.circuits[position].work)
            props.put("rest", circuitsObject.circuits[position].rest)
            props.put("iconID", circuitsObject.circuits[position].iconId)

            (activity as MainActivity).mixpanel.track("Circuit Shared", props)

            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out my Chrono Circuit: $url"
                )

                type = "text/plain"
            }
            startActivity(Intent.createChooser(sendIntent, null))
        }

        bottomSheetFrag.setContentView(fragBinding.root)
        bottomSheetFrag.show()
    }

    @SuppressLint("SetTextI18n")
    private fun deleteCircuit(dialog: BottomSheetDialog?, position: Int) {
        val builder =
            MaterialAlertDialogBuilder(requireContext(), R.style.CustomMaterialDialog).create()
        val dialogBinding = DialogAlertBinding.inflate(LayoutInflater.from(requireContext()))

        dialogBinding.dialogTitle.text =
            "Delete " + circuitsObject.circuits[position].name
        dialogBinding.dialogSubtitle.text = getString(R.string.delete_circuit_subtitle)
        dialogBinding.confirm.text = getString(R.string.delete)
        dialogBinding.cancel.text = getString(R.string.cancel)

        dialogBinding.cancel.setOnClickListener {
            builder.dismiss()
        }

        dialogBinding.confirm.setOnClickListener {
            builder.dismiss()
            dialog!!.dismiss()

            circuitsObject.circuits.remove(circuitsObject.circuits[position])
            recyclerView.adapter?.notifyItemRemoved(position)
            recyclerView.adapter?.notifyItemRangeChanged(position, circuitsObject.circuits.size)

            if (recyclerView.adapter?.itemCount!! == 0) {
                loadEmptyUI()
            }

            PreferenceManager.put(circuitsObject, Constants.CIRCUITS)
        }

        builder.setView(dialogBinding.root)
        builder.show()
    }

    private fun observeSettings() {
        if (PreferenceManager.get<Int>(Constants.GET_READY_SETTING) != null) {
            readyTime = PreferenceManager.get<Int>(Constants.GET_READY_SETTING)!!
        }

        if (PreferenceManager.get<Boolean>(Constants.AUDIO_SETTING) != null) {
            audioPrompts = PreferenceManager.get<Boolean>(Constants.AUDIO_SETTING)!!
        }

        if (PreferenceManager.get<Boolean>(Constants.LAST_REST_SETTING) != null) {
            lastRest = PreferenceManager.get<Boolean>(Constants.LAST_REST_SETTING)!!
        }

        soundEffect = PreferenceManager.get(Constants.SOUND_EFFECT_SETTING).replace("\"", "")

        settingsViewModel.getReadyTime.observe(viewLifecycleOwner, { _readyTime ->
            readyTime = (_readyTime.substring(0, _readyTime.length - 1)).toInt()
        })

        settingsViewModel.audioPrompts.observe(viewLifecycleOwner, { prompts ->
            audioPrompts = prompts
        })

        settingsViewModel.lastRest.observe(viewLifecycleOwner, { rest ->
            lastRest = rest
        })

        settingsViewModel.soundEffect.observe(viewLifecycleOwner, { effect ->
            soundEffect = effect
        })
    }

    private fun loadData() {
        circuitsObject = PreferenceManager.get<CircuitsObject>(Constants.CIRCUITS)!!

        if (circuitsObject.circuits.size > 0) {
            loadDashboardUI()

            recyclerView.adapter = CircuitItemAdapter(
                circuitsObject.circuits,
                { circuitObject: CircuitObject -> circuitClicked(circuitObject) },
                { position: Int -> showMoreMenu(position) }, requireContext()
            )

            bind.sortChips.apply {
                when (PreferenceManager.get(Constants.SORT_PREFERENCE)) {
                    Constants.ALPHABETICAL -> {
                        clearCheck()
                        check(R.id.chip_alphabetical)
                    }
                    Constants.RECENTLY_ADDED -> {
                        clearCheck()
                        check(R.id.chip_date)
                    }
                    Constants.POPULARITY -> {
                        clearCheck()
                        check(R.id.chip_popular)
                    }
                }
            }
        } else {
            loadEmptyUI()
        }
    }

    private fun loadEmptyUI() {
        bind.recyclerView.visibility = View.GONE
        bind.sortChips.visibility = View.GONE
        bind.emptyLayout.root.visibility = View.VISIBLE

        bounceFab = YoYo.with(Techniques.Bounce)
            .duration(1000)
            .repeat(YoYo.INFINITE)
            .playOn(bind.addCircuit)
    }

    private fun loadDashboardUI() {
        bind.recyclerView.visibility = View.VISIBLE
        bind.sortChips.visibility = View.VISIBLE
        bind.emptyLayout.root.visibility = View.GONE

        bounceFab?.stop()
    }

    private fun createLink(position: Int): String {
        val dynamicLink = Firebase.dynamicLinks.dynamicLink {
            link = Uri.parse(circuitsObject.circuits[position].generateDeeplinkURL(requireContext()))
            domainUriPrefix = "https://chronofit.page.link/"
            androidParameters {
            }
        }
        return dynamicLink.uri.toString()
    }
}