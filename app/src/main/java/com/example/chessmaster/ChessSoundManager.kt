
package com.example.chessmaster

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log

class ChessSoundManager(context: Context) {

    companion object {
        private const val TAG = "ChessSoundManager"
    }

    private val soundPool: SoundPool
    private val soundIds = mutableMapOf<String, Int>()
    private val loadedSounds = mutableSetOf<Int>()

    private var released = false

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSounds.add(sampleId)
                Log.d(TAG, "Sound loaded successfully: $sampleId")
            } else {
                Log.e(TAG, "Sound failed to load: $sampleId, status=$status")
            }
        }

        soundIds["move"] = soundPool.load(context, R.raw.chess_move, 1)
        soundIds["capture"] = soundPool.load(context, R.raw.chess_capture, 1)
        soundIds["check"] = soundPool.load(context, R.raw.chess_check, 1)
    }

    fun playMove() = play("move")

    fun playCapture() = play("capture")

    fun playCheck() = play("check")

    private fun play(name: String) {
        if (released) {
            Log.w(TAG, "Cannot play $name: sound manager released")
            return
        }

        val soundId = soundIds[name]

        if (soundId == null) {
            Log.e(TAG, "Missing sound ID: $name")
            return
        }

        if (soundId !in loadedSounds) {
            Log.w(TAG, "Sound not loaded yet: $name")
            return
        }

        val streamId = soundPool.play(
            soundId,
            1f,
            1f,
            1,
            0,
            1f
        )

        Log.d(TAG, "Playing $name, streamId=$streamId")

        if (streamId == 0) {
            Log.e(TAG, "SoundPool could not start playback for $name")
        }
    }

    fun release() {
        if (!released) {
            released = true
            soundPool.release()
            loadedSounds.clear()
        }
    }
}

