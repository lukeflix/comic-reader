package com.comicreader.app.utils

import android.os.Environment
import java.io.File

data class ComicFile(
    val name: String,
    val path: String,
    val type: String,
    val size: Long,
    val pages: Int = 0
)

class ComicScanner {
    
    private val extensions = listOf("cbr", "cbz", "pdf", "epub")
    
    fun scanDevice(): List<ComicFile> {
        val comics = mutableListOf<ComicFile>()
        val storageDir = Environment.getExternalStorageDirectory()
        scanDirectory(storageDir, comics)
        
        val commonDirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            File(storageDir, "Comics"),
            File(storageDir, "Download"),
            File(storageDir, "Documents")
        )
        
        for (dir in commonDirs) {
            if (dir.exists()) {
                scanDirectory(dir, comics)
            }
        }
        
        return comics.distinctBy { it.path }
    }
    
    private fun scanDirectory(directory: File, comics: MutableList<ComicFile>) {
        try {
            val files = directory.listFiles() ?: return
            for (file in files) {
                if (file.isDirectory && !file.name.startsWith(".")) {
                    if (file.path.length - directory.path.length < 200) {
                        scanDirectory(file, comics)
                    }
                } else if (file.isFile) {
                    val ext = file.extension.lowercase()
                    if (ext in extensions) {
                        comics.add(
                            ComicFile(
                                name = file.nameWithoutExtension,
                                path = file.absolutePath,
                                type = ext,
                                size = file.length()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) { }
    }
    
    fun getPageCount(filePath: String): Int {
        return try {
            when {
                filePath.endsWith(".cbz") -> getCbzPageCount(filePath)
                else -> 0
            }
        } catch (e: Exception) { 0 }
    }
    
    private fun getCbzPageCount(path: String): Int {
        var count = 0
        java.util.zip.ZipFile(path).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (!entry.isDirectory && entry.name.matches(Regex(".*\\.(jpg|jpeg|png|webp)$", RegexOption.IGNORE_CASE))) {
                    count++
                }
            }
        }
        return count
    }
}
