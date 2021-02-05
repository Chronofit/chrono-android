package ca.chronofit.chrono.circuit

import android.annotation.SuppressLint
import android.app.Activity
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import ca.chronofit.chrono.R
import ca.chronofit.chrono.databinding.ActivityCircuitTimerBinding
import ca.chronofit.chrono.util.BaseActivity
import ca.chronofit.chrono.util.constants.Constants
import ca.chronofit.chrono.util.constants.Events
import ca.chronofit.chrono.util.objects.CircuitObject
import ca.chronofit.chrono.util.objects.PreferenceManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.dialog_alert.view.*
import kotlin.math.roundToInt

class CircuitTimerActivity : BaseActivity() {
    private lateinit var bind: ActivityCircuitTimerBinding

    enum class TimerState { INIT, RUNNING, PAUSED }
    enum class RunningState { READY, INIT, WORK, REST }

    private lateinit var mInterstitialAd: InterstitialAd

    private val mInterstitialAdUnitId: String by lazy {
//        "ca-app-pub-5592526048202421/8639444717" // ACTUAL
        "ca-app-pub-3940256099942544/1033173712" // TEST
    }

    private val celebrateTimeout = 2500L // Timeout delay
    private var getReadyTime: Int = 5
    private var audioPrompts: Boolean = true
    private var skipLastRest: Boolean = false
    private var adsEnabled: Boolean? = false

    private lateinit var countdown: CountDownTimer
    private var secondsLeft: Float = 0.0f

    private var timerState: TimerState = TimerState.INIT
    private var runningState: RunningState = RunningState.INIT

    private lateinit var circuit: CircuitObject
    private var currentSet: Int = 0
    private var sets: Int = 0
    private var timeRest: Int = 0
    private var timeWork: Int = 0
    private var criticalSeconds: Int = 0

