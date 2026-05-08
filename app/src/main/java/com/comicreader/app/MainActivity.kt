package com.comicreader.app

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.ImageView
import android.widget.Toast
import android.graphics.Color
import android.graphics.Bitmap
import android.view.Gravity
import android.view.View
import java.util.zip.ZipFile

class MainActivity : Activity() {

    private val PICK_FOLDER = 1234
    private val PICK_FILE = 5678
    private var comics: MutableList<ComicItem> = mutableListOf()
    private lateinit var bodyContainer: LinearLayout

    data class ComicItem(
        val name: String,
        val path: String,
        val type: String,
        val coverBitmap: Bitmap? = null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F7F7F5"))
        }

        layout.addView(TextView(this).apply {
            text = "Comic Reader"
            textSize = 22f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#B71C1C"))
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 40)
        })

        bodyContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            addView(bodyContainer)
        }
        layout.addView(scrollView)

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(0, 0, 0, 20)
        }

        listOf("Library", "Search", "Reading", "Favs").forEachIndexed { index, name ->
            val tab = TextView(this).apply {
                text = name
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#ABABAB"))
                setPadding(10, 30, 10, 30)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    for (i in 0 until tabs.childCount)
                        (tabs.getChildAt(i) as TextView).setTextColor(Color.parseColor("#ABABAB"))
                    setTextColor(Color.parseColor("#111111"))
                    showTab(index)
                }
            }
            tabs.addView(tab)
        }
        layout.addView(tabs)
        setContentView(layout)
        (tabs.getChildAt(0) as TextView).performClick()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_FOLDER && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                scanFolder(uri)
                showTab(0)
            }
        }

        if (requestCode == PICK_FILE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                addFile(uri)
                showTab(0)
            }
        }
    }

    private fun scanFolder(folderUri: Uri) {
        val docId = DocumentsContract.getTreeDocumentId(folderUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, docId)

        contentResolver.query(childrenUri, arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        ), null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(1) ?: continue
                val mime = cursor.getString(2) ?: continue
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    val childId = cursor.getString(0)
                    scanFolder(DocumentsContract.buildDocumentUriUsingTree(folderUri, childId))
                } else {
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext in listOf("cbr", "cbz", "pdf", "epub")) {
                        val fileId = cursor.getString(0)
                        val fileUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, fileId)
                        val cover = extractCover(fileUri, ext, name)
                        comics.add(ComicItem(name, fileUri.toString(), ext, cover))
                    }
                }
            }
        }
    }

    private fun addFile(uri: Uri) {
        var name = "desconocido"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = cursor.getString(idx)
            }
        }
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext in listOf("cbr", "cbz", "pdf", "epub")) {
            val cover = extractCover(uri, ext, name)
            comics.add(ComicItem(name, uri.toString(), ext, cover))
        }
    }

    private fun extractCover(uri: Uri, type: String, name: String): Bitmap? {
        if (type != "cbz" && type != "cbr") return null

        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val tempFile = java.io.File(cacheDir, "cover_$name")
                tempFile.outputStream().use { inputStream.copyTo(it) }

                var bitmap: Bitmap? = null
                try {
                    ZipFile(tempFile).use { zip ->
                        val entries = zip.entries().toList()
                            .filter { !it.isDirectory && it.name.matches(Regex(".*\\.(jpg|jpeg|png|webp)$", RegexOption.IGNORE_CASE)) }
                            .sortedBy { it.name }

                        if (entries.isNotEmpty()) {
                            val coverEntry = entries.first()
                            val coverStream = zip.getInputStream(coverEntry)
                            bitmap = BitmapFactory.decodeStream(coverStream)
                            coverStream.close()
                        }
                    }
                } catch (e: Exception) { }

                tempFile.delete()
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun showTab(index: Int) {
        bodyContainer.removeAllViews()
        if (index == 0) showLibrary()
        else {
            bodyContainer.addView(TextView(this).apply {
                text = when(index) { 1 -> "Buscar" 2 -> "Lectura" else -> "Favoritos" }
                textSize = 16f; gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#666666")); setPadding(40, 80, 40, 40)
            })
        }
    }

    private fun showLibrary() {
        // Botones de acción
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 8)
            gravity = Gravity.CENTER
        }

        btnRow.addView(TextView(this).apply {
            text = "📁 Carpeta"
            textSize = 13f; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#B71C1C"))
            setPadding(20, 14, 20, 14)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 8 }
            setOnClickListener { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), PICK_FOLDER) }
        })

        btnRow.addView(TextView(this).apply {
            text = "📄 Archivo"
            textSize = 13f; gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#B71C1C")); setBackgroundColor(Color.WHITE)
            setPadding(20, 14, 20, 14)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
                }, PICK_FILE)
            }
        })

        bodyContainer.addView(btnRow)

        if (comics.isEmpty()) {
            bodyContainer.addView(TextView(this).apply {
                text = "\n\nSin cómics\nToca un botón para agregar"
                textSize = 15f; gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#999999")); setPadding(20, 40, 20, 20)
            })
        } else {
            bodyContainer.addView(TextView(this).apply {
                text = "${comics.size} cómics"
                textSize = 13f; setTextColor(Color.parseColor("#999999"))
                setPadding(20, 16, 20, 8)
            })

            // Grid de cómics
            val grid = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 0, 12, 20)
            }

            var row: LinearLayout? = null
            comics.forEachIndexed { idx, comic ->
                if (idx % 2 == 0) {
                    row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, 0, 0, 12)
                    }
                    grid.addView(row)
                }

                row?.addView(createComicCard(comic, idx))
            }

            bodyContainer.addView(grid)
        }
    }

    private fun createComicCard(comic: ComicItem, idx: Int): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = if (idx % 2 == 0) 6 else 0
                marginStart = if (idx % 2 == 1) 6 else 0
            }
            setBackgroundColor(Color.WHITE)
            setPadding(0, 0, 0, 8)
            setOnClickListener {
                val intent = Intent(this@MainActivity, ReaderActivity::class.java).apply {
                    putExtra("comic_uri", comic.path)
                    putExtra("comic_name", comic.name)
                    putExtra("comic_type", comic.type)
                }
                startActivity(intent)
            }
        }

        // Portada
        val coverView = if (comic.coverBitmap != null) {
            ImageView(this).apply {
                setImageBitmap(comic.coverBitmap)
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 280
                )
                setBackgroundColor(Color.parseColor("#EEEEEE"))
            }
        } else {
            TextView(this).apply {
                text = comic.type.uppercase()
                textSize = 32f; gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#B71C1C"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 280
                )
            }
        }
        card.addView(coverView)

        // Título
        card.addView(TextView(this).apply {
            text = comic.name
            textSize = 12f; setTextColor(Color.parseColor("#111111"))
            setPadding(10, 8, 10, 2); maxLines = 2
            gravity = Gravity.CENTER
        })

        // Tipo
        card.addView(TextView(this).apply {
            text = comic.type.uppercase()
            textSize = 10f; setTextColor(Color.parseColor("#999999"))
            setPadding(10, 0, 10, 4); gravity = Gravity.CENTER
        })

        // Botón eliminar
        card.addView(TextView(this).apply {
            text = "✕"
            textSize = 14f; setTextColor(Color.parseColor("#FF4444"))
            gravity = Gravity.CENTER; setPadding(0, 4, 0, 0)
            setOnClickListener {
                comics.removeAt(idx)
                showTab(0)
            }
        })

        return card
    }
}
