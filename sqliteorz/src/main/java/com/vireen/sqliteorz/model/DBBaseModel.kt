package com.vireen.sqliteorz.model

import android.annotation.SuppressLint
import android.database.Cursor
import android.util.Base64
import android.util.Log
import androidx.core.database.getBlobOrNull
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getFloatOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getShortOrNull
import androidx.core.database.getStringOrNull
import com.vireen.sqliteorz.core.OrzHelper
import com.vireen.sqliteorz.core.DBException
import com.vireen.sqliteorz.core.DBOrz
import com.vireen.sqliteorz.core.TAG
import com.vireen.sqliteorz.core.buildSetString
import com.vireen.sqliteorz.core.supportFields
import com.vireen.sqliteorz.core.type2Sqlite
import java.lang.reflect.Field
import java.util.Locale

/**
 * 通过反射实现
 * 为继承此类的实体类提供封装的insert, update, delete, select
 * 无须对每个model手动解析
 */
open class DBBaseModel {
    //记录id
    var id: Int = -1
    var createTime = System.currentTimeMillis()
    var updateTime = System.currentTimeMillis()

    open fun save(): Boolean{
        synchronized(this::class.java){
            id = OrzHelper.model(this).insert(this)
        }
        return id != -1
    }

    inline fun <reified T: DBBaseModel> T.find(where: String = "1=1", orderMap: Map<String, Int> = mapOf(), limit: Int = -1, offset: Int = -1): List<T> {
        var whereStr = where
        if (where.isBlank() && id > -1){
            whereStr = "id = $id"
        }
        return DBOrz.findModels(whereStr, orderMap, limit, offset)
    }

    /**
     * if use func like *many, I suggest override
     * @see fieldStr (members)
     * @see valueStr (v1, v2) string need ''
     *
     * some device can only insert 500 logs to sqlite once
     */
    open fun <T: DBBaseModel> saveMany(objects: List<T>){
        val temp = mutableListOf<T>()
        for (obj in objects){
            temp.add(obj)
            if (temp.size == 500){
                OrzHelper.model(this).insertMany(temp)
                temp.clear()
            }
        }
        // insert the rest objects
        OrzHelper.model(this).insertMany(temp)
    }

    /**
     * 默认以id操作
     * @param where default "" just like sql attention please：where的字符串中的特殊字符请手动转义
     */
    fun delete(where: String = ""): Boolean{
        if (where == "" && id == -1){
            Log.w(
                TAG,
                "where is empty & id = -1, can't finger out which record U want to delete, or U want to delete all"
            )
            return false
        }
        var w = where
        if (where == ""){
            w = "id = $id"
        }
        return OrzHelper.use().delete(this::class.java, w)
    }

    /**
     * update by own id default
     *  param: "this" is a hide param, which means what them will to be
     * @param where default "" just like sql attention please：where的字符串中的特殊字符请手动转义
     */
    open fun update(setMaps: Map<String, Any?>, where: String = ""): Boolean{
        if (setMaps.isEmpty()){
            Log.w(TAG, "set map is empty, can't finger out what U want to update")
            return false
        }
        if (where == "" && id == -1){
            Log.w(TAG, "where is empty & id = -1, can't finger out which record U want to update, or U want to update all")
            return false
        }
        var w = where
        if (where == ""){
            w = "id = $id"
        }
        return OrzHelper.model(this).update(this::class.java, w, buildSetString(setMaps))
    }

    fun count(where: String = "1 = 1"): Long{
        return OrzHelper.model(this).count(this, where)
    }

    /**
     * get fields and format like this: (m1, m2, m3, ...)
     * baseModel use reflection
     * you can override by hard code, will be faster a little
     */
    open fun fieldStr(): String{
        var sql = "("
        for (field: Field in getFields()){
            field.isAccessible = true
            if (field.name != "id"){
                sql += field.name + ", "
            }
        }
        return sql.substringBeforeLast(", ")+ ")"
    }

