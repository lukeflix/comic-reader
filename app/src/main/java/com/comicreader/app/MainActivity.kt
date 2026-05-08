package com.comicreader.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import android.graphics.Color
import android.view.Gravity

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
                        comics.add(ComicItem(name, fileUri.toString(), ext))
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
            comics.add(ComicItem(name, uri.toString(), ext))
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
        bodyContainer.addView(TextView(this).apply {
            text = "SELECCIONAR CARPETA"
            textSize = 14f; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#B71C1C"))
            setPadding(32, 20, 32, 20)
            setOnClickListener { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), PICK_FOLDER) }
        })

        bodyContainer.addView(TextView(this).apply {
            text = "SELECCIONAR ARCHIVO"
            textSize = 14f; gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#B71C1C")); setBackgroundColor(Color.WHITE)
            setPadding(32, 20, 32, 20)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 8 }
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
                }, PICK_FILE)
            }
        })

        if (comics.isEmpty()) {
            bodyContainer.addView(TextView(this).apply {
                text = "\nSin comics\nUsa los botones para agregar"
                textSize = 14f; gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#999999")); setPadding(20, 30, 20, 20)
            })
        } else {
            bodyContainer.addView(TextView(this).apply {
                text = "\n${comics.size} comics:"
                textSize = 14f; setTextColor(Color.parseColor("#111111")); setPadding(20, 20, 20, 10)
            })

            comics.forEachIndexed { idx, comic ->
                val item = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE)
                    setPadding(16, 12, 16, 12); gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 4 }
                }

                item.addView(TextView(this@MainActivity).apply {
                    text = comic.type.uppercase(); textSize = 12f
                    setTextColor(Color.parseColor("#B71C1C")); setPadding(0, 0, 12, 0)
                })

                item.addView(TextView(this@MainActivity).apply {
                    text = comic.name; textSize = 14f
                    setTextColor(Color.parseColor("#111111"))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })

                item.addView(TextView(this@MainActivity).apply {
                    text = "▶"; textSize = 18f; setTextColor(Color.parseColor("#B71C1C")); setPadding(8, 0, 8, 0)
                    setOnClickListener {
                        val intent = Intent(this@MainActivity, ReaderActivity::class.java).apply {
                            putExtra("comic_uri", comic.path)
                            putExtra("comic_name", comic.name)
                            putExtra("comic_type", comic.type)
                        }
                        startActivity(intent)
                    }
                })

                item.addView(TextView(this@MainActivity).apply {
                    text = "X"; textSize = 16f; setTextColor(Color.parseColor("#FF0000"))
                    setOnClickListener { comics.removeAt(idx); showLibrary() }
                })

                bodyContainer.addView(item)
            }
        }
    }
}
