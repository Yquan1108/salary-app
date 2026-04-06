package com.salaryapp.jigong.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class SalaryStatsExportRow(
    val workDate: String,
    val workerName: String,
    val siteName: String,
    val duration: String,
    val phoneNumber: String,
    val unitPrice: String,
    val amount: String
)

data class SalaryStatsExportSummary(
    val recordCount: Int,
    val totalAmount: String
)

data class SalaryStatsExportFilters(
    val workerName: String,
    val siteName: String,
    val startDate: String,
    val endDate: String
)

sealed interface ExportSalaryStatsResult {
    data class Success(
        val uri: Uri,
        val fileName: String
    ) : ExportSalaryStatsResult

    data class Error(val message: String) : ExportSalaryStatsResult
}

class SalaryStatsExportRepository(
    private val context: Context
) {
    suspend fun exportSalaryStats(
        rows: List<SalaryStatsExportRow>,
        summary: SalaryStatsExportSummary,
        filters: SalaryStatsExportFilters
    ): ExportSalaryStatsResult {
        if (rows.isEmpty()) {
            return ExportSalaryStatsResult.Error("没有可导出的数据")
        }

        val fileName = buildFileName(filters)
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(
                MediaStore.MediaColumns.MIME_TYPE,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(collection, values)
            ?: return ExportSalaryStatsResult.Error("导出失败，请稍后重试")

        return try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                writeWorkbook(outputStream, rows, summary, filters)
            } ?: return ExportSalaryStatsResult.Error("导出失败，无法写入文件")

            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            ExportSalaryStatsResult.Success(uri = uri, fileName = fileName)
        } catch (t: Throwable) {
            resolver.delete(uri, null, null)
            ExportSalaryStatsResult.Error("导出失败，请稍后重试")
        }
    }

    private fun buildFileName(filters: SalaryStatsExportFilters): String {
        val suffix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val workerPart = filters.workerName.ifBlank { "全部工人" }.safeFilePart()
        val sitePart = filters.siteName.ifBlank { "全部工地" }.safeFilePart()
        return "工资统计_${workerPart}_${sitePart}_${suffix}.xlsx"
    }

    private fun writeWorkbook(
        outputStream: java.io.OutputStream,
        rows: List<SalaryStatsExportRow>,
        summary: SalaryStatsExportSummary,
        filters: SalaryStatsExportFilters
    ) {
        ZipOutputStream(outputStream).use { zip ->
            zip.writeEntry("[Content_Types].xml", contentTypesXml())
            zip.writeEntry("_rels/.rels", rootRelsXml())
            zip.writeEntry("docProps/app.xml", appPropsXml())
            zip.writeEntry("docProps/core.xml", corePropsXml())
            zip.writeEntry("xl/workbook.xml", workbookXml())
            zip.writeEntry("xl/_rels/workbook.xml.rels", workbookRelsXml())
            zip.writeEntry("xl/worksheets/sheet1.xml", sheetXml(rows, summary, filters))
        }
    }

    private fun ZipOutputStream.writeEntry(path: String, content: String) {
        putNextEntry(ZipEntry(path))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun contentTypesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
            <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
            <Default Extension="xml" ContentType="application/xml"/>
            <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
            <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
            <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
        </Types>
    """.trimIndent()

    private fun rootRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
            <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
        </Relationships>
    """.trimIndent()

    private fun appPropsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"
            xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
            <Application>SalaryApp</Application>
        </Properties>
    """.trimIndent()

    private fun corePropsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
            xmlns:dc="http://purl.org/dc/elements/1.1/"
            xmlns:dcterms="http://purl.org/dc/terms/"
            xmlns:dcmitype="http://purl.org/dc/dcmitype/"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
            <dc:creator>SalaryApp</dc:creator>
            <cp:lastModifiedBy>SalaryApp</cp:lastModifiedBy>
        </cp:coreProperties>
    """.trimIndent()

    private fun workbookXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
            xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
            <sheets>
                <sheet name="工资统计" sheetId="1" r:id="rId1"/>
            </sheets>
        </workbook>
    """.trimIndent()

    private fun workbookRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
        </Relationships>
    """.trimIndent()

    private fun sheetXml(
        rows: List<SalaryStatsExportRow>,
        summary: SalaryStatsExportSummary,
        filters: SalaryStatsExportFilters
    ): String {
        val tableRows = buildList {
            add(listOf("筛选工人", filters.workerName.ifBlank { "全部" }))
            add(listOf("筛选工地", filters.siteName.ifBlank { "全部" }))
            add(listOf("开始日期", filters.startDate))
            add(listOf("结束日期", filters.endDate))
            add(emptyList())
            add(listOf("日期", "工人", "工地", "时长", "电话", "工价", "金额"))
            rows.forEach { row ->
                add(
                    listOf(
                        row.workDate,
                        row.workerName,
                        row.siteName,
                        row.duration,
                        row.phoneNumber,
                        row.unitPrice,
                        row.amount
                    )
                )
            }
            add(emptyList())
            add(listOf("记录数", summary.recordCount.toString()))
            add(listOf("总金额", summary.totalAmount))
        }

        val xmlRows = tableRows.mapIndexed { rowIndex, row ->
            val rowNumber = rowIndex + 1
            if (row.isEmpty()) {
                """<row r="$rowNumber"/>"""
            } else {
                val cells = row.mapIndexed { colIndex, value ->
                    val cellRef = "${columnName(colIndex)}$rowNumber"
                    inlineTextCell(cellRef, value)
                }.joinToString("")
                """<row r="$rowNumber">$cells</row>"""
            }
        }.joinToString("")

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <sheetData>$xmlRows</sheetData>
            </worksheet>
        """.trimIndent()
    }

    private fun inlineTextCell(cellRef: String, value: String): String {
        return """<c r="$cellRef" t="inlineStr"><is><t>${value.escapeXml()}</t></is></c>"""
    }

    private fun columnName(index: Int): String {
        var value = index
        val builder = StringBuilder()
        do {
            builder.insert(0, ('A'.code + (value % 26)).toChar())
            value = (value / 26) - 1
        } while (value >= 0)
        return builder.toString()
    }
}

private fun String.escapeXml(): String {
    return this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}

private fun String.safeFilePart(): String {
    return replace(Regex("""[\\/:*?"<>|]"""), "_").ifBlank { "全部" }
}
