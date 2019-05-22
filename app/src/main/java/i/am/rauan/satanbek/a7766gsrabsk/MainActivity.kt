@file:Suppress("IMPLICIT_CAST_TO_ANY", "DEPRECATION")

package i.am.rauan.satanbek.a7766gsrabsk

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.res.Configuration
import android.database.sqlite.SQLiteDatabase
import android.os.*
import android.provider.Settings
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDelegate
import android.support.v7.content.res.AppCompatResources
import android.support.v7.view.ContextThemeWrapper
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import com.google.firebase.firestore.FirebaseFirestore
import i.am.rauan.satanbek.a7766gsrabsk.db.PlaylistModel
import i.am.rauan.satanbek.a7766gsrabsk.db.SongModel
import i.am.rauan.satanbek.a7766gsrabsk.db.SqliteDatabase


@SuppressLint("ByteOrderMark")
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    var TAG: String = "main"

    //Create placeholder for user's consent to record_audio permission.
    //This will be used in handling callback
    private val MY_PERMISSIONS_RECORD_AUDIO: Int = 1

    private lateinit var runnable: Runnable
    private var handler: Handler = Handler()
    private var storage: FirebaseStorage? = null
    // Access a Cloud Firestore instance from your Activity
    val db = FirebaseFirestore.getInstance()

    private var sharedStorage: Storage? = null

    private var player: MediaPlayerService? = null
    private var serviceBound = false
    private var updateUIReciver: BroadcastReceiver? = null

    private var songs: ArrayList<SongModel> = ArrayList()

    private lateinit var drawer: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    private lateinit var linearLayoutManager: LinearLayoutManager
    private var recyclerAdapter: RecyclerAdapter? = null

    //AudioPlayer notification ID
    val NOTIFICATION_ID = 101

    var sqliteDatabase: SqliteDatabase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))

        toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        toggle.isDrawerIndicatorEnabled = false
        toggle.setToolbarNavigationClickListener {
            drawer_layout.openDrawer(GravityCompat.START)
        }
        toggle.setHomeAsUpIndicator(R.drawable.ic_humburger)

        nav_view.setNavigationItemSelectedListener(this)
        nav_view.itemIconTintList = null
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                // Set NavView bg
                nav_view.background = AppCompatResources.getDrawable(this, R.drawable.ic_nav_bg_vector)
            } catch (ex: Exception) {
                Log.d(TAG, "Unable to set vector background to NavigationView, API Level = ${Build.VERSION.SDK_INT}")
            }
        }

        // Init storage
        sharedStorage = Storage(this)
        // Init database
        sqliteDatabase = SqliteDatabase(this)

        createFolder("gani-matebayev")

        getPlaylistFromFirebase()
        createMenu()

        loadSongs()

        initUI()
        requestAudioPermissions(false)
        initMediaService()
    }

    override fun onStart() {
        super.onStart()

    }

    private fun getPlaylistFromFirebase() {
        db.collection("playlist").get()
            .addOnSuccessListener {result  ->
                for (doc in result) {

                    var playlist = PlaylistModel(0, doc.id, doc.data.get("name") as String, doc.data.get("image") as String, Integer.parseInt(doc.data.get("count").toString()), "")

                   var success = sqliteDatabase?.addSingerPlaylist(playlist)
                    Log.d(TAG, "GET PLAYLIST: $success ${playlist}")
                }

                createMenu()
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ${exception}")
            }
    }

    private fun createMenu() {
        var menu = nav_view.menu
        menu.clear()

        var subMenu = menu.addSubMenu("")
        subMenu.add(0, MENU_ID_FAVORITE, Menu.FIRST, "Таңдаулы").setIcon(R.drawable.ic_star)

        if (sqliteDatabase != null) {
            sqliteDatabase?.getAllOwnPlaylist()!!.forEach {
                subMenu.add(0, it.id!!, Menu.FIRST, it.name).setIcon(R.drawable.ic_menu_headphone)
            }
        }

//        subMenu.add(0, Menu.FIRST + 1, Menu.FIRST, "Менің жинағым").setIcon(R.drawable.ic_menu_headphone)

        subMenu.add(0, 0, Menu.FIRST, "").isEnabled = false

        subMenu.add(0, MENU_ID_SONGS, Menu.FIRST, "Барлық әндер").setIcon(R.drawable.ic_sign_note_1)
        if (sqliteDatabase != null) {
            Log.d(TAG, "SINGER Plalist, ${sqliteDatabase?.getAllSingerPlaylist()!!}")

            sqliteDatabase?.getAllSingerPlaylist()!!.forEach {
                subMenu.add(0, it.id!!, Menu.FIRST, it.name).setIcon(R.drawable.ic_album)
            }
        }
//        subMenu.add(0, Menu.FIRST + 3, Menu.FIRST, "Ауылды аңсау").setIcon(R.drawable.ic_album)
//        subMenu.add(0, Menu.FIRST + 4, Menu.FIRST, "Шырқашы жүрек").setIcon(R.drawable.ic_album)

        subMenu.add(0, MENU_ID_LOADED, Menu.FIRST, "Сақтаулы").setIcon(R.drawable.ic_menu_loaded)
//        subMenu.add(0, MENU_ID_ADD, Menu.FIRST, "Жинақ ашу").setIcon(R.drawable.ic_add)

        subMenu.add(0, 0, Menu.FIRST, "").isEnabled = false
        subMenu.add(0, 0, Menu.FIRST, "").isEnabled = false

        nav_view.invalidate()



//        if (sqliteDatabase != null) {
//            sqliteDatabase!!.addSingerPlaylist(PlaylistModel(0, "Шырқашы жүрек", "null", "null"))
//            sqliteDatabase!!.addOwnPlaylist(PlaylistModel(0, "Best of Gani", "null", "null"))
//
//            createMenu()
//        }
    }

    override fun onRestart() {
        super.onRestart()

        var notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
    // Первоначальная настройка UI элементов.
    private fun initUI() {
        setColorMode()

        mediaPlayerContainer.visibility = View.GONE

        // Обнавляем UI элементы, в зависимости от recyclerView, Service
        val updateUIFilter = IntentFilter()
        updateUIFilter.addAction(sharedStorage?.updateUIReceiverAction)
        updateUIReciver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                if (intent.getStringExtra("tv_duration") != "" && intent.getStringExtra("tv_duration") != null) {
                    Log.d(TAG, "onReceive tv_duration = ${intent.getStringExtra("tv_duration")}")
                    tv_duration.text = intent.getStringExtra("tv_duration")
                }

                if(intent.getBooleanExtra("media_playing", false)) {
                    activateVisualizer()
                    activateSeekBar()
                    showPauseButton()
                }

                if (intent.getBooleanExtra("showPlayButton", false)) {
                    showPlayButton()
                }
                if (intent.getBooleanExtra("showPauseButton", false)) {
                    showPauseButton()
                }

                if(intent.getBooleanExtra("seek_bar_progress", false)) {
                    seek_bar.progress = intent.getIntExtra("seek_bar_progress_value", 0)
                }
                if(intent.getBooleanExtra("player_stop", false)) {
                    handler.removeCallbacks(runnable)
                }

                // PLAY
                if (intent.getBooleanExtra(sharedStorage?.updateUIPlay, false)) {
                    mediaPlayerContainer.visibility = View.VISIBLE
                    recyclerAdapter!!.play()

                    val broadcastIntent = Intent(sharedStorage!!.playNewSongReceiverAction)
                    broadcastIntent.putExtra("play", true)
                    sendBroadcast(broadcastIntent)
                }

                // PAUSE
                if (intent.getBooleanExtra(sharedStorage?.updateUIPause, false)) {
                    recyclerAdapter!!.pause()
                    Pause()
                }

                // RESUME
                if (intent.getBooleanExtra(sharedStorage?.updateUIResume, false)) {
                    Play()
                }

                // STOP
                if (intent.getBooleanExtra(sharedStorage?.updateUIStop, false)) {

                }

                // NEXT
                if (intent.getBooleanExtra(sharedStorage?.updateUISkipToNext, false)) {
                    recyclerAdapter!!.selected()
                    Next()
                }

                // PREVIOUS
                if (intent.getBooleanExtra(sharedStorage?.updateUISkipToPrevious, false)) {
                    recyclerAdapter!!.selected()
                    Previous()
                }

                // PLAY Update just UI
                if (intent.getBooleanExtra(sharedStorage?.updateUIPlayUpdate, false)) {
                    showPauseButton()
                    activateSeekBar()
                    activateVisualizer()

                    recyclerAdapter?.play()
                }

                // PAUSE Update just UI
                if (intent.getBooleanExtra(sharedStorage?.updateUIPauseUpdate, false)) {
                    showPlayButton()

                    recyclerAdapter?.pause()
                }
            }
        }

        registerReceiver(updateUIReciver, updateUIFilter)

        // PLAY
        playBtn.setOnClickListener {
            Play()
        }

        // PAUSE
        pauseBtn.setOnClickListener{
            Pause()
        }

        // NEXT
        nextBtn.setOnClickListener {
            Next()
        }

        // PREVIOUS
        prevBtn.setOnClickListener {
            Previous()
        }

        // Seek bar change listener
        seek_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                if (b) {
                    player?.seekTo(i * 1000)
                    sharedStorage!!.setResumePosition(i)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })

        refreshBtn.setOnClickListener{
            showRefreshOne()
        }
        refreshOneBtn.setOnClickListener{
            showRefresh()
        }
        shuffleBtn.setOnClickListener{
            showShuffleActive()
        }
        shuffleBtnActive.setOnClickListener{
            showShuffle()
        }


        when(sharedStorage!!.getRefreshMode()) {
            0 -> showRefresh()
            1 -> showRefreshOne()
        }

        when(sharedStorage!!.getShuffleMode()) {
            0 -> showShuffle()
            1 -> showShuffleActive()
        }

