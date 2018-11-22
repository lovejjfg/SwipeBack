package com.lovejjfg.swipeback.demo

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.backLayout
import kotlinx.android.synthetic.main.activity_main.toolbar

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        toolbar.setOnClickListener {
            startActivity(Intent(this, Main2Activity::class.java))
        }
        backLayout.setOnClickListener {
            startActivity(Intent(this, Main2Activity::class.java))
        }
    }
}
