package ca.chronofit.chrono.util.objects

import android.content.Context
import ca.chronofit.chrono.R
import java.util.Date

class CircuitObject {
    var name: String? = null
    var sets: Int? = null
    var work: Int? = null
    var rest: Int? = null
    var count: Int? = null
    var iconId: Int? = null
    var date: Date? = null

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