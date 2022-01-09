package ca.chronofit.chrono.util.objects

import android.content.Context
import ca.chronofit.chrono.R
import java.util.Date

data class CircuitObject(
    val name: String,
    val sets: Int,
    val work: Int,
    val rest: Int,
    val count: Int,
    val iconId: Int,
) {

    override fun toString(): String {
        return "Circuit [name: ${this.name}, sets: ${this.sets}, work: ${this.work}, rest: ${this.rest}]"
    }

    fun generateDeeplinkURL(context: Context): String {
        return context.getString(
            R.string.circuit_deeplink_url,
            name,
            sets.toString(),
            work.toString(),
            rest.toString()
        )
    }

}