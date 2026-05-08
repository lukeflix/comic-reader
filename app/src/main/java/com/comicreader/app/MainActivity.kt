package com.comicreader.app

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.widget.LinearLayout
import android.graphics.Color
import android.view.Gravity

class MainActivity : Activity() {

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

        val bodyContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
            id = 9999
        }
        layout.addView(bodyContainer)

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(0, 0, 0, 20)
        }

        val tabNames = listOf("Library", "Search", "Reading", "Favs")
        val tabTexts = listOf(
            "Aquí verás tus cómics\ny podrás escanear archivos CBR/CBZ",
            "Busca cómics por nombre o autor",
            "Continúa leyendo donde lo dejaste",
            "Tus cómics favoritos"
        )

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
                    
                    bodyContainer.removeAllViews()
                    bodyContainer.addView(TextView(this@MainActivity).apply {
                        text = tabTexts[index]
                        textSize = 16f
                        gravity = Gravity.CENTER
                        setTextColor(Color.parseColor("#666666"))
                        setPadding(40, 60, 40, 40)
                    })
                }
            }
            tabs.addView(tab)
        }
        layout.addView(tabs)

        setContentView(layout)

        (tabs.getChildAt(0) as TextView).performClick()
    }
}