//        createFolder("GaniMatebayev")

    }

    // Set color mode to DARK or LIGHT
    private fun setColorMode() {
        when(sharedStorage?.getColorMode()) {
            sharedStorage?.DARK_MODE -> {
                MainContainer.setBackgroundColor(resources.getColor(R.color.dark_blue))

                mediaPlayerContainer.background = AppCompatResources.getDrawable(this, R.drawable.media_player_bg)
                pauseBtn.background = AppCompatResources.getDrawable(this, R.drawable.fol_verctor)
                toolbar.background = AppCompatResources.getDrawable(this, R.drawable.toolbar_background)

                refreshBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_refresh))
                refreshOneBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_refresh_one))

                prevBtn.background = AppCompatResources.getDrawable(this, R.drawable.rounded_btn)
                prevBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_prev))

                playBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_play))
                pauseBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_pause))

                nextBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_next))
                nextBtn.background = AppCompatResources.getDrawable(this, R.drawable.rounded_btn)

                shuffleBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_shuffle))
                shuffleBtnActive.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_shuffle_active))

            }

            sharedStorage?.LIGHT_MODE -> {
                MainContainer.setBackgroundColor(resources.getColor(R.color.invert_dark_blue))

                mediaPlayerContainer.background = AppCompatResources.getDrawable(this, R.drawable.invert_media_player_bg)
                pauseBtn.background = AppCompatResources.getDrawable(this, R.drawable.fol_verctor_invert)
                toolbar.background = AppCompatResources.getDrawable(this, R.drawable.invert_toolbar_background)

                refreshBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_refresh_invert))
                refreshOneBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_refresh_one_invert))

                prevBtn.background = AppCompatResources.getDrawable(this, R.drawable.rounded_btn_invert)
                prevBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_prev_invert))

                playBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_play))
                pauseBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_pause_invert))

                nextBtn.background = AppCompatResources.getDrawable(this, R.drawable.rounded_btn_invert)
                nextBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_next_invert))

                shuffleBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_shuffle_invert))
                shuffleBtnActive.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_shuffle_active_invert))

