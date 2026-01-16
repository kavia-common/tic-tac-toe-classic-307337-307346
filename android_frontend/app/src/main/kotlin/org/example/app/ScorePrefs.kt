package org.example.app

import android.content.Context

internal object ScorePrefs {
    const val PREFS_NAME: String = "ttt_scoreboard"
    const val KEY_X_WINS: String = "X_WINS"
    const val KEY_O_WINS: String = "O_WINS"
    const val KEY_DRAWS: String = "DRAWS"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getXWins(context: Context): Int = prefs(context).getInt(KEY_X_WINS, 0)
    fun getOWins(context: Context): Int = prefs(context).getInt(KEY_O_WINS, 0)
    fun getDraws(context: Context): Int = prefs(context).getInt(KEY_DRAWS, 0)

    fun incrementXWins(context: Context) {
        val p = prefs(context)
        val next = p.getInt(KEY_X_WINS, 0) + 1
        p.edit().putInt(KEY_X_WINS, next).apply()
    }

    fun incrementOWins(context: Context) {
        val p = prefs(context)
        val next = p.getInt(KEY_O_WINS, 0) + 1
        p.edit().putInt(KEY_O_WINS, next).apply()
    }

    fun incrementDraws(context: Context) {
        val p = prefs(context)
        val next = p.getInt(KEY_DRAWS, 0) + 1
        p.edit().putInt(KEY_DRAWS, next).apply()
    }

    fun reset(context: Context) {
        prefs(context).edit()
            .putInt(KEY_X_WINS, 0)
            .putInt(KEY_O_WINS, 0)
            .putInt(KEY_DRAWS, 0)
            .apply()
    }
}
