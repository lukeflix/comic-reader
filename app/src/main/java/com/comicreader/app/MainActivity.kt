package com.comicreader.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.LinearLayout
import android.view.Gravity
import android.graphics.Color
import android.view.View

class MainActivity : AppCompatActivity() {
    
    private lateinit var container: LinearLayout
    private lateinit var tabs: List<TextView>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        container = findViewById(R.id.container)
        
        // Obtener las pestañas
        val tab1 = findViewById<TextView>(R.id.tab_library)
        val tab2 = findViewById<TextView>(R.id.tab_search)
        val tab3 = findViewById<TextView>(R.id.tab_reading)
        val tab4 = findViewById<TextView>(R.id.tab_favorites)
        
        tabs = listOf(tab1, tab2, tab3, tab4)
        
        // Click listeners
        tab1.setOnClickListener { selectTab(0) }
        tab2.setOnClickListener { selectTab(1) }
        tab3.setOnClickListener { selectTab(2) }
        tab4.setOnClickListener { selectTab(3) }
        
        // Iniciar en Library
        selectTab(0)
    }
    
    private fun selectTab(index: Int) {
        // Resetear todos los tabs
        tabs.forEach { it.setTextColor(Color.parseColor("#ABABAB")) }
        
        // Activar el seleccionado
        tabs[index].setTextColor(Color.parseColor("#111111"))
        
        // Limpiar contenedor
        container.removeAllViews()
        
        // Mostrar contenido según pestaña
        val content = when(index) {
            0 -> createLibraryTab()
            1 -> createSearchTab()
            2 -> createReadingTab()
            3 -> createFavoritesTab()
            else -> TextView(this)
        }
        
        container.addView(content)
    }
    
    private fun createLibraryTab(): TextView {
        return TextView(this).apply {
            text = "📚 Biblioteca\n\nTus cómics aparecerán aquí"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#555555"))
            setPadding(40, 80, 40, 80)
        }
    }
    
    private fun createSearchTab(): TextView {
        return TextView(this).apply {
            text = "🔍 Buscar cómics"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#555555"))
            setPadding(40, 80, 40, 80)
        }
    }
    
    private fun createReadingTab(): TextView {
        return TextView(this).apply {
            text = "📖 Lectura\n\nEstás leyendo: Ninguno"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#555555"))
            setPadding(40, 80, 40, 80)
        }
    }
    
    private fun createFavoritesTab(): TextView {
        return TextView(this).apply {
            text = "⭐ Favoritos\n\nNo tienes favoritos aún"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#555555"))
            setPadding(40, 80, 40, 80)
        }
    }
}
