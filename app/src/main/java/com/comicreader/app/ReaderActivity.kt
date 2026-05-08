package com.comicreader.app

import android.app.Activity
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import android.graphics.Bitmap
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

class ReaderActivity : Activity() {

    private var pages: MutableList<String> = mutableListOf()
    private var currentPage: Int = 0
    private var isDoublePage: Boolean = false
    private var tempFile: File? = null

    private lateinit var singleImage: ImageView
    private lateinit var leftImage: ImageView
    private lateinit var rightImage: ImageView
    private lateinit var pageText: TextView
    private lateinit var topBar: LinearLayout
    private lateinit var bottomBar: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = Uri.parse(intent.getStringExtra("comic_uri") ?: "")
        val name = intent.getStringExtra("comic_name") ?: "Comic"

        if (uri == null) { finish(); return }

        isDoublePage = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        createLayout(name)

        Thread {
            try {
                tempFile = File(cacheDir, "comic_temp")
                contentResolver.openInputStream(uri)?.use { input ->
                    tempFile!!.outputStream().use { output -> input.copyTo(output) }
                }

                loadPages()

                runOnUiThread {
                    currentPage = 0
                    showPage()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }.start()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        isDoublePage = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        showPage()
    }

    override fun onDestroy() {
        super.onDestroy()
        tempFile?.delete()
    }

    private fun createLayout(name: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }

        topBar = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#CC000000"))
            setPadding(16, 30, 16, 10)
        }

        topBar.addView(TextView(this).apply {
            text = "←"; textSize = 22f; setTextColor(Color.WHITE)
            setOnClickListener { finish() }
        })

        topBar.addView(TextView(this).apply {
            text = name; textSize = 14f; setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        layout.addView(topBar)

        val pageLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        leftImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
            visibility = View.GONE
        }
        pageLayout.addView(leftImage)

        singleImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
            setOnClickListener {
                topBar.visibility = if (topBar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                bottomBar.visibility = if (bottomBar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        }
        pageLayout.addView(singleImage)

        rightImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
            visibility = View.GONE
        }
        pageLayout.addView(rightImage)

        layout.addView(pageLayout)

        bottomBar = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#CC000000"))
            setPadding(16, 10, 16, 30)
            gravity = Gravity.CENTER
        }

        bottomBar.addView(TextView(this).apply {
            text = "◀"; textSize = 24f; setTextColor(Color.WHITE)
            setPadding(24, 12, 24, 12); setOnClickListener { prevPage() }
        })

        pageText = TextView(this).apply {
            text = "Cargando..."; textSize = 14f; setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        bottomBar.addView(pageText)

        bottomBar.addView(TextView(this).apply {
            text = "▶"; textSize = 24f; setTextColor(Color.WHITE)
            setPadding(24, 12, 24, 12); setOnClickListener { nextPage() }
        })

        layout.addView(bottomBar)
        setContentView(layout)
    }

    private fun loadPages() {
        pages.clear()
        try {
            ZipFile(tempFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val n = entry.name.lowercase()
                    if (!entry.isDirectory && (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp"))) {
                        pages.add(entry.name)
                    }
                }
            }
            pages.sort()
        } catch (e: Exception) {
            // Si falla como ZIP, intentar como stream directo (para PDFs o imágenes sueltas)
            pages.add("direct")
        }
    }

    private fun showPage() {
        if (pages.isEmpty()) return

        if (pages[0] == "direct") {
            singleImage.visibility = View.VISIBLE
            leftImage.visibility = View.GONE
            rightImage.visibility = View.GONE
            val bmp = BitmapFactory.decodeFile(tempFile!!.absolutePath)
            singleImage.setImageBitmap(bmp)
            pageText.text = "1/1"
            return
        }

        if (isDoublePage) {
            singleImage.visibility = View.GONE
            leftImage.visibility = View.VISIBLE
            rightImage.visibility = View.VISIBLE
            leftImage.setImageBitmap(getPageBitmap(currentPage))
            rightImage.setImageBitmap(if (currentPage + 1 < pages.size) getPageBitmap(currentPage + 1) else null)
        } else {
            leftImage.visibility = View.GONE
            rightImage.visibility = View.GONE
            singleImage.visibility = View.VISIBLE
            singleImage.setImageBitmap(getPageBitmap(currentPage))
        }

        pageText.text = "${currentPage + 1} / ${pages.size}"
    }

    private fun getPageBitmap(index: Int): Bitmap? {
        if (index >= pages.size) return null
        return try {
            ZipFile(tempFile).use { zip ->
                val entry = zip.getEntry(pages[index])
                if (entry != null) {
                    zip.getInputStream(entry).use { stream ->
                        val opts = BitmapFactory.Options().apply {
                            inSampleSize = 1
                        }
                        BitmapFactory.decodeStream(stream, null, opts)
                    }
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun nextPage() {
        val step = if (isDoublePage) 2 else 1
        if (currentPage + step < pages.size) {
            currentPage += step
            showPage()
        }
    }

    private fun prevPage() {
        val step = if (isDoublePage) 2 else 1
        if (currentPage - step >= 0) {
            currentPage -= step
            showPage()
        }
    }
}
