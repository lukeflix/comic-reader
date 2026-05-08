package com.comicreader.app.utils

import android.os.Environment
import java.io.File

data class ComicFile(
    val name: String,
    val path: String,
    val type: String,
    val size: Long
)

class ComicScanner {
    
    fun scan(): List<ComicFile> {
        val comics = mutableListOf<ComicFile>()
        val dirs = listOf(
            Environment.getExternalStorageDirectory(),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        )
        
        for (dir in dirs) {
            if (dir != null && dir.exists()) {
                searchDir(dir, comics)
            }
        }
        
        return comics.distinctBy { it.path }.sortedByDescending { it.size }
    }
    
    private fun searchDir(dir: File, comics: MutableList<ComicFile>) {
        try {
            dir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val ext = file.extension.lowercase()
                    if (ext in listOf("cbr", "cbz", "pdf", "epub")) {
                        comics.add(ComicFile(
                            name = file.nameWithoutExtension,
                            path = file.absolutePath,
                            type = ext,
                            size = file.length()
                        ))
                    }
                } else if (file.isDirectory && !file.name.startsWith(".")) {
                    if (file.path.length - dir.path.length < 300) {
                        searchDir(file, comics)
                    }
                }
            }
        } catch (e: Exception) { }
    }
}
