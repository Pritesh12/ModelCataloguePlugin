package org.modelcatalogue.core

import grails.plugin.springsecurity.SpringSecurityUtils
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.modelcatalogue.core.dataimport.excel.ConfigExcelLoader
import org.modelcatalogue.core.dataimport.excel.ExcelLoader
import org.modelcatalogue.core.dataimport.excel.HeadersMap
import org.modelcatalogue.core.persistence.DataModelGormService
import org.modelcatalogue.core.security.MetadataRolesUtils
import org.modelcatalogue.core.util.builder.BuildProgressMonitor
import org.modelcatalogue.core.util.builder.DefaultCatalogueBuilder
import org.springframework.web.multipart.MultipartFile

class LoinicImportService extends AbstractDataImportService {

    DataModelGormService dataModelGormService
    DataImportRowMapsService dataImportRowMapsService

    @Override
    String getContentType() {
        'application/vnd.ms-excel'

    }

    @Override
    String getExecuteBackgroundMessage() {
        'Imported from Excel'
    }

    Long importFile(GrailsParameterMap params, MultipartFile file, MultipartFile xmlConfigFile) {
        InputStream xmlConfigStream = xmlConfigFile.inputStream

        boolean isAdmin = SpringSecurityUtils.ifAnyGranted(MetadataRolesUtils.getRolesFromAuthority('ADMIN').join(','))
        DefaultCatalogueBuilder defaultCatalogueBuilder = new DefaultCatalogueBuilder(dataModelService, elementService, isAdmin)
        Asset asset = assetService.storeAsset(params, file, contentType)
        final Long assetId = asset.id
        defaultCatalogueBuilder.monitor = BuildProgressMonitor.create("Importing $file.originalFilename", assetId)
        InputStream inputStream = file.inputStream
        String name = params.modelName
        Long userId = springSecurityService.principal?.id

        executorService.submit {
            DataModel.withTransaction {

                try {
                    Workbook wb = WorkbookFactory.create(inputStream)
                    ConfigExcelLoader loader = new ConfigExcelLoader(name, xmlConfigStream)
                    List<Map<String,String>> rowMaps = loader.buildRowMaps(wb)
                    DataModel dataModel = dataImportRowMapsService.processRowMaps(rowMaps, loader.headersMap, name)
                    assetGormService.finalizeAsset(assetId, dataModel, userId)

                } catch (Exception e) {
                    logError(assetId, e)
                }

            }
        }

        assetId
    }

    @Override
    void loadInputStream(DefaultCatalogueBuilder defaultCatalogueBuilder, InputStream inputStream, String name) {

    }
}
