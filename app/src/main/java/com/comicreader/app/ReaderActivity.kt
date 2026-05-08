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
import java.util.zip.ZipFile

class ReaderActivity : Activity() {

    private var comicUri: Uri? = null
    private var comicName: String = ""
    private var comicType: String = ""
    private var pages: MutableList<String> = mutableListOf()
    private var currentPage: Int = 0
    private var isDoublePage: Boolean = false
    private var tempFile: File? = null

    private lateinit var mainLayout: LinearLayout
    private lateinit var topBar: LinearLayout
    private lateinit var singleImage: ImageView
    private lateinit var leftImage: ImageView
    private lateinit var rightImage: ImageView
    private lateinit var pageText: TextView
    private lateinit var bottomBar: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        comicUri = Uri.parse(intent.getStringExtra("comic_uri") ?: "")
        comicName = intent.getStringExtra("comic_name") ?: "Comic"
        comicType = intent.getStringExtra("comic_type") ?: ""

        if (comicUri == null) {
            Toast.makeText(this, "Error: sin comic", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        isDoublePage = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        createLayout()
        copyToTemp()
        loadPages()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        isDoublePage = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        refreshView()
    }

    override fun onDestroy() {
        super.onDestroy()
        tempFile?.delete()
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
        }

        topBar.addView(TextView(this).apply {
            text = "← Volver"
            textSize = 16f; setTextColor(Color.WHITE)
            setOnClickListener { finish() }
        })

        topBar.addView(TextView(this).apply {
            text = comicName
            textSize = 14f; setTextColor(Color.WHITE)
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
        }
        pageLayout.addView(leftImage)

        singleImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
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

        bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#CC000000"))
            setPadding(16, 10, 16, 30)
            gravity = Gravity.CENTER
        }

        bottomBar.addView(TextView(this).apply {
            text = "◀"
            textSize = 24f; setTextColor(Color.WHITE)
            setPadding(24, 12, 24, 12)
            setOnClickListener { prevPage() }
        })

        pageText = TextView(this).apply {
            text = "0 / 0"
            textSize = 14f; setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        bottomBar.addView(pageText)

        bottomBar.addView(TextView(this).apply {
            text = "▶"
            textSize = 24f; setTextColor(Color.WHITE)
            setPadding(24, 12, 24, 12)
            setOnClickListener { nextPage() }
        })

        mainLayout.addView(bottomBar)

        setContentView(mainLayout)

        singleImage.setOnClickListener {
            topBar.visibility = if (topBar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            bottomBar.visibility = if (bottomBar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }

    private fun copyToTemp() {
        try {
            contentResolver.openInputStream(comicUri!!)?.use { input ->
                tempFile = File(cacheDir, "comic_temp.${comicType}")
                tempFile!!.outputStream().use { output -> input.copyTo(output) }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al leer archivo", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun loadPages() {
        if (tempFile == null || !tempFile!!.exists()) {
            Toast.makeText(this, "Archivo no encontrado", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            ZipFile(tempFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name.lowercase()
                    if (!entry.isDirectory && (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp"))) {
                        pages.add(entry.name)
                    }
                }
            }
            pages.sort()
            
            if (pages.isEmpty()) {
                Toast.makeText(this, "Sin imágenes en el archivo", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            currentPage = 0
            refreshView()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun refreshView() {
        if (pages.isEmpty()) return

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
        if (index >= pages.size || tempFile == null) return null
        return try {
            ZipFile(tempFile).use { zip ->
                val entry = zip.getEntry(pages[index])
                if (entry != null) {
                    zip.getInputStream(entry).use { stream ->
                        BitmapFactory.decodeStream(stream)
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
            refreshView()
        }
    }

    private fun prevPage() {
        val step = if (isDoublePage) 2 else 1
        if (currentPage - step >= 0) {
            currentPage -= step
            refreshView()
        }
    }
}