    /**
     * get field values and format like this: (v1, v2, v3, ...)
     * String fields should use
     * @see escapeChar and '' like: '${escapeChar(member)}'
     * baseModel use reflection
     * you can override by hard code, will be faster a little
     */
    open fun valueStr(): String{
        val valueStrBuilder = StringBuilder("(")
        for (field: Field in getFields()){
            field.isAccessible = true
            val v = if (field.get(this) == null){
                "null"
            }else {
                when(field.type.name){
                    "int", "java.lang.Integer" -> field.getInt(this).toString()
                    "short", "java.lang.Short" -> field.getShort(this).toString()
                    "long", "java.lang.Long" -> field.getLong(this).toString()
                    "boolean", "java.lang.Boolean" -> (if (field.getBoolean(this)) 1 else 0).toString()
                    "float", "java.lang.Float" -> field.getFloat(this).toString()
                    "double", "java.lang.Double" -> field.getDouble(this).toString()
                    "java.lang.String" -> "'${escapeChar((field.get(this)?:"").toString())}'"
                    "char", "java.lang.Character" -> field.getChar(this)
                    "[B" -> "'${
                        Base64.encodeToString(
                            field.get(this) as ByteArray?,
                            Base64.DEFAULT
                        )
                    }'"
                    else -> throw DBException("un-support field type: ${field.type.name}, field: ${field.name}")
                }
            }

            if (field.name != "id"){
                valueStrBuilder.append("$v, ")
            }
        }
        return valueStrBuilder.toString().substringBeforeLast(", ") + ")"
    }

    protected fun escapeChar(str: String): String{
        return if (str.contains("'"))
            str.replace("'", "''")
        else
            str
    }

    /**
     * 自定义表名
     * 默认使用上级dir+model name
     */
    open fun getTableName(): String{
        var clazzName = this.javaClass.name.substringBeforeLast(".").substringAfterLast(".")
        clazzName += this.javaClass.simpleName
        return clazzName.lowercase(Locale.ROOT)
    }

    /**
     * cursor 2 model what we expect object by reflection
     * if select too many records frequently, you can override it
     */
    @SuppressLint("Range")
    open fun <T: DBBaseModel> cursor2Model(cursor: Cursor, clazz: Class<T>): T {
        val backup = clazz.newInstance()?: throw DBException("cursor to model error, ${clazz.name}")
        for (field: Field in getFields()){
            field.isAccessible = true
            when(field.type.name){
                "int", "java.lang.Integer" -> field.setInt(backup, cursor.getIntOrNull(cursor.getColumnIndex(field.name))?: 0)
                "short", "java.lang.Short" -> field.setShort(backup, cursor.getShortOrNull(cursor.getColumnIndex(field.name))?: 0)
                "boolean", "java.lang.Boolean" -> field.setBoolean(backup, cursor.getIntOrNull(cursor.getColumnIndex(field.name)) == 1)
                "long", "java.lang.Long" -> field.setLong(backup, cursor.getLongOrNull(cursor.getColumnIndex(field.name))?: 0L)
                "float", "java.lang.Float" -> field.setFloat(backup, cursor.getFloatOrNull(cursor.getColumnIndex(field.name))?: 0f)
                "double", "java.lang.Double" -> field.setDouble(backup, cursor.getDoubleOrNull(cursor.getColumnIndex(field.name))?: 0.0)
                "char", "java.lang.Character" -> {
                    cursor.getStringOrNull(cursor.getColumnIndex(field.name)).let {
                        if (!it.isNullOrEmpty()){
                            field.setChar(backup, it[0])
                        }
                    }
                }
                "java.lang.String" -> field.set(backup, cursor.getStringOrNull(cursor.getColumnIndex(field.name)))
                "[B" -> {
                    val bytes = cursor.getBlobOrNull(cursor.getColumnIndex(field.name))
                    if (bytes == null){
                        field.set(backup, null)
//                        field.set(backup, ByteArray(0))
                        Log.e(
                            TAG,
                            "find a null bytes member data, is that right? field name: ${field.name}"
                        )
                    }else{
                        field.set(backup, Base64.decode(String(bytes), Base64.DEFAULT))
                    }
                }
                else -> {
                    throw DBException("un-support field type: ${field.type.name}, field: ${field.name}")
                }
            }
        }
        return backup
    }

    //create table sql
    open fun getCreateSql(): String{
        //生成建表sql
        var sql = "CREATE TABLE IF NOT EXISTS ${getTableName()} (id INTEGER PRIMARY KEY AUTOINCREMENT"
        for (field: Field in getFields()){
            if (field.name == "id")
                continue
            //name type annotations
            sql += ", ${field.name} ${type2Sqlite(field)}"
        }
        sql += ")"
        return sql
    }

    private fun getFields(): Set<Field>{
        val clazz = this.javaClass
        val set = mutableSetOf<Field>()
        for (item in clazz.superclass.declaredFields + clazz.declaredFields){
            if (item.type.name in supportFields) {
                set.add(item)
            }
        }

        return set
    }
}