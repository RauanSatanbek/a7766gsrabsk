package i.am.rauan.satanbek.a7766gsrabsk

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup

class RecyclerAdapter(private val songs: ArrayList<Song>, private val context: Context): RecyclerView.Adapter<SongsHolder>() {

    var holders: ArrayList<SongsHolder> = ArrayList()
    private var sharedStorage: Storage = Storage(context)

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): SongsHolder {
        return SongsHolder(LayoutInflater.from(context).inflate(R.layout.song_item, p0, false), context)
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    override fun onBindViewHolder(holder: SongsHolder, p1: Int) {
        holder.setColorMode()
        holder.song = songs[p1]
        holder.setData(songs[p1])

        if (holder in holders) {
            Log.d("main", "holders exits: ${holders.size}")
        } else {
            holders.add(holder)
            Log.d("main", "holders: ${holders.size}")
        }

    }

    fun setColorModeForHolders() {
        for (holder in holders) {
            holder.setColorMode()
        }
    }

    fun pause() {
        for (i in holders) {
            if (i.isPlaying) {
                i.pause()
            }
        }
    }

    fun play() {
        offAll()

        for (i in holders) {
            if (i.song.ID == sharedStorage.getCurrentSong().ID) {
                i.playing()
            }
        }
    }

    fun resume() {
        offAll()

        for (i in holders) {
            if (i.song.ID == sharedStorage.getCurrentSong().ID) {
                i.resume()
            }
        }
    }

    fun offAll() {
        for (i in holders) {
            if (i.song.ID != sharedStorage.getCurrentSong().ID) {
                i.off()
            }
        }
    }

    fun next() {
        for (i in holders) {
            if (i.isPlaying) {
                i.off()
            }
            if (i.song.ID == sharedStorage.getCurrentSong().ID) {
                i.playing()
            }
        }
    }

    fun previous() {
        for (i in holders) {
            if (i.isPlaying) {
                i.off()
            }
            if (i.song.ID == sharedStorage.getCurrentSong().ID) {
                i.playing()
            }
        }
    }

    fun setAudioSessionID(audioSessionId: Int) {
        for (i in holders) {
            if (i.isPlaying) {
                i.setAudioSessionID(audioSessionId)
            }
        }
    }
}
