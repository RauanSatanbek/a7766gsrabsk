package i.am.rauan.satanbek.a7766gsrabsk

import android.content.Context
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
    var download = itemView.imageButtonDownload
    var play = itemView.imageButtonPlay
    var pause = itemView.imageButtonPause

    var selectedItem = itemView.selectedItemBg
    var songTitle = itemView.song_title
    private var sharedStorage: Storage = Storage(context)

    init {
        itemView.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.songItem -> {
                playing()
                Log.d("main", "Adapter clicked to item with position = $adapterPosition")
            }
        }
    }

    private fun playing() {
        note.visibility = View.VISIBLE
        earphone.visibility = View.INVISIBLE

        play.visibility = View.GONE
        play.isEnabled = false

        pause.visibility = View.VISIBLE
        pause.isEnabled = true

        selectedItem.visibility = View.VISIBLE

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