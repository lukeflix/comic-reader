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

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(0, 0, 0, 20)
        }

        listOf("Library", "Search", "Reading", "Favs").forEach { name ->
            tabs.addView(TextView(this).apply {
                text = name
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#111111"))
                setPadding(20, 30, 20, 30)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
        layout.addView(tabs)

        val body = TextView(this).apply {
            text = "Bienvenido a Comic Reader\n\nTus cómics aparecerán aquí"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#666666"))
            setPadding(40, 100, 40, 40)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        layout.addView(body)

        setContentView(layout)
    }
}
