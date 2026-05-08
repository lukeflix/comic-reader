package com.comicreader.app

import android.os.Bundle
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Toast
import android.view.Gravity
import android.graphics.Color
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var container: LinearLayout
    private lateinit var tabs: List<TextView>
    private var selectedTab = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        container = findViewById(R.id.container)
        
        val tab1 = findViewById<TextView>(R.id.tab_library)
        val tab2 = findViewById<TextView>(R.id.tab_search)
        val tab3 = findViewById<TextView>(R.id.tab_reading)
        val tab4 = findViewById<TextView>(R.id.tab_favorites)
        
        tabs = listOf(tab1, tab2, tab3, tab4)
        
        tab1.setOnClickListener { changeTab(0) }
        tab2.setOnClickListener { changeTab(1) }
        tab3.setOnClickListener { changeTab(2) }
        tab4.setOnClickListener { changeTab(3) }
        
        changeTab(0)
    }
    
    private fun changeTab(index: Int) {
        selectedTab = index
        
        for (i in tabs.indices) {
            if (i == index) {
                tabs[i].setTextColor(Color.parseColor("#111111"))
            } else {
                tabs[i].setTextColor(Color.parseColor("#ABABAB"))
            }
        }
        
        container.removeAllViews()
        
        val text = when(index) {
            0 -> "📚 Mi Biblioteca\n\nPróximamente:\nEscanear cómics CBR/CBZ/PDF/ePub"
            1 -> "🔍 Buscar\n\nPróximamente:\nBuscar cómics por título o autor"
            2 -> "📖 Lectura\n\nPróximamente:\nContinuar donde dejaste"
            3 -> "⭐ Favoritos\n\nPróximamente:\nTus cómics favoritos"
            else -> ""
        }
        
        val tv = TextView(this).apply {
            this.text = text
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#666666"))
            setPadding(40, 80, 40, 80)
        }
        
        container.addView(tv)
        Toast.makeText(this, tabs[index].text, Toast.LENGTH_SHORT).show()
    }
}
