package com.comicreader.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import android.graphics.Color
import android.view.Gravity
import java.io.File

class MainActivity : Activity() {

    private val PICK_FOLDER = 1234
    private val PICK_FILE = 5678
    private var comics: MutableList<ComicItem> = mutableListOf()
    private lateinit var bodyContainer: LinearLayout

    data class ComicItem(val name: String, val path: String, val type: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F7F7F5"))
        }

        val title = TextView(this).apply {
            text = "Comic Reader"
            textSize = 22f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#B71C1C"))
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 40)
        }
        layout.addView(title)

        bodyContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
            addView(bodyContainer)
        }
        layout.addView(scrollView)

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(0, 0, 0, 20)
        }

        val tabNames = listOf("Library", "Search", "Reading", "Favs")
        tabNames.forEachIndexed { index, name ->
            val tab = TextView(this).apply {
                text = name
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#ABABAB"))
                setPadding(10, 30, 10, 30)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    for (i in 0 until tabs.childCount) {
                        (tabs.getChildAt(i) as TextView).setTextColor(Color.parseColor("#ABABAB"))
                    }
                    setTextColor(Color.parseColor("#111111"))
                    showTabContent(index)
                }
            }
            tabs.addView(tab)
        }
        layout.addView(tabs)

        setContentView(layout)
        (tabs.getChildAt(0) as TextView).performClick()
        
        checkIntentForFile(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkIntentForFile(intent)
    }

    private fun checkIntentForFile(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                val name = getFileName(uri)
                val path = uri.toString()
                val ext = name.substringAfterLast('.', "").lowercase()
                if (ext in listOf("cbr", "cbz", "pdf", "epub")) {
                    comics.add(ComicItem(name, path, ext))
                    showLibraryTab()
                    Toast.makeText(this, "$name agregado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "desconocido"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = cursor.getString(idx)
            }
        }
        return name
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_FOLDER && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val folderName = getFileName(uri)
                scanFolderUri(uri)
                showLibraryTab()
                Toast.makeText(this, "Carpeta: $folderName", Toast.LENGTH_SHORT).show()
            }
        }

        if (requestCode == PICK_FILE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val name = getFileName(uri)
                val path = uri.toString()
                val ext = name.substringAfterLast('.', "").lowercase()
                if (ext in listOf("cbr", "cbz", "pdf", "epub")) {
                    comics.add(ComicItem(name, path, ext))
                    showLibraryTab()
                    Toast.makeText(this, "$name agregado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun scanFolderUri(folderUri: Uri) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            folderUri, DocumentsContract.getTreeDocumentId(folderUri)
        )
        
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
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, childId)
                    scanFolderUri(childUri)
                } else {
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext in listOf("cbr", "cbz", "pdf", "epub")) {
                        val docId = cursor.getString(0)
                        val fileUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, docId)
                        comics.add(ComicItem(name, fileUri.toString(), ext))
                    }
                }
            }
        }
    }

    private fun showTabContent(index: Int) {
        bodyContainer.removeAllViews()
        if (index == 0) showLibraryTab()
        else {
            bodyContainer.addView(TextView(this).apply {
                text = when(index) { 1 -> "Buscar" 2 -> "Lectura" else -> "Favoritos" }
                textSize = 16f; gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#666666"))
                setPadding(40, 80, 40, 40)
            })
        }
    }

    private fun showLibraryTab() {
        val btnSelectFolder = TextView(this).apply {
            text = "SELECCIONAR CARPETA"
            textSize = 14f; gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#B71C1C"))
            setPadding(32, 20, 32, 20)
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                startActivityForResult(intent, PICK_FOLDER)
            }
        }
        bodyContainer.addView(btnSelectFolder)

        val btnSelectFile = TextView(this).apply {
            text = "SELECCIONAR ARCHIVO"
            textSize = 14f; gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#B71C1C"))
            setBackgroundColor(Color.parseColor("#FFFFFF"))
            setPadding(32, 20, 32, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 8 }
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                        "application/x-cbr", "application/x-cbz",
                        "application/pdf", "application/epub+zip",
                        "application/octet-stream"
                    ))
                }
                startActivityForResult(intent, PICK_FILE)
            }
        }
        bodyContainer.addView(btnSelectFile)

        if (comics.isEmpty()) {
            bodyContainer.addView(TextView(this).apply {
                text = "\nSin comics\nUsa los botones para agregar"
                textSize = 14f; gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#999999"))
                setPadding(20, 30, 20, 20)
            })
        } else {
            bodyContainer.addView(TextView(this).apply {
                text = "\n${comics.size} comics cargados:"
                textSize = 14f
                setTextColor(Color.parseColor("#111111"))
                setPadding(20, 20, 20, 10)
            })

            for ((idx, comic) in comics.withIndex()) {
                val item = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setBackgroundColor(Color.WHITE)
                    setPadding(16, 12, 16, 12)
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = 4 }
                }

                item.addView(TextView(this@MainActivity).apply {
                    text = comic.type.uppercase()
                    textSize = 12f
                    setTextColor(Color.parseColor("#B71C1C"))
                    setPadding(0, 0, 12, 0)
                })

                item.addView(TextView(this@MainActivity).apply {
                    text = comic.name
                    textSize = 14f
                    setTextColor(Color.parseColor("#111111"))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })

                val deleteBtn = TextView(this@MainActivity).apply {
                    text = "X"
                    textSize = 16f
                    setTextColor(Color.parseColor("#FF0000"))
                    setPadding(12, 0, 0, 0)
                    setOnClickListener {
                        comics.removeAt(idx)
                        showLibraryTab()
                    }
                }
                item.addView(deleteBtn)

                bodyContainer.addView(item)
            }
        }
    }
}
