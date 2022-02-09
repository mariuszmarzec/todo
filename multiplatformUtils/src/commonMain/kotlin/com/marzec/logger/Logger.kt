package com.marzec.logger

interface Logger {

    fun log(tag: String, message: String)

    companion object : Logger {

        lateinit var logger: Logger

        override fun log(tag: String, message: String) {
            logger.log(tag, message)
        }
    }
}
