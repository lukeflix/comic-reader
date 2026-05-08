package com.comicreader.app

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.view.Gravity
import android.graphics.Color
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.util.zip.ZipFile

class ReaderActivity : AppCompatActivity() {
    
    private lateinit var imageView: ImageView
    private lateinit var pageText: TextView
    private lateinit var titleText: TextView
    
    private var comicPath: String = ""
    private var comicName: String = ""
    private var pages: MutableList<String> = mutableListOf()
    private var currentPage: Int = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        comicPath = intent.getStringExtra("comic_path") ?: ""
        comicName = intent.getStringExtra("comic_name") ?: "Desconocido"
        
        if (comicPath.isEmpty()) {
            Toast.makeText(this, "Error: No se encontró el cómic", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        createLayout()
        loadComic()
    }
    
    private fun createLayout() {
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }
        
        // Barra superior
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#CC000000"))
            setPadding(16, 32, 16, 8)
            gravity = Gravity.CENTER_VERTICAL
        }
        
        val backButton = TextView(this).apply {
            text = "← Volver"
            textSize = 14f
            setTextColor(Color.WHITE)
            setOnClickListener { finish() }
        }
        topBar.addView(backButton)
        
        titleText = TextView(this).apply {
            text = comicName
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        topBar.addView(titleText)
        
        rootLayout.addView(topBar)
        
        // Visor de imagen
        imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
        }
        rootLayout.addView(imageView)
        
        // Barra inferior
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#CC000000"))
            setPadding(16, 8, 16, 24)
            gravity = Gravity.CENTER
        }
        
        val prevButton = TextView(this).apply {
            text = "◀ ANTERIOR"
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(8, 8, 8, 8)
            setOnClickListener { prevPage() }
        }
        bottomBar.addView(prevButton)
        
        pageText = TextView(this).apply {
            text = "0 / 0"
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        bottomBar.addView(pageText)
        
        val nextButton = TextView(this).apply {
            text = "SIGUIENTE ▶"
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(8, 8, 8, 8)
            setOnClickListener { nextPage() }
        }
        bottomBar.addView(nextButton)
        
        rootLayout.addView(bottomBar)
        
        setContentView(rootLayout)
    }
    
    private fun loadComic() {
        try {
            if (comicPath.endsWith(".cbz")) {
                loadCbz()
            } else {
                Toast.makeText(this, "Formato no soportado aún", Toast.LENGTH_LONG).show()
                finish()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun loadCbz() {
        pages.clear()
        ZipFile(comicPath).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (!entry.isDirectory && entry.name.matches(Regex(".*\\.(jpg|jpeg|png|webp)$", RegexOption.IGNORE_CASE))) {
                    pages.add(entry.name)
                }
            }
            pages.sort()
        }
        
        currentPage = 0
        showPage()
    }
    
    private fun showPage() {
        if (pages.isEmpty()) {
            Toast.makeText(this, "No se encontraron páginas", Toast.LENGTH_LONG).show()
            return
        }
        
        try {
            ZipFile(comicPath).use { zip ->
                val entry = zip.getEntry(pages[currentPage])
                val inputStream = zip.getInputStream(entry)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                imageView.setImageBitmap(bitmap)
                inputStream.close()
            }
            
            pageText.text = "${currentPage + 1} / ${pages.size}"
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar página: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun nextPage() {
        if (currentPage < pages.size - 1) {
            currentPage++
            showPage()
        }
    }
    
    private fun prevPage() {
        if (currentPage > 0) {
            currentPage--
            showPage()
        }
    }
}
