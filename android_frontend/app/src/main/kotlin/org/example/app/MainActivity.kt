package org.example.app

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var playerIndicator: TextView
    private lateinit var resultBanner: TextView
    private lateinit var restartButton: Button

    private val cellButtons: Array<Button> by lazy {
        arrayOf(
            findViewById(R.id.cell0),
            findViewById(R.id.cell1),
            findViewById(R.id.cell2),
            findViewById(R.id.cell3),
            findViewById(R.id.cell4),
            findViewById(R.id.cell5),
            findViewById(R.id.cell6),
            findViewById(R.id.cell7),
            findViewById(R.id.cell8),
        )
    }

    private var currentPlayer: Char = 'X'

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerIndicator = findViewById(R.id.playerIndicator)
        resultBanner = findViewById(R.id.resultBanner)
        restartButton = findViewById(R.id.restartButton)

        // Minimal wiring for UI compile/run; game logic is intentionally simple here.
        // Tapping an empty cell fills it with the current player's symbol and flips the player.
        cellButtons.forEach { button ->
            button.setOnClickListener {
                if (button.text.isNullOrEmpty()) {
                    button.text = currentPlayer.toString()
                    // Placeholder banner update; win/draw logic will be added later.
                    resultBanner.text = "Last move: $currentPlayer"
                    resultBanner.setTextColor(0xFF64748B.toInt()) // secondary (#64748b)

                    currentPlayer = if (currentPlayer == 'X') 'O' else 'X'
                    updatePlayerIndicator()
                }
            }
        }

        restartButton.setOnClickListener {
            resetBoardUi()
        }

        // Initial state
        resetBoardUi()
    }

    private fun updatePlayerIndicator() {
        playerIndicator.text = "Player: $currentPlayer"
        // primary (#3b82f6)
        playerIndicator.setTextColor(0xFF3B82F6.toInt())
    }

    private fun resetBoardUi() {
        currentPlayer = 'X'
        cellButtons.forEach { it.text = "" }

        updatePlayerIndicator()

        resultBanner.text = "Tap a square to start"
        // secondary (#64748b)
        resultBanner.setTextColor(0xFF64748B.toInt())
    }
}
