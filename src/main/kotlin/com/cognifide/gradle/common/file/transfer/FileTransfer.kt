package com.cognifide.gradle.common.file.transfer

import java.io.File

interface FileTransfer {

    /**
     * Checks if supports particular URL
     */
    fun handles(fileUrl: String): Boolean

    /**
     * Downloads file with given name from directory available at specified URL.
     */
    fun downloadFrom(dirUrl: String, fileName: String, target: File)

    /**
     * Downloads file from specified URL.
     */
    fun download(fileUrl: String, target: File) {
        val (dirUrl, fileName) = FileUtils.splitUrl(fileUrl)
        downloadFrom(dirUrl, fileName, target)
    }

    /**
     * Downloads file from specified URL to temporary directory with preserving file name.
     */
    fun download(fileUrl: String): File

    /**
     * Downloads file from specified URL to specified directory with preserving file name.
     */
    fun downloadTo(fileUrl: String, dir: File) = File(dir, FileUtils.nameFromUrl(fileUrl)).apply {
        download(fileUrl, this)
    }

    /**
     * Uploads file to directory available at specified URL and set given name.
     */
    fun uploadTo(dirUrl: String, fileName: String, source: File)

    /**
     * Uploads file to directory available at specified URL.
     */
    fun uploadTo(dirUrl: String, source: File) {
        val fileName = source.name

        uploadTo(dirUrl, fileName, source)
    }

    /**
     * Uploads file to specified URL.
     */
    fun upload(fileUrl: String, source: File) {
        val (dirUrl, fileName) = FileUtils.splitUrl(fileUrl)
        uploadTo(dirUrl, fileName, source)
    }

    /**
     * Deletes file of given name in directory available at specified URL.
     */
    fun deleteFrom(dirUrl: String, fileName: String)

    /**
     * Deletes file available at specified URL.
     */
    fun delete(fileUrl: String) {
        val (dirUrl, fileName) = FileUtils.splitUrl(fileUrl)
        deleteFrom(dirUrl, fileName)
    }

    /**
     * Lists files in directory available at specified URL.
     */
    fun list(dirUrl: String): List<FileEntry>

    /**
     * Deletes all files in directory available at specified URL.
     */
    fun truncate(dirUrl: String)

    /**
     * Checks if file at specified URL exists.
     */
    fun exists(fileUrl: String): Boolean {
        val (dirUrl, fileName) = FileUtils.splitUrl(fileUrl)
        return exists(dirUrl, fileName)
    }

    /**
     * Checks if file with given name exists in directory at specified URL.
     */
    fun exists(dirUrl: String, fileName: String): Boolean = stat(dirUrl, fileName) != null

    /**
     * Gets file status of given name in directory at specified URL.
     */
    fun stat(dirUrl: String, fileName: String): FileEntry?

    /**
     * Gets file status at specified URL.
     */
    fun stat(fileUrl: String): FileEntry? {
        val (dirUrl, fileName) = FileUtils.splitUrl(fileUrl)
        return stat(dirUrl, fileName)
    }
}
