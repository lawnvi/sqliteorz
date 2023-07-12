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
}