    private var tone: ToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_circuit_timer)
        bind = DataBindingUtil.setContentView(this, R.layout.activity_circuit_timer)
        // Possibly show loading screen

        circuit = GsonBuilder().create()
            .fromJson(intent.getStringExtra("circuitObject"), CircuitObject::class.java)
        getReadyTime = intent.getIntExtra("readyTime", 5)
        audioPrompts = intent.getBooleanExtra("audioPrompts", true)
        skipLastRest = intent.getBooleanExtra("lastRest", false)

        // Initialize stuff
        updateButtonUI()
        updateRestUI()
        loadAds()

        bind.startButton.setOnClickListener {
            FirebaseAnalytics.getInstance(this).logEvent(Events.CIRCUIT_STARTED, Bundle())
            start()
        }

        bind.pauseButton.setOnClickListener {
            FirebaseAnalytics.getInstance(this).logEvent(Events.CIRCUIT_PAUSED, Bundle())
            pause()
        }

        bind.resumeButton.setOnClickListener {
            FirebaseAnalytics.getInstance(this).logEvent(Events.CIRCUIT_RESTARTED, Bundle())
            resume()
        }

        bind.countdown.setOnClickListener {
            if (timerState == TimerState.RUNNING && runningState != RunningState.READY) {
                FirebaseAnalytics.getInstance(this).logEvent(Events.CIRCUIT_PAUSED, Bundle())
                pause()
            } else if (timerState == TimerState.PAUSED) {
                FirebaseAnalytics.getInstance(this).logEvent(Events.CIRCUIT_RESTARTED, Bundle())
                resume()
            }
        }

        bind.stopButton.setOnClickListener {
            countdown.cancel()
            timerState = TimerState.INIT
            runningState = RunningState.INIT
            updateButtonUI()
            updateRestUI()
        }

        bind.closeButton.setOnClickListener {
            FirebaseAnalytics.getInstance(this).logEvent(Events.CIRCUIT_EXITED, Bundle())
            // Make sure that the timer is shut down
            if (timerState != TimerState.INIT) {
                countdown.cancel()
            }
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun startTimer(seconds: Int, wasPaused: Boolean) {
        var time = seconds.toFloat() * 1000 + 250
        if (wasPaused) {
            time = secondsLeft * 1000 + 250
        }
        countdown = object : CountDownTimer(time.toLong(), 250) {
            override fun onTick(p0: Long) {
                if ((p0.toFloat().roundToInt() / 1000.0f) != secondsLeft) {
                    secondsLeft = (p0.toFloat() / 1000.0f).roundToInt().toFloat()
                    updateTimerUI()
//                    createNotification(secondsLeft)
                }
            }

            override fun onFinish() {
                if (currentSet != 0) {
                    when (runningState) {
                        RunningState.READY -> {
                            workout()
                        }
                        RunningState.WORK -> {
                            if (currentSet == 1 && skipLastRest) {
                                celebrate()
                            } else {
                                rest()
                            }
                        }
                        RunningState.REST -> {
                            workout()
                        }
                        else -> celebrate()
                    }
                } else {
                    celebrate()
                }
            }
        }.start()
    }

    private fun loadAds() {
        // Initialize and load up the ad for later
        MobileAds.initialize(this)

        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd.adUnitId = mInterstitialAdUnitId
        mInterstitialAd.loadAd(AdRequest.Builder().build())
    }

    private fun celebrate() {
        // Load celebrate layout
        bind.mainLayout.visibility = View.GONE
        bind.celebrateLayout.visibility = View.VISIBLE

        FirebaseAnalytics.getInstance(this).logEvent(Events.CIRCUIT_COMPLETED, Bundle())
        // Wait 2.5 seconds before showing the finish prompt
        Handler(
            Looper.getMainLooper()
        ).postDelayed(
            {
                isDone()
            }, celebrateTimeout
        )
    }

    private fun isDone() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (PreferenceManager.get<Int>(Constants.NUM_COMPLETE) != null) {
            val increment = (PreferenceManager.get<Int>(Constants.NUM_COMPLETE)!! + 1)
            PreferenceManager.put(increment, Constants.NUM_COMPLETE)
        } else {
            PreferenceManager.put(1, Constants.NUM_COMPLETE)
        }

        val builder =
            MaterialAlertDialogBuilder(this, R.style.CustomMaterialDialog).create()
        val dialogView = layoutInflater.inflate(R.layout.dialog_alert, null)

        // Set Dialog Views
        dialogView.dialog_title.text = getString(R.string.circuit_complete)
        dialogView.dialog_subtitle.text = getString(R.string.circuit_complete_subtitle)
        dialogView.confirm.text = getString(R.string.circuit_complete_confirm)
        dialogView.cancel.text = getString(R.string.circuit_complete_cancel)

        dialogView.confirm.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent))
        dialogView.confirm.setTextColor(ContextCompat.getColor(this, R.color.white))

        // User wants to return to dashboard
        dialogView.confirm.setOnClickListener {
            builder.dismiss()
            setResult(Activity.RESULT_OK)
            finish()

            // Show the ad
            if (mInterstitialAd.isLoaded && adsEnabled!!) {
                mInterstitialAd.show()
            } else {
                Log.d("AD", "The interstitial wasn't loaded yet.")
            }
        }

        // If the user wants to run the circuit again
        dialogView.cancel.setOnClickListener {
            // Show the ad if it loaded
            if (mInterstitialAd.isLoaded && adsEnabled!!) {
                mInterstitialAd.adListener = object : AdListener() {
                    override fun onAdClosed() {
                        // Reload the circuit
                        super.onAdClosed()
                        builder.dismiss()

                        bind.celebrateLayout.visibility = View.GONE
                        bind.mainLayout.visibility = View.VISIBLE

                        timerState = TimerState.INIT
                        runningState = RunningState.INIT
                        updateButtonUI()
                        updateRestUI()
                    }
                }
                mInterstitialAd.show()
            } else {
                // Ad didn't load but restart the circuit
                Log.d("AD", "The interstitial wasn't loaded yet.")
                // Reload the circuit
                builder.dismiss()

                bind.celebrateLayout.visibility = View.GONE
                bind.mainLayout.visibility = View.VISIBLE

                timerState = TimerState.INIT
                runningState = RunningState.INIT
                updateButtonUI()
                updateRestUI()
            }
        }

        builder.setCancelable(false)
        builder.setCanceledOnTouchOutside(false)

        // Display the Dialog
        builder.setView(dialogView)
        builder.show()
    }

    private fun getReady() {
        runningState = RunningState.READY
        timerState = TimerState.RUNNING
        bind.initButtonLayout.visibility = View.GONE
        updateRestUI()
        startTimer(getReadyTime, false)
    }

    private fun workout() {
        if (audioPrompts) tone.startTone(ToneGenerator.TONE_DTMF_D, 750)
        runningState = RunningState.WORK
        updateButtonUI()
        updateRestUI()
        startTimer(timeWork, false)
    }

    private fun rest() {
        if (audioPrompts) tone.startTone(ToneGenerator.TONE_DTMF_2, 500)
        runningState = RunningState.REST
        updateRestUI()
        startTimer(timeRest, false)
        currentSet -= 1
    }

    private fun pause() {
        countdown.cancel()
        timerState = TimerState.PAUSED
        updateButtonUI()
    }

    private fun start() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        loadTimer(circuit)
        getReady()
    }

    private fun resume() {
        startTimer(secondsLeft.toInt(), true)
        timerState = TimerState.RUNNING
        updateRestUI()
        updateButtonUI()
    }

    // Update UI for every tick, possibly need to do more in the future
    fun updateTimerUI() {
        if (criticalSeconds != 0 && secondsLeft <= criticalSeconds && runningState == RunningState.WORK) {
            bind.mainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.stop_red))
            bind.countdown.setTextColor(ContextCompat.getColor(this, R.color.white))
            bind.currentSet.setTextColor(ContextCompat.getColor(this, R.color.white))
            bind.currentState.setTextColor(ContextCompat.getColor(this, R.color.white))
            bind.closeButton.setImageResource(R.drawable.ic_close_white)
        }
        if (timeRest > 5 && runningState == RunningState.REST && secondsLeft <= 5) {
            bind.currentState.text = getString(R.string.get_ready)
        }
        bind.countdown.text = (secondsLeft).toInt().toString()
    }

    private fun updateButtonUI() {
        when (timerState) {
            TimerState.INIT -> {
                bind.initButtonLayout.visibility = View.VISIBLE
                bind.runButtonLayout.visibility = View.GONE
                bind.pauseButtonLayout.visibility = View.GONE
                bind.countdown.text = "0"
            }
            TimerState.RUNNING -> {
                bind.initButtonLayout.visibility = View.GONE
                bind.runButtonLayout.visibility = View.VISIBLE
                bind.pauseButtonLayout.visibility = View.GONE
            }
            TimerState.PAUSED -> {
                bind.initButtonLayout.visibility = View.GONE
                bind.runButtonLayout.visibility = View.GONE
                bind.pauseButtonLayout.visibility = View.VISIBLE

                // Just for paused we put non-button UI stuff
                bind.mainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
                bind.countdown.setTextColor(ContextCompat.getColor(this, R.color.black))
                bind.currentSet.setTextColor(ContextCompat.getColor(this, R.color.dark_grey))
                bind.currentState.setTextColor(ContextCompat.getColor(this, R.color.dark_grey))
                bind.closeButton.setImageResource(R.drawable.ic_close_grey)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateRestUI() {
        when (runningState) {
            RunningState.READY -> {
                bind.currentState.text = getString(R.string.get_ready)
            }
            RunningState.INIT -> {
                bind.currentSet.text = getString(R.string.empty)
                bind.currentState.text = getString(R.string.lets_go)

                bind.closeButton.setImageResource(R.drawable.ic_close_grey)
            }
            RunningState.WORK -> {
                bind.currentState.text = getString(R.string.workout)
                bind.currentSet.text = "Set " + (sets - currentSet.toString().toInt() + 1)

                bind.mainLayout.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.beautiful_blue
                    )
                )
                bind.countdown.setTextColor(ContextCompat.getColor(this, R.color.white))
                bind.currentSet.setTextColor(ContextCompat.getColor(this, R.color.white))
                bind.currentState.setTextColor(ContextCompat.getColor(this, R.color.white))
                bind.closeButton.setImageResource(R.drawable.ic_close_white)
            }
            RunningState.REST -> {
                bind.currentState.text = getString(R.string.rest)

                bind.mainLayout.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.rest_yellow
                    )
                )
                bind.countdown.setTextColor(ContextCompat.getColor(this, R.color.white))
                bind.currentSet.setTextColor(ContextCompat.getColor(this, R.color.white))
                bind.currentState.setTextColor(ContextCompat.getColor(this, R.color.white))
                bind.closeButton.setImageResource(R.drawable.ic_close_white)
            }
        }
    }

    private fun loadTimer(circuit: CircuitObject) {
        sets = circuit.sets!!
        currentSet = sets
        timeRest = circuit.rest!!
        timeWork = circuit.work!!

        criticalSeconds = if (timeWork > 5) {
            5
        } else {
            0
        }
    }

//    private fun createNotification(time: Float) {
//        val builder =
//            NotificationCompat.Builder(this, getString(R.string.timer_notification_channel_id))
//                .setSmallIcon(R.drawable.ic_notification_logo)
//                .setContentTitle("Notification test")
//                .setContentText(time.toString())
//                .setPriority(NotificationCompat.PRIORITY_LOW)
//
//        with(NotificationManagerCompat.from(this)) {
//            //notificationId is a unique int for each notification that you must define
//            notify(2, builder.build())
//        }
//    }
}