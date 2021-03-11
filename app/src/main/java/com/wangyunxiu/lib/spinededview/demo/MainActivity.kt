package com.wangyunxiu.lib.spinededview.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        spinedEditView.setItems(listOf("ABC","ACB","CBA"))
        spinedEditView2.setItems(listOf("ABC","ACB","CBA"))
    }
}