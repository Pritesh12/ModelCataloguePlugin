package org.modelcatalogue.core

import grails.transaction.Transactional
import groovy.util.logging.Slf4j
import org.modelcatalogue.core.api.ElementStatus
import org.modelcatalogue.core.dataimport.excel.ConfigHeadersMap
import org.modelcatalogue.core.persistence.DataClassGormService
import org.modelcatalogue.core.persistence.DataElementGormService
import org.modelcatalogue.core.persistence.DataModelGormService
import org.modelcatalogue.core.persistence.DataTypeGormService
import org.modelcatalogue.core.persistence.MeasurementUnitGormService
import org.modelcatalogue.core.persistence.PrimitiveTypeGormService
import org.modelcatalogue.core.publishing.DraftContext
import org.modelcatalogue.core.publishing.PublishingContext

@Slf4j
class DataImportRowMapsService {
    final Integer MAX_METADATA_LEN = 2000
    final String DEFAULT_MU_NAME = null

    DataModelGormService dataModelGormService
    ElementService elementService
    DataClassGormService dataClassGormService
    DataElementGormService dataElementGormService
    DataTypeGormService dataTypeGormService
    MeasurementUnitGormService measurementUnitGormService
    PrimitiveTypeGormService primitiveTypeGormService

    /**
     *
     * @param rowMaps
     * @param headersMap
     * @param dataModelName
     * @return
     */
    @Transactional
    DataModel processRowMaps(List<Map<String, String>> rowMaps, Map<String, Object> headersMap, String dataModelName = this.dataModelName) {
        int count = 0
        DataModel dataModel = processDataModel(dataModelName)

        for (Map<String, String> rowMap in rowMaps) {
            log.info("creating row" + count)
            DataClass dc = processDataClass(dataModel, headersMap, rowMap)
            MeasurementUnit mu = processMeasurementUnit(dataModel, headersMap, rowMap)
            DataType dt = processDataType(dataModel, headersMap, rowMap, mu)
            DataElement de = processDataElement(dataModel, headersMap, rowMap, dt)
            de.addToContainedIn(dc)
            saveDataElement(de)
            count++
        }
        log.info("finished importing rows#" + rowMaps.size())
        dataModel
    }

    /**
     *
     * @param dataModel
     * @param headersMap
     * @param rowMap
     * @return
     */
    DataClass processDataClass(DataModel dataModel, Map<String, Object> headersMap, Map<String, String> rowMap) {
        String regEx = headersMap['classSeparator'] ?: "\\."
        //take the class name and split to see if there is a hierarchy
        String dcCode = tryHeader(ConfigHeadersMap.containingDataClassCode, headersMap, rowMap)
        String dcNames = tryHeader(ConfigHeadersMap.containingDataClassName, headersMap, rowMap)
        String dcDescription = tryHeader(ConfigHeadersMap.containingDataClassDescription, headersMap, rowMap)
        String[] dcNameList = dcNames.split(regEx)
        Integer maxDcNameIx = dcNameList.length - 1
        Integer dcNameIx = 0
        DataClass dc, parentDC
        String className = dcNameList[dcNameIx]

        //if "class" separated by . (regEx) create class hierarchy if applicable,
        //if not then populate the parent data class with the appropriate data element
        while (dcNameIx < maxDcNameIx) { // we are just checking names at this point (above the leaf)
            dc = findDataClassByfindByNameAndDataModel(className, dataModel)
            if (!dc) {
                dc = dataClassGormService.saveWithNameAndDataModel(className, dataModel)
                // any cat id or description will not apply here
            }
            // maybe check if the parent link is already in the incomingRelationships before calling addToChildOf?
            if (parentDC) {
                addAsChildTo(dc, parentDC)
            }

            //last one will be the one that contains the data element
            parentDC = dc
            className = dcNameList[++dcNameIx]
        }
        // now we are processing the actual (leaf) class, so need to check if there is a model catalogue id (dcCode)
        if (dcCode && (dc = findDataClassByModelCatalogueIdAndDataModel(className, dataModel))) {
            if (className != dc.getName()) { // yes, check if the name has changed
                dc.setName(className)
                saveDataClass(dc)

            }
        } else {
            // see if there is a data class with this name - if so make sure you get the right version i.e. highest version number
            // it will be the latest one - only one of the same name per class and the data model is version specific
            dc = findDataClassByfindByNameAndDataModel(className, dataModel)
        }
        if (dc) { // we found a DC, just need to check the description
            if (dcDescription != dc.getDescription()) {
                dc.setDescription(dcDescription)
                saveDataClass(dc)
            }
        } else { // need to create one - this time with all the parameters
            // the data class doesn't already exist in the model so create it
            Map<String, Object> params = paramsAddCodeNameDesc([dataModel: dataModel], dcCode, className, dcDescription)
            DataClass dataClassInstance = new DataClass(params)
            dc = saveDataClass(dataClassInstance)
        }
        // maybe check if the parent link is already in the incomingRelationships before calling addToChildOf?
        if (parentDC) {
            addAsChildTo(dc, parentDC)
        }

        dc
    }

