package com.example.uitnotify.monitors

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

class SingleLiveEvent<T> : LiveData<T>() {
    private val pending = AtomicBoolean(false)

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(owner) {
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(it)
            }
        }
    }

    public override fun setValue(value: T) {
        pending.set(true)
        super.setValue(value)
    }
}

object AppClosedEvent {
    val appClosedEvent = SingleLiveEvent<Boolean>()
}