package org.modelcatalogue.core.dataimport.excel

import grails.util.Holders
import groovy.util.logging.Log
import org.apache.poi.ss.usermodel.*
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.springframework.context.ApplicationContext

/**
 * This used to be a class for one purpose ("importData", now called "buildXmlFromStandardWorkbookSheet"), but now we have made it a parent class of
 * future NT Excel Loaders, so that they can access similar methods.
 * This may not be the best way
 */
@Log
class ConfigExcelLoader extends ExcelLoader {
    String dataModelName
    Map<String, String> headersMap

    ConfigExcelLoader(String dataModelName, InputStream xmlInput) {
        this.dataModelName = dataModelName
        this.headersMap = this.parseXml(xmlInput)
    }

    /**
     * This parses an XML file that contains a headersMap (maps logical to physical column names)
     * It also assigns the result to a class variable which is used as a default map if none is passed
     * @param xmlInput
     * @return the parsed XML file as a headers map, or null if XML file was not a headersMap
     */
    Map<String, String> parseXml(groovy.util.slurpersupport.GPathResult xml) {
        if (xml.name() == 'headersMap') {
            Map<String, String> hdrMap = [:]
            List<String> metadataKeys = []
            for (groovy.util.slurpersupport.Node n in xml.childNodes()) {
                if (n.name == 'metadata') {
                    metadataKeys += n.text()
                } else if (n.text()) {
                    hdrMap[n.name()] = n.text()
                }
            }
            hdrMap['metadata'] = metadataKeys
            return this.headersMap = hdrMap
        } else {
            return null
        }
    }
    /**
     * This parses an XML file that contains a headersMap (maps logical to physical column names)
     * It also assigns the result to a class variable which is used as a default map if none is passed
     * @param xmlInput
     * @return the parsed XML file as a headers map, or null if XML file was not a headersMap
     */
    Map<String, String> parseXml(InputStream xmlInput) {
        return parseXml(new XmlSlurper().parse(xmlInput))
    }

    /**
     * This parses an XML file that contains a headersMap (maps logical to physical column names)
     * It also assigns the result to a class variable which is used as a default map if none is passed
     * @param xmlReader
     * @return the parsed XML file as a headers map, or null if XML file was not a headersMap
     */
    Map<String, String> parseXml(Reader xmlReader) {
        return parseXml(new XmlSlurper().parse(xmlReader))
    }

    static protected Map<String, String> createRowMap(Row row, List<String> headers) {
        Map<String, String> rowMap = new LinkedHashMap<>()
        // Important that it's LinkedHashMap, for order to be kept, to get the last section which is metadata!
        for (Cell cell : row) {
            rowMap = updateRowMap(rowMap, cell, headers)
        }
        return rowMap
    }
    ApplicationContext context = Holders.getApplicationContext()
    SessionFactory sessionFactory = (SessionFactory) context.getBean('sessionFactory')
    Session session = sessionFactory.getCurrentSession()
    def propertyInstanceMap = org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin.PROPERTY_INSTANCE_MAP

    /**
     * getCatalogueElementDtoFromRow
     * @param Cell cell
     * @param List rowData
     * @return CatalogueElementDto
     */
    static protected Map<String, String> updateRowMap(Map<String,String> rowMap, Cell cell,  List<String> headers) {
        def colIndex = cell.getColumnIndex()
        rowMap[headers[colIndex]] = valueHelper(cell)
        rowMap
    }

    static List<String> getRowData(Row row) {
        def data = []
        for (Cell cell : row) {
            getValue(cell, data)
        }
        data
    }

    List<Map<String, String>> getRowMaps(Sheet sheet, headersMap) {
        Iterator<Row> rowIt = sheet.rowIterator()
        Row row = rowIt.next()
        List<String> headers = getRowData(row)
        log.info("Headers are ${headers as String}")
        List<Map<String, String>> rowMaps = []
        while (rowIt.hasNext()) {
            row = rowIt.next()
            Map<String, String> rowMap = createRowMap(row, headers)
            rowMaps << rowMap
        }
        return rowMaps
    }

    static boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c)
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK)
                return false
        }
        return true
    }

    static void getValue(Cell cell, List<String> data) {
        def colIndex = cell.getColumnIndex()
        data[colIndex] = valueHelper(cell)
    }

    static String valueHelper(Cell cell){
        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_STRING:
                return cell.getRichStringCellValue().getString().trim()
            case Cell.CELL_TYPE_NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue()
                }
                return cell.getNumericCellValue()
            case Cell.CELL_TYPE_BOOLEAN:
                return cell.getBooleanCellValue()
            case Cell.CELL_TYPE_FORMULA:
                return cell.getCellFormula()
        }
        return ""
    }

    /**
     * This thing with headersMap is done in a particular way to generically handle a few excel formats;
     * regardless of the order of the headers.. and handle legacy "Classifications/Models" instead of "Data Models/Data Classes";
     * in future we will prefer to use a list of headers which exactly matches the headers in the file;
     * The headersMap maps internal header names to actual header names used in the spreadsheet. There is a default setting.
     * @param headersMap
     * @param workbook
     */
    List<Map<String,String>> buildRowMaps(Workbook workbook){
        // use default headersMap if headersMap is null
        // headersMap maps internal names of headers to what are hopefully the headers used in the actual spreadsheet.
        if (headersMap == null) {
            headersMap = this.headersMap
        }
        log.info("Using headersMap ${headersMap as String}")
        if (!workbook) {
            throw new IllegalArgumentException("Excel file contains no worksheet!")
        }

        Sheet sheet = workbook.getSheetAt(0)
        getRowMaps(sheet, headersMap)
    }

    String outOfBoundsWith(Closure<String> c, String s = '') {
        try {
            return c()
        } catch (ArrayIndexOutOfBoundsException a) {
            return s
        }
    }
}