    void addAsChildTo(CatalogueElement child, CatalogueElement parent) {
        Set<Relationship> incoming = child.getIncomingRelationships()
        for (Relationship rel in incoming) {
            if (rel.getSource() == parent && rel.getDestination() == child && rel.getRelationshipType().getId() == RelationshipType.hierarchyType.getId()) {
                return // is already a child - will usually be the case
            }
        }
        child.addToChildOf(parent)
    }


    MeasurementUnitProperties measurementUnitProperties(Map<String, Object> headersMap, Map<String, String> rowMap) {
        //import the measurement unit for the data type (to be used in the creation of data type if applicable)
        String muCatId = tryHeader(ConfigHeadersMap.measurementUnitCode, headersMap, rowMap)
        String muSymbol = tryHeader(ConfigHeadersMap.measurementUnitSymbol, headersMap, rowMap)
        String muName = tryHeader(ConfigHeadersMap.measurementUnitName, headersMap, rowMap) ?: (muSymbol ?: (muCatId ?: DEFAULT_MU_NAME))
        new MeasurementUnitProperties(name: muName, symbol: muSymbol, modelCatalogueId: muCatId)
    }

    MeasurementUnit findMeasurementUnit(MeasurementUnitProperties muProps, DataModel dataModel) {
        if (muProps.modelCatalogueId) {
            return findMeasurementUnitByModelCatalogueIdAndDataModel(muProps.modelCatalogueId, dataModel)

        } else if (muProps.name) { //see if a datatype with this name already exists in this model
            return findMeasurementUnitByNameAndDataModel(muProps.name, dataModel)

        } else if (muProps.symbol) {
            return findMeasurementUnitBySymbolAndDataModel(muProps.symbol, dataModel)
        }
        null
    }

    void populateMeasurementUnit(MeasurementUnit mu, MeasurementUnitProperties muProps) {
        mu.name = (mu.name != muProps.name) ? muProps.name : mu.name
        mu.symbol = (mu.symbol != muProps.symbol) ? muProps.symbol : mu.symbol
        mu.modelCatalogueId = (mu.modelCatalogueId != muProps.modelCatalogueId) ? muProps.modelCatalogueId : mu.modelCatalogueId
    }

    MeasurementUnit processMeasurementUnit(DataModel dataModel, Map<String, Object> headersMap, Map<String, String> rowMap) {
        MeasurementUnitProperties muProps = measurementUnitProperties(headersMap, rowMap)
        if (muProps.name == DEFAULT_MU_NAME) { // there is no measurement unit
            return null
        }
        MeasurementUnit mu = findMeasurementUnit(muProps, dataModel)
        if (!mu) {
            mu = new MeasurementUnit()
            mu.setDataModel(dataModel)
        }
        populateMeasurementUnit(mu, muProps)
        saveMeasurementUnit(mu)
        mu
    }

    DataTypeProperties dataTypeProperties(Map<String, Object> headersMap, Map<String, String> rowMap) {
        String dtCode = tryHeader(ConfigHeadersMap.dataTypeCode, headersMap, rowMap)
        String dtName = tryHeader(ConfigHeadersMap.dataTypeName, headersMap, rowMap)
        new DataTypeProperties(modelCatalogueId: dtCode, name: dtName)
    }

    /**
     *
     * @param dataModel
     * @param headersMap
     * @param rowMap
     * @param mu
     * @return
     */
    DataType processDataType(DataModel dataModel, Map<String, Object> headersMap, Map<String, String> rowMap, MeasurementUnit mu) {
        DataTypeProperties dataTypeProperties = dataTypeProperties(headersMap, rowMap)

        DataType dt

        //see if a datatype with the model catalogue id already exists in this model

        if (dataTypeProperties.modelCatalogueId && (dt = findDataTypeByModelCatalogueIdAndDataModel(dataTypeProperties.modelCatalogueId, dataModel))) {
            if ((dataTypeProperties.name ?: '') != dt.getName()) {
                dt.setName(dataTypeProperties.name)
                saveDataType(dt)
            }
        } else if (dataTypeProperties.name && (dt = findDataTypeByNameAndDataModel(dataTypeProperties.name, dataModel))) {
            //see if a datatype with this name already exists in this model
            if (dataTypeProperties.modelCatalogueId != dt.getModelCatalogueId()) {
                dt = null
                // create a new datatype further on - it is unlikely to have datatypes with the same name but different catalogue ids
//                dt.setModelCatalogueId(dataTypeProperties.modelCatalogueId)
//                updated = true
            }
        }
        //if no dt then create one
        if (!dt) {
            if (mu) {
                Map<String, Object> params = paramsAddCodeNameDesc([dataModel: dataModel, measurementUnit: mu], dataTypeProperties.modelCatalogueId, dataTypeProperties.name)
                PrimitiveType primitiveTypeInstance = new PrimitiveType(params)
                dt = savePrimitiveType(primitiveTypeInstance)
            } else {
                Map<String, Object> params = paramsAddCodeNameDesc([dataModel: dataModel], dataTypeProperties.modelCatalogueId, dataTypeProperties.name)
                DataType dataTypeInstance = new DataType(params)
                dt = saveDataType(dataTypeInstance)
            }
        }
        dt
    }

    /**
     *
     * @param dataModelName
     * @return
     */
    @Transactional
    DataModel processDataModel(String dataModelName) {
        //see if an open EHR model already exists, if not create one
        //could consider changing this - if there are multiple versions - should make sure we use the latest one.
        DataModel dataModel = findDataModelByName(dataModelName)

        if (dataModel) {
            log.info("Found Data Model: ${dataModelName}")
            // if one exists, check to see if it's a draft
            // but if it's finalised create a new version
            if (dataModel.status != ElementStatus.DRAFT) {
                DraftContext context = DraftContext.userFriendly()
                return elementService.createDraftVersion(dataModel, PublishingContext.nextPatchVersion(dataModel.semanticVersion), context)
            }
            return dataModel
        }

        log.info("Creating new DataModel: ${dataModelName}")
        saveDataModel(dataModelName)
    }

    DataElementProperties dataElementProperties(Map<String, Object> headersMap, Map<String, String> rowMap) {
        String deCode = tryHeader(ConfigHeadersMap.dataElementCode, headersMap, rowMap)
        String deName = tryHeader(ConfigHeadersMap.dataElementName, headersMap, rowMap)
        String deDescription = tryHeader(ConfigHeadersMap.dataElementDescription, headersMap, rowMap)
        new DataElementProperties(modelCatalogueId: deCode, name: deName, description: deDescription)
    }

    /**
     *
     * @param dataModel
     * @param headersMap
     * @param rowMap
     * @param dt
     * @return
     */
    DataElement processDataElement(DataModel dataModel, Map<String, Object> headersMap, Map<String, String> rowMap, DataType dt) {
        Boolean updated = false
        List<String> metadataKeys = headersMap['metadata']
        DataElementProperties dataTypeProperties = dataElementProperties(headersMap, rowMap)

        //see if a data element exists with this model catalogue id
        DataElement de

        if (dataTypeProperties.modelCatalogueId || dataTypeProperties.name) {
            de = findDataElementByModelCatalogueIdAndDataModel(dataTypeProperties.modelCatalogueId, dataModel)

            if (dataTypeProperties.modelCatalogueId && de) {
                String oldDeName = de.getName()
                if (dataTypeProperties.name != oldDeName) {
                    de.setName(deName)
                    updated = true
                }

            } else if (dataTypeProperties.name) {
                // TODO this throws a runtime exception
                de = null // dataElementGormService.findByNameAndDataModel(dataElementProperties.name, dataModel)
                if (de) {  //if not see if a data element exists in this model with the same name
                    String oldDeCatId = de.getModelCatalogueId()
                    if (dataTypeProperties.modelCatalogueId != oldDeCatId) {
                        // have a new DE - will not happen if no code (cat id)
                        de = newDataElement(dataModel, dt, rowMap, metadataKeys, dataTypeProperties.modelCatalogueId, dataTypeProperties.name, dataTypeProperties.description)
                        updated = true
                    }
                }
            }
        }

        if (de) {
            DataType oldDeDataType = de.getDataType()
            String oldDeDescription = de.getDescription()
            if (dataTypeProperties.description != oldDeDescription) {
                de.setDescription(dataTypeProperties.description)
                updated = true
            }
            if (dt != oldDeDataType) {
                de.setDataType(dt)
                updated = true
            }
            if (updateMetadata(de, headersMap, rowMap, metadataKeys)) {
                updated = true
            }
        } else { //if no de then create one
            de = newDataElement(dataModel, dt, rowMap, metadataKeys, dataTypeProperties.modelCatalogueId, dataTypeProperties.name, dataTypeProperties.description)
            updated = true
        }
        updated ? saveDataElement(de) : de
    }

/**
 *
 * @param dataModel
 * @param dt
 * @param rowMap
 * @param metadataKeys
 * @param deCode
 * @param deName
 * @param deDescription
 * @return
 */
    DataElement newDataElement(DataModel dataModel, DataType dt, Map<String, String> rowMap, List<String> metadataKeys, String deCode, String deName, String deDescription) {
        Map<String, Object> params = paramsAddCodeNameDesc([dataModel: dataModel, dataType: dt], deCode, deName, deDescription)
        DataElement dataElementInstance = new DataElement(params)
        DataElement de = saveDataElement(dataElementInstance)
        addMetadata(de, rowMap, metadataKeys)
        de
    }

    /**
     *
     * @param de
     * @param rowMap
     * @param metadataKeys
     * @return
     */
    Boolean addMetadata(DataElement de, Map<String, String> rowMap, List<String> metadataKeys) {
        Boolean updated = false
        for (String key in metadataKeys) {
            String keyValue = rowMap.get(key)
            if (keyValue) {
                de.ext.put(key, keyValue.take(MAX_METADATA_LEN).toString())
                updated = true
            }
        }
        updated
    }

    /**
     *
     * @param de
     * @param headersMap
     * @param rowMap
     * @param metadataKeys
     * @return
     */
    Boolean updateMetadata(DataElement de, Map<String, Object> headersMap, Map<String, String> rowMap, List<String> metadataKeys) {
        Boolean updated = false
        if (de.ext.isEmpty()) { // no existing metadata, so just insert the new metadata
            updated = addMetadata(de, rowMap, metadataKeys)
        } else { // we need to update it (possible inserts, edits & deletes)
            for (String newKey in metadataKeys) { // first go through the new row
                String newValue = rowMap.get(newKey)?.take(MAX_METADATA_LEN)
                String oldValue = de.ext.get(newKey)
                if (oldValue != newValue) {
                    de.ext.put(newKey, newValue) // inserts or updates
                    updated = true
                }
            }
            for (oldKey in de.ext.keySet()) {
                if (!rowMap.get(oldKey)) {
                    de.ext.remove(oldKey)
                    updated = true
                }
            }
        }
        updated
    }

    String tryHeader(String internalHeaderName, Map<String, Object> headersMap, Map<String, String> rowMap) {
        // headersMap maps internal names of headers to what are hopefully the headers used in the actual spreadsheet.
        String entry = rowMap.get(headersMap.get(internalHeaderName))
        if (entry) {
            return entry
        } else {
            /*log.info("Trying to use internalHeaderName '$internalHeaderName', which headersMap corresponds to " +
                "header ${headersMap.get(internalHeaderName)}, from rowMap ${rowMap as String}, nothing found.")*/
            return null
        }
    }

    /**
     *
     * @param params
     * @param code
     * @param name
     * @param symbol
     * @return
     */
    Map<String, Object> paramsAddCodeNameDesc(Map<String, Object> params, String code, String name, String desc = null) {
        if (code) {
            params['modelCatalogueId'] = code
        }
        if (name) {
            params['name'] = name
        }
        if (desc) {
            params['description'] = desc
        }
        params
    }

    /**
     *
     * @param key
     * @param oldValue
     * @param newValue
     * @param params
     * @return
     */
    Map<String, String> update(String key, String oldValue, String newValue, Map<String, String> params = null) {
        if (oldValue != newValue) {
            if (params == null) {
                params = [key: newValue]
            } else {
                params[key] = newValue
            }
        }
        params
    }


    DataClass saveDataClass(DataClass dc) {
        dataClassGormService.save(dc)
    }

    MeasurementUnit saveMeasurementUnit(MeasurementUnit measurementUnit) {
        measurementUnitGormService.save(measurementUnit)
    }

    PrimitiveType savePrimitiveType(PrimitiveType primitiveTypeInstance) {
        primitiveTypeGormService.save(primitiveTypeInstance)
    }

    DataType saveDataType(DataType dataTypeInstance) {
        dataTypeGormService.save(dataTypeInstance)
    }

    DataModel saveDataModel(String dataModelName) {
        dataModelGormService.saveWithName(dataModelName)
    }

    DataModel findDataModelByName(String dataModelName) {
        dataModelGormService.findByName(dataModelName)
    }

    MeasurementUnit findMeasurementUnitByModelCatalogueIdAndDataModel(String muCataId, DataModel dataModel) {
        measurementUnitGormService.findByModelCatalogueIdAndDataModel(muCatId, dataModel)
    }

    MeasurementUnit findMeasurementUnitByNameAndDataModel(String muName, DataModel dataModel) {
        measurementUnitGormService.findByNameAndDataModel(muName, dataModel)
    }

    MeasurementUnit findMeasurementUnitBySymbolAndDataModel(String muSymbol, DataModel dataModel) {
        measurementUnitGormService.findBySymbolAndDataModel(muSymbol, dataModel)
    }

    DataClass findDataClassByfindByNameAndDataModel(String className, DataModel dataModel) {
        dataClassGormService.findByNameAndDataModel(className, dataModel)
    }

    DataClass findDataClassByModelCatalogueIdAndDataModel(String className, DataModel dataModel) {
        dataClassGormService.findByModelCatalogueIdAndDataModel(className, dataModel)
    }

    DataType findDataTypeByModelCatalogueIdAndDataModel(String dtCode, DataModel dataModel) {
        dataTypeGormService.findByModelCatalogueIdAndDataModel(dtCode, dataModel)
    }

    DataType findDataTypeByNameAndDataModel(String dtName, DataModel dataModel) {
        dataTypeGormService.findByNameAndDataModel(dtName, dataModel)
    }

    DataElement findDataElementByModelCatalogueIdAndDataModel(String modelCatalogueId, DataModel dataModel) {
        dataElementGormService.findByModelCatalogueIdAndDataModel(modelCatalogueId, dataModel)
    }

    DataElement saveDataElement(DataElement dataElement) {
        dataElementGormService.save(dataElement)
    }
}

class DataTypeProperties {
    String name
    String modelCatalogueId
}

class DataElementProperties {
    String name
    String modelCatalogueId
    String description
}

class MeasurementUnitProperties {
    String name
    String symbol
    String modelCatalogueId
}
