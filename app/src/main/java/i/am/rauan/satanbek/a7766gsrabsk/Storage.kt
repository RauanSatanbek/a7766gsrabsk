package i.am.rauan.satanbek.a7766gsrabsk

import android.content.Context
import android.content.SharedPreferences
import android.provider.MediaStore.Audio
import com.google.gson.reflect.TypeToken
import com.google.gson.Gson
import android.Manifest.permission_group.STORAGE
import android.R.id.edit



class Storage(context: Context) {
    val updateUIReceiverAction = "i.am.rauan.satanbek.a7766gsrabsk.gani.matebayev.UPDATE_UI"
    val playNewSongReceiverAction = "i.am.rauan.satanbek.a7766gsrabsk.gani.matebayev.PLAY_NEW_SONG"
    private val STORAGE: String = "i.am.rauan.satanbek.a7766gsrabsk.gani.matebayev.STORAGE"
    private var preferences: SharedPreferences? = null
    private var context: Context? = null

    init {
        this.context = context
    }

    private var shuffleMode = "SHUFFLE_MODE"
    private var refreshMode = "REFRESH_MODE"
    private var mediaPause = "PAUSE"
    private var resumePosition = "RESUME_POSITION"
    private var colorMode = "COLOR_MODE"
    var DARK_MODE = 0
    var LIGHT_MODE = 1

    fun setSuffleMode(mode: Int) {
        var editor: SharedPreferences.Editor = getEditor()

        editor.putInt(shuffleMode, mode)
        editor.apply()
    }

    fun getShuffleMode(): Int {
        return getPreferences()!!.getInt(shuffleMode, 0)
    }

    fun setRefreshMode(mode: Int) {
        var editor: SharedPreferences.Editor = getEditor()

        editor.putInt(refreshMode, mode)
        editor.apply()
    }

    fun getRefreshMode(): Int {
        return getPreferences()!!.getInt(refreshMode, 0)
    }

    fun setPause(mode: Boolean) {
        var editor: SharedPreferences.Editor = getEditor()

        editor.putBoolean(mediaPause, mode)
        editor.apply()
    }

    fun getPause(): Boolean {
        return getPreferences()!!.getBoolean(mediaPause, true)
    }

    fun setResumePosition(position: Int) {
        var editor: SharedPreferences.Editor = getEditor()

        editor.putInt(resumePosition, position)
        editor.apply()
    }

    fun getResumePosition(): Int {
        return getPreferences()!!.getInt(resumePosition, 0)
    }


    fun setDarkMode() {
        var editor: SharedPreferences.Editor = getEditor()

        editor.putInt(colorMode, DARK_MODE)
        editor.apply()
    }

    fun setLightMode() {
        var editor: SharedPreferences.Editor = getEditor()

        editor.putInt(colorMode, LIGHT_MODE)
        editor.apply()
    }

    fun getColorMode(): Int {
        return getPreferences()!!.getInt(colorMode, DARK_MODE)
    }

    fun storeSongs(arrayList: ArrayList<Song>) {
        val editor = getEditor()
        val gson = Gson()
        val json = gson.toJson(arrayList)
        editor.putString("audioArrayList", json)
        editor.apply()
    }

    fun loadSongs(): ArrayList<Song> {
        preferences = getPreferences()
        val gson = Gson()
        val json = preferences?.getString("audioArrayList", null)
        val type = object : TypeToken<ArrayList<Song>>() {}.type

        return gson.fromJson(json, type)
    }

    fun setCurrentSongIndex(position: Int) {
        var editor: SharedPreferences.Editor = getEditor()

        editor.putInt("currentSongIndex", position)
        editor.apply()
    }

    fun getCurrentSongIndex(): Int {
        return getPreferences()!!.getInt("currentSongIndex", 0)
    }

    private fun getPreferences(): SharedPreferences? {
        return context?.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)
    }

    private fun getEditor(): SharedPreferences.Editor {
        preferences = getPreferences()
        return preferences!!.edit()
    }
}