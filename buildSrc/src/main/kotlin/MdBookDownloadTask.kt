import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.util.Locale
import java.util.zip.GZIPInputStream
import javax.inject.Inject

abstract class MdBookDownloadTask : DefaultTask() {

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @get:Input
    abstract val version: Property<String>

    @get:OutputDirectory
    abstract val binDirectory: DirectoryProperty

    init {
        version.convention("v0.5.4")
    }

    @TaskAction
    fun download() {
        val mdBookVersion = version.get()
        val osName = System.getProperty("os.name").lowercase(Locale.ROOT)
        val arch = System.getProperty("os.arch").lowercase(Locale.ROOT)
        val isWindows = osName.contains("windows")

        val target = when {
            osName.contains("mac") || osName.contains("darwin") ->
                when (arch) {
                    "aarch64", "arm64" -> "aarch64-apple-darwin"
                    "x86_64", "amd64" -> "x86_64-apple-darwin"
                    else -> error("Unsupported macOS architecture: $arch")
                }

            isWindows ->
                when (arch) {
                    "x86_64", "amd64" -> "x86_64-pc-windows-msvc"
                    else -> error("Unsupported Windows architecture: $arch")
                }

            osName.contains("linux") ->
                when (arch) {
                    "x86_64", "amd64" -> "x86_64-unknown-linux-gnu"
                    "aarch64", "arm64" -> "aarch64-unknown-linux-musl"
                    else -> error("Unsupported Linux architecture: $arch")
                }

            else -> error("Unsupported operating system: $osName")
        }

        val executableName = if (isWindows) "mdbook.exe" else "mdbook"
        val extension = if (isWindows) "zip" else "tar.gz"
        val binDir = binDirectory.get().asFile
        binDir.mkdirs()

        val executable = binDir.resolve(executableName)
        val archiveName = "mdbook-$mdBookVersion-$target.$extension"
        val downloadUrl =
            "https://github.com/rust-lang/mdBook/releases/download/" +
                "$mdBookVersion/$archiveName"

        val archive = binDir.resolve(archiveName)
        val tarFile = binDir.resolve("mdbook-$mdBookVersion-$target.tar")

        logger.lifecycle("Downloading mdBook $mdBookVersion for $target")

        try {
            URI(downloadUrl).toURL().openConnection().apply {
                setRequestProperty("User-Agent", "Gradle-mdBook-Downloader")
                connectTimeout = 15_000
                readTimeout = 60_000
            }.getInputStream().use { input ->
                archive.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val archiveTree = if (isWindows) {
                archiveOperations.zipTree(archive)
            } else {
                GZIPInputStream(archive.inputStream()).use { input ->
                    tarFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                archiveOperations.tarTree(tarFile)
            }

            fileSystemOperations.copy {
                from(archiveTree)
                include(executableName)
                into(binDir)
            }

            check(executable.isFile) {
                "Failed to extract $executableName from $archiveName"
            }

            if (!isWindows) {
                check(executable.setExecutable(true)) {
                    "Failed to make mdBook executable"
                }
            }

            logger.lifecycle("Installed mdBook at ${executable.absolutePath}")
        } finally {
            archive.delete()
            tarFile.delete()
        }
    }
}