//                mVisualizer.setBackgroundColor(resources.getColor(R.color.dark_blue))
            }
        }

        if (recyclerAdapter != null) {
            recyclerAdapter?.setColorModeForHolders()
        } else {
            loadSongs()
            recyclerAdapter?.setColorModeForHolders()
        }
    }

    private fun showPauseButton() {
        mediaPlayerContainer.visibility = View.VISIBLE
        pauseBtn.visibility = View.VISIBLE
        pauseBtn.isEnabled = true

        playBtn.visibility = View.INVISIBLE
        playBtn.isEnabled = false
    }

    private fun showPlayButton() {
        mediaPlayerContainer.visibility = View.VISIBLE
        pauseBtn.visibility = View.INVISIBLE
        pauseBtn.isEnabled = false

        playBtn.visibility = View.VISIBLE
        playBtn.isEnabled = true
    }

    private fun showShuffle() {
        shuffleBtn.visibility = View.VISIBLE
        shuffleBtn.isEnabled = true
        sharedStorage!!.setSuffleMode(0)

        shuffleBtnActive.visibility = View.GONE
        shuffleBtnActive.isEnabled = false
    }

    private fun showShuffleActive() {
        shuffleBtnActive.visibility = View.VISIBLE
        shuffleBtnActive.isEnabled = true
        sharedStorage!!.setSuffleMode(1)

        shuffleBtn.visibility = View.GONE
        shuffleBtn.isEnabled = false
    }

    private fun showRefresh() {
        refreshBtn.visibility = View.VISIBLE
        refreshBtn.isEnabled = true
        sharedStorage!!.setRefreshMode(0)


        refreshOneBtn.visibility = View.GONE
        refreshOneBtn.isEnabled = false
    }

    private fun showRefreshOne() {
        refreshBtn.visibility = View.GONE
        refreshBtn.isEnabled = false


        refreshOneBtn.visibility = View.VISIBLE
        refreshOneBtn.isEnabled = true
        sharedStorage!!.setRefreshMode(1)
    }

    private fun activateVisualizer () {
        //get the AudioSessionId from your MediaPlayer and pass it to the visualizer
        try {
            requestAudioPermissions(false)
            val audioSessionId = player!!.getAudioSessionId()
            if (audioSessionId != -1)
                mVisualizer.setAudioSessionId(audioSessionId)

//                recyclerAdapter?.setAudioSessionID(audioSessionId)

        } catch (e: Exception) {
            Log.e(TAG, "setUpMediaView: mVisualizer.setAudioSessionId - ${e.toString()}")
        }
    }

    private fun activateSeekBar() {
        seek_bar.max = player!!.getSeconds()

        runnable = Runnable {
            seek_bar.progress = player!!.getCurrentSeconds()
            tv_pass.text = player!!.getCurrentDurationInMinutes()
//            tv_duration.text = player!!.getDurationInMinutes()

            handler.postDelayed(runnable, 1000)
        }

        handler.postDelayed(runnable, 1000)
    }

    // Request for AUDIO_RECORD Permission
    fun requestAudioPermissions(play: Boolean) {
        //If permission is granted, then go to play audio
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED && play) {

            Play()
        }

        else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {

            //When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "Өлең тыңдау үшін, RECORD_AUDIO - ны қолдануға рұқсатыңызды беріңіз!", Toast.LENGTH_LONG).show()

                //Give user option to still opt-in the permissions
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    MY_PERMISSIONS_RECORD_AUDIO)

            } else {
                // Show user dialog to grant permission to record audio
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    MY_PERMISSIONS_RECORD_AUDIO)
            }
        }
    }

    private fun loadSongs() {
        db.collection("songs").get()
            .addOnSuccessListener {result  ->
                for (doc in result) {
                    Log.d(TAG, "GET DOC: ${doc.id} ${doc.data}")

                    var sm = doc.data
                    var song = SongModel("", doc.id, sm.get("name") as String?, sm.get("playlist") as String?, sm.get("file") as String?, sm.get("text") as String?, "", sm.get("url") as String?)
                    songs.add(song)
                    sqliteDatabase?.addSong(song)
                }

                sharedStorage?.storeSongs(songs)
                initRecyclerView(INIT_RECYCLER_FIREBASE)

            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ${exception}")


                initRecyclerView(INIT_RECYCLER_DOWNLOAD)
            }

    }

    fun initRecyclerView(initType: Int) {
        linearLayoutManager = LinearLayoutManager(this)
        listOfSongs.layoutManager = linearLayoutManager
        songs = ArrayList()

        if (initType == INIT_RECYCLER_FIREBASE) {
            songs = sqliteDatabase!!.getAllSongs()
            sharedStorage?.storeSongs(songs)
        } else if( initType == INIT_RECYCLER_DOWNLOAD) {
            songs = sqliteDatabase!!.getLoadedSongs()
            sharedStorage?.storeSongs(songs)
        }

        recyclerAdapter = RecyclerAdapter(songs, this)

        listOfSongs.adapter = recyclerAdapter
    }

    fun initRecyclerViewWithSingerPlaylist(playlist: PlaylistModel) {
        linearLayoutManager = LinearLayoutManager(this)
        listOfSongs.layoutManager = linearLayoutManager
        songs = ArrayList()

        songs = sqliteDatabase!!.getSongsBy("playlist", playlist.key!!)
        sharedStorage?.storeSongs(songs)

        recyclerAdapter = RecyclerAdapter(songs, this)

        listOfSongs.adapter = recyclerAdapter
    }

    private fun initMediaService() {
        //Check is service is active
        if (!serviceBound) {
            val playerIntent = Intent(this, MediaPlayerService::class.java)
            startService(playerIntent)
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } else {
            // Service is active
            // Send media with BroadcastReceiver
            if (!sharedStorage?.getPause()!!) {
                val broadcastIntent = Intent(sharedStorage!!.playNewSongReceiverAction)
                sendBroadcast(broadcastIntent)
                recyclerAdapter?.play()
            }
        }
    }

    private fun Play() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            if (sharedStorage?.getPause()!!) {
                player?.resumeMedia()

                activateVisualizer()
                activateSeekBar()
                showPauseButton()

                recyclerAdapter?.play()
                sharedStorage?.setPause(false)
            }
        } else {
            requestAudioPermissions(true)
        }
    }

    private fun Pause() {
        if (!sharedStorage?.getPause()!!) {
            player?.pauseMedia()
        }

        showPlayButton()
        recyclerAdapter?.pause()
        sharedStorage?.setPause(true)
    }

    private fun Next() {
        player?.skipToNext()

        activateVisualizer()
        activateSeekBar()
        showPauseButton()

        recyclerAdapter?.next()
        sharedStorage?.setPause(false)

    }

    private fun Previous() {
        player?.skipToPrevious()

        activateVisualizer()
        activateSeekBar()
        showPauseButton()

        recyclerAdapter?.previous()
        sharedStorage?.setPause(false)
    }

    //Binding this Client to the AudioPlayer Service
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as MediaPlayerService.LocalBinder
            player = binder.service
            serviceBound = true

            Toast.makeText(this@MainActivity, "Service Bound", Toast.LENGTH_SHORT).show()

            when(sharedStorage!!.getPause()) {
                true -> {
                    showPlayButton()
                }
                false -> {
                    Log.d(TAG, "DSHADUIWHDOIJWA")
                    if (!serviceBound) {
                        initMediaService()
                    }

                    Play()
                    mediaPlayerContainer.visibility = View.VISIBLE
                }
            }

        }

        override fun onServiceDisconnected(name: ComponentName) {
            serviceBound = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putBoolean("ServiceState", serviceBound)

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        serviceBound = savedInstanceState!!.getBoolean("ServiceState")
    }

    // Handle request permissions
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            MY_PERMISSIONS_RECORD_AUDIO -> {

                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                    Log.i(TAG, "Permission has been denied by user")
                    Toast.makeText(this, "Рұқсат күтілуде!", Toast.LENGTH_LONG).show()
                } else {
                    Log.i(TAG, "Permission has been granted by user")
                    requestAudioPermissions(true)
                }
            }
        }
    }

    private fun createFolder(fileName: String){
        val folder = filesDir
        val f = File(folder, fileName)
        if (!f.exists()) {
            f.mkdirs()

            Log.d(TAG, "is folder created: ${f.absolutePath}")
        } else {
            Log.d(TAG, "folder is exists: ${f.absolutePath}")
        }

        sharedStorage?.storeFileDir(f.absolutePath)

        val songs = File("$folder/$fileName", "songs")
        if (!songs.exists()) {
            songs.mkdir()
            Log.d(TAG, "songs: folder created: ${songs.absolutePath}")
        }

        val images = File("$folder/$fileName", "images")
        if (!images.exists()) {
            images.mkdir()
            Log.d(TAG, "images: folder created: ${images.absolutePath}")
        }

        // Create Folder in internal storage
//        var filepath = Environment.getExternalStorageDirectory().getPath()
//        val f = File(filepath, fileName)
//        val songs = File("$filepath/$fileName", "songs")
//        val imgs = File("$filepath/$fileName", "imgs")
//
//        var bsongs = songs.mkdir()
//        var bimgs = imgs.mkdir()
    }


    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        toggle.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "navItemSelected: ${item.itemId} ${item.title}")

        if (drawer_layout != null) {
            drawer_layout.closeDrawer(GravityCompat.START)
        }
        when (item.itemId) {
            MENU_ID_ADD -> {
                val builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogCustom))
                val inflater = layoutInflater
                builder.setTitle("Жаңа жинақ ашу")
                val dialogLayout = inflater.inflate(R.layout.alert_dialog_with_edittext, null)
                val editText  = dialogLayout.findViewById<EditText>(R.id.editText)
                builder.setView(dialogLayout)
                builder.setPositiveButton("OK") { dialogInterface, i ->
                    if (editText.text.toString() != "") {
                        if (sqliteDatabase == null) {
                            sqliteDatabase = SqliteDatabase(this)
                        }

                        sqliteDatabase?.addOwnPlaylist(PlaylistModel(0, "", editText.text.toString(), "", 0, ""))

                        Toast.makeText(applicationContext, "${editText.text} жинағы сәтті ашылды", Toast.LENGTH_SHORT).show()
                        createMenu()
                    }
                }

                builder.show()
            }

            MENU_ID_LOADED -> {
                initRecyclerView(INIT_RECYCLER_DOWNLOAD)
            }

            MENU_ID_SONGS -> {
                initRecyclerView(INIT_RECYCLER_FIREBASE)
            }

            else -> {
                var playlist = sqliteDatabase?.getPlaylistBy("id", item.itemId.toString())

                if (playlist != null) {
                    initRecyclerViewWithSingerPlaylist(playlist)
                }

                Log.d(TAG, "Menu clicked: $playlist")

            }
        }

        return true
    }

    //setting menu in action bar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onStop() {
        super.onStop()

        if (sharedStorage?.getPause()!!) {
            player?.buildNotification(PlaybackStatus.PAUSED)
        } else {
            player?.buildNotification(PlaybackStatus.PLAYING)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (mVisualizer != null)
            mVisualizer.release()

        if (serviceBound) {
            unbindService(serviceConnection)

            player?.stopSelf()
        }

        unregisterReceiver(updateUIReciver)

        try {
            handler.removeCallbacks(runnable)
        } catch (e: Exception) {
            Log.d(TAG, "handler removeCallbacks: ${e.toString()}")
        }

        sharedStorage?.setPause(true)
    }

    companion object {
        private const val MENU_ID_FAVORITE = 101
        private const val MENU_ID_SONGS = 102
        private const val MENU_ID_LOADED = 103
        private const val MENU_ID_ADD = 104
        private const val MENU_ID_SINGER_PLAYLIST = 105

        private const val INIT_RECYCLER_DOWNLOAD = 1
        private const val INIT_RECYCLER_FIREBASE = 2
    }
}
