package com.comicreader.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import android.view.Gravity
import android.graphics.Color
import android.view.View
import com.comicreader.app.utils.ComicScanner
import com.comicreader.app.utils.ComicFile

class MainActivity : AppCompatActivity() {
    
    private lateinit var container: LinearLayout
    private lateinit var tabs: List<TextView>
    private val comicScanner = ComicScanner()
    private var comics: List<ComicFile> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        container = findViewById(R.id.container)
        
        val tab1 = findViewById<TextView>(R.id.tab_library)
        val tab2 = findViewById<TextView>(R.id.tab_search)
        val tab3 = findViewById<TextView>(R.id.tab_reading)
        val tab4 = findViewById<TextView>(R.id.tab_favorites)
        
        tabs = listOf(tab1, tab2, tab3, tab4)
        
        tab1.setOnClickListener { selectTab(0) }
        tab2.setOnClickListener { selectTab(1) }
        tab3.setOnClickListener { selectTab(2) }
        tab4.setOnClickListener { selectTab(3) }
        
        requestPermissions()
        selectTab(0)
    }
    
    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES), 100)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 100)
            }
        }
    }
    
    private fun selectTab(index: Int) {
        tabs.forEach { it.setTextColor(Color.parseColor("#ABABAB")) }
        tabs[index].setTextColor(Color.parseColor("#111111"))
        container.removeAllViews()
        
        val content = when(index) {
            0 -> createLibraryTab()
            1 -> createSearchTab()
            2 -> createReadingTab()
            3 -> createFavoritesTab()
            else -> TextView(this)
        }
        
        container.addView(content)
    }
    
    private fun createLibraryTab(): View {
        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        
        layout.addView(TextView(this).apply {
            text = "📚 Mi Biblioteca"
            textSize = 20f
            setTextColor(Color.parseColor("#111111"))
            setPadding(0, 0, 0, 16)
        })
        
        val scanButton = TextView(this).apply {
            text = "🔍 ESCANEAR CÓMICS"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#B71C1C"))
            setPadding(32, 16, 32, 16)
            setOnClickListener { scanComics(layout) }
        }
        layout.addView(scanButton)
        
        layout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 16)
        })
        
        if (comics.isEmpty()) {
            layout.addView(TextView(this).apply {
                text = "No hay cómics encontrados.\nToca el botón para escanear."
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#999999"))
                setPadding(16, 48, 16, 48)
            })
        } else {
            for (comic in comics) {
                layout.addView(createComicItem(comic))
            }
        }
        
        scrollView.addView(layout)
        return scrollView
    }
    
    private fun createComicItem(comic: ComicFile): View {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
        }
        
        val icon = when(comic.type) {
            "cbr", "cbz" -> "📖"
            "pdf" -> "📄"
            "epub" -> "📕"
            else -> "📁"
        }
        
        item.addView(TextView(this).apply {
            text = icon
            textSize = 28f
            setPadding(0, 0, 16, 0)
        })
        
        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        infoLayout.addView(TextView(this).apply {
            text = comic.name
            textSize = 14f
            setTextColor(Color.parseColor("#111111"))
        })
        
        infoLayout.addView(TextView(this).apply {
            text = "${comic.type.uppercase()} · ${formatSize(comic.size)}"
            textSize = 11f
            setTextColor(Color.parseColor("#999999"))
        })
        
        item.addView(infoLayout)
        
        item.addView(TextView(this).apply {
            text = "▶"
            textSize = 18f
            setTextColor(Color.parseColor("#B71C1C"))
            setOnClickListener {
                openComic(comic)
            }
        })
        
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        container.addView(item)
        container.addView(View(this).apply {
            setBackgroundColor(Color.parseColor("#EEEEEE"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1)
        })
        
        return container
    }
    
    private fun openComic(comic: ComicFile) {
        val intent = Intent(this, ReaderActivity::class.java).apply {
            putExtra("comic_path", comic.path)
            putExtra("comic_name", comic.name)
        }
        startActivity(intent)
    }
    
    private fun scanComics(layout: LinearLayout) {
        Toast.makeText(this, "Escaneando...", Toast.LENGTH_SHORT).show()
        
        Thread {
            comics = comicScanner.scanDevice()
            
            runOnUiThread {
                selectTab(0)
                Toast.makeText(this, "Encontrados: ${comics.size} cómics", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }
    
    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
        }
    }
    
    private fun createSearchTab(): View {
        return TextView(this).apply {
            text = "🔍 Buscar cómics\n\nPróximamente..."
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#999999"))
            setPadding(40, 80, 40, 80)
        }
    }
    
    private fun createReadingTab(): View {
        return TextView(this).apply {
            text = "📖 Continuar leyendo\n\nPróximamente..."
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#999999"))
            setPadding(40, 80, 40, 80)
        }
    }
    
    private fun createFavoritesTab(): View {
        return TextView(this).apply {
            text = "⭐ Favoritos\n\nPróximamente..."
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#999999"))
            setPadding(40, 80, 40, 80)
        }
    }
}
