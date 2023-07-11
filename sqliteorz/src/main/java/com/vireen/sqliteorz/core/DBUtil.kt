package com.vireen.sqliteorz.core

import android.util.Base64
import android.util.Log
import java.lang.reflect.Field

//以对象的class得到表名
//internal fun<T:DBBaseModel> getTableName(clazz: Class<T>): String{
//    var clazzName = clazz.name.substringBeforeLast(".").substringAfterLast(".")
//    clazzName += clazz.simpleName
//    return clazzName.lowercase(Locale.ROOT)
//}

//获取class类型对应sqlite类型
internal fun type2Sqlite(field: Field): String{
    return when(field.type.name){
        "java.lang.String", "java.lang.Character", "char" -> "TEXT"
        "double", "float", "java.lang.Float", "java.lang.Double" -> "REAL"
        "boolean", "short", "int", "long", "java.lang.Integer", "java.lang.Short", "java.lang.Long", "java.lang.Boolean" -> "INTEGER"
        "[B" -> "BLOB"
        else -> {
            throw DBException("un-support field type: ${field.type.name}, field: ${field.name}")
        }
    }
}

/**
 * escape for where
 * "%", "_", "[", "]", "&", "(", ")", "/" -> /_
 * "'" -> "''"
 * @param str mostly, it means where
 */
private fun escapeString(str: String): String{
    val list = listOf("%", "_", "[", "]", "&", "(", ")", "/")
    var res = str
    for (s in list){
        if (str.contains(s)){
            res = res.replace(s, "/$s")
        }
    }
    if (str.contains("'"))
        res = res.replace("'", "''")
    return res
}

internal fun escapeChar(str: String): String{
    return if (str.contains("'"))
        str.replace("'", "''")
    else
        str
}
//
fun buildSetString(map: Map<String, Any?>): String {
    val updateTime = System.currentTimeMillis()
    val setBuilder = StringBuilder("updateTime = $updateTime")
    for (s in map) {
        setBuilder.append(", ").append(s.key).append(" = ")
        if (s.value == null){
            setBuilder.append("null")
            Log.e(TAG, "update ${s.key} to null, is that your wish? I'm strongly not recommend set null value")
            continue
        }
        setBuilder.append(
            when(s.value!!.javaClass.name) {
                "java.lang.String" -> "'${escapeChar(s.value.toString())}'"
                "char", "java.lang.Character" -> "'${s.value}'"
                "[B" -> "'${Base64.encodeToString(s.value as ByteArray, Base64.DEFAULT)}'"
                "boolean", "java.lang.Boolean" -> if (s.value == true) 1 else 0
                else -> s.value
            }
        )
    }
    return if (setBuilder.isNotEmpty())
        setBuilder.toString().substringAfter(", ")
    else
        setBuilder.toString()
}

fun buildOrderStr(map: Map<String, Int>): String {
    if (map.isEmpty())
        return "id"
    val sb = StringBuilder()
    for (m in map){
        sb.append(m.key).append(" ")
        if (m.value > 0){
            sb.append("asc, ")
        }else {
            sb.append("desc, ")
        }
    }
    return sb.toString().substringBeforeLast(", ")
}