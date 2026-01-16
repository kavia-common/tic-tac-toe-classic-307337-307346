package org.example.app

import android.util.Log
import kotlin.random.Random

/**
 * Provides move selection for a 3x3 Tic Tac Toe board.
 *
 * Board indices:
 * 0 1 2
 * 3 4 5
 * 6 7 8
 */
internal object AiPlayer {

    private const val TAG = "AiPlayer"

    /**
     * Difficulty levels for Computer mode AI.
     */
    internal enum class Difficulty {
        EASY,
        MEDIUM,
        HARD,
    }

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

    /**
     * PUBLIC_INTERFACE
     *
     * Choose the best move index (0..8) for [aiSymbol] given the current [board].
     *
     * Uses minimax for optimal play on a 3x3 board.
     *
     * @param board Current board state. Must be length 9. null means empty.
     * @param aiSymbol The symbol the AI will place ('X' or 'O').
     * @return An empty cell index (0..8), or null if there are no available moves.
     */
    // PUBLIC_INTERFACE
    fun chooseMove(board: Array<Char?>, aiSymbol: Char): Int? {
        require(board.size == 9) { "board must be size 9" }
        require(aiSymbol == 'X' || aiSymbol == 'O') { "aiSymbol must be X or O" }

        val opponent = other(aiSymbol)

        // If no moves, return null.
        val available = availableMoves(board)
        if (available.isEmpty()) return null

        var bestScore = Int.MIN_VALUE
        var bestMove: Int? = null

        // For determinism, iterate in index order.
        for (move in available) {
            val next = board.copyOf()
            next[move] = aiSymbol
            val score = minimax(
                board = next,
                maximizing = false,
                aiSymbol = aiSymbol,
                opponentSymbol = opponent,
                depth = 0,
            )
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }

        Log.d(TAG, "chooseMove(ai=$aiSymbol) -> $bestMove (score=$bestScore)")
        return bestMove
    }

    /**
     * PUBLIC_INTERFACE
     *
     * Choose a move using a [difficulty] policy:
     * - EASY: random available move.
     * - MEDIUM: 50% random, 50% minimax best.
     * - HARD: minimax best (same as [chooseMove]).
     *
     * @param board Current board state. Must be length 9. null means empty.
     * @param aiSymbol The symbol the AI will place ('X' or 'O').
     * @param difficulty AI difficulty policy.
     * @return An empty cell index (0..8), or null if there are no available moves.
     */
    // PUBLIC_INTERFACE
    fun chooseMove(
        board: Array<Char?>,
        aiSymbol: Char,
        difficulty: Difficulty,
    ): Int? {
        require(board.size == 9) { "board must be size 9" }
        require(aiSymbol == 'X' || aiSymbol == 'O') { "aiSymbol must be X or O" }

        val available = availableMoves(board)
        if (available.isEmpty()) return null

        val move = when (difficulty) {
            Difficulty.EASY -> chooseRandomMove(available)
            Difficulty.MEDIUM -> {
                // 50% random / 50% best
                if (Random.nextBoolean()) chooseRandomMove(available) else chooseMove(board, aiSymbol)
            }
            Difficulty.HARD -> chooseMove(board, aiSymbol)
        }

        Log.d(TAG, "chooseMove(ai=$aiSymbol,difficulty=$difficulty) -> $move")
        return move
    }

    private fun chooseRandomMove(availableMoves: List<Int>): Int {
        // Kotlin Random is fine here; no need for cryptographic strength.
        return availableMoves[Random.nextInt(availableMoves.size)]
    }

    private fun minimax(
        board: Array<Char?>,
        maximizing: Boolean,
        aiSymbol: Char,
        opponentSymbol: Char,
        depth: Int,
    ): Int {
        val winner = findWinner(board)
        if (winner != null) {
            // Prefer faster wins / slower losses.
            return if (winner == aiSymbol) (10 - depth) else (-10 + depth)
        }
        if (isDraw(board)) return 0

        val moves = availableMoves(board)
        if (maximizing) {
            var best = Int.MIN_VALUE
            for (m in moves) {
                val next = board.copyOf()
                next[m] = aiSymbol
                best = maxOf(
                    best,
                    minimax(next, maximizing = false, aiSymbol = aiSymbol, opponentSymbol = opponentSymbol, depth = depth + 1),
                )
            }
            return best
        } else {
            var best = Int.MAX_VALUE
            for (m in moves) {
                val next = board.copyOf()
                next[m] = opponentSymbol
                best = minOf(
                    best,
                    minimax(next, maximizing = true, aiSymbol = aiSymbol, opponentSymbol = opponentSymbol, depth = depth + 1),
                )
            }
            return best
        }
    }

    private fun availableMoves(board: Array<Char?>): List<Int> {
        val result = ArrayList<Int>(9)
        for (i in board.indices) {
            if (board[i] == null) result.add(i)
        }
        return result
    }

    private fun findWinner(board: Array<Char?>): Char? {
        for (line in winLines) {
            val a = board[line[0]]
            val b = board[line[1]]
            val c = board[line[2]]
            if (a != null && a == b && b == c) return a
        }
        return null
    }

    private fun isDraw(board: Array<Char?>): Boolean {
        return board.all { it != null } && findWinner(board) == null
    }

    private fun other(symbol: Char): Char = if (symbol == 'X') 'O' else 'X'
}
