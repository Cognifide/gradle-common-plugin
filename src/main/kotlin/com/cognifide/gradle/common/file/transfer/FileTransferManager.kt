package com.cognifide.gradle.common.file.transfer

import com.cognifide.gradle.common.CommonExtension
import com.cognifide.gradle.common.file.FileException
import com.cognifide.gradle.common.file.transfer.generic.CustomFileTransfer
import com.cognifide.gradle.common.file.transfer.generic.PathFileTransfer
import com.cognifide.gradle.common.file.transfer.generic.UrlFileTransfer
import com.cognifide.gradle.common.file.transfer.http.HttpFileTransfer
import com.cognifide.gradle.common.file.transfer.resolve.ResolveFileTransfer
import com.cognifide.gradle.common.file.transfer.sftp.SftpFileTransfer
import com.cognifide.gradle.common.file.transfer.smb.SmbFileTransfer
import java.io.File
import org.apache.commons.io.FilenameUtils

/**
 * Facade for transferring files over multiple protocols HTTP/SFTP/SMB and custom.
 *
 * Handles locking files for avoiding uncompleted downloads.
 * Prevents unnecessary download if file on local server already exist.
 * Prevents unnecessary uploads if file on remote servers already exist.
 */
class FileTransferManager(private val common: CommonExtension) : FileTransfer {

    private val logger = common.project.logger

    val factory = FileTransferFactory(common)

    val http = HttpFileTransfer(common)

    fun http(options: HttpFileTransfer.() -> Unit) {
        http.apply(options)
    }

    val sftp = SftpFileTransfer(common)

    fun sftp(options: SftpFileTransfer.() -> Unit) {
        sftp.apply(options)
    }

    val smb = SmbFileTransfer(common)

    fun smb(options: SmbFileTransfer.() -> Unit) {
        smb.apply(options)
    }

    val resolve = ResolveFileTransfer(common)

    fun resolve(options: ResolveFileTransfer.() -> Unit) {
        resolve.apply(options)
    }

    val url = UrlFileTransfer(common)

    fun url(options: UrlFileTransfer.() -> Unit) {
        url.apply(options)
    }

    val path = PathFileTransfer(common)

    fun path(options: PathFileTransfer.() -> Unit) {
        path.apply(options)
    }

    private val custom = mutableListOf<CustomFileTransfer>()

    private val all get() = (custom + arrayOf(http, sftp, smb, resolve, url, path)).filter { it.enabled }

    /**
     * Downloads file from specified URL to temporary directory with preserving file name.
     */
    override fun download(fileUrl: String) = download(fileUrl, common.temporaryFile(FilenameUtils.getName(fileUrl)))

    /**
     * Downloads file of given name from directory at specified URL.
     */
    override fun downloadFrom(dirUrl: String, fileName: String, target: File) = downloadUsing(handling(dirUrl), dirUrl, fileName, target)

    /**
     * Downloads file from specified URL using dedicated transfer type.
     */
    fun downloadUsing(transfer: FileTransfer, fileUrl: String, target: File) {
        val (dirUrl, fileName) = FileTransfer.splitFileUrl(fileUrl)
        return downloadUsing(transfer, dirUrl, fileName, target)
    }

    /**
     * Downloads file of given name from directory at specified URL using dedicated transfer type.
     */
    fun downloadUsing(transfer: FileTransfer, dirUrl: String, fileName: String, target: File) {
        if (target.exists()) {
            common.logger.info("Downloading file from URL '$dirUrl/$fileName' to '$target' skipped as of it already exists.")
            return
        }

        target.parentFile.mkdirs()

        val tmp = File(target.parentFile, "${target.name}$TMP_SUFFIX")
        if (tmp.exists()) {
            tmp.delete()
        }

        transfer.downloadFrom(dirUrl, fileName, tmp)
        tmp.renameTo(target)
    }

