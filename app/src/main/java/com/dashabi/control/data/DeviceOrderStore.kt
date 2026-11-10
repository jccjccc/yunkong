package com.dashabi.control.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 用户自定义设备在账号总览筛选条里的顺序（按设备码保存）。 */
class DeviceOrderStore(context: Context) {
    private val prefs = context.getSharedPreferences("device_order", Context.MODE_PRIVATE)
    private val _order = MutableStateFlow(load())
    val order: StateFlow<List<String>> = _order

    fun save(codes: List<String>) {
        _order.value = codes.distinct()
        prefs.edit().putString("codes", codes.joinToString(",")).apply()
    }

    fun move(code: String, delta: Int, allCodes: List<String>) {
        val list = (allCodes).toMutableList()
        val i = list.indexOf(code)
        if (i < 0) return
        val j = (i + delta).coerceIn(0, list.size - 1)
        if (i == j) return
        val tmp = list[i]
        list[i] = list[j]
        list[j] = tmp
        save(list)
    }

    private fun load(): List<String> =
        prefs.getString("codes", "").orEmpty().split(',').filter { it.isNotBlank() }
}
