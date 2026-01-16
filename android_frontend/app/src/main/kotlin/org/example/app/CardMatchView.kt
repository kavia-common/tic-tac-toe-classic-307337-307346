package org.example.app

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import androidx.gridlayout.widget.GridLayout

/**
 * A custom view that displays and controls a Card Match (memory) game using classic Views (no Compose).
 *
 * This view:
 * - Builds a responsive GridLayout of Buttons.
 * - Delegates rules/state to [CardMatchManager].
 * - Exposes callbacks so the host Activity can render metrics and announce accessibility updates.
 */
class CardMatchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : GridLayout(context, attrs, defStyleAttr) {

    private val manager = CardMatchManager()
    private var cardButtons: Array<Button> = emptyArray()

    private var lockInteractionsUntilUptimeMs: Long = 0L

    private var onMetricsChanged: ((moves: Int, matchedPairs: Int, totalPairs: Int) -> Unit)? = null
    private var onAnnounce: ((message: String) -> Unit)? = null

    /**
     * PUBLIC_INTERFACE
     *
     * Set a listener that receives updated moves / pair counts whenever the game state changes.
     */
    // PUBLIC_INTERFACE
    fun setOnMetricsChangedListener(listener: ((moves: Int, matchedPairs: Int, totalPairs: Int) -> Unit)?) {
        onMetricsChanged = listener
        notifyMetrics()
    }

    /**
     * PUBLIC_INTERFACE
     *
     * Set a listener to announce accessibility messages (Activity can route it to a live region).
     */
    // PUBLIC_INTERFACE
    fun setOnAnnounceListener(listener: ((message: String) -> Unit)?) {
        onAnnounce = listener
    }

    /**
     * PUBLIC_INTERFACE
     *
     * Restart this card game. Does not affect Tic Tac Toe.
     */
    // PUBLIC_INTERFACE
    fun restartGame() {
        val width = measuredWidth.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val cardCount = manager.chooseCardCountForWidthPx(width)
        manager.restart(cardCount)
        rebuildGridForCurrentState()
        render()
        announce("Restarted card game")
    }

    /**
     * PUBLIC_INTERFACE
     *
     * Export state for Activity to save during configuration changes.
     */
    // PUBLIC_INTERFACE
    fun exportState(): CardMatchState = manager.getState()

    /**
     * PUBLIC_INTERFACE
     *
     * Restore previously saved state. Caller should invoke after view is inflated.
     */
    // PUBLIC_INTERFACE
    fun restoreState(restored: CardMatchState) {
        manager.restore(restored)
        rebuildGridForCurrentState()
        render()
        announce("Restored card game")
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Build initial grid if needed.
        if (cardButtons.isEmpty()) {
            restartGame()
        }
    }

