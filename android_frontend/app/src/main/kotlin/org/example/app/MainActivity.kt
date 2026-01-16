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

    /**
     * Represents the state of a cell:
     *  - null => empty
     *  - 'X' or 'O' => occupied
     */
    private val board: Array<Char?> = Array(9) { null }

    private var currentPlayer: Char = 'X'
    private var gameOver: Boolean = false

    // Win lines for a 3x3 board: rows, columns, diagonals.
    private val winLines: Array<IntArray> = arrayOf(
        intArrayOf(0, 1, 2),
        intArrayOf(3, 4, 5),
        intArrayOf(6, 7, 8),

        intArrayOf(0, 3, 6),
        intArrayOf(1, 4, 7),
        intArrayOf(2, 5, 8),

        intArrayOf(0, 4, 8),
        intArrayOf(2, 4, 6),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerIndicator = findViewById(R.id.playerIndicator)
        resultBanner = findViewById(R.id.resultBanner)
        restartButton = findViewById(R.id.restartButton)

        // Hook up cell taps -> place a move if allowed.
        cellButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                onCellTapped(index)
            }
        }

        restartButton.setOnClickListener {
            resetGame()
        }

        // Initial UI state
        resetGame()
    }

    // PUBLIC_INTERFACE
    private fun resetGame() {
        /** Fully resets board + UI state so a new game can start from Player X. */
        for (i in board.indices) board[i] = null
        gameOver = false
        currentPlayer = 'X'

        // Clear and (re)enable all cells.
        cellButtons.forEach { button ->
            button.text = ""
            button.isEnabled = true
            // Restore default text color in case we set banner colors after win/draw.
            button.setTextColor(0xFF111827.toInt()) // text (#111827)
        }

        updatePlayerIndicator()

        // Neutral banner prompt
        setBannerNeutral("Tap a square to start")
    }

    // PUBLIC_INTERFACE
    private fun onCellTapped(index: Int) {
        /** Handles the full game flow when a cell is tapped. */
        if (gameOver) return

        // Ignore taps on occupied squares
        if (board[index] != null) return

        // Place move
        board[index] = currentPlayer
        cellButtons[index].text = currentPlayer.toString()

        // Check end conditions
        val winner = findWinner()
        if (winner != null) {
            gameOver = true
            setBannerWin("Player $winner wins!")
            lockBoard()
            return
        }

        if (isDraw()) {
            gameOver = true
            setBannerDraw("It's a draw!")
            lockBoard()
            return
        }

        // Continue game: swap player and update UI
        currentPlayer = if (currentPlayer == 'X') 'O' else 'X'
        updatePlayerIndicator()
        setBannerNeutral("Your turn: $currentPlayer")
    }

    private fun lockBoard() {
        // Prevent further moves once game ends.
        cellButtons.forEach { it.isEnabled = false }
        // Player indicator can remain showing whose turn it would have been; we keep it as-is.
    }

    private fun findWinner(): Char? {
        for (line in winLines) {
            val a = board[line[0]]
            val b = board[line[1]]
            val c = board[line[2]]
            if (a != null && a == b && b == c) return a
        }
        return null
    }

    private fun isDraw(): Boolean {
        // Draw means the board is full and nobody won.
        return board.all { it != null } && findWinner() == null
    }

    private fun updatePlayerIndicator() {
        playerIndicator.text = "Player: $currentPlayer"
        // primary (#3b82f6)
        playerIndicator.setTextColor(0xFF3B82F6.toInt())
    }

    private fun setBannerNeutral(message: String) {
        resultBanner.text = message
        // secondary (#64748b)
        resultBanner.setTextColor(0xFF64748B.toInt())
    }

    private fun setBannerWin(message: String) {
        resultBanner.text = message
        // error-ish / strong attention (#EF4444)
        resultBanner.setTextColor(0xFFEF4444.toInt())
    }

    private fun setBannerDraw(message: String) {
        resultBanner.text = message
        // success/teal accent (#06b6d4)
        resultBanner.setTextColor(0xFF06B6D4.toInt())
    }
}