    /**
     * Uploads file to directory at specified URL and set given name.
     */
    override fun uploadTo(dirUrl: String, fileName: String, source: File) = uploadUsing(handling(dirUrl), dirUrl, fileName, source)

    /**
     * Uploads file to file at specified URL using dedicated transfer type.
     */
    fun uploadUsing(transfer: FileTransfer, fileUrl: String, source: File) {
        val (dirUrl, fileName) = FileTransfer.splitFileUrl(fileUrl)
        return uploadUsing(transfer, dirUrl, fileName, source)
    }

    /**
     * Uploads file to directory at specified URL and set given name using dedicated transfer type.
     */
    fun uploadUsing(transfer: FileTransfer, dirUrl: String, fileName: String, source: File) {
        val fileUrl = "$dirUrl/$fileName"

        try {
            if (stat(dirUrl, fileName) != null) { // 'stat' may be unsupported
                logger.info("Uploading file to URL '$fileUrl' skipped as of it already exists on server.")
                return
            }
        } catch (e: FileException) {
            logger.debug("Cannot check status of uploaded file at URL '$fileUrl'", e)
        }

        transfer.uploadTo(dirUrl, fileName, source)
    }

    /**
     * Lists files in directory available at specified URL.
     */
    override fun list(dirUrl: String): List<FileEntry> = handling(dirUrl).list(dirUrl)

    /**
     * Deletes file of given name in directory at specified URL.
     */
    override fun deleteFrom(dirUrl: String, fileName: String) = handling(dirUrl).deleteFrom(dirUrl, fileName)

    /**
     * Deletes all files in directory available at specified URL.
     */
    override fun truncate(dirUrl: String) = handling(dirUrl).truncate(dirUrl)

    /**
     * Gets file status of given name in directory at specified URL.
     */
    override fun stat(dirUrl: String, fileName: String): FileEntry? = handling(dirUrl).stat(dirUrl, fileName)

    /**
     * Check if there is any file transfer supporting specified URL.
     */
    override fun handles(fileUrl: String): Boolean = all.any { it.handles(fileUrl) }

    /**
     * Get file transfer supporting specified URL.
     */
    fun handling(fileUrl: String): FileTransferHandler = all.find { it.handles(fileUrl) }
            ?: throw FileException("File transfer supporting URL '$fileUrl' not found!")

    /**
     * Register custom file transfer for e.g downloading / uploading files from cloud storages like:
     * Amazon S3, Google Cloud Storage etc.
     */
    fun custom(name: String, definition: CustomFileTransfer.() -> Unit) {
        custom.add(CustomFileTransfer(common).apply {
            this.name = name
            this.protocols = listOf("$name://*")

            apply(definition)
        })
    }

    /**
     * Get custom (or built-in) file transfer by name.
     */
    fun named(name: String): FileTransfer {
        return all.find { it.name == name } ?: throw FileException("File transfer named '$name' not found!")
    }

    var user: String? = null

    var password: String? = null

    var domain: String? = null

    val credentials: Pair<String, String> get() = when {
        user != null && password != null -> (user!! to password!!)
        else -> throw FileTransferException("File transfer credentials are missing!")
    }

    val credentialsString get() = credentials.run { "$first:$second" }

    /**
     * Shorthand method to set same credentials for all protocols requiring it.
     *
     * Useful only in specific cases, when e.g company storage offers accessing files via multiple protocols
     * using same AD credentials.
     */
    fun credentials(user: String?, password: String?, domain: String? = null) {
        this.user = user
        this.password = password
        this.domain = domain

        http.client.basicUser = user
        http.client.basicPassword = password

        sftp.user = user
        sftp.password = password

        smb.user = user
        smb.password = password
        smb.domain = domain
    }

    init {
        // override specific credentials if common specified
        credentials(
                common.prop.string("fileTransfer.user"),
                common.prop.string("fileTransfer.password"),
                common.prop.string("fileTransfer.domain")
        )
    }

    companion object {
        const val NAME = "manager"

        const val TMP_SUFFIX = ".tmp"
    }
}
