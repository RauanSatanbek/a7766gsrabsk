package i.am.rauan.satanbek.a7766gsrabsk

import android.content.Context
import android.content.Intent
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.song_item.view.*

class SongsHolder(itemView: View, private val context: Context) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
    private var itemView: View = itemView

    val songItem = itemView.songItem
    val hr = itemView.hr
    var note = itemView.imageButtonNote
    var earphone = itemView.imageButtonEarPhone
    var mGifVisualizer = itemView.mGifVisualizer
    var download = itemView.imageButtonDownload
    var play = itemView.imageButtonPlay
    var pause = itemView.imageButtonPause

    var selectedItem = itemView.selectedItemBg
    var songTitle = itemView.song_title
    private var sharedStorage: Storage = Storage(context)
    var song: Song = Song(0, 0, "", "", "", "", false, "", "")

    var isPlaying = false

    init {
        itemView.setOnClickListener(this)
        play.setOnClickListener(this)
        pause.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.imageButtonPlay -> {
                if(sharedStorage.getCurrentSong() != null && song.ID != sharedStorage.getCurrentSong().ID) {
                    sharedStorage.setResumePosition(0)
                }

                sharedStorage.setCurrentSong(song)

                var intent = Intent(sharedStorage.updateUIReceiverAction)
                intent.putExtra(sharedStorage.updateUIPlay, true)
                context.sendBroadcast(intent)

                playing()
                Log.d("main", "Adapter clicked to item with position = $adapterPosition")
            }

            R.id.imageButtonPause -> {
                isPlaying = false

                var intent = Intent(sharedStorage.updateUIReceiverAction)
                intent.putExtra(sharedStorage.updateUIPause, true)
                context.sendBroadcast(intent)

                off()

                selectedItem.visibility = View.VISIBLE
            }
        }
    }

    fun playing() {
        note.visibility = View.INVISIBLE
//        earphone.visibility = View.VISIBLE
        mGifVisualizer.visibility = View.VISIBLE

        play.visibility = View.GONE
        play.isEnabled = false

        pause.visibility = View.VISIBLE
        pause.isEnabled = true

        selectedItem.visibility = View.VISIBLE

        isPlaying = true
    }

    fun resume() {
        note.visibility = View.VISIBLE
//        earphone.visibility = View.VISIBLE
        mGifVisualizer.visibility = View.INVISIBLE

        play.visibility = View.VISIBLE
        play.isEnabled = true

        pause.visibility = View.GONE
        pause.isEnabled = false

        selectedItem.visibility = View.VISIBLE

        isPlaying = false
    }

    fun setAudioSessionID(sessionID: Int) {
    }

    fun off() {
        note.visibility = View.VISIBLE
//        earphone.visibility = View.INVISIBLE
        mGifVisualizer.visibility = View.INVISIBLE

        play.visibility = View.VISIBLE
        play.isEnabled = true

        pause.visibility = View.GONE
        pause.isEnabled = false

        selectedItem.visibility = View.INVISIBLE

        isPlaying = false

    }

    fun pause() {
        off()

        selectedItem.visibility = View.VISIBLE
    }

    fun setData(song: Song) {
        off()

        songTitle.text = song.title

        if (this.song.ID == sharedStorage.getCurrentSong().ID) {
            if (sharedStorage.getPause()) {
                resume()
            } else {
                playing()
            }
        }
    }

    fun setColorMode() {
        when(sharedStorage.getColorMode()) {
            sharedStorage.DARK_MODE -> {
                songItem.setBackgroundColor(context.resources.getColor(R.color.dark_blue))
                selectedItem.setBackgroundColor(context.resources.getColor(R.color.white_dark_blue))
                hr.setBackgroundColor(context.resources.getColor(R.color.hr))
                songTitle.setTextColor(context.resources.getColor(R.color.white))
            }

            sharedStorage.LIGHT_MODE -> {
                songItem.setBackgroundColor(context.resources.getColor(R.color.invert_dark_blue))
                selectedItem.setBackgroundColor(context.resources.getColor(R.color.invert_white_dark_blue))
                hr.setBackgroundColor(context.resources.getColor(R.color.invert_hr))
                songTitle.setTextColor(context.resources.getColor(R.color.black))
            }
        }
    }

}