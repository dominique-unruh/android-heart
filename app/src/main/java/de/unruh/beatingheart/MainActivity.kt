package de.unruh.beatingheart

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import java.lang.RuntimeException
import kotlin.random.Random

/* TODO: Document media source:
 * Audio: http://soundbible.com/2162-Human-Heartbeat.html
 * Image: https://www.publicdomainpictures.net/en/view-image.php?image=234702&picture=fancy-heart
 *
 * TODO: require proximity sensor in manifest
 *
 * TODO: Adjust min/max heart rate nicely
 *
 * TODO: Slightly different background color (depends on proximity?)
 */

class AnimatorLooper(vararg animations: Animator, delay: Int = 1000) {
    fun start() {
        animSet.start()
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun pause() {
        animSet.pause()
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun resume() {
        animSet.resume()
    }

    var delay : Int = delay
        set(value) {
            if (!animSet.isRunning) // Changing this while isRunning confuses inner animator's delay for some reason
                animSet.startDelay = value.toLong()
            field = value
        }

    private val animSet = AnimatorSet().apply {
        playSequentially(*animations)
        doOnEnd {
            startDelay = this@AnimatorLooper.delay.toLong()
            start()
        }
    }
}

class MainActivity : AppCompatActivity(), SensorEventListener {
    override fun onSensorChanged(event: SensorEvent?) {
        if (event==null) return

        val proximity = event.values[0]
        val maxProximity = proximitySensor.maximumRange

        val heartRate = (proximity/maxProximity) * (maxHeartDelay-minHeartDelay) + minHeartDelay

        looper.delay = heartRate.toInt()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    private val maxHeartDelay by lazy { resources.getInteger(R.integer.maxHeartDelay) }
    private val minHeartDelay by lazy { resources.getInteger(R.integer.minHeartDelay) }

    override fun onDestroy() {
        super.onDestroy()
        audio.release()
    }

    private val sensorManager by lazy { getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    private val proximitySensor by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) }


    private val audio by lazy { MediaPlayer.create(this, R.raw.heartsound) }

    private val heart : ImageView by lazy { findViewById<ImageView>(R.id.heart) }

    private val heartAnimator by lazy {
        AnimatorInflater.loadAnimator(this, R.animator.heart).apply {
            setTarget(heart)
            doOnStart {
                try {
                    // TODO get rid of the click noise
                    // seekTo ensures that the audio restarts even if it hasn't finished playing yet
                    audio.seekTo(0)
                    audio.start()
//                    MediaPlayer.create(this@MainActivity, R.raw.heartsound).apply {
//                        start()
//                    }
                } catch (e : Throwable) {
                    incCount(e)
                }
            }
        }
    }

    private val looper by lazy { AnimatorLooper(heartAnimator, delay = maxHeartDelay) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        looper.start()

        heart.setOnClickListener {
            looper.delay = Random.nextInt(minHeartDelay, maxHeartDelay)
//            looper.delay = 0L
        }

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE

        incCount("min/max $minHeartDelay $maxHeartDelay ${R.integer.minHeartDelay} ${R.integer.animDelay1}")
    }

    override fun onPause() {
        super.onPause()
        looper.pause()
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()

        proximitySensor?.also { proximity ->
            sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL)
        }

        looper.delay = maxHeartDelay

        looper.resume()
    }

    // TODO remove
    private var count = 0
    // TODO remove
    @SuppressLint("SetTextI18n")
    fun incCount(msg: Any = "") {
        count ++
        val textView = findViewById<TextView>(R.id.textView2)
        val text = textView.text.lines()
        val text2 = text.takeLast(10) + "$count $msg"
        textView.text = text2.joinToString(separator = "\n")
    }
}
