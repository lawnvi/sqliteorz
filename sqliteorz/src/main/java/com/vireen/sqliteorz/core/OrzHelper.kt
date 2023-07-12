package com.vireen.sqliteorz.core

import android.content.Context
import android.util.Log
import com.vireen.sqliteorz.model.DBBaseModel

/**
 * 日常使用此类操作DB
 */
internal class DBox(
    var driver: DBDriver,
    var db: DBHelper,
)


object OrzHelper {
    private val driverMap: MutableMap<String, DBox> = mutableMapOf()
    private var defaultName = ""


    fun initialize(context: Context, default: String = "", vararg drivers: DBDriver) {
        for (driver in drivers) {
            if (driver.models.isEmpty()) {
                Log.e(TAG, "driver ${driver.name} not contains any models, are u ok?")
                continue
            }
            DBox(driver, DBHelper(context, driver)).let {
                this.driverMap[driver.name] = it
            }
        }
        if (this.driverMap.isEmpty()) {
            throw DBException("not provide any drivers")
        }
        this.defaultName = default.ifBlank { defaultName(context) }
    }

    fun use(name: String = defaultName): DBHelper {
        driverMap[name] ?: throw DBException("not match any db name, break it, name: $name")
        return driverMap[name]?.db!!
    }

    fun <T: DBBaseModel> model(clazz: Class<T>): DBHelper {
        for (item in driverMap) {
            if (item.value.driver.models.contains(clazz))
                return use(item.key)
        }
        throw DBException("not match any db")
    }

    fun model(model: DBBaseModel): DBHelper {
        return model(model::class.java)
    }
}