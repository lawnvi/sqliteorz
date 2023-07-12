package com.vireen.sqliteorz.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.vireen.sqliteorz.R
import com.vireen.sqliteorz.core.DBDriver
import com.vireen.sqliteorz.core.DBOrz
import com.vireen.sqliteorz.core.OrzHelper
import com.vireen.sqliteorz.core.TAG
import com.vireen.sqliteorz.databinding.ActivityMainBinding
import com.vireen.sqliteorz.demo.model.DemoModel
import java.lang.StringBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initDB()

        binding.insert.setOnClickListener {
            val obj = DemoModel("张三", age = (System.currentTimeMillis()%100).toInt())
            obj.save()
            updateContent()
        }

        binding.update.setOnClickListener {
            DemoModel("").update(mapOf("name" to "李四"), "id > 1")
            updateContent()
        }

        binding.delete.setOnClickListener {
            DemoModel("").delete("id > 0")
            updateContent()
        }
    }

    private fun initDB() {
        val driver = DBDriver()
        driver.registerModel(DemoModel::class.java)
        OrzHelper.initialize(this.applicationContext, drivers = arrayOf(driver))
    }

    private fun updateContent() {
        val count = DBOrz.count<DemoModel>("id > 0")
        if (count > 0L) {
            val objs = DBOrz.findModels<DemoModel>("id > 0")
            val sb = StringBuilder()
            sb.append( "ALL count: $count\n")
            for (item in objs) {
                sb.append(item.toString()).append("\n")
            }
            binding.output.text = sb.toString()
        }else {
            binding.output.text = "没有找到数据"
        }
    }
}