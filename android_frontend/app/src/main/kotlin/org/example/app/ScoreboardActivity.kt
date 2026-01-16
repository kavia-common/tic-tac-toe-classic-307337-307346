package org.example.app

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class ScoreboardActivity : Activity() {

    private lateinit var xWinsValue: TextView
    private lateinit var oWinsValue: TextView
    private lateinit var drawsValue: TextView
    private lateinit var statusAnnouncer: TextView
    private lateinit var resetButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable system "Up" navigation (works together with parentActivityName in manifest).
        actionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.scoreboard_title)

        setContentView(R.layout.activity_scoreboard)

        xWinsValue = findViewById(R.id.xWinsValue)
        oWinsValue = findViewById(R.id.oWinsValue)
        drawsValue = findViewById(R.id.drawsValue)
        statusAnnouncer = findViewById(R.id.scoreboardStatusAnnouncer)
        resetButton = findViewById(R.id.resetScoresButton)

        resetButton.setOnClickListener {
            ScorePrefs.reset(this)
            renderScores(announce = true, announceText = getString(R.string.scores_reset_announcement))
        }

        renderScores(announce = false, announceText = null)
    }

    override fun onResume() {
        super.onResume()
        // Refresh in case user played games and returned.
        renderScores(announce = false, announceText = null)
    }

    override fun onNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun renderScores(announce: Boolean, announceText: String?) {
        val x = ScorePrefs.getXWins(this)
        val o = ScorePrefs.getOWins(this)
        val d = ScorePrefs.getDraws(this)

        xWinsValue.text = x.toString()
        oWinsValue.text = o.toString()
        drawsValue.text = d.toString()

        if (announce) {
            val msg = announceText ?: getString(R.string.scores_updated_announcement)
            // Use a dedicated view to announce for TalkBack.
            statusAnnouncer.text = msg
            statusAnnouncer.announceForAccessibility(msg)
        }
    }
}
