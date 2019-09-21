package de.unruh.beatingheart

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.annotation.SuppressLint
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
 */

class AnimatorLooper(vararg animations: Animator, delay: Long = 1000) {
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

    var delay = delay
        set(value) {
            animSet.startDelay = value
            field = value
        }

    private val animSet = AnimatorSet().apply {
        playSequentially(*animations)
        doOnEnd {
            startDelay = this@AnimatorLooper.delay
            start()
        }
    }
}

class MainActivity : AppCompatActivity() {
//    private var heartDelay = 3000L
//    private val heartBeatDuration = 200L

    override fun onDestroy() {
        super.onDestroy()
        audio.release()
    }

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

    private val looper by lazy { AnimatorLooper(heartAnimator) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        looper.start()

        heart.setOnClickListener {
            looper.delay = Random.nextLong(0, 2000)
//            looper.delay = 0L
        }

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE
    }

    override fun onPause() {
        super.onPause()
        looper.pause()
    }

    override fun onResume() {
        super.onResume()
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
