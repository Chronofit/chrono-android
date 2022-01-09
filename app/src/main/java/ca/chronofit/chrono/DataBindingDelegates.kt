package ca.chronofit.chrono

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * This is a replacement for DataBindingUtil.inflate(inflater, layoutRes,
 * rootView, boolean)
 *
 * use private val binding: FragmentMainBinding by BindFragment(R.layout.fragment_main) instead
 */
class BindFragment<in R : Fragment, out T : ViewDataBinding>(
    @LayoutRes private val layoutRes: Int
) : ReadOnlyProperty<R, T> {

    private var value: T? = null

    override operator fun getValue(thisRef: R, property: KProperty<*>): T {
        if (value == null) {
            value = DataBindingUtil.inflate<T>(
                thisRef.layoutInflater, layoutRes,
                thisRef.view?.rootView as ViewGroup?, false
            )
        }
        return value!!
    }
}

/**
 * Binds a view to its item.
 *
 * val binding: ItemPlanetBinding = view.bind()
 */
fun <T : ViewDataBinding> View.bind(): T = DataBindingUtil.bind(this)!!