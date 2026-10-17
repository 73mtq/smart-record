package com.nonen.Bookkeeping.core

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 轻量 XLSX 写入（与 parse/XlsxSupport 读取器配套，零第三方依赖）。
 * 字符串走 sharedStrings，数值走原生数字单元格；时间等以文本写入，
 * 避免 Excel 序列号/日期样式的解析歧义。
 * 支持单工作表与多工作表导出。
 */
object MiniXlsx {

    data class Sheet(val name: String, val rows: List<List<Any?>>)

    /** 写出仅含单个工作表的最小 xlsx。String → 文本单元格，Number → 数字单元格，null → 空单元格。 */
    fun write(rows: List<List<Any?>>): ByteArray =
        writeSheets(listOf(Sheet("账单", rows)))

    /** 写出包含多个工作表的标准 xlsx 文件 */
    fun writeSheets(sheets: List<Sheet>): ByteArray {
        val shared = ArrayList<String>()
        val sharedIndex = HashMap<String, Int>()
        fun sharedIdOf(value: String): Int = sharedIndex.getOrPut(value) {
            shared.add(value); shared.size - 1
        }

        val sheetXmlList = sheets.map { sheet ->
            val sheetXml = StringBuilder()
            sheetXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
            sheetXml.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
            sheet.rows.forEachIndexed { rowIndex, cells ->
                sheetXml.append("<row r=\"").append(rowIndex + 1).append("\">")
                cells.forEachIndexed { colIndex, cell ->
                    if (cell == null) return@forEachIndexed
                    val ref = colLetter(colIndex) + (rowIndex + 1)
                    when (cell) {
                        is Number -> sheetXml.append("<c r=\"").append(ref).append("\"><v>")
                            .append(cell).append("</v></c>")
                        else -> sheetXml.append("<c r=\"").append(ref).append("\" t=\"s\"><v>")
                            .append(sharedIdOf(cell.toString())).append("</v></c>")
                    }
                }
                sheetXml.append("</row>")
            }
            sheetXml.append("</sheetData></worksheet>")
            sheetXml.toString()
        }

        val sharedXml = StringBuilder()
        sharedXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        sharedXml.append(
            "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
                "count=\"${shared.size}\" uniqueCount=\"${shared.size}\">",
        )
        shared.forEach { value ->
            sharedXml.append("<si><t>").append(escape(value)).append("</t></si>")
        }
        sharedXml.append("</sst>")

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun entry(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            val contentTypesXml = buildString {
                append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
                append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
                append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
                append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
                sheets.forEachIndexed { idx, _ ->
                    append("<Override PartName=\"/xl/worksheets/sheet${idx + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
                }
                append("<Override PartName=\"/xl/sharedStrings.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml\"/>")
                append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
                append("</Types>")
            }
            entry("[Content_Types].xml", contentTypesXml)

            entry(
                "_rels/.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                    "</Relationships>",
            )

            val workbookXml = buildString {
                append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">")
                append("<sheets>")
                sheets.forEachIndexed { idx, sheet ->
                    append("<sheet name=\"${escape(sheet.name)}\" sheetId=\"${idx + 1}\" r:id=\"rId${idx + 1}\"/>")
                }
                append("</sheets></workbook>")
            }
            entry("xl/workbook.xml", workbookXml)

            val workbookRelsXml = buildString {
                append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">")
                sheets.forEachIndexed { idx, _ ->
                    append("<Relationship Id=\"rId${idx + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet${idx + 1}.xml\"/>")
                }
                append("<Relationship Id=\"rId${sheets.size + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings\" Target=\"sharedStrings.xml\"/>")
                append("<Relationship Id=\"rId${sheets.size + 2}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>")
                append("</Relationships>")
            }
            entry("xl/_rels/workbook.xml.rels", workbookRelsXml)

            entry(
                "xl/styles.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
                    "<fonts count=\"1\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>" +
                    "<fills count=\"1\"><fill><patternFill patternType=\"none\"/></fill></fills>" +
                    "<borders count=\"1\"><border/></borders>" +
                    "<cellStyleXfs count=\"1\"><xf/></cellStyleXfs>" +
                    "<cellXfs count=\"1\"><xf/></cellXfs>" +
                    "</styleSheet>",
            )
            entry("xl/sharedStrings.xml", sharedXml.toString())
            sheetXmlList.forEachIndexed { idx, xml ->
                entry("xl/worksheets/sheet${idx + 1}.xml", xml)
            }
        }
        return out.toByteArray()
    }

    /** 便捷读取：用配套的 XlsxSupport 读回首个工作表（供导入与测试复用） */
    fun read(bytes: ByteArray): List<List<String?>> =
        com.nonen.Bookkeeping.parse.XlsxSupport.readFirstSheet(bytes)

    private fun colLetter(index: Int): String {
        var i = index
        val sb = StringBuilder()
        while (i >= 0) {
            sb.insert(0, ('A' + i % 26))
            i = i / 26 - 1
        }
        return sb.toString()
    }

    private fun escape(value: String): String = buildString(value.length) {
        for (ch in value) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(ch)
            }
        }
    }
}
