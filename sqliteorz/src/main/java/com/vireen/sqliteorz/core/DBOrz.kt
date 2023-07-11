package com.vireen.sqliteorz.core

import android.util.Log
import com.vireen.sqliteorz.model.DBBaseModel

object DBOrz{
    inline fun <reified T: DBBaseModel> findModels(where: String = "1=1", orderMap: Map<String, Int> = mapOf(), limit: Int = -1, offset: Int = -1): List<T>{
        if (where.isBlank()){
            Log.w(TAG, "where is blank, can't finger out which record U want to find, or U want to find all")
            return mutableListOf()
        }
        val order = buildOrderStr(orderMap)
        return OrzHelper.model(T::class.java).select(T::class.java, where, order, limit, offset)
    }

    inline fun <reified T: DBBaseModel> findOne(where: String = "", id: Int = -1): T?{
        var w = where
        if (w.isBlank() && id >= 0){
            w = "id = $id"
        }
        if (w.isBlank()){
            Log.w(TAG, "where is blank and id is $id, can't finger out which record U want to find")
            return null
        }
        val list = findModels<T>(w)
        if (list.isEmpty()){
            return null
        }
        if (list.size > 1){
            Log.w(TAG, "find ${list.size} models, but I just return index 0, $w")
        }
        return list[0]
    }

    fun <T: DBBaseModel> saveModels(objects: List<T>){
        if (objects.isEmpty()){
            return
        }
        val temp = mutableListOf<T>()
        for (obj in objects){
            temp.add(obj)
            if (temp.size == 500){
                OrzHelper.model(obj).insertMany(temp)
                temp.clear()
            }
        }
        // insert the rest objects
        OrzHelper.model(objects[0]).insertMany(temp)
    }

    inline fun <reified T: DBBaseModel> update(setMaps: Map<String, Any?>, where: String): Boolean{
        if (setMaps.isEmpty()){
            Log.w(TAG, "set map is empty, can't finger out what U want to update")
            return false
        }
        if (where == ""){
            Log.w(TAG, "where is empty & id = -1, can't finger out which record U want to update, or U want to update all")
            return false
        }
        return OrzHelper.model(T::class.java).update(T::class.java, where, buildSetString(setMaps))
    }

    inline fun <reified T: DBBaseModel> count(where: String = "1 = 1"): Long{
        if (where.isBlank()){
            return 0
        }
        return OrzHelper.model(T::class.java).count<T>(T::class.java.newInstance(), where)
    }

    inline fun <reified T: DBBaseModel> delete(where: String): Boolean {
        if (where.isBlank()){
            return false
        }
        return OrzHelper.model(T::class.java).delete(T::class.java, where)
    }
}