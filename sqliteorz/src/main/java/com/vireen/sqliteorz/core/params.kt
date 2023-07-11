package com.vireen.sqliteorz.core

import android.content.Context

const val TAG = "sqlite-orz"

internal val supportFields = arrayOf(
    "int", "java.lang.Integer",
    "short", "java.lang.Short",
    "boolean", "java.lang.Boolean",
    "long", "java.lang.Long",
    "float", "java.lang.Float",
    "double", "java.lang.Double",
    "char", "java.lang.Character",
    "java.lang.String",
    "[B"
)

internal fun defaultName(context: Context): String {
    return context.packageName.substringAfterLast(".")
}