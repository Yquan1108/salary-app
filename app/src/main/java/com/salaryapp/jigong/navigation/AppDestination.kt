package com.salaryapp.jigong.navigation

sealed class AppDestination(
    val route: String,
    val displayName: String
) {
    data object Onboarding : AppDestination("onboarding", "开始使用")
    data object Home : AppDestination("home", "首页")
    data object WorkRecord : AppDestination("work_record", "记今天做工")
    data object WorkRecordEditor : AppDestination("work_record_editor?recordId={recordId}&saved={saved}", "新增记工")
    data object Photo : AppDestination("photo", "存工地照片")
    data object PhotoSearch : AppDestination("photo_search", "找工地照片")
    data object SalaryStats : AppDestination("salary_stats", "工资汇总")
    data object Worker : AppDestination("worker", "工人名单")
    data object Site : AppDestination("site", "工地名单")
    data object Settings : AppDestination("settings", "设置")

    fun editorRoute(recordId: Long? = null, saved: Boolean = false): String {
        return "work_record_editor?recordId=${recordId ?: -1}&saved=$saved"
    }
}
