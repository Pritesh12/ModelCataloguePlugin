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
            dataElementGormService.save(de)

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
            dc = dataClassGormService.findByNameAndDataModel(className, dataModel)
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
        if (dcCode && (dc = dataClassGormService.findByModelCatalogueIdAndDataModel(className, dataModel))) {
            if (className != dc.getName()) { // yes, check if the name has changed
                dc.setName(className)
                dataClassGormService.save(dc)
            }
        } else {
            // see if there is a data class with this name - if so make sure you get the right version i.e. highest version number
            // it will be the latest one - only one of the same name per class and the data model is version specific
            dc = dataClassGormService.findByNameAndDataModel(className, dataModel)
        }
        if (dc) { // we found a DC, just need to check the description
            if (dcDescription != dc.getDescription()) {
                dc.setDescription(dcDescription)
                dataClassGormService.save(dc)
            }
        } else { // need to create one - this time with all the parameters
            // the data class doesn't already exist in the model so create it
            Map<String, Object> params = paramsAddCodeNameDesc([dataModel: dataModel], dcCode, className, dcDescription)
            dc = dataClassGormService.save(params)
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

    /**
     *
     * @param dataModel
     * @param headersMap
     * @param rowMap
     * @return
     */
    MeasurementUnit processMeasurementUnit(DataModel dataModel, Map<String, Object> headersMap, Map<String, String> rowMap) {
        //import the measurement unit for the data type (to be used in the creation of data type if applicable)
        String muCatId = tryHeader(ConfigHeadersMap.measurementUnitCode, headersMap, rowMap)
        String muSymbol = tryHeader(ConfigHeadersMap.measurementUnitSymbol, headersMap, rowMap)
        String muName = tryHeader(ConfigHeadersMap.measurementUnitName, headersMap, rowMap) ?: (muSymbol ?: (muCatId ?: DEFAULT_MU_NAME))

        MeasurementUnit mu

        if (muName == DEFAULT_MU_NAME) { // there is no measurement unit
            return null
        }

        if (muCatId) {
            mu = measurementUnitGormService.findByModelCatalogueIdAndDataModel(muCatId, dataModel)
        } else if (muName) { //see if a datatype with this name already exists in this model
            mu = measurementUnitGormService.findByNameAndDataModel(muName, dataModel)
        } else if (muSymbol) {
            mu = measurementUnitGormService.findBySymbolAndDataModel(muSymbol, dataModel)
        }
        // all this to test
        //if no mu then create one
        if (!mu) {
            mu = new MeasurementUnit()
            mu.setDataModel(dataModel)
            mu.setName(muName)
            if (muCatId) {
                mu.setModelCatalogueId(muCatId)
            }
            if (muSymbol) {
                mu.setSymbol(muSymbol)
            }
            measurementUnitGormService.save(mu)
        } else {
            Map<String, String> params = update('modelCatalogueId', mu.getModelCatalogueId(), muCatId)
            params = update('name', mu.getName(), muName, params)
            params = update('symbol', mu.getSymbol(), muSymbol, params)
            if (params) { // will be null if no updates
                mu.save(params)
            }
        }
        return mu
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
        String dtCode = tryHeader(ConfigHeadersMap.dataTypeCode, headersMap, rowMap)
        String dtName = tryHeader(ConfigHeadersMap.dataTypeName, headersMap, rowMap)
        DataType dt

        //see if a datatype with the model catalogue id already exists in this model

        if (dtCode && (dt = dataTypeGormService.findByModelCatalogueIdAndDataModel(dtCode, dataModel))) {
            if ((dtName ?: '') != dt.getName()) {
                dt.setName(dtName)
                dataTypeGormService.save(dt)
            }
        } else if (dtName && (dt = dataTypeGormService.findByNameAndDataModel(dtName, dataModel))) {
            //see if a datatype with this name already exists in this model
            if (dtCode != dt.getModelCatalogueId()) {
                dt = null
                // create a new datatype further on - it is unlikely to have datatypes with the same name but different catalogue ids
//                dt.setModelCatalogueId(dtCode)
//                updated = true
            }
        }
        //if no dt then create one
        if ( !dt ) {
            if (mu) {
                Map<String, Object> params = paramsAddCodeNameDesc([dataModel: dataModel, measurementUnit: mu], dtCode, dtName)
                dt = primitiveTypeGormService.save(params)
            } else {
                Map<String, Object> params = paramsAddCodeNameDesc([dataModel: dataModel], dtCode, dtName)
                dt = dataTypeGormService.save(params)
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
        DataModel dataModel = dataModelGormService.findByName(dataModelName)

        if ( dataModel ) {
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
        dataModelGormService.saveWithName(dataModelName)
    }

    /**
     * flushes a batch of updates and releases memory
     */
    void cleanGORM() {
        try {
            session.flush()
        } catch (Exception e) {
            log.error(session)
            log.error(" error: " + e.message)
            throw e
        }
        session.clear()
        propertyInstanceMap.get().clear()
        log.info("cleaned up GORM")
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
        String deCode = tryHeader(ConfigHeadersMap.dataElementCode, headersMap, rowMap)
        String deName = tryHeader(ConfigHeadersMap.dataElementName, headersMap, rowMap)
        String deDescription = tryHeader(ConfigHeadersMap.dataElementDescription, headersMap, rowMap)
        //see if a data element exists with this model catalogue id
        DataElement de

        if (deCode || deName) {
            de = dataElementGormService.findByModelCatalogueIdAndDataModel(deCode, dataModel)

            if (deCode && de) {
                String oldDeName = de.getName()
                if (deName != oldDeName) {
                    de.setName(deName)
                    updated = true
                }

            } else if (deName) {
                // TODO this throws a runtime exception
                de = null // dataElementGormService.findByNameAndDataModel(deName, dataModel)
                if ( de ) {  //if not see if a data element exists in this model with the same name
                    String oldDeCatId = de.getModelCatalogueId()
                    if (deCode != oldDeCatId) { // have a new DE - will not happen if no code (cat id)
                        de = newDataElement(dataModel, dt, rowMap, metadataKeys, deCode, deName, deDescription)
                        updated = true
                    }
                }
            }
        }

        if (de) {
            DataType oldDeDataType = de.getDataType()
            String oldDeDescription = de.getDescription()
            if (deDescription != oldDeDescription) {
                de.setDescription(deDescription)
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
            de = newDataElement(dataModel, dt, rowMap, metadataKeys, deCode, deName, deDescription)
            updated = true
        }
        updated ? dataElementGormService.save(de) : de
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
        DataElement de = dataElementGormService.save(params)
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
            if (params == null)
                params = [key: newValue]
            else
                params[key] = newValue
        }
        return params
    }
}
