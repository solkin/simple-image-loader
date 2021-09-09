package com.tomclaw.imageloader.core

interface ViewHolder<T> {

    fun optSize(): ViewSize?

    fun getSize(): ViewSize

    var tag: Any?

    fun get(): T

}