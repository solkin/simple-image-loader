package com.tomclaw.imageloader.core

interface ViewHolder<T> {

    fun getSize(): ViewSize

    var tag: Any?

    fun get(): T

}