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
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import java.io.File
import java.util.zip.ZipFile

class ReaderActivity : Activity() {

    private var pages: MutableList<String> = mutableListOf()
    private var pdfPages: Int = 0
    private var currentPage: Int = 0
    private var isDoublePage: Boolean = false
    private var tempFile: File? = null
    private var isPdf: Boolean = false
    private var pdfDocument: PDDocument? = null

    private lateinit var mainLayout: LinearLayout
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
        val type = intent.getStringExtra("comic_type") ?: ""

        if (uri == null) { finish(); return }

        isDoublePage = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        isPdf = type == "pdf"

        createLayout(name)

        Thread {
            try {
                tempFile = File(cacheDir, "comic.$type")
                contentResolver.openInputStream(uri)?.use { input ->
                    tempFile!!.outputStream().use { output -> input.copyTo(output) }
                }

                if (isPdf) {
                    loadPdf()
                } else {
                    loadArchive()
                }

                runOnUiThread {
                    currentPage = 0
                    refreshView()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        isDoublePage = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        refreshView()
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfDocument?.close()
        tempFile?.delete()
    }

    private fun createLayout(name: String) {
        mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }

        topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#CC000000"))
            setPadding(16, 30, 16, 10)
            gravity = Gravity.CENTER_VERTICAL
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

        mainLayout.addView(topBar)

        val pageLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        leftImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
            visibility = View.GONE
            setOnClickListener { prevPage() }
        }
        pageLayout.addView(leftImage)

        singleImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
            setOnClickListener { toggleBars() }
        }
        pageLayout.addView(singleImage)

        rightImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
            visibility = View.GONE
            setOnClickListener { nextPage() }
        }
        pageLayout.addView(rightImage)

        mainLayout.addView(pageLayout)

        bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
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

        mainLayout.addView(bottomBar)
        setContentView(mainLayout)
    }

    private fun loadArchive() {
        pages.clear()
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
    }

    private fun loadPdf() {
        PDFBoxResourceLoader.init(this)
        pdfDocument = PDDocument.load(tempFile)
        pdfPages = pdfDocument!!.numberOfPages
    }

    private fun refreshView() {
        if (isPdf) {
            showPdfPage()
        } else {
            showArchivePage()
        }
        val total = if (isPdf) pdfPages else pages.size
        pageText.text = "${currentPage + 1} / $total"
    }

    private fun showArchivePage() {
        if (pages.isEmpty()) return

        if (isDoublePage) {
            singleImage.visibility = View.GONE
            leftImage.visibility = View.VISIBLE
            rightImage.visibility = View.VISIBLE
            leftImage.setImageBitmap(getArchiveBitmap(currentPage))
            rightImage.setImageBitmap(if (currentPage + 1 < pages.size) getArchiveBitmap(currentPage + 1) else null)
        } else {
            leftImage.visibility = View.GONE
            rightImage.visibility = View.GONE
            singleImage.visibility = View.VISIBLE
            singleImage.setImageBitmap(getArchiveBitmap(currentPage))
        }
    }

    private fun showPdfPage() {
        try {
            val renderer = PDFRenderer(pdfDocument!!)
            val bitmap = renderer.renderImage(currentPage, 2f)
            singleImage.visibility = View.VISIBLE
            leftImage.visibility = View.GONE
            rightImage.visibility = View.GONE
            singleImage.setImageBitmap(bitmap)
        } catch (e: Exception) { }
    }

    private fun getArchiveBitmap(index: Int): Bitmap? {
        if (index >= pages.size) return null
        return try {
            ZipFile(tempFile).use { zip ->
                val entry = zip.getEntry(pages[index])
                zip.getInputStream(entry).use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        } catch (e: Exception) { null }
    }

    private fun toggleBars() {
        topBar.visibility = if (topBar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        bottomBar.visibility = if (bottomBar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun nextPage() {
        val step = if (isDoublePage && !isPdf) 2 else 1
        val total = if (isPdf) pdfPages else pages.size
        if (currentPage + step < total) {
            currentPage += step
            refreshView()
        }
    }

    private fun prevPage() {
        val step = if (isDoublePage && !isPdf) 2 else 1
        if (currentPage - step >= 0) {
            currentPage -= step
            refreshView()
        }
    }
}