    private fun rebuildGridForCurrentState() {
        removeAllViews()

        val s = manager.getState()
        val width = measuredWidth.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val columns = manager.chooseColumnCountForWidthPx(width)

        columnCount = columns
        rowCount = (s.cardCount() + columns - 1) / columns
        useDefaultMargins = false
        alignmentMode = ALIGN_MARGINS

        cardButtons = Array(s.cardCount()) { index ->
            val b = Button(context).apply {
                id = View.generateViewId()
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 0
                    columnSpec = spec(UNDEFINED, 1f)
                    rowSpec = spec(UNDEFINED, 1f)

                    val m = resources.getDimensionPixelSize(R.dimen.ttt_card_match_cell_margin)
                    setMargins(m, m, m, m)
                }

                minHeight = resources.getDimensionPixelSize(R.dimen.ttt_card_match_cell_min_size)
                minWidth = resources.getDimensionPixelSize(R.dimen.ttt_touch_target_min)
                textSize = resources.getDimension(R.dimen.ttt_card_match_text) / resources.displayMetrics.scaledDensity
                isAllCaps = false

                setOnClickListener { onCardClicked(index) }
            }
            addView(b)
            b
        }
    }

    private fun onCardClicked(index: Int) {
        // If currently waiting to flip back, ignore.
        if (SystemClock.uptimeMillis() < lockInteractionsUntilUptimeMs) return

        val result = manager.flip(index)
        when (result) {
            is CardMatchManager.FlipResult.Ignored -> return
            is CardMatchManager.FlipResult.Flipped -> {
                render()
                announce("Card revealed")
            }
            is CardMatchManager.FlipResult.Match -> {
                render()
                announce("Match found")
            }
            is CardMatchManager.FlipResult.Mismatch -> {
                render()
                announce("Not a match")
                // Lock taps briefly and flip back after delay.
                lockInteractionsUntilUptimeMs = SystemClock.uptimeMillis() + FLIP_BACK_DELAY_MS
                postDelayed(
                    {
                        manager.flipBackUnmatchedPair()
                        render()
                    },
                    FLIP_BACK_DELAY_MS,
                )
            }
        }
    }

    private fun render() {
        val s = manager.getState()
        if (cardButtons.size != s.cardCount()) {
            rebuildGridForCurrentState()
        }

        for (i in 0 until s.cardCount()) {
            val b = cardButtons[i]
            val faceUp = s.faceUp[i]
            val matched = s.matched[i]

            if (faceUp || matched) {
                b.text = valueToSymbol(s.values[i])
                b.background = context.getDrawable(R.drawable.ttt_card_match_card_up)
                b.isEnabled = !matched
                b.alpha = if (matched) 0.75f else 1.0f
                b.contentDescription = if (matched) {
                    "Card ${i + 1} of ${s.cardCount()}: matched ${b.text}"
                } else {
                    "Card ${i + 1} of ${s.cardCount()}: face up ${b.text}"
                }
            } else {
                b.text = ""
                b.background = context.getDrawable(R.drawable.ttt_card_match_card_down)
                b.isEnabled = true
                b.alpha = 1.0f
                b.contentDescription = "Card ${i + 1} of ${s.cardCount()}: face down"
            }
            b.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        }

        notifyMetrics()
    }

    private fun notifyMetrics() {
        val s = manager.getState()
        val totalPairs = s.cardCount() / 2
        onMetricsChanged?.invoke(s.moves, s.matchedPairs, totalPairs)
    }

    private fun announce(message: String) {
        onAnnounce?.invoke(message)
        // Also announce directly from the view for redundancy; host still provides live region.
        announceForAccessibility(message)
    }

    private fun valueToSymbol(value: Int): String {
        // Simple, dependency-free "icon set": letters A..H (supports up to 16 cards / 8 pairs).
        val letter = ('A'.code + (value % 26)).toChar()
        return letter.toString()
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val bundle = Bundle()
        bundle.putParcelable(STATE_SUPER, superState)

        val s = manager.getState()
        bundle.putIntArray(STATE_VALUES, s.values)
        bundle.putBooleanArray(STATE_FACE_UP, s.faceUp)
        bundle.putBooleanArray(STATE_MATCHED, s.matched)
        bundle.putInt(STATE_MOVES, s.moves)
        bundle.putInt(STATE_MATCHED_PAIRS, s.matchedPairs)
        bundle.putInt(STATE_FIRST, s.firstSelectedIndex)
        bundle.putInt(STATE_SECOND, s.secondSelectedIndex)

        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            val superState = state.getParcelable<Parcelable>(STATE_SUPER)
            super.onRestoreInstanceState(superState)

            val values = state.getIntArray(STATE_VALUES)
            val faceUp = state.getBooleanArray(STATE_FACE_UP)
            val matched = state.getBooleanArray(STATE_MATCHED)

            if (values != null && faceUp != null && matched != null) {
                val restored = CardMatchState(
                    values = values,
                    faceUp = faceUp,
                    matched = matched,
                    moves = state.getInt(STATE_MOVES, 0),
                    matchedPairs = state.getInt(STATE_MATCHED_PAIRS, 0),
                    firstSelectedIndex = state.getInt(STATE_FIRST, -1),
                    secondSelectedIndex = state.getInt(STATE_SECOND, -1),
                )
                restoreState(restored)
                return
            }
        }
        super.onRestoreInstanceState(state)
    }

    private companion object {
        private const val FLIP_BACK_DELAY_MS = 650L

        private const val STATE_SUPER = "cm_super"
        private const val STATE_VALUES = "cm_values"
        private const val STATE_FACE_UP = "cm_face_up"
        private const val STATE_MATCHED = "cm_matched"
        private const val STATE_MOVES = "cm_moves"
        private const val STATE_MATCHED_PAIRS = "cm_matched_pairs"
        private const val STATE_FIRST = "cm_first"
        private const val STATE_SECOND = "cm_second"
    }
}
