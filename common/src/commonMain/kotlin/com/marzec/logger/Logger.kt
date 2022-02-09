package com.marzec.logger

interface Logger {

    fun log(tag: String, message: String)

    companion object {

        lateinit var logger: Logger
    }
}
