package com.comicreader.app

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import android.graphics.Color
import android.view.Gravity
import com.comicreader.app.utils.ComicScanner
import com.comicreader.app.utils.ComicFile

class MainActivity : Activity() {

    private val scanner = ComicScanner()
    private var comics: List<ComicFile> = emptyList()
    private lateinit var bodyContainer: LinearLayout

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
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
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
    }

    private fun showTabContent(index: Int) {
        bodyContainer.removeAllViews()

        when (index) {
            0 -> showLibraryTab()
            1 -> showSimpleTab("Busca cómics por nombre o autor")
            2 -> showSimpleTab("Continúa leyendo donde lo dejaste")
            3 -> showSimpleTab("Tus cómics favoritos")
        }
    }

    private fun showLibraryTab() {
        val scanBtn = TextView(this).apply {
            text = "ESCANEAR CÓMICS"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#B71C1C"))
            setPadding(32, 20, 32, 20)
            setOnClickListener {
                Toast.makeText(this@MainActivity, "Escaneando...", Toast.LENGTH_SHORT).show()
                Thread {
                    comics = scanner.scan()
                    runOnUiThread {
                        showLibraryTab()
                        Toast.makeText(this@MainActivity, "Encontrados: ${comics.size} cómics", Toast.LENGTH_SHORT).show()
                    }
                }.start()
            }
        }
        bodyContainer.addView(scanBtn)

        if (comics.isEmpty()) {
            bodyContainer.addView(TextView(this).apply {
                text = "\nNo hay cómics.\nToca el botón para escanear."
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#999999"))
                setPadding(20, 40, 20, 20)
            })
        } else {
            for (comic in comics) {
                bodyContainer.addView(createComicItem(comic))
            }
        }
    }

    private fun createComicItem(comic: ComicFile): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(16, 12, 16, 12)
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 4 }

            addView(TextView(this@MainActivity).apply {
                text = when(comic.type) { "cbr" -> "📘" "cbz" -> "📗" "pdf" -> "📕" else -> "📙" }
                textSize = 24f
                setPadding(0, 0, 12, 0)
            })

            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                
                addView(TextView(this@MainActivity).apply {
                    text = comic.name
                    textSize = 14f
                    setTextColor(Color.parseColor("#111111"))
                })
                
                addView(TextView(this@MainActivity).apply {
                    text = "${comic.type.uppercase()} · ${formatSize(comic.size)}"
                    textSize = 11f
                    setTextColor(Color.parseColor("#999999"))
                })
            })

            addView(TextView(this@MainActivity).apply {
                text = "▶"
                textSize = 18f
                setTextColor(Color.parseColor("#B71C1C"))
                setPadding(12, 0, 0, 0)
                setOnClickListener {
                    Toast.makeText(this@MainActivity, "Abriendo: ${comic.name}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun showSimpleTab(text: String) {
        bodyContainer.addView(TextView(this).apply {
            text = text
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#666666"))
            setPadding(40, 80, 40, 40)
        })
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
        }
    }
}
