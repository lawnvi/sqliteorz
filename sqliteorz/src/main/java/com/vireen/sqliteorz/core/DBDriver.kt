package com.vireen.sqliteorz.core

import android.content.Context
import android.os.Environment
import android.util.Log
import com.vireen.sqliteorz.model.DBBaseModel
import java.io.File
import kotlin.collections.HashMap

class DBDriver(
    internal var name: String = "",
    internal var version: Int = 1,
    private var dirPath: String = ""
) {
    //存放创建语句
    val createSql: HashMap<String, String> = HashMap()
    val models: MutableSet<Class<out DBBaseModel>> = mutableSetOf()

    /**
     * 没有限制命名，请自行遵守规范
     * 生成建表sql
     * 存放table name
     */
    fun registerModel(obj: DBBaseModel) {
        //生成建表sql
        val sql = obj.getCreateSql()
        Log.i(TAG, "add create sql: $sql")
        createSql[obj.getTableName()] = sql
        this.models.add(obj::class.java)
    }

    fun registerModels(models: Set<DBBaseModel>) {
        for (item in models) {
            //生成建表sql
            val sql = item.getCreateSql()
            Log.i(TAG, "add create sql: $sql")
            createSql[item.getTableName()] = sql
            this.models.add(item::class.java)
        }
    }

    fun registerModel(T: Class<out DBBaseModel>) {
        //生成建表sql
        val model = T.newInstance()
        val sql = model.getCreateSql()
        Log.i(TAG, "add create sql: $sql")
        createSql[model.getTableName()] = sql
        this.models.add(T)
    }

    fun registerModels(Ts: Set<Class<out DBBaseModel>>) {
        for (T in Ts) {
            //生成建表sql
            val model = T.newInstance()
            val sql = model.getCreateSql()
            Log.i(TAG, "add create sql: $sql")
            createSql[model.getTableName()] = sql
        }
        this.models.addAll(Ts)
    }

    internal fun getPath(context: Context): String {
        if (dirPath.isBlank()) {
            dirPath = Environment.getExternalStorageDirectory().absolutePath
        }

        val f = File(dirPath)
        if (!f.exists()) {
            f.mkdirs()
        }

        if (name.isBlank()) {
            name = defaultName(context)
        }

        dirPath = if (dirPath.endsWith("/")) dirPath else "$dirPath/"
        return dirPath + name
    }
}