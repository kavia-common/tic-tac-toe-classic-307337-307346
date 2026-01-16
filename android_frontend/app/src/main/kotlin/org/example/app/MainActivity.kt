package org.example.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.ToggleButton

class MainActivity : Activity() {

    private lateinit var playerIndicator: TextView
    private lateinit var resultBanner: TextView
    private lateinit var restartButton: Button
    private lateinit var scoreboardButton: Button

    // Mode controls (new)
    private lateinit var modeIndicator: TextView
    private lateinit var modeToggle: ToggleButton
    private lateinit var symbolRadioGroup: RadioGroup
    private lateinit var radioPlayerX: RadioButton
    private lateinit var radioPlayerO: RadioButton

    private val mainHandler = Handler(Looper.getMainLooper())

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

    // Computer mode state
    private var gameMode: GameMode = GameMode.TWO_PLAYERS
    private var playerSymbol: Char = 'X' // in Computer mode only; persisted
    private var aiSymbol: Char = 'O'
    private var aiThinking: Boolean = false

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

    private enum class GameMode { TWO_PLAYERS, COMPUTER }

    private object SettingsPrefs {
        const val PREFS_NAME: String = "ttt_settings"
        const val KEY_MODE: String = "MODE" // "two_player" | "computer"
        const val KEY_PLAYER_SYMBOL: String = "PLAYER_SYMBOL" // "X" | "O"

        fun loadMode(context: Context): GameMode {
            val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = p.getString(KEY_MODE, "two_player") ?: "two_player"
            return if (raw == "computer") GameMode.COMPUTER else GameMode.TWO_PLAYERS
        }

        fun saveMode(context: Context, mode: GameMode) {
            val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = if (mode == GameMode.COMPUTER) "computer" else "two_player"
            p.edit().putString(KEY_MODE, raw).apply()
        }

        fun loadPlayerSymbol(context: Context): Char {
            val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = p.getString(KEY_PLAYER_SYMBOL, "X") ?: "X"
            return if (raw == "O") 'O' else 'X'
        }

        fun savePlayerSymbol(context: Context, symbol: Char) {
            val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = if (symbol == 'O') "O" else "X"
            p.edit().putString(KEY_PLAYER_SYMBOL, raw).apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerIndicator = findViewById(R.id.playerIndicator)
        resultBanner = findViewById(R.id.resultBanner)
        restartButton = findViewById(R.id.restartButton)
        scoreboardButton = findViewById(R.id.scoreboardButton)

        // Mode controls
        modeIndicator = findViewById(R.id.modeIndicator)
        modeToggle = findViewById(R.id.modeToggle)
        symbolRadioGroup = findViewById(R.id.symbolRadioGroup)
        radioPlayerX = findViewById(R.id.radioPlayerX)
        radioPlayerO = findViewById(R.id.radioPlayerO)

        // Load persisted settings (default: Two Players, Player = X)
        gameMode = SettingsPrefs.loadMode(this)
        playerSymbol = SettingsPrefs.loadPlayerSymbol(this)
        aiSymbol = other(playerSymbol)

        // Apply loaded settings to UI
        applySettingsToControls()

        // Hook up cell taps -> place a move if allowed.
        cellButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                onCellTapped(index)
            }
        }

        // Mode toggle handler (persist + reset)
        modeToggle.setOnCheckedChangeListener { _, isChecked ->
            gameMode = if (isChecked) GameMode.COMPUTER else GameMode.TWO_PLAYERS
            SettingsPrefs.saveMode(this, gameMode)
            updateModeUi()
            resetGame()
        }

