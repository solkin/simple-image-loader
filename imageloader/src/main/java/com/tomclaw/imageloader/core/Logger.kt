package com.tomclaw.imageloader.core

import android.util.Log

/**
 * Logger interface for Simple Image Loader.
 * Implement this interface to customize logging behavior.
 */
interface Logger {

    fun d(tag: String, message: String)
    fun w(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)

    companion object {
        /**
         * Default logger that does nothing (production mode).
         */
        val NONE: Logger = object : Logger {
            override fun d(tag: String, message: String) {}
            override fun w(tag: String, message: String) {}
            override fun e(tag: String, message: String, throwable: Throwable?) {}
        }

        /**
         * Debug logger that outputs to Android Logcat.
         */
        val LOGCAT: Logger = object : Logger {
            override fun d(tag: String, message: String) {
                Log.d(tag, message)
            }

            override fun w(tag: String, message: String) {
                Log.w(tag, message)
            }

            override fun e(tag: String, message: String, throwable: Throwable?) {
                if (throwable != null) {
                    Log.e(tag, message, throwable)
                } else {
                    Log.e(tag, message)
                }
            }
        }
    }
}

/**
 * Global logger instance. Set to [Logger.LOGCAT] to enable debug logging.
 *
 * Example:
 * ```
 * // Enable debug logging
 * SimpleImageLoaderLog.logger = Logger.LOGCAT
 *
 * // Or use custom logger
 * SimpleImageLoaderLog.logger = object : Logger {
 *     override fun d(tag: String, message: String) {
 *         Timber.tag(tag).d(message)
 *     }
 *     // ...
 * }
 * ```
 */
object SimpleImageLoaderLog {
    @Volatile
    var logger: Logger = Logger.NONE
}

