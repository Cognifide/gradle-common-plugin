package com.cognifide.gradle.common.file.transfer.sftp

import com.cognifide.gradle.common.file.FileException

class SftpFileException : FileException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
