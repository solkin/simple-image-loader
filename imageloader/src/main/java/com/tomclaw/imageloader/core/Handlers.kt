package com.tomclaw.imageloader.core

class Handlers<T> {

    var success: (ViewHolder<T>, Result) -> Unit = { _, _ -> }
        private set
    var placeholder: (ViewHolder<T>) -> Unit = {}
        private set
    var error: (ViewHolder<T>) -> Unit = {}
        private set

    /**
     * Adds a placeholder handler to the chain.
     * Multiple handlers can be composed by calling this method multiple times.
     */
    fun placeholderHandler(handler: (ViewHolder<T>) -> Unit) = apply {
        val previous = this.placeholder
        this.placeholder = { view ->
            previous(view)
            handler(view)
        }
    }

    /**
     * Adds an error handler to the chain.
     * Multiple handlers can be composed by calling this method multiple times.
     */
    fun errorHandler(handler: (ViewHolder<T>) -> Unit) = apply {
        val previous = this.error
        this.error = { view ->
            previous(view)
            handler(view)
        }
    }

    /**
     * Adds a success handler to the chain.
     * Multiple handlers can be composed by calling this method multiple times.
     */
    fun successHandler(handler: (ViewHolder<T>, Result) -> Unit) = apply {
        val previous = this.success
        this.success = { view, result ->
            previous(view, result)
            handler(view, result)
        }
    }

}