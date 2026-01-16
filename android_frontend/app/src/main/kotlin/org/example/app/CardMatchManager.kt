package org.example.app

import kotlin.math.min
import kotlin.random.Random

/**
 * Card Match (memory) game engine. Holds and mutates game state, independent from Android UI.
 */
internal class CardMatchManager(
    private val random: Random = Random.Default,
) {

    private var state: CardMatchState = newGameState(cardCount = 12)

    /**
     * PUBLIC_INTERFACE
     *
     * Get current game state.
     */
    // PUBLIC_INTERFACE
    fun getState(): CardMatchState = state

    /**
     * PUBLIC_INTERFACE
     *
     * Start a fresh game.
     *
     * @param cardCount number of cards; must be even.
     */
    // PUBLIC_INTERFACE
    fun restart(cardCount: Int) {
        state = newGameState(cardCount)
    }

    /**
     * PUBLIC_INTERFACE
     *
     * Restore from a previously saved state.
     */
    // PUBLIC_INTERFACE
    fun restore(restored: CardMatchState) {
        state = restored
    }

    /**
     * Result of a user flip action. UI can use this to announce events / schedule flip-back.
     */
    internal sealed class FlipResult {
        data object Ignored : FlipResult()
        data class Flipped(val index: Int) : FlipResult()
        data class Match(val firstIndex: Int, val secondIndex: Int, val value: Int) : FlipResult()
        data class Mismatch(val firstIndex: Int, val secondIndex: Int) : FlipResult()
    }

    /**
     * PUBLIC_INTERFACE
     *
     * Attempt to flip a card at [index]. Enforces the rule "at most two cards face up at a time".
     *
     * - If two cards become face-up and match: mark them matched (and keep face-up).
     * - If they mismatch: keep them face-up for now; UI should call [flipBackUnmatchedPair] after a delay.
     */
    // PUBLIC_INTERFACE
    fun flip(index: Int): FlipResult {
        val s = state
        if (index < 0 || index >= s.cardCount()) return FlipResult.Ignored

        // Don't allow interaction with matched or already face-up cards.
        if (s.matched[index] || s.faceUp[index]) return FlipResult.Ignored

        // If already two selected and waiting to flip back, ignore further taps.
        if (s.firstSelectedIndex >= 0 && s.secondSelectedIndex >= 0) return FlipResult.Ignored

        val nextFaceUp = s.faceUp.copyOf()
        nextFaceUp[index] = true

        var first = s.firstSelectedIndex
        var second = s.secondSelectedIndex
        var moves = s.moves
        var matchedPairs = s.matchedPairs
        val matched = s.matched.copyOf()

        if (first < 0) {
            first = index
            state = s.copy(faceUp = nextFaceUp, firstSelectedIndex = first)
            return FlipResult.Flipped(index)
        }

        // Second flip of the turn.
        second = index
        moves += 1

        val firstValue = s.values[first]
        val secondValue = s.values[second]

        return if (firstValue == secondValue) {
            matched[first] = true
            matched[second] = true
            matchedPairs += 1
            // Keep them face up and clear selections.
            state = s.copy(
                faceUp = nextFaceUp,
                matched = matched,
                moves = moves,
                matchedPairs = matchedPairs,
                firstSelectedIndex = -1,
                secondSelectedIndex = -1,
            )
            FlipResult.Match(firstIndex = first, secondIndex = second, value = firstValue)
        } else {
            // Keep both face up for now, mark that we have a pending pair.
            state = s.copy(
                faceUp = nextFaceUp,
                moves = moves,
                firstSelectedIndex = first,
                secondSelectedIndex = second,
            )
            FlipResult.Mismatch(firstIndex = first, secondIndex = second)
        }
    }

    /**
     * PUBLIC_INTERFACE
     *
     * If the current selection is a mismatched pair, flip them back down.
     * Safe to call even if no mismatched pair is currently selected.
     */
    // PUBLIC_INTERFACE
    fun flipBackUnmatchedPair() {
        val s = state
        val first = s.firstSelectedIndex
        val second = s.secondSelectedIndex
        if (first < 0 || second < 0) return

        // If they were matched somehow, do nothing.
        if (s.matched[first] || s.matched[second]) {
            state = s.copy(firstSelectedIndex = -1, secondSelectedIndex = -1)
            return
        }

        val nextFaceUp = s.faceUp.copyOf()
        nextFaceUp[first] = false
        nextFaceUp[second] = false

        state = s.copy(
            faceUp = nextFaceUp,
            firstSelectedIndex = -1,
            secondSelectedIndex = -1,
        )
    }

    private fun newGameState(cardCount: Int): CardMatchState {
        require(cardCount > 0 && cardCount % 2 == 0) { "cardCount must be a positive even number" }

        // Values are 0..pairs-1 twice, shuffled.
        val pairCount = cardCount / 2
        val values = IntArray(cardCount)
        var idx = 0
        for (v in 0 until pairCount) {
            values[idx++] = v
            values[idx++] = v
        }

        // Shuffle.
        for (i in values.indices.reversed()) {
            val j = random.nextInt(i + 1)
            val tmp = values[i]
            values[i] = values[j]
            values[j] = tmp
        }

        return CardMatchState(
            values = values,
            faceUp = BooleanArray(cardCount) { false },
            matched = BooleanArray(cardCount) { false },
            moves = 0,
            matchedPairs = 0,
            firstSelectedIndex = -1,
            secondSelectedIndex = -1,
        )
    }

    /**
     * Decide a responsive card count based on available width. We prefer 4 columns; fall back to 3 on narrow screens.
     */
    internal fun chooseCardCountForWidthPx(widthPx: Int): Int {
        // Conservative heuristic:
        // - If screen is wide enough, use 16 cards (4x4).
        // - Otherwise 12 cards (4x3).
        val threshold = 720 // px; intentionally simple and density-agnostic for this sample
        return if (widthPx >= threshold) 16 else 12
    }

    internal fun chooseColumnCountForWidthPx(widthPx: Int): Int {
        // Keep 4 columns for both 12 and 16; use 3 for very narrow.
        val narrowThreshold = 480
        return if (widthPx < narrowThreshold) 3 else 4
    }

    internal fun maxPairs(state: CardMatchState): Int = min(state.cardCount() / 2, Int.MAX_VALUE)
}
