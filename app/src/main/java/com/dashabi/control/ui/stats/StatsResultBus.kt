package com.dashabi.control.ui.stats

/** 战绩页解析出的最近一次结果，供详情页读取。 */
object StatsResultBus {
    @Volatile
    var latest: String? = null
}
