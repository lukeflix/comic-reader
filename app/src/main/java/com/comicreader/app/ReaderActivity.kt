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
import java.io.InputStream
import java.util.zip.ZipFile

class ReaderActivity : Activity() {

    private var comicUri: Uri? = null
    private var comicName: String = ""
    private var comicType: String = ""
    private var pages: MutableList<String> = mutableListOf()
    private var currentPage: Int = 0
    private var isDoublePage: Boolean = false

    private lateinit var mainLayout: LinearLayout
    private lateinit var topBar: LinearLayout
    private lateinit var pageLayout: LinearLayout
    private lateinit var leftImage: ImageView
    private lateinit var rightImage: ImageView
    private lateinit var singleImage: ImageView
    private lateinit var pageText: TextView
    private lateinit var bottomBar: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        comicUri = Uri.parse(intent.getStringExtra("comic_uri") ?: "")
        comicName = intent.getStringExtra("comic_name") ?: "Desconocido"
        comicType = intent.getStringExtra("comic_type") ?: ""

        if (comicUri == null) {
            Toast.makeText(this, "Error: sin comic", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        isDoublePage = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        createLayout()
        loadComic()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        isDoublePage = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        refreshViewMode()
    }

    private fun createLayout() {
        mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }

        topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#CC000000"))
            setPadding(16, 30, 16, 10)
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.VISIBLE
        }

        val backBtn = TextView(this).apply {
            text = "← Volver"
            textSize = 16f
            setTextColor(Color.WHITE)
            setOnClickListener { finish() }
        }
        topBar.addView(backBtn)

        val titleView = TextView(this).apply {
            text = comicName
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        topBar.addView(titleView)

        val pageNumTop = TextView(this).apply {
            text = ""
            textSize = 14f
            setTextColor(Color.WHITE)
            id = 5555
        }
        topBar.addView(pageNumTop)

        mainLayout.addView(topBar)

        pageLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }

        leftImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
            visibility = View.GONE
        }
        pageLayout.addView(leftImage)

        singleImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
        }
        pageLayout.addView(singleImage)

        rightImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
            visibility = View.GONE
        }
        pageLayout.addView(rightImage)

        mainLayout.addView(pageLayout)

        setContentView(mainLayout)

        singleImage.setOnClickListener { toggleBars() }
        leftImage.setOnClickListener { prevPage() }
        rightImage.setOnClickListener { nextPage() }
    }

    private fun createBottomBar() {
        mainLayout.removeView(bottomBar)
        bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#CC000000"))
            setPadding(16, 10, 16, 30)
            gravity = Gravity.CENTER
        }

        val prevBtn = TextView(this).apply {
            text = "◀"
            textSize = 22f
            setTextColor(Color.WHITE)
            setPadding(20, 10, 20, 10)
            setOnClickListener { prevPage() }
        }
        bottomBar.addView(prevBtn)

        pageText = TextView(this).apply {
            text = "0 / 0"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        bottomBar.addView(pageText)

        val nextBtn = TextView(this).apply {
            text = "▶"
            textSize = 22f
            setTextColor(Color.WHITE)
            setPadding(20, 10, 20, 10)
            setOnClickListener { nextPage() }
        }
        bottomBar.addView(nextBtn)

        mainLayout.addView(bottomBar)
    }

    private fun toggleBars() {
        if (topBar.visibility == View.VISIBLE) {
            topBar.visibility = View.GONE
            if (::bottomBar.isInitialized) bottomBar.visibility = View.GONE
        } else {
            topBar.visibility = View.VISIBLE
            if (::bottomBar.isInitialized) bottomBar.visibility = View.VISIBLE
        }
    }

    private fun loadComic() {
        try {
            if (comicType == "cbz") {
                loadCbz()
            } else if (comicType == "cbr") {
                Toast.makeText(this, "CBR: Renombra a .cbz si es ZIP", Toast.LENGTH_LONG).show()
                loadCbrAsZip()
            } else {
                Toast.makeText(this, "Formato no soportado: $comicType", Toast.LENGTH_LONG).show()
                finish()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun loadCbz() {
        pages.clear()
        contentResolver.openInputStream(comicUri!!)?.use { inputStream ->
            val tempFile = java.io.File(cacheDir, "temp_comic.cbz")
            tempFile.outputStream().use { inputStream.copyTo(it) }

            ZipFile(tempFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (!entry.isDirectory && entry.name.matches(Regex(".*\\.(jpg|jpeg|png|webp)$", RegexOption.IGNORE_CASE))) {
                        pages.add(entry.name)
                    }
                }
            }
            tempFile.delete()
        }
        pages.sort()
        currentPage = 0
        createBottomBar()
        refreshViewMode()
    }

    private fun loadCbrAsZip() {
        pages.clear()
        contentResolver.openInputStream(comicUri!!)?.use { inputStream ->
            val tempFile = java.io.File(cacheDir, "temp_comic.cbr")
            tempFile.outputStream().use { inputStream.copyTo(it) }

            try {
                ZipFile(tempFile).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (!entry.isDirectory && entry.name.matches(Regex(".*\\.(jpg|jpeg|png|webp)$", RegexOption.IGNORE_CASE))) {
                            pages.add(entry.name)
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "No se pudo abrir. Asegurate que sea ZIP", Toast.LENGTH_LONG).show()
                finish()
            }
            tempFile.delete()
        }
        pages.sort()
        currentPage = 0
        createBottomBar()
        refreshViewMode()
    }

    private fun refreshViewMode() {
        if (pages.isEmpty()) return

        if (isDoublePage) {
            singleImage.visibility = View.GONE
            leftImage.visibility = View.VISIBLE
            rightImage.visibility = View.VISIBLE
            showDoublePage()
        } else {
            leftImage.visibility = View.GONE
            rightImage.visibility = View.GONE
            singleImage.visibility = View.VISIBLE
            showSinglePage()
        }

        pageText.text = "${currentPage + 1} / ${pages.size}"
        val topPageNum = findViewById<TextView>(5555)
        topPageNum?.text = "${currentPage + 1}/${pages.size}"
    }

    private fun showSinglePage() {
        val bitmap = extractPage(currentPage)
        singleImage.setImageBitmap(bitmap)
    }

    private fun showDoublePage() {
        val leftBmp = extractPage(currentPage)
        leftImage.setImageBitmap(leftBmp)

        if (currentPage + 1 < pages.size) {
            val rightBmp = extractPage(currentPage + 1)
            rightImage.setImageBitmap(rightBmp)
        } else {
            rightImage.setImageBitmap(null)
        }
    }

    private fun extractPage(index: Int): Bitmap? {
        if (index >= pages.size) return null

        return try {
            contentResolver.openInputStream(comicUri!!)?.use { inputStream ->
                val tempFile = java.io.File(cacheDir, "temp_page_$index")
                tempFile.outputStream().use { inputStream.copyTo(it) }

                var bitmap: Bitmap? = null
                ZipFile(tempFile).use { zip ->
                    val entry = zip.getEntry(pages[index])
                    if (entry != null) {
                        val pageStream = zip.getInputStream(entry)
                        bitmap = BitmapFactory.decodeStream(pageStream)
                        pageStream.close()
                    }
                }
                tempFile.delete()
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun nextPage() {
        val step = if (isDoublePage) 2 else 1
        if (currentPage + step < pages.size) {
            currentPage += step
            refreshViewMode()
        }
    }

    private fun prevPage() {
        val step = if (isDoublePage) 2 else 1
        if (currentPage - step >= 0) {
            currentPage -= step
            refreshViewMode()
        }
    }
}
