package com.tomclaw.imageloader.core

import java.io.File

interface Decoder {

    fun decode(file: File, width: Int, height: Int): Result?

}