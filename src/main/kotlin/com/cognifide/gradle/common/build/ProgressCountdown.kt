package com.cognifide.gradle.common.build

import com.cognifide.gradle.common.utils.Formats
import org.gradle.api.Project

class ProgressCountdown(project: Project) {

    var time: Long = 0

    var timePrefix = "Waiting... time left"

    var loggerInterval = 100

    private val logger = ProgressLogger.of(project)

    private var progress: (Long) -> String = { "$timePrefix: ${Formats.duration(it)}" }

    fun progress(messageComposer: (Long) -> String) {
        this.progress = messageComposer
    }

    fun run() {
        if (time <= 0) {
            return
        }

        val start = System.currentTimeMillis()

        logger.launch {
            while (true) {
                val current = System.currentTimeMillis()
                val delta = current - start
                val countdown = time - delta

                if (countdown <= 0) {
                    break
                }

                logger.progress(progress(countdown))
                Behaviors.waitFor(loggerInterval)
            }
        }
    }
}
