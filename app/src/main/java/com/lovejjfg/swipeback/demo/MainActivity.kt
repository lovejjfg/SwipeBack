package com.lovejjfg.swipeback.demo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.backLayout
import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.android.synthetic.main.activity_main2.fab

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        toolbar.setOnClickListener {
            startActivity(Intent(this, Main2Activity::class.java))
        }
        backLayout.callback = {
            startActivity(Intent(this, Main2Activity::class.java))
        }
        fab.setOnClickListener { view ->
            startActivity(Intent(this, Main2Activity::class.java))
        }
    }
}
