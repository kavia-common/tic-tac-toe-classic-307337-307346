package org.example.app

/**
 * Models for the Card Match (memory) game.
 */
internal data class CardMatchCard(
    val id: Int,
    val value: Int,
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false,
)

/**
 * Persistable state for Card Match game (configuration changes).
 *
 * Stored as primitive arrays in a Bundle.
 */
internal data class CardMatchState(
    val values: IntArray,
    val faceUp: BooleanArray,
    val matched: BooleanArray,
    val moves: Int,
    val matchedPairs: Int,
    val firstSelectedIndex: Int, // -1 if none
    val secondSelectedIndex: Int, // -1 if none (useful while waiting for flip-back)
) {
    init {
        require(values.size == faceUp.size && values.size == matched.size) { "State arrays must match size" }
    }

    fun cardCount(): Int = values.size
}
