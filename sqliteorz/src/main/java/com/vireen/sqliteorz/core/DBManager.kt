package com.vireen.sqliteorz.core

import android.content.Context
import android.util.Log
import java.io.File

class DBManager {
    companion object{
        private var instance: DBManager? = null
        @Synchronized
        fun getInstance(): DBManager{
            if (instance == null){
                instance = DBManager()
            }
            return instance!!
        }
    }

    //数据库存放位置
    private var dbPath = ""
    //库名对应db, 一个app可能存在多个数据库
    private val dbMap = HashMap<String, DBDriver>()
    //存放dbHelper实例
    private val dbHelpers = HashMap<String, DBHelper>()

    /**
     * 请在配置dbConfig后调用
     * 差不多就是最后
     */
    fun initManager(context: Context) {
        for (m in dbMap){
            dbHelpers[m.key] = DBHelper(context, m.value)
//            Log.d(TAG, "name ${m.key} path: ${dbHelpers[m.key]?.readableDatabase?.path}")
        }
    }

    fun setDBPath(path: String){
        val f = File(path)
        if (!f.exists()){
            f.mkdirs()
        }
        dbPath = if (path.endsWith("/")) path else "$path/"
    }

    fun getDBPath(): String{
        return dbPath
    }

    /**
     * 添加db
     */
    fun addDBConfig(config: DBDriver){
        dbMap[config.name] = config
    }

    /**
     * 根据库名获取db对象
     * 默认获取主库
     */
    fun getDBHelper(name: String): DBHelper?{
        if (dbHelpers.containsKey(name))
            return dbHelpers[name]
        Log.e(TAG, "not find db: $name")
        return null
    }
}