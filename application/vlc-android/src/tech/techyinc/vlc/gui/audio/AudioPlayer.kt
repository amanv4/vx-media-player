/*****************************************************************************
 * AudioPlayer.kt
 *
 * Copyright © 2011-2019 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 */

package tech.techyinc.vlc.gui.audio

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.support.v4.media.session.PlaybackStateCompat
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.onEach
import org.videolan.medialibrary.Tools
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import tech.techyinc.resources.*
import tech.techyinc.tools.*
import tech.techyinc.vlc.PlaybackService
import tech.techyinc.vlc.R
import tech.techyinc.vlc.databinding.AudioPlayerBinding
import tech.techyinc.vlc.gui.AudioPlayerContainerActivity
import tech.techyinc.vlc.gui.InfoActivity
import tech.techyinc.vlc.gui.dialogs.CtxActionReceiver
import tech.techyinc.vlc.gui.dialogs.PlaybackSpeedDialog
import tech.techyinc.vlc.gui.dialogs.SleepTimerDialog
import tech.techyinc.vlc.gui.dialogs.showContext
import tech.techyinc.vlc.gui.helpers.*
import tech.techyinc.vlc.gui.helpers.AudioUtil.setRingtone
import tech.techyinc.vlc.gui.helpers.UiTools.addToPlaylist
import tech.techyinc.vlc.gui.video.VideoPlayerActivity
import tech.techyinc.vlc.gui.view.AudioMediaSwitcher
import tech.techyinc.vlc.gui.view.AudioMediaSwitcher.AudioMediaSwitcherListener
import tech.techyinc.vlc.manageAbRepeatStep
import tech.techyinc.vlc.media.PlaylistManager.Companion.hasMedia
import tech.techyinc.vlc.util.launchWhenStarted
import tech.techyinc.vlc.util.share
import tech.techyinc.vlc.viewmodels.BookmarkModel
import tech.techyinc.vlc.viewmodels.PlaybackProgress
import tech.techyinc.vlc.viewmodels.PlaylistModel
import java.text.DateFormat.getTimeInstance
import java.util.*
import kotlin.math.absoluteValue

private const val TAG = "VLC/AudioPlayer"
private const val SEARCH_TIMEOUT_MILLIS = 10000L

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@Suppress("UNUSED_PARAMETER")
class AudioPlayer : Fragment(), PlaylistAdapter.IPlayer, TextWatcher, IAudioPlayerAnimator by AudioPlayerAnimator() {

    private lateinit var binding: AudioPlayerBinding
    private lateinit var playlistAdapter: PlaylistAdapter
    private lateinit var settings: SharedPreferences
    private val handler by lazy(LazyThreadSafetyMode.NONE) { Handler() }
    lateinit var playlistModel: PlaylistModel
    lateinit var bookmarkModel: BookmarkModel
    private lateinit var optionsDelegate: PlayerOptionsDelegate
    private lateinit var bookmarkListDelegate: BookmarkListDelegate

    private var showRemainingTime = false
    private var previewingSeek = false
    private var playerState = 0
    private lateinit var pauseToPlay: AnimatedVectorDrawableCompat
    private lateinit var playToPause: AnimatedVectorDrawableCompat
    private lateinit var pauseToPlaySmall: AnimatedVectorDrawableCompat
    private lateinit var playToPauseSmall: AnimatedVectorDrawableCompat

    private lateinit var abRepeatAddMarker: Button
    private var audioPlayProgressMode:Boolean = false
    private var lastEndsAt = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            playerState = it.getInt("player_state")
            wasPlaying = it.getBoolean("was_playing")
            showRemainingTime = it.getBoolean("show_remaining_time")
        }
        playlistAdapter = PlaylistAdapter(this)
        settings = Settings.getInstance(requireContext())
        playlistModel = PlaylistModel.get(this)
        playlistModel.progress.observe(this@AudioPlayer, { it?.let { updateProgress(it) } })
        playlistModel.speed.observe(this@AudioPlayer, { showChips() })
        playlistAdapter.setModel(playlistModel)
        playlistModel.dataset.asFlow().conflate().onEach {
            doUpdate()
            playlistAdapter.update(it)
            delay(50L)
        }.launchWhenStarted(lifecycleScope)
        bookmarkModel = BookmarkModel.get(requireActivity())
        PlayerOptionsDelegate.playerSleepTime.observe(this@AudioPlayer, {
            showChips()
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = AudioPlayerBinding.inflate(inflater)
        setupAnimator(binding)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.songsList.layoutManager = LinearLayoutManager(view.context)
        binding.songsList.adapter = playlistAdapter
        binding.audioMediaSwitcher.setAudioMediaSwitcherListener(headerMediaSwitcherListener)
        binding.coverMediaSwitcher.setAudioMediaSwitcherListener(mCoverMediaSwitcherListener)
        binding.playlistSearchText.editText?.addTextChangedListener(this)

        val callback = SwipeDragItemTouchHelperCallback(playlistAdapter, true)
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(binding.songsList)

        binding.fragment = this

        binding.next.setOnTouchListener(LongSeekListener(true))
        binding.previous.setOnTouchListener(LongSeekListener(false))

        registerForContextMenu(binding.songsList)
        userVisibleHint = true
        showCover(settings.getBoolean("audio_player_show_cover", false))
        binding.playlistSwitch.setImageResource(if (isShowingCover()) R.drawable.ic_playlist_audio else R.drawable.ic_playlist_audio_on)
        binding.timeline.setOnSeekBarChangeListener(timelineListener)

        //For resizing purpose, we have to cache this twice even if it's from the same resource
        playToPause = AnimatedVectorDrawableCompat.create(requireActivity(), R.drawable.anim_play_pause_video)!!
        pauseToPlay = AnimatedVectorDrawableCompat.create(requireActivity(), R.drawable.anim_pause_play_video)!!
        playToPauseSmall = AnimatedVectorDrawableCompat.create(requireActivity(), R.drawable.anim_play_pause_video)!!
        pauseToPlaySmall = AnimatedVectorDrawableCompat.create(requireActivity(), R.drawable.anim_pause_play_video)!!
        onSlide(0f)
        abRepeatAddMarker = binding.abRepeatContainer.findViewById<Button>(R.id.ab_repeat_add_marker)
        playlistModel.service?.playlistManager?.abRepeat?.observe(viewLifecycleOwner, { abvalues ->
            binding.abRepeatA = if (abvalues.start == -1L) -1F else abvalues.start / playlistModel.service!!.playlistManager.player.getLength().toFloat()
            binding.abRepeatB = if (abvalues.stop == -1L) -1F else abvalues.stop / playlistModel.service!!.playlistManager.player.getLength().toFloat()
            binding.abRepeatMarkerA.visibility = if (abvalues.start == -1L) View.GONE else View.VISIBLE
            binding.abRepeatMarkerB.visibility = if (abvalues.stop == -1L) View.GONE else View.VISIBLE
            playlistModel.service?.manageAbRepeatStep(binding.abRepeatReset, binding.abRepeatStop, binding.abRepeatContainer, abRepeatAddMarker)
        })
        playlistModel.service?.playlistManager?.abRepeatOn?.observe(viewLifecycleOwner, {
            binding.abRepeatMarkerGuidelineContainer.visibility = if (it) View.VISIBLE else View.GONE
            abRepeatAddMarker.visibility = if (it) View.VISIBLE else View.GONE
            binding.audioPlayProgress.visibility = if (!shouldHidePlayProgress()) View.VISIBLE else View.GONE

            playlistModel.service?.manageAbRepeatStep(binding.abRepeatReset, binding.abRepeatStop, binding.abRepeatContainer, abRepeatAddMarker)
        })

        abRepeatAddMarker.setOnClickListener {
            playlistModel.service?.playlistManager?.setABRepeatValue(binding.timeline.progress.toLong())
        }

        audioPlayProgressMode = Settings.getInstance(requireActivity()).getBoolean(AUDIO_PLAY_PROGRESS_MODE, false)
        binding.audioPlayProgress.setOnClickListener {
            audioPlayProgressMode = !audioPlayProgressMode
            Settings.getInstance(requireActivity()).putSingle(AUDIO_PLAY_PROGRESS_MODE, audioPlayProgressMode)
            playlistModel.progress.value?.let { updateProgress(it) }
        }
        binding.playbackSpeedQuickAction.setOnClickListener {
            val newFragment = PlaybackSpeedDialog.newInstance()
            newFragment.show(requireActivity().supportFragmentManager, "playback_speed")
        }
        binding.playbackSpeedQuickAction.setOnLongClickListener {
            playlistModel.service?.setRate(1F, true)
            showChips()
            true
        }
        binding.sleepQuickAction.setOnClickListener {
            val newFragment = SleepTimerDialog.newInstance()
            newFragment.show(requireActivity().supportFragmentManager, "time")
        }
        binding.sleepQuickAction.setOnLongClickListener {
            playlistModel.service?.setSleep(null)
            showChips()
            true
        }
    }

    private fun showChips() {
        if (playlistModel.speed.value == 1.0F && PlayerOptionsDelegate.playerSleepTime.value == null) {
            binding.playbackChips.setGone()
        } else {
            binding.playbackChips.setVisible()
            binding.playbackSpeedQuickAction.setGone()
            binding.sleepQuickAction.setGone()
            playlistModel.speed.value?.let {
                if (it != 1.0F) binding.playbackSpeedQuickAction.setVisible()
                binding.playbackSpeedQuickAction.text = it.formatRateString()
            }
            PlayerOptionsDelegate.playerSleepTime.value?.let {
                binding.sleepQuickAction.setVisible()
                binding.sleepQuickAction.text = DateFormat.getTimeFormat(requireContext()).format(it.time)
            }
        }
    }

    override fun onResume() {
        onStateChanged(playerState)
        showRemainingTime = Settings.getInstance(requireContext()).getBoolean(SHOW_REMAINING_TIME, false)
        super.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("player_state", playerState)
        outState.putBoolean("was_playing", wasPlaying)
        outState.putBoolean("show_remaining_time", showRemainingTime)
    }

    private val ctxReceiver: CtxActionReceiver = object : CtxActionReceiver {
        override fun onCtxAction(position: Int, option: Long) {
            if (position in 0 until playlistAdapter.itemCount) when (option) {
                CTX_SET_RINGTONE -> activity?.setRingtone(playlistAdapter.getItem(position))
                CTX_ADD_TO_PLAYLIST -> {
                    val mw = playlistAdapter.getItem(position)
                    requireActivity().addToPlaylist(listOf(mw))
                }
                CTX_REMOVE_FROM_PLAYLIST -> view?.let {
                    val mw = playlistAdapter.getItem(position)
                    val cancelAction = Runnable { playlistModel.insertMedia(position, mw) }
                    val message = String.format(getString(R.string.remove_playlist_item), mw.title)
                    UiTools.snackerWithCancel(requireActivity(), message, null, cancelAction)
                    playlistModel.remove(position)
                }
                CTX_STOP_AFTER_THIS -> playlistModel.stopAfter(position)
                CTX_INFORMATION -> showInfoDialog(playlistAdapter.getItem(position))
                CTX_SHARE -> lifecycleScope.launch { (requireActivity() as AppCompatActivity).share(playlistAdapter.getItem(position)) }
            }
        }
    }

    private fun showInfoDialog(media: MediaWrapper) {
        val i = Intent(requireActivity(), InfoActivity::class.java)
        i.putExtra(TAG_ITEM, media)
        startActivity(i)
    }

    override fun onPopupMenu(view: View, position: Int, item: MediaWrapper?) {
        val activity = activity
        if (activity === null || position >= playlistAdapter.itemCount) return
        var flags = CTX_REMOVE_FROM_PLAYLIST or CTX_STOP_AFTER_THIS or CTX_INFORMATION
        if (item?.uri?.scheme != "content") flags = flags or CTX_ADD_TO_PLAYLIST or CTX_SHARE or CTX_SET_RINGTONE
        showContext(activity, ctxReceiver, position, item?.title ?: "", flags)
    }

    private suspend fun doUpdate() {
        if (isVisible && playlistModel.switchToVideo()) return
        binding.playlistPlayasaudioOff.visibility = if (playlistModel.videoTrackCount > 0) View.VISIBLE else View.GONE
        updatePlayPause()
        updateShuffleMode()
        updateRepeatMode()
        binding.audioMediaSwitcher.updateMedia(playlistModel.service)
        binding.coverMediaSwitcher.updateMedia(playlistModel.service)
        updateBackground()
    }

    private var wasPlaying = true
    private fun updatePlayPause() {
        val ctx = context ?: return
        val playing = playlistModel.playing
        val text = ctx.getString(if (playing) R.string.pause else R.string.play)

        val drawable = if (playing) playToPause else pauseToPlay
        val drawableSmall = if (playing) playToPauseSmall else pauseToPlaySmall
        binding.playPause.setImageDrawable(drawable)
        binding.headerPlayPause.setImageDrawable(drawableSmall)
        if (playing != wasPlaying) {
            drawable.start()
            drawableSmall.start()
        }

        playlistAdapter.setCurrentlyPlaying(playing)
        binding.playPause.contentDescription = text
        binding.headerPlayPause.contentDescription = text
        wasPlaying = playing
    }

    private var wasShuffling = false
    private fun updateShuffleMode() {
        val ctx = context ?: return
        binding.shuffle.visibility = if (playlistModel.canShuffle) View.VISIBLE else View.INVISIBLE
        val shuffling = playlistModel.shuffling
        if (wasShuffling == shuffling) return
        binding.shuffle.setImageResource(if (shuffling) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle)
        binding.shuffle.contentDescription = ctx.getString(if (shuffling) R.string.shuffle_on else R.string.shuffle)
        wasShuffling = shuffling
    }

    private var previousRepeatType = -1
    private fun updateRepeatMode() {
        val ctx = context ?: return
        val repeatType = playlistModel.repeatType
        if (previousRepeatType == repeatType) return
        when (repeatType) {
            PlaybackStateCompat.REPEAT_MODE_ONE -> {
                binding.repeat.setImageResource(R.drawable.ic_repeat_one)
                binding.repeat.contentDescription = ctx.getString(R.string.repeat_single)
            }
            PlaybackStateCompat.REPEAT_MODE_ALL -> {
                binding.repeat.setImageResource(R.drawable.ic_repeat_all)
                binding.repeat.contentDescription = ctx.getString(R.string.repeat_all)
            }
            else -> {
                binding.repeat.setImageResource(R.drawable.ic_repeat)
                binding.repeat.contentDescription = ctx.getString(R.string.repeat)
            }
        }
        previousRepeatType = repeatType
    }

    private fun updateProgress(progress: PlaybackProgress) {
        if (playlistModel.currentMediaPosition == -1) return
        binding.length.text = progress.lengthText
        binding.timeline.max = progress.length.toInt()
        binding.progressBar.max = progress.length.toInt()

        if (!previewingSeek) {
            val displayTime = if (showRemainingTime) Tools.millisToString(progress.time - progress.length) else progress.timeText
            binding.headerTime.text = displayTime
            binding.time.text = displayTime
            binding.timeline.progress = progress.time.toInt()
            binding.progressBar.progress = progress.time.toInt()
        }
        binding.songsList.setPadding(binding.songsList.paddingLeft, binding.songsList.paddingTop, binding.songsList.paddingRight, binding.audioPlayProgress.height + 8.dp)

        lifecycleScope.launchWhenStarted {
            val text = withContext(Dispatchers.Default) {
                val medias = playlistModel.medias ?: return@withContext ""
                withContext(Dispatchers.Main) { if (!shouldHidePlayProgress()) binding.audioPlayProgress.setVisible() else binding.audioPlayProgress.setGone() }
                if (playlistModel.currentMediaPosition == -1) return@withContext ""
                val elapsedTracksTime = playlistModel.previousTotalTime ?: return@withContext ""
                val progressTime = elapsedTracksTime + progress.time
                val totalTime = playlistModel.getTotalTime()
                val progressTimeText = Tools.millisToString(
                    if (showRemainingTime && totalTime > 0) totalTime - progressTime else progressTime,
                    false,
                    true,
                    false
                )
                val totalTimeText = Tools.millisToString(totalTime, false, false, false)
                val currentProgressText = if (progressTimeText.isNullOrEmpty()) "0:00" else progressTimeText

                val textTrack = getString(R.string.track_index, "${playlistModel.currentMediaPosition + 1} / ${medias.size}")

                val textProgress = if (audioPlayProgressMode) {
                    val endsAt = System.currentTimeMillis() + totalTime - progressTime
                    if ((lastEndsAt - endsAt).absoluteValue > 1) lastEndsAt = endsAt
                    getString(
                        R.string.audio_queue_progress_finished,
                        getTimeInstance(java.text.DateFormat.MEDIUM).format(lastEndsAt)
                    )
                } else if (showRemainingTime && totalTime > 0) getString(
                    R.string.audio_queue_progress_remaining,
                    "$currentProgressText"
                )
                else getString(
                        R.string.audio_queue_progress,
                        if (totalTimeText.isNullOrEmpty()) "$currentProgressText" else "$currentProgressText / $totalTimeText"
                    )
                "$textTrack  •  $textProgress"
            }
            binding.audioPlayProgress.text = text
        }
    }

    fun shouldHidePlayProgress() = abRepeatAddMarker.visibility != View.GONE || (::bookmarkListDelegate.isInitialized && bookmarkListDelegate.visible) || playlistModel.medias?.size ?: 0 < 2

    override fun onSelectionSet(position: Int) {
        if (playerState != BottomSheetBehavior.STATE_COLLAPSED && playerState != BottomSheetBehavior.STATE_HIDDEN) {
            binding.songsList.scrollToPosition(position)
        }
    }

    override fun playItem(position: Int, item: MediaWrapper) {
        clearSearch()
        playlistModel.play(playlistModel.getPlaylistPosition(position, item))
    }

    fun onTimeLabelClick(view: View) {
        showRemainingTime = !showRemainingTime
        Settings.getInstance(requireContext()).edit().putBoolean(SHOW_REMAINING_TIME, showRemainingTime).apply()
        playlistModel.progress.value?.let { updateProgress(it) }
    }

    fun onPlayPauseClick(view: View) {
        if (playlistModel.service?.isPausable == false) {
            UiTools.snackerConfirm(requireActivity(), getString(R.string.stop_unpaubale)) {
                playlistModel.stop()
            }
            return
        }
        playlistModel.togglePlayPause()
    }

    fun onStopClick(view: View): Boolean {
        playlistModel.stop()
        return true
    }

    fun onNextClick(view: View) {
        if (!playlistModel.next()) UiTools.snacker(requireActivity(), R.string.lastsong)
    }

    fun onPreviousClick(view: View) {
        if (!playlistModel.previous()) UiTools.snacker(requireActivity(),  R.string.firstsong)
    }

    fun onRepeatClick(view: View) {
        playlistModel.repeatType = when (playlistModel.repeatType) {
            PlaybackStateCompat.REPEAT_MODE_NONE -> PlaybackStateCompat.REPEAT_MODE_ALL
            PlaybackStateCompat.REPEAT_MODE_ALL -> PlaybackStateCompat.REPEAT_MODE_ONE
            else -> PlaybackStateCompat.REPEAT_MODE_NONE
        }
        updateRepeatMode()
    }

    fun onPlaylistSwitchClick(view: View) {
        switchShowCover()
        settings.putSingle("audio_player_show_cover", isShowingCover())
        binding.playlistSwitch.setImageResource(if (isShowingCover()) R.drawable.ic_playlist_audio else R.drawable.ic_playlist_audio_on)
    }

    fun onShuffleClick(view: View) {
        playlistModel.shuffle()
        updateShuffleMode()
    }

    fun onResumeToVideoClick(v: View) {
        playlistModel.currentMediaWrapper?.let {
            if (PlaybackService.hasRenderer()) VideoPlayerActivity.startOpened(v.context,
                    it.uri, playlistModel.currentMediaPosition)
            else if (hasMedia()) {
                it.removeFlags(MediaWrapper.MEDIA_FORCE_AUDIO)
                lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) { playlistModel.switchToVideo() }
            }
        }
    }

    fun showAdvancedOptions(v: View) {
        if (!isVisible) return
        if (!this::optionsDelegate.isInitialized) {
            val service = playlistModel.service ?: return
            val activity = activity as? AppCompatActivity ?: return
            optionsDelegate = PlayerOptionsDelegate(activity, service)
            optionsDelegate.setBookmarkClickedListener {
                if (!this::bookmarkListDelegate.isInitialized) {
                    bookmarkListDelegate = BookmarkListDelegate(activity, service, bookmarkModel)
                    bookmarkListDelegate.visibilityListener = {
                        binding.audioPlayProgress.visibility = if (shouldHidePlayProgress()) View.GONE else View.VISIBLE
                    }
                    bookmarkListDelegate.markerContainer = binding.bookmarkMarkerContainer
                }
                bookmarkListDelegate.show()
                bookmarkListDelegate.setProgressHeight(binding.time.y)
            }
        }
        optionsDelegate.show()
    }

    fun onSearchClick(v: View) {
        manageSearchVisibilities(true)
        binding.playlistSearchText.editText?.requestFocus()
        if (isShowingCover()) onPlaylistSwitchClick(binding.playlistSwitch)
        val imm = v.context.applicationContext.getSystemService<InputMethodManager>()!!
        imm.showSoftInput(binding.playlistSearchText.editText, InputMethodManager.SHOW_IMPLICIT)
        handler.postDelayed(hideSearchRunnable, SEARCH_TIMEOUT_MILLIS)
    }

    fun onABRepeatStopClick(v: View) {
        playlistModel.service?.playlistManager?.clearABRepeat()
    }

    fun onABRepeatResetClick(v: View) {
        playlistModel.service?.playlistManager?.resetABRepeatValues()
    }

    override fun beforeTextChanged(charSequence: CharSequence, start: Int, before: Int, count: Int) {}

    fun backPressed(): Boolean {
        if (this::optionsDelegate.isInitialized && optionsDelegate.isShowing()) {
            optionsDelegate.hide()
            return true
        }
        if (::bookmarkListDelegate.isInitialized && bookmarkListDelegate.visible) {
            bookmarkListDelegate.hide()
            return true
        }
        return clearSearch()
    }

    fun clearSearch(): Boolean {
        if (this::playlistModel.isInitialized) playlistModel.filter(null)
        return hideSearchField()
    }

    private fun hideSearchField(): Boolean {
        if (binding.playlistSearchText.visibility != View.VISIBLE) return false
        binding.playlistSearchText.editText?.apply {
            removeTextChangedListener(this@AudioPlayer)
            setText("")
            addTextChangedListener(this@AudioPlayer)
        }
        UiTools.setKeyboardVisibility(binding.playlistSearchText, false)
        manageSearchVisibilities(false)
        return true
    }

    override fun onTextChanged(charSequence: CharSequence, start: Int, before: Int, count: Int) {
        val length = charSequence.length
        if (length > 0) {
            playlistModel.filter(charSequence)
            handler.removeCallbacks(hideSearchRunnable)
        } else {
            playlistModel.filter(null)
            hideSearchField()
        }
    }

    override fun afterTextChanged(editable: Editable) {}

    override fun onDestroy() {
        super.onDestroy()
        if (this::optionsDelegate.isInitialized) optionsDelegate.release()
    }

    private inner class LongSeekListener(var forward: Boolean) : View.OnTouchListener {
        var length = -1L

        var possibleSeek = 0
        var vibrated = false

        @RequiresPermission(Manifest.permission.VIBRATE)
        var seekRunnable: Runnable = object : Runnable {
            override fun run() {
                if (!vibrated) {
                    AppContextProvider.appContext.getSystemService<Vibrator>()?.vibrate(80)
                    vibrated = true
                }

                if (forward) {
                    if (length <= 0 || possibleSeek < length) possibleSeek += 4000
                } else {
                    if (possibleSeek > 4000) possibleSeek -= 4000
                    else if (possibleSeek <= 4000) possibleSeek = 0
                }

                binding.time.text = Tools.millisToString(if (showRemainingTime) possibleSeek - length else possibleSeek.toLong())
                binding.timeline.progress = possibleSeek
                binding.progressBar.progress = possibleSeek
                handler.postDelayed(this, 50)
            }
        }

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    possibleSeek = playlistModel.time.toInt()
                    previewingSeek = true
                    vibrated = false
                    length = playlistModel.length
                    handler.postDelayed(seekRunnable, 1000)
                    return false
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(seekRunnable)
                    previewingSeek = false
                    if (event.eventTime - event.downTime >= 1000L) {
                        playlistModel.time = possibleSeek.toLong().coerceAtLeast(0L).coerceAtMost(playlistModel.length)
                        v.isPressed = false
                        return true
                    }
                    return false
                }
            }
            return false
        }
    }

    private fun showPlaylistTips() {
        val activity = activity as? AudioPlayerContainerActivity
        activity?.showTipViewIfNeeded(R.id.audio_playlist_tips, PREF_PLAYLIST_TIPS_SHOWN)
    }

    fun onStateChanged(newState: Int) {
        playerState = newState
        when (newState) {
            BottomSheetBehavior.STATE_COLLAPSED -> {
                backPressed()
                onSlide(0f)
            }
            BottomSheetBehavior.STATE_EXPANDED -> {
                onSlide(1f)
                showPlaylistTips()
                playlistAdapter.currentIndex = playlistModel.currentMediaPosition
            }
        }
    }

    private var timelineListener: OnSeekBarChangeListener = object : OnSeekBarChangeListener {

        override fun onStopTrackingTouch(seekBar: SeekBar) {}

        override fun onStartTrackingTouch(seekBar: SeekBar) {}

        override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser) {
                playlistModel.time = progress.toLong()
                binding.time.text = Tools.millisToString(if (showRemainingTime) progress - playlistModel.length else progress.toLong())
                binding.headerTime.text = Tools.millisToString(progress.toLong())
            }
        }
    }

    private val headerMediaSwitcherListener = object : AudioMediaSwitcherListener {

        override fun onMediaSwitching() {}

        override fun onMediaSwitched(position: Int) {
            when (position) {
                AudioMediaSwitcherListener.PREVIOUS_MEDIA -> playlistModel.previous(true)
                AudioMediaSwitcherListener.NEXT_MEDIA -> playlistModel.next()
            }
        }

        override fun onTouchClick() {
            val activity = activity as AudioPlayerContainerActivity
            activity.slideUpOrDownAudioPlayer()
        }

        override fun onTouchLongClick() {
            val trackInfo = playlistModel.title ?: return

            requireActivity().copy("VLC - song name", trackInfo)
            UiTools.snacker(requireActivity(), R.string.track_info_copied_to_clipboard)
        }

        override fun onTouchDown() {}

        override fun onTouchUp() {}
    }

    private val mCoverMediaSwitcherListener = object : AudioMediaSwitcherListener by AudioMediaSwitcher.EmptySwitcherListener {

        override fun onMediaSwitching() {
            (activity as? AudioPlayerContainerActivity)?.playerBehavior?.lock(true)
        }

        override fun onMediaSwitched(position: Int) {
            when (position) {
                AudioMediaSwitcherListener.PREVIOUS_MEDIA -> playlistModel.previous(true)
                AudioMediaSwitcherListener.NEXT_MEDIA -> playlistModel.next()
            }
            (activity as? AudioPlayerContainerActivity)?.playerBehavior?.lock(false)
        }
    }

    private val hideSearchRunnable by lazy(LazyThreadSafetyMode.NONE) {
        Runnable {
            hideSearchField()
            playlistModel.filter(null)
        }
    }
}