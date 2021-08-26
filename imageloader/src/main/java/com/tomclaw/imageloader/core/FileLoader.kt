package com.tomclaw.imageloader.core

import java.io.File

interface FileLoader {

    val schemes: List<String>

    fun load(uri: String, file: File): Boolean

}
