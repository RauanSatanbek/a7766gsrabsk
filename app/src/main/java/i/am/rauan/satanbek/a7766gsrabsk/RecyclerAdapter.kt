package i.am.rauan.satanbek.a7766gsrabsk

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup

class RecyclerAdapter(private val songs: ArrayList<Song>, private val context: Context): RecyclerView.Adapter<SongsHolder>() {

    var holders: ArrayList<SongsHolder> = ArrayList()

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): SongsHolder {
        return SongsHolder(LayoutInflater.from(context).inflate(R.layout.song_item, p0, false), context)
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    override fun onBindViewHolder(holder: SongsHolder, p1: Int) {
        holder.setColorMode()
        holder.songTitle.text = songs[p1].title

        holders.add(holder)
    }

    fun setColorModeForHolders() {
        for (holder in holders) {
            holder.setColorMode()
        }
    }
}
