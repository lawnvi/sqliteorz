package com.vireen.sqliteorz.demo.model

import com.vireen.sqliteorz.model.DBBaseModel

data class DemoModel(
    var name: String = "",
    var age: Int = 18,
    var gender: Boolean = true,
    var content: String = "",
): DBBaseModel() {

    override fun toString(): String {
        return "ID: $id Name: $name Age: $age Gender: $gender"
    }

    /**
     * 自定义表名
     */
    override fun getTableName(): String {
        return super.getTableName()
    }

    /**
     * 批量插入时自定义
     * @fieldStr
     * @valueStr
     * 可能会快点，但是一般不建议自定义，除非你确定这是正确的
     */
    override fun fieldStr(): String {
        return super.fieldStr()
    }

    override fun valueStr(): String {
        return super.valueStr()
    }
}