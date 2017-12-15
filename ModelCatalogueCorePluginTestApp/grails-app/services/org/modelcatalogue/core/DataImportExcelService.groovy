package org.modelcatalogue.core

import org.modelcatalogue.core.dataimport.excel.ExcelLoader
import org.modelcatalogue.core.dataimport.excel.HeadersMap
import org.modelcatalogue.core.util.builder.DefaultCatalogueBuilder

class DataImportExcelService extends AbstractDataImportService {

    @Override
    String getContentType() {
        'application/vnd.ms-excel'
    }

    @Override
    String getExecuteBackgroundMessage() {
        'Imported from Excel'
    }

    @Override
    void loadInputStream(DefaultCatalogueBuilder defaultCatalogueBuilder, InputStream inputStream, String name) {
        ExcelLoader parser = new ExcelLoader()
        parser.buildModelFromStandardWorkbookSheet(HeadersMap.createForStandardExcelLoader(), inputStream, )
    }

}
