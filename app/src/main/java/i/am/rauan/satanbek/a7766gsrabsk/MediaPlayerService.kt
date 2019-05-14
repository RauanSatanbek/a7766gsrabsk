package i.am.rauan.satanbek.a7766gsrabsk

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.media.MediaPlayer
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.media.AudioManager
import android.os.Binder
import android.util.Log
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.widget.Toast


class MediaPlayerService : Service(), MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener,
    MediaPlayer.OnBufferingUpdateListener, AudioManager.OnAudioFocusChangeListener {

    private var LOG_TAG: String = "MediaPlayer error"
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var audioManager: AudioManager

    // Path to the audio file
    private var mediaFile: String? = null

    // Binder given to clients
    private val iBinder = LocalBinder()

    // Handle incoming phone calls
    private var ongoingCall = false
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyManager: TelephonyManager? = null
    private var sharedStorage: Storage? = null
    private var songs: ArrayList<Song>? = null

    // Becoming noisy
    private var becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            //Pause media on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia()
            buildNotification(PlaybackStatus.PAUSED)
        }
    }

    private fun registerBecomingNoisyReceiver() {
        var intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(becomingNoisyReceiver, intentFilter)
    }

    // Play new song
    private var playNewSongReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(LOG_TAG, "playNewSongReceiver: onReceive")

            stopMedia()
            mediaPlayer?.reset()
            initMediaPlayer()
            playMedia()
        }
    }

    private fun registerPlayNewSongReceiver() {
        var intentFilter = IntentFilter(sharedStorage!!.playNewSongReceiverAction)
        registerReceiver(playNewSongReceiver, intentFilter)
    }


    override fun onCreate() {
        super.onCreate()

        // init storage
        sharedStorage = Storage(this)

        // load songs
        songs = sharedStorage?.loadSongs()

        // ACTION_AUDIO_BECOMING_NOISY -- change in audio outputs -- BroadcastReceiver
        registerBecomingNoisyReceiver()

        // Play new sog receiver
        registerPlayNewSongReceiver()
    }

    override fun onBind(intent: Intent): IBinder {
        return iBinder
    }

    override fun onBufferingUpdate(mp: MediaPlayer, percent: Int) {
        // Invoked indicating buffering status of
        // a media resource being streamed over the network.
    }

     override fun onCompletion(mp: MediaPlayer) {
        // Invoked when playback of a media source has completed.
         stopMedia()

         // Stop the service
         stopSelf()
    }

    // Handle errors
    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        // Invoked when there has been an error during an asynchronous operation.
        when(what) {
            MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK -> {
                Log.d(LOG_TAG, "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK: $extra")
            }
            MediaPlayer.MEDIA_ERROR_SERVER_DIED -> {
                Log.d(LOG_TAG, "MEDIA_ERROR_SERVER_DIED: $extra")
            }
            MediaPlayer.MEDIA_ERROR_UNKNOWN -> {
                Log.d(LOG_TAG, "MEDIA_ERROR_UNKNOWN: $extra")
            }
        }

        return false
    }

    override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        // Invoked to communicate some info.
        return false
    }

    override fun onPrepared(mp: MediaPlayer) {
        // Invoked when the media source is ready for playback.
//        playMedia()
    }

    override fun onSeekComplete(mp: MediaPlayer) {
        // Invoked indicating the completion of a seek operation.
    }

    override fun onAudioFocusChange(focusChange: Int) {
        // Invoked when the audio focus of the system is updated.
        Log.d(LOG_TAG, "AUDIO FOCUS: $focusChange")
        when(focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // resume playback
                if (!sharedStorage!!.getPause()) {
                    if (mediaPlayer == null) initMediaPlayer()
                    else if (!mediaPlayer!!.isPlaying) {
                        playMedia()

                        val local = Intent(sharedStorage?.updateUIReceiverAction)
                        local.putExtra("showPauseButton", true)
                        sendBroadcast(local)
                    }
                }

                mediaPlayer?.setVolume(1.0f, 1.0f)
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mediaPlayer!!.isPlaying) {
                    val local = Intent(sharedStorage?.updateUIReceiverAction)
                    local.putExtra("showPauseButton", true)
                    sendBroadcast(local)

                    sharedStorage?.setPause(true)
                    stopMedia()
                }

                mediaPlayer?.release()
                mediaPlayer = null
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lost ficus for a short time. but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer!!.isPlaying) mediaPlayer?.setVolume(0.1f, 0.1f)
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Lost ficus for a short time. but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer!!.isPlaying) {
                    mediaPlayer?.pause()

                    val local = Intent(sharedStorage?.updateUIReceiverAction)
                    local.putExtra("showPlayButton", true)
                    sendBroadcast(local)
                }
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        var result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // Focus gained
            return true
        }

        // Could not gain focus
        return false
    }

    private fun removeAudioFocus(): Boolean {
        if (audioManager == null) {
            return true
        }

        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocus(this)
    }

    inner class LocalBinder : Binder(), IBinder {
        val service: MediaPlayerService
            get() = this@MediaPlayerService
    }

    private fun initMediaPlayer() {
        var song = songs?.get(sharedStorage?.getCurrentSongIndex()!!)

        mediaPlayer = MediaPlayer.create(this, song?.id!!)

        // Set up MediaPlayer event listener
        mediaPlayer?.setOnCompletionListener(this)
        mediaPlayer?.setOnErrorListener(this)
        mediaPlayer?.setOnPreparedListener(this)
        mediaPlayer?.setOnBufferingUpdateListener(this)
        mediaPlayer?.setOnSeekCompleteListener(this)
        mediaPlayer?.setOnInfoListener(this)

        mediaPlayer?.setOnCompletionListener {
            when (sharedStorage?.getRefreshMode()) {
                0 -> {
                    val local = Intent(sharedStorage?.updateUIReceiverAction)
                    local.putExtra("showPlayButton", true)
                    sendBroadcast(local)
                }
                1 -> {
                    playMedia()

                }
            }


            // Update Seek bar progress
            val local = Intent()
            local.action = sharedStorage?.updateUIReceiverAction
            local.putExtra("seek_bar_progress", true)
            local.putExtra("seek_bar_progress_value", 0)
            local.putExtra("tv_duration", mediaPlayer?.currentDurationInMinutes)
            this.sendBroadcast(local)

            Toast.makeText(this, "END", Toast.LENGTH_LONG)
        }
    }


    fun playMedia() {
        if (mediaPlayer == null) restoreMedia()
        Log.d(LOG_TAG, "playMedia")

        if (!mediaPlayer!!.isPlaying) {

            mediaPlayer?.start()
            sharedStorage?.setPause(false)

            sendDuration()
        }
    }

    private fun restoreMedia() {
        if (mediaPlayer != null) mediaPlayer!!.release()

        if (!requestAudioFocus()) {
            // Could not gain focus
            stopSelf()
        }

        initMediaPlayer()
    }

    fun stopMedia() {
        if (mediaPlayer == null) restoreMedia()
        Log.d(LOG_TAG, "stopMedia $mediaPlayer")

        if (mediaPlayer == null) return
        if (mediaPlayer!!.isPlaying) {
            sharedStorage!!.setResumePosition(getCurrentSeconds())

            mediaPlayer?.stop()
            sharedStorage?.setPause(true)
        }
    }

    fun pauseMedia() {
        if (mediaPlayer == null) restoreMedia()
        Log.d(LOG_TAG, "pauseMedia")

        if (mediaPlayer!!.isPlaying) {
            mediaPlayer?.pause()
            sharedStorage?.setResumePosition(mediaPlayer!!.currentPosition)
            sharedStorage?.setPause(true)
        }
    }

    fun resumeMedia() {
        if (mediaPlayer == null) restoreMedia()

        Log.d(LOG_TAG, "resumeMedia")
        if (!mediaPlayer!!.isPlaying) {
            mediaPlayer?.seekTo(sharedStorage!!.getResumePosition())
            mediaPlayer?.start()
            sharedStorage?.setPause(false)

            sendDuration()
        }
    }

    private fun sendDuration() {
        val local = Intent(sharedStorage?.updateUIReceiverAction)
        local.putExtra("tv_duration", mediaPlayer?.durationInMinutes)
        local.putExtra("media_playing", true)
        local.putExtra("seek_bar_progress", true)
        local.putExtra("seek_bar_progress_value", getCurrentSeconds())
        sendBroadcast(local)
    }

    fun seekTo(to: Int) {
        mediaPlayer?.seekTo(to)
    }

    fun getSeconds(): Int {
        if (mediaPlayer != null)
            return mediaPlayer!!.seconds

        return 0
    }
    fun getCurrentSeconds(): Int {
        if (mediaPlayer != null)
            return mediaPlayer!!.currentSeconds

        return 0
    }
    fun getCurrentDurationInMinutes(): String {
        if (mediaPlayer != null)
            return mediaPlayer!!.currentDurationInMinutes

        return "0.00"
    }
    fun getDurationInMinutes(): String {
        if (mediaPlayer != null)
            return mediaPlayer!!.durationInMinutes

        return "0.00"
    }

    fun getAudioSessionId(): Int {
        if (mediaPlayer != null)
            return mediaPlayer!!.audioSessionId

        return -1
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        //An audio file is passed to the service through putExtra();
        mediaFile = intent?.extras?.getString("media")
        intent?.extras?.getInt("song_index")?.let { sharedStorage?.setCurrentSongIndex(it) }

        // Request Song focus
        if (!requestAudioFocus()) {
            // Could not gain focus
            stopSelf()
        }

        if (mediaFile != null && mediaFile != "") initMediaPlayer()

        Log.d(LOG_TAG, "onStartCommand: $mediaFile")
        return super.onStartCommand(intent, flags, startId)
    }



    private fun buildNotification(paused: PlaybackStatus) {

    }

    // Handle incoming calls
    private fun callStateListener() {
        // get the telephony manager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        // start listening for PhoneState changes

        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                when (state) {
                    // if at least one call or phone ringing
                    // pause the MediaPlayer
                    TelephonyManager.CALL_STATE_OFFHOOK, TelephonyManager.CALL_STATE_RINGING -> {
                        if (mediaPlayer != null) {
                            pauseMedia()
                            ongoingCall = true
                        }
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        // Phone idle, start playing

                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false
                                resumeMedia()
                            }
                        }
                    }
                }
                super.onCallStateChanged(state, phoneNumber)
            }
        }

        // Register the listener with the telephone manager
        // Listen for changes to the device call state.
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }
    override fun onDestroy() {
        super.onDestroy()

        if (mediaPlayer != null) {
            stopMedia()
            mediaPlayer?.release()
        }

        removeAudioFocus()
    }
}


val MediaPlayer.seconds:Int
    get() {
        return this.duration / 1000
    }

val MediaPlayer.currentSeconds:Int
    get() {
        return this.currentPosition / 1000
    }

val MediaPlayer.durationInMinutes:String
    get() {
        var seconds = this.seconds % 60
        return "${this.seconds / 60}.${if (seconds < 10) "0$seconds" else seconds }"
    }


val MediaPlayer.currentDurationInMinutes:String
    get() {
        var seconds = this.currentSeconds % 60
        return "${this.currentSeconds / 60}.${if (seconds < 10) "0$seconds" else seconds }"
    }