        // Symbol selection handler (persist + reset) - only relevant for Computer mode.
        symbolRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            playerSymbol = if (checkedId == R.id.radioPlayerO) 'O' else 'X'
            aiSymbol = other(playerSymbol)
            SettingsPrefs.savePlayerSymbol(this, playerSymbol)
            updateModeUi()
            resetGame()
        }

        restartButton.setOnClickListener {
            resetGame()
        }

        scoreboardButton.setOnClickListener {
            // Navigate to scoreboard dashboard.
            startActivity(Intent(this, ScoreboardActivity::class.java))
        }

        // Initial UI state
        resetGame()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Prevent delayed runnables from firing after Activity is destroyed.
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun applySettingsToControls() {
        modeToggle.isChecked = (gameMode == GameMode.COMPUTER)
        if (playerSymbol == 'O') {
            radioPlayerO.isChecked = true
        } else {
            radioPlayerX.isChecked = true
        }
        updateModeUi()
    }

    private fun updateModeUi() {
        modeIndicator.text =
            if (gameMode == GameMode.COMPUTER) getString(R.string.mode_computer) else getString(R.string.mode_two_players)

        // Only enable the symbol chooser in Computer mode (keeps UI simple).
        symbolRadioGroup.isEnabled = (gameMode == GameMode.COMPUTER)
        radioPlayerX.isEnabled = (gameMode == GameMode.COMPUTER)
        radioPlayerO.isEnabled = (gameMode == GameMode.COMPUTER)
        symbolRadioGroup.alpha = if (gameMode == GameMode.COMPUTER) 1.0f else 0.45f
    }

    // PUBLIC_INTERFACE
    private fun resetGame() {
        /** Fully resets board + UI state so a new game can start based on selected mode/settings. */
        mainHandler.removeCallbacksAndMessages(null)

        for (i in board.indices) board[i] = null
        gameOver = false
        aiThinking = false

        // In both modes, X always starts by rules. In computer mode, that means:
        // - if Player is X -> Player starts
        // - if Player is O -> Computer starts as X
        currentPlayer = 'X'

        val defaultTextColor = getColor(R.color.ttt_text)

        // Clear and (re)enable all cells.
        cellButtons.forEach { button ->
            button.text = ""
            button.isEnabled = true
            button.alpha = 1.0f
            button.setTextColor(defaultTextColor)
            // Ensure touch target is respected and looks consistent even on older devices.
            button.minHeight = resources.getDimensionPixelSize(R.dimen.ttt_touch_target_min)
            button.minWidth = resources.getDimensionPixelSize(R.dimen.ttt_touch_target_min)
        }

        updatePlayerIndicator()

        // Neutral banner prompt
        setBannerNeutral(
            if (gameMode == GameMode.COMPUTER) {
                // Hint which side the user plays.
                val user = "You: $playerSymbol"
                val comp = "${getString(R.string.computer_label)}: $aiSymbol"
                "Tap a square to start • $user • $comp"
            } else {
                "Tap a square to start"
            },
        )

        // If computer should start, schedule AI move.
        maybeTriggerAiTurn()
    }

    // PUBLIC_INTERFACE
    private fun onCellTapped(index: Int) {
        /** Handles the full game flow when a cell is tapped. */
        if (gameOver) return

        // In Computer mode, ignore taps during AI thinking or if it isn't the player's turn.
        if (gameMode == GameMode.COMPUTER) {
            if (aiThinking) return
            if (currentPlayer != playerSymbol) return
        }

        // Ignore taps on occupied squares
        if (board[index] != null) return

        applyMove(index, currentPlayer)

        // After a user move in computer mode, possibly trigger AI.
        maybeTriggerAiTurn()
    }

    private fun maybeTriggerAiTurn() {
        if (gameOver) return
        if (gameMode != GameMode.COMPUTER) return
        if (currentPlayer != aiSymbol) return

        // Schedule AI move after a short delay for UX.
        aiThinking = true
        lockBoardForAiThinking()
        setBannerNeutral("Computer is thinking…")

        mainHandler.postDelayed(
            {
                if (gameOver) return@postDelayed

                val aiMove = AiPlayer.chooseMove(board, aiSymbol)
                Log.d("MainActivity", "AI move chosen: $aiMove for symbol=$aiSymbol")

                if (aiMove == null) {
                    // Should only happen when board is full; fall back to draw check.
                    aiThinking = false
                    unlockBoardAfterAiThinking()
                    if (isDraw()) {
                        endDraw()
                    }
                    return@postDelayed
                }

                applyMove(aiMove, aiSymbol)

                // If game continues and it's now player's turn, re-enable board.
                if (!gameOver) {
                    aiThinking = false
                    unlockBoardAfterAiThinking()
                    setBannerNeutral("Your turn: $currentPlayer")
                }
            },
            350L,
        )
    }

    private fun applyMove(index: Int, symbol: Char) {
        // Place move
        board[index] = symbol
        cellButtons[index].text = symbol.toString()
        applyMarkStyling(index, symbol)

        // Check end conditions
        val winner = findWinner()
        if (winner != null) {
            gameOver = true

            // Persist scoreboard update (cumulative, across restarts).
            if (winner == 'X') {
                ScorePrefs.incrementXWins(this)
            } else {
                ScorePrefs.incrementOWins(this)
            }

            setBannerWin(
                if (gameMode == GameMode.COMPUTER) {
                    if (winner == playerSymbol) "You win!" else "Computer wins!"
                } else {
                    "Player $winner wins!"
                },
            )
            lockBoard()
            return
        }

        if (isDraw()) {
            endDraw()
            return
        }

        // Continue game: swap player and update UI
        currentPlayer = other(symbol)
        updatePlayerIndicator()

        // In 2P, keep prior behavior of prompting with current player.
        if (gameMode == GameMode.TWO_PLAYERS) {
            setBannerNeutral("Your turn: $currentPlayer")
        } else {
            // In Computer mode, we will set a "thinking" banner when AI triggers.
            if (currentPlayer == playerSymbol) {
                setBannerNeutral("Your turn: $currentPlayer")
            }
        }
    }

    private fun endDraw() {
        gameOver = true

        // Persist scoreboard update (cumulative, across restarts).
        ScorePrefs.incrementDraws(this)

        setBannerDraw("It's a draw!")
        lockBoard()
    }

    private fun lockBoardForAiThinking() {
        // Disable all cells while AI is thinking to prevent accidental taps.
        cellButtons.forEach { button ->
            button.isEnabled = false
            button.alpha = 0.65f
        }
    }

    private fun unlockBoardAfterAiThinking() {
        // Re-enable only empty cells (occupied remain effectively blocked by logic).
        cellButtons.forEachIndexed { idx, button ->
            button.isEnabled = (board[idx] == null) && !gameOver
            button.alpha = 1.0f
        }
    }

    private fun lockBoard() {
        // Prevent further moves once game ends.
        aiThinking = false
        cellButtons.forEach { button ->
            button.isEnabled = false
            // Disabled visuals: subtly reduce prominence while keeping legibility.
            button.alpha = 0.55f
        }
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
        // In Computer mode, show both whose turn (X/O) and which side.
        playerIndicator.text =
            if (gameMode == GameMode.COMPUTER) {
                val owner = if (currentPlayer == playerSymbol) "You" else getString(R.string.computer_label)
                "$owner: $currentPlayer"
            } else {
                "Player: $currentPlayer"
            }

        // Use distinct accent per player for stronger visual guidance.
        val colorRes = if (currentPlayer == 'X') R.color.ttt_primary else R.color.ttt_success
        playerIndicator.setTextColor(getColor(colorRes))
    }

    private fun setBannerNeutral(message: String) {
        resultBanner.text = message
        resultBanner.setTextColor(getColor(R.color.ttt_secondary))
        // Announce updates for accessibility services.
        resultBanner.announceForAccessibility(message)
    }

    private fun setBannerWin(message: String) {
        resultBanner.text = message
        // Win = success accent (requested)
        resultBanner.setTextColor(getColor(R.color.ttt_success))
        resultBanner.announceForAccessibility(message)
    }

    private fun setBannerDraw(message: String) {
        resultBanner.text = message
        // Draw = secondary
        resultBanner.setTextColor(getColor(R.color.ttt_secondary))
        resultBanner.announceForAccessibility(message)
    }

    private fun applyMarkStyling(index: Int, mark: Char) {
        val button = cellButtons[index]
        val colorRes = if (mark == 'X') R.color.ttt_primary else R.color.ttt_success
        button.setTextColor(getColor(colorRes))

        // Also update contentDescription so TalkBack reads the state of the cell.
        val position = index + 1
        button.contentDescription = "Cell $position of 9: $mark"
        button.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    private fun other(symbol: Char): Char = if (symbol == 'X') 'O' else 'X'
}
