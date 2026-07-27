package com.opoleyes.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.opoleyes.data.model.TestData

object DataProvider {
    private val gson = Gson()
    private var cachedData: List<TestData>? = null

    fun loadData(context: Context): List<TestData> {
        cachedData?.let { return it }
        val json = context.assets.open("data.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<TestData>>() {}.type
        val data: List<TestData> = gson.fromJson(json, type)
        cachedData = data
        return data
    }

    fun getTestDataMap(context: Context): Map<String, TestData> {
        return loadData(context).associateBy { it.test.id }
    }

    fun getTests(context: Context) = loadData(context).map { it.test }

    fun getTemaTests(context: Context) = loadData(context).map { it.test }.filter { it.tema != null }.sortedBy { it.tema }
}
