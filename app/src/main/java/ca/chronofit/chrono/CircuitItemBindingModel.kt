package ca.chronofit.chrono

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable

class CircuitItemBindingModel : BaseObservable() {

    @get:Bindable
    var name: String = ""
        set(value) {
            field = value
            notifyPropertyChanged(BR.name)
        }

    @get:Bindable
    var sets: Int = 0
        set(value) {
            field = value
            notifyPropertyChanged(BR.sets)
        }

    @get:Bindable
    var work: Int = 0
        set(value) {
            field = value
            notifyPropertyChanged(BR.work)
        }

    @get:Bindable
    var rest: Int = 0
        set(value) {
            field = value
            notifyPropertyChanged(BR.rest)
        }
}