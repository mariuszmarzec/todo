package com.marzec.logger

interface Logger {

    fun log(tag: String, message: String)

    fun log(tag: String, message: String, t: Throwable)

    companion object : Logger {

        lateinit var logger: Logger

        override fun log(tag: String, message: String) {
            logger.log(tag, message)
        }

        override fun log(tag: String, message: String, t: Throwable) {
            logger.log(tag, message, t)
        }
    }
}
