package com.tomclaw.imageloader.core

interface ViewHolder<T> {

    fun optSize(): ViewSize?

    /**
     * Calls [onSize] on the main thread once the view has its size: at once if it has one
     * already, else as soon as it is laid out with one. Never blocks; called on the main thread.
     *
     * @return a handle that stops waiting, so that [onSize] is not called after all
     */
    fun awaitSize(onSize: (ViewSize) -> Unit): Cancellable

    var tag: Any?

    fun get(): T

}

fun interface Cancellable {

    fun cancel()

}
