package com.vireen.sqliteorz.core

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.vireen.sqliteorz.model.DBBaseModel
import java.lang.StringBuilder
import java.lang.reflect.Field

/**
 * DBHelper
 */
class DBHelper(context: Context, config: DBDriver) :
    SQLiteOpenHelper(context, config.getPath(context), null, config.version) {

    private val dbInfo = config

    override fun onCreate(db: SQLiteDatabase?) {
        for (map in dbInfo.createSql) {
            db?.execSQL(map.value)
            Log.i(TAG, "db: ${dbInfo.name} create table: ${map.key}, sql: ${map.value}")
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        Log.i(TAG, "update database version from $oldVersion to $newVersion")
        if (oldVersion < newVersion){
            for (map in dbInfo.createSql) {
                db?.execSQL("DROP TABLE IF EXISTS ${map.key}")
            }
            onCreate(db)
        }
    }

    fun <T: DBBaseModel> select(clazz: Class<T>, where: String, orderBy: String, limit: Int, offset: Int): List<T>{
        val model = clazz.newInstance()
        val list = arrayListOf<T>()
        var sql = "select * from ${model.getTableName()} where $where ORDER BY $orderBy"
        if (limit > 0 && offset > -1){
            sql += " LIMIT $limit OFFSET $offset"
        }
        val db = this.writableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(sql, null)
            if (cursor.moveToFirst()) {
                do {
                    list.add(model.cursor2Model(cursor, clazz))
                }while (cursor.moveToNext())
            }
        }catch (e: Exception){
            Log.e(TAG, "select err: ${e.message} ${e.cause}")
            e.printStackTrace()
        }finally {
            cursor?.close()
        }
        return list
    }

    @SuppressLint("Range")
    @Deprecated("move to DBBaseModel")
    private fun cursor2Any(cursor: Cursor, obj: Any): Any{
        val backup = obj.javaClass.newInstance()
        val fields = backup.javaClass.declaredFields.toMutableList()
        fields.addAll(backup.javaClass.superclass?.declaredFields as Array<out Field>)
        for (field: Field in fields){
            field.isAccessible = true
            when(field.type.name){
                "int" -> field.setInt(backup, cursor.getInt(cursor.getColumnIndex(field.name)))
                "long" -> field.setLong(backup, cursor.getLong(cursor.getColumnIndex(field.name)))
                "float" -> field.setFloat(backup, cursor.getFloat(cursor.getColumnIndex(field.name)))
                "double" -> field.setDouble(backup, cursor.getDouble(cursor.getColumnIndex(field.name)))
                "java.lang.String" -> field.set(backup, cursor.getString(cursor.getColumnIndex(field.name)))
                else -> {
                    //Log.d(TAG, "select field ${field.name} ${field.type.name}")
                }
            }
        }
        return backup
    }

    fun <T: DBBaseModel>update(clazz: Class<T>, where: String, set: String): Boolean{
        val model = clazz.newInstance()
        val sql = "UPDATE ${model.getTableName()} set $set where $where"
        //Log.d(TAG, "update sql: $sql")
        val db = this.writableDatabase
        return try {
            db.execSQL(sql)
            true
        }catch (e: SQLException){
            e.printStackTrace()
            false
        }
    }

    fun <T: DBBaseModel> delete(clazz: Class<T>, where: String): Boolean{
        val model = clazz.newInstance()
        val sql = "DELETE FROM ${model.getTableName()} WHERE $where"
        //Log.d(TAG, "delete sql: $sql")
        val db = this.writableDatabase
        return try {
            db.execSQL(sql)
            true
        }catch (e: SQLException){
            e.printStackTrace()
            false
        }
    }

    @SuppressLint("Recycle")
    @Synchronized
    fun <T: DBBaseModel>insert(model: T): Int{
        val sb = StringBuilder()
        sb.append("INSERT INTO ").append(model.getTableName())
            .append(" ").append(model.fieldStr()).append(" VALUES ")
            .append(model.valueStr())
        val db = this.writableDatabase
        var cursor: Cursor? = null
        return try {
            db.execSQL(sb.toString())
            val sql2 = "select last_insert_rowid() from ${model.getTableName()}"
            cursor = db.rawQuery(sql2, null)
            var lastId = -1
            if (cursor.moveToFirst()){
                lastId = cursor.getInt(0)
            }
            return lastId
        }catch (e: SQLException){
            e.printStackTrace()
            Log.e(TAG, "insert error: ${e.cause} ${e.message}")
            -1
        }finally {
            cursor?.close()
        }
    }

    fun <T: DBBaseModel> insertMany(models: List<T>){
        if (models.isEmpty()){
            Log.w(TAG, "DBHelper insert returned, empty list")
            return
        }

        if (models.size > 500){
            Log.w(TAG, "DBHelper insert warning, insert size is ${models.size}, may failed in some device")
        }

        val sb = StringBuilder()
        sb.append("INSERT OR IGNORE INTO ")
            .append(models[0].getTableName()).append(" ")
            .append(models[0].fieldStr()).append(" VALUES ")
        for (m in models){
            sb.append(m.valueStr()).append(", ")
        }
        return try {
            this.writableDatabase.execSQL(sb.toString().substringBeforeLast(", "))
        }catch (e: SQLException){
            Log.e(TAG, "insert many error: ${e.cause} ${e.message}")
            e.printStackTrace()
        }
    }

    fun <T: DBBaseModel> count(model: T, where: String): Long{
        val sql = "select count(*) from ${model.getTableName()} where $where"
        val db = this.writableDatabase
        var count = 0L
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(sql, null)
            cursor.moveToFirst()
            count = cursor.getLong(0)
        }catch (e: Exception){
            Log.e(TAG, "select err: ${e.message} ${e.cause}")
            e.printStackTrace()
        }finally {
            cursor?.close()
        }
        return count
    }

    //拼接对象成员到列名
    @Deprecated("move to DBBaseModel")
    private fun fieldStr(before: String, model: Any): String{
        var sql = "("
        for (field: Field in model.javaClass.declaredFields){
            //Log.d(TAG, "name ${field.name} type ${field.type.name}")
            field.isAccessible = true
            if (field.name != "id"){
                sql += field.name + ", "
            }
        }
        return sql.substringBeforeLast(", ")+ ")"
    }

    @Deprecated("move to DBBaseModel")
    private fun valueStr(model: Any): String{
        var values = "("
        for (field: Field in model.javaClass.declaredFields){
            //Log.d(TAG, "name ${field.name} type ${field.type.name}")
            field.isAccessible = true
            val v = when(field.type.name){
                "int" -> field.getInt(model).toString()
                "long" -> field.getLong(model).toString()
                "float" -> field.getFloat(model).toString()
                "double" -> field.getDouble(model).toString()
                "java.lang.String" -> {
                    val s = escapeChar((field.get(model)?:"").toString())
                    "'$s'"
                }
                else -> null
            }
            if (field.name != "id"){
                values += "$v, "
            }
        }
        return values.substringBeforeLast(", ")+ ")"
    }

    //仅转义'
    private fun escapeChar(str: String): String{
        return if (str.contains("'"))
            str.replace("'", "''")
        else
            str
    }

    //set 替换为map传参
    @Deprecated("move to DBBaseModel")
    private fun buildSetString(map: Map<String, Any>): String {
        var set = ""
        for (s in map) {
            set = if (s.value.javaClass.name == "java.lang.String") {
                "$set, ${s.key} = '${escapeChar(s.value.toString())}'"
            } else {
                "$set, ${s.key} = ${s.value}"
            }
        }
        return if (set.isNotEmpty())
            set.substringAfter(", ")
        else
            set
    }
}