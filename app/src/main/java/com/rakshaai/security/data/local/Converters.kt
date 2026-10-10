package com.rakshaai.security.data.local

import androidx.room.TypeConverter
import com.rakshaai.security.data.model.RiskSignal
import org.json.JSONArray
import org.json.JSONObject

class Converters {

    @TypeConverter
    fun fromRiskSignals(signals: List<RiskSignal>): String {
        val arr = JSONArray()
        for (sig in signals) {
            val obj = JSONObject()
            obj.put("title", sig.title)
            obj.put("description", sig.description)
            obj.put("isPresent", sig.isPresent)
            arr.put(obj)
        }
        return arr.toString()
    }

    @TypeConverter
    fun toRiskSignals(json: String): List<RiskSignal> {
        val list = mutableListOf<RiskSignal>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    RiskSignal(
                        title = obj.optString("title", ""),
                        description = obj.optString("description", ""),
                        isPresent = obj.optBoolean("isPresent", true)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return JSONArray(list).toString()
    }

    @TypeConverter
    fun toStringList(json: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
        } catch (_: Exception) {}
        return list
    }
}
