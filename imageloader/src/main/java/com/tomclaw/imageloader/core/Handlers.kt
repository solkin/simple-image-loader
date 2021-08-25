package com.tomclaw.imageloader.core

class Handlers<T> {

    var success: (ViewHolder<T>, Result) -> Unit = { _, _ -> }
    var placeholder: (ViewHolder<T>) -> Unit = {}
    var error: (ViewHolder<T>) -> Unit = {}

    fun placeholderHandler(placeholder: (ViewHolder<T>) -> Unit) = apply {
        this.placeholder = placeholder
    }

    fun errorHandler(error: (ViewHolder<T>) -> Unit) = apply {
        this.error = error
    }

    fun successHandler(success: (ViewHolder<T>, Result) -> Unit) = apply {
        this.success = success
    }

}