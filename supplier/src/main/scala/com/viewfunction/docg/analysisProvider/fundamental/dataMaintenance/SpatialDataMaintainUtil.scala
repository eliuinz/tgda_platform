package com.github.tgda.supplier.fundamental.dataMaintenance

import com.github.tgda.supplier.exception.AnalysisProviderRuntimeException
import com.github.tgda.supplier.fundamental.dataMaintenance
import query.analysis.com.github.tgda.engine.core.QueryParameters
import com.github.tgda.coreRealm.realmServiceCore.term.{AttributeDataType, AttributeKind}
import termImpl.neo4j.spi.term.com.github.tgda.engine.core.Neo4JAttributeKindImpl
import com.github.tgda.compute.applicationCapacity.compute.exception.{DataSliceExistException, DataSlicePropertiesStructureException}
import com.github.tgda.supplier.fundamental.spatial.GeospatialScaleLevel.{CountryLevel, GeospatialScaleLevel, GlobalLevel, LocalLevel}
import com.github.tgda.supplier.fundamental.spatial.SpatialAnalysisConstant
import term.com.github.tgda.engine.core.Geospatial.GeospatialScaleGrade
import util.com.github.tgda.engine.core.RealmConstant
import com.github.tgda.compute.applicationCapacity.compute.dataComputeUnit.dataService.{DataServiceInvoker, DataSlice, DataSlicePropertyType}
import util.dataComputeUnit.dataCompute.applicationCapacity.dataCompute.com.github.tgda.CoreRealmOperationUtil
import org.geotools.data.shapefile.ShapefileDataStore
import org.geotools.data.simple.{SimpleFeatureCollection, SimpleFeatureIterator, SimpleFeatureSource}
import org.geotools.data.{FileDataStore, FileDataStoreFinder}
import org.geotools.referencing.CRS
import org.opengis.feature.GeometryAttribute
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}

import java.io.File
import java.nio.charset.Charset
import java.util

class SpatialDataMaintainUtil {

  @throws(classOf[AnalysisProviderRuntimeException])
  def syncGeospatialConceptionKindToDataSlice(dataServiceInvoker:DataServiceInvoker, conceptionKindName: String, dataSliceName: String, dataSliceGroup: String,
                                              conceptionEntityPropertyMap: util.HashMap[String, DataSlicePropertyType],geospatialScaleLevel:GeospatialScaleLevel):DataSlice={
    val targetDataSlice = dataServiceInvoker.getDataSlice(dataSliceName)
    if(targetDataSlice != null){
      throw new AnalysisProviderRuntimeException("DataSlice with name "+dataSliceName +" already exist.")
    }

    val dataSlicePropertyMap: util.HashMap[String, DataSlicePropertyType] = new util.HashMap[String, DataSlicePropertyType]()
    dataSlicePropertyMap.put(RealmConstant._GeospatialGeometryType,DataSlicePropertyType.STRING)

    if(conceptionEntityPropertyMap != null){
      dataSlicePropertyMap.putAll(conceptionEntityPropertyMap)
    }

    geospatialScaleLevel match {
      case GlobalLevel =>
        dataSlicePropertyMap.put(RealmConstant._GeospatialGLGeometryContent,DataSlicePropertyType.STRING);
        dataSlicePropertyMap.put(RealmConstant._GeospatialGlobalCRSAID,DataSlicePropertyType.STRING);
      case CountryLevel =>
        dataSlicePropertyMap.put(RealmConstant._GeospatialCLGeometryContent,DataSlicePropertyType.STRING);
        dataSlicePropertyMap.put(RealmConstant._GeospatialCountryCRSAID,DataSlicePropertyType.STRING);
      case LocalLevel =>
        dataSlicePropertyMap.put(RealmConstant._GeospatialLLGeometryContent,DataSlicePropertyType.STRING);
        dataSlicePropertyMap.put(RealmConstant._GeospatialLocalCRSAID,DataSlicePropertyType.STRING);
    }

    //val dataSliceOperationResult =
    CoreRealmOperationUtil.syncConceptionKindToDataSlice(conceptionKindName,dataSliceName,dataSliceGroup,dataSlicePropertyMap,null)
    dataServiceInvoker.getDataSlice(dataSliceName)
  }

  @throws(classOf[AnalysisProviderRuntimeException])
  def syncGeospatialToDataSlice(dataServiceInvoker: DataServiceInvoker): Unit = {
    val dataSliceGroup = SpatialAnalysisConstant.GeospatialScaleDataSliceSystemGroup
    val existDataSlices = dataServiceInvoker.listDataSlices()
    if(existDataSlices.contains(SpatialAnalysisConstant.GeospatialScaleContinentDataSlice)){
      throw new AnalysisProviderRuntimeException("DataSlice with name "+SpatialAnalysisConstant.GeospatialScaleContinentDataSlice +" already exist.")
    }
    if(existDataSlices.contains(SpatialAnalysisConstant.GeospatialScaleCountryRegionDataSlice)){
      throw new AnalysisProviderRuntimeException("DataSlice with name "+SpatialAnalysisConstant.GeospatialScaleCountryRegionDataSlice +" already exist.")
    }
    if(existDataSlices.contains(SpatialAnalysisConstant.GeospatialScaleProvinceDataSlice)){
      throw new AnalysisProviderRuntimeException("DataSlice with name "+SpatialAnalysisConstant.GeospatialScaleProvinceDataSlice +" already exist.")
    }
    if(existDataSlices.contains(SpatialAnalysisConstant.GeospatialScalePrefectureDataSlice)){
      throw new AnalysisProviderRuntimeException("DataSlice with name "+SpatialAnalysisConstant.GeospatialScalePrefectureDataSlice +" already exist.")
    }
    if(existDataSlices.contains(SpatialAnalysisConstant.GeospatialScaleCountyDataSlice)){
      throw new AnalysisProviderRuntimeException("DataSlice with name "+SpatialAnalysisConstant.GeospatialScaleCountyDataSlice +" already exist.")
    }
    if(existDataSlices.contains(SpatialAnalysisConstant.GeospatialScaleTownshipDataSlice)){
      throw new AnalysisProviderRuntimeException("DataSlice with name "+SpatialAnalysisConstant.GeospatialScaleTownshipDataSlice +" already exist.")
    }
    if(existDataSlices.contains(SpatialAnalysisConstant.GeospatialScaleVillageDataSlice)){
      throw new AnalysisProviderRuntimeException("DataSlice with name "+SpatialAnalysisConstant.GeospatialScaleVillageDataSlice +" already exist.")
    }

    val dataSyncPerLoadResultNum =  100000000
    val queryParameters = new QueryParameters
    queryParameters.setResultNumber(dataSyncPerLoadResultNum)
    val degreeOfParallelismNum = 3

    val pkList = new util.ArrayList[String]
    pkList.add(CoreRealmOperationUtil.RealmGlobalUID)
    try {
      // For Continent
      var dataSlicePropertyMap = new util.HashMap[String, DataSlicePropertyType]
      dataSlicePropertyMap.put("ISO_Code", DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant.GeospatialChineseNameProperty, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant.GeospatialEnglishNameProperty, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put("ChineseFullName", DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant.GeospatialCodeProperty, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant.GeospatialProperty, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant.GeospatialScaleGradeProperty, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(CoreRealmOperationUtil.RealmGlobalUID, DataSlicePropertyType.STRING)
      var containsAttributesKinds = buildAttributeKindList(dataSlicePropertyMap)
      CoreRealmOperationUtil.syncInnerDataKindEntitiesToDataSlice(dataServiceInvoker, RealmConstant.GeospatialScaleContinentEntityClass, dataSliceGroup,
        containsAttributesKinds, queryParameters, SpatialAnalysisConstant.GeospatialScaleContinentDataSlice, true, degreeOfParallelismNum)

      // For CountryRegion
      dataSlicePropertyMap = new util.HashMap[String, DataSlicePropertyType]
      dataSlicePropertyMap.put("Alpha_2Code", DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put("Alpha_3Code", DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put("NumericCode", DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put("ISO3166_2Code", DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant.GeospatialEnglishNameProperty, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant.GeospatialChineseNameProperty, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put("belongedContinent", DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put("capitalChineseName", DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put("capitalEnglishName", DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant.GeospatialCodeProperty, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant.GeospatialProperty, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant.GeospatialScaleGradeProperty, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant._GeospatialGeometryType, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant._GeospatialGlobalCRSAID, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant._GeospatialGLGeometryContent, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(CoreRealmOperationUtil.RealmGlobalUID, DataSlicePropertyType.STRING)
      containsAttributesKinds = buildAttributeKindList(dataSlicePropertyMap)
      CoreRealmOperationUtil.syncInnerDataKindEntitiesToDataSlice(dataServiceInvoker, RealmConstant.GeospatialScaleCountryRegionEntityClass, dataSliceGroup,
        containsAttributesKinds, queryParameters, SpatialAnalysisConstant.GeospatialScaleCountryRegionDataSlice, true, degreeOfParallelismNum)

      // For Province
      dataSlicePropertyMap = new util.HashMap[String, DataSlicePropertyType]
      dataSlicePropertyMap.put("ISO3166_1Alpha_2Code", DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put("ISO3166_2SubDivisionCode", DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put("ISO3166_2SubdivisionName", DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put("ISO3166_2SubdivisionCategory", DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant.GeospatialCodeProperty, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant.GeospatialProperty, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant.GeospatialScaleGradeProperty, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put("DivisionCategory_EN", DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put("DivisionCategory_CH", DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant.GeospatialEnglishNameProperty, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant.GeospatialChineseNameProperty, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant._GeospatialGLGeometryPOI, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant._GeospatialGlobalCRSAID, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant._GeospatialGeometryType, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant._GeospatialGLGeometryContent, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put("ChinaDivisionCode", DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant._GeospatialCLGeometryPOI, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant._GeospatialCountryCRSAID, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant._GeospatialCLGeometryContent, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(CoreRealmOperationUtil.RealmGlobalUID, DataSlicePropertyType.STRING)
      containsAttributesKinds = buildAttributeKindList(dataSlicePropertyMap)
      CoreRealmOperationUtil.syncInnerDataKindEntitiesToDataSlice(dataServiceInvoker, RealmConstant.GeospatialScaleProvinceEntityClass, dataSliceGroup,
        containsAttributesKinds, queryParameters, SpatialAnalysisConstant.GeospatialScaleProvinceDataSlice, true, degreeOfParallelismNum)

      // For Prefecture
      val targetPrefectureDataSlice = dataServiceInvoker.getDataSlice(RealmConstant.GeospatialScalePrefectureEntityClass)
      dataSlicePropertyMap = new util.HashMap[String, DataSlicePropertyType]
      dataSlicePropertyMap.put("ChinaParentDivisionCode", DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put("ChinaDivisionCode", DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put("ChinaProvinceName", DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant.GeospatialCodeProperty, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant.GeospatialProperty, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant._GeospatialGeometryType, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant._GeospatialGlobalCRSAID, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant._GeospatialGLGeometryContent, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant._GeospatialCountryCRSAID, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant._GeospatialCLGeometryContent, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant._GeospatialGLGeometryPOI, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant._GeospatialCLGeometryPOI, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant._GeospatialGLGeometryBorder, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant._GeospatialCLGeometryBorder, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant.GeospatialScaleGradeProperty, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(RealmConstant.GeospatialChineseNameProperty, DataSlicePropertyType.STRING)
      dataSlicePropertyMap.put(CoreRealmOperationUtil.RealmGlobalUID, DataSlicePropertyType.STRING)
      containsAttributesKinds = buildAttributeKindList(dataSlicePropertyMap)
      CoreRealmOperationUtil.syncInnerDataKindEntitiesToDataSlice(dataServiceInvoker, RealmConstant.GeospatialScalePrefectureEntityClass, dataSliceGroup,
        containsAttributesKinds, queryParameters, SpatialAnalysisConstant.GeospatialScalePrefectureDataSlice, true, degreeOfParallelismNum)

      // For County
      dataSlicePropertyMap.put("ChinaPrefectureName", DataSlicePropertyType.STRING)
      containsAttributesKinds = buildAttributeKindList(dataSlicePropertyMap)
      CoreRealmOperationUtil.syncInnerDataKindEntitiesToDataSlice(dataServiceInvoker, RealmConstant.GeospatialScaleCountyEntityClass, dataSliceGroup,
        containsAttributesKinds, queryParameters, SpatialAnalysisConstant.GeospatialScaleCountyDataSlice, true, degreeOfParallelismNum)

      // For Township
      dataSlicePropertyMap.put("ChinaCountyName", DataSlicePropertyType.STRING)
      containsAttributesKinds = buildAttributeKindList(dataSlicePropertyMap)
      CoreRealmOperationUtil.syncInnerDataKindEntitiesToDataSlice(dataServiceInvoker, RealmConstant.GeospatialScaleTownshipEntityClass, dataSliceGroup,
        containsAttributesKinds, queryParameters, SpatialAnalysisConstant.GeospatialScaleTownshipDataSlice, true, degreeOfParallelismNum)

      // For Village
      dataSlicePropertyMap.put("ChinaTownshipName", DataSlicePropertyType.STRING)
      containsAttributesKinds = buildAttributeKindList(dataSlicePropertyMap)
      CoreRealmOperationUtil.syncInnerDataKindEntitiesToDataSlice(dataServiceInvoker, RealmConstant.GeospatialScaleVillageEntityClass, dataSliceGroup,
        containsAttributesKinds, queryParameters, SpatialAnalysisConstant.GeospatialScaleVillageDataSlice, true, degreeOfParallelismNum)
    } catch {
      case e: DataSliceExistException =>
        e.printStackTrace()
      case e: DataSlicePropertiesStructureException =>
        e.printStackTrace()
    }
  }

  def getGeospatialDataSlice(dataServiceInvoker: DataServiceInvoker,geospatialScaleGrade:GeospatialScaleGrade): DataSlice = {
    var innerDataKindName:String = null
    geospatialScaleGrade match {
      case GeospatialScaleGrade.CONTINENT =>
        innerDataKindName = SpatialAnalysisConstant.GeospatialScaleContinentDataSlice
      case GeospatialScaleGrade.COUNTRY_REGION =>
        innerDataKindName = SpatialAnalysisConstant.GeospatialScaleCountryRegionDataSlice
      case GeospatialScaleGrade.PROVINCE =>
        innerDataKindName = SpatialAnalysisConstant.GeospatialScaleProvinceDataSlice
      case GeospatialScaleGrade.PREFECTURE =>
        innerDataKindName = SpatialAnalysisConstant.GeospatialScalePrefectureDataSlice
      case GeospatialScaleGrade.COUNTY =>
        innerDataKindName = SpatialAnalysisConstant.GeospatialScaleCountyDataSlice
      case GeospatialScaleGrade.TOWNSHIP =>
        innerDataKindName = SpatialAnalysisConstant.GeospatialScaleTownshipDataSlice
      case GeospatialScaleGrade.VILLAGE =>
        innerDataKindName = SpatialAnalysisConstant.GeospatialScaleVillageDataSlice
    }
    dataServiceInvoker.getDataSlice(innerDataKindName)
  }

  private def buildAttributeKindList(dataSlicePropertyMap: util.Map[String, DataSlicePropertyType]) = {
    val attributeList = new util.ArrayList[AttributeKind]
    import scala.collection.JavaConversions._
    for (entry <- dataSlicePropertyMap.entrySet) {
      val attributeKindName = entry.getKey
      if (!(attributeKindName == CoreRealmOperationUtil.RealmGlobalUID)) {
        val mapValue = entry.getValue
        var attributeDataType:AttributeDataType = null
        mapValue match {
          case DataSlicePropertyType.BOOLEAN =>
            attributeDataType = AttributeDataType.BOOLEAN
          case DataSlicePropertyType.INT =>
            attributeDataType = AttributeDataType.INT
          case DataSlicePropertyType.SHORT =>
            attributeDataType = AttributeDataType.SHORT
          case DataSlicePropertyType.LONG =>
            attributeDataType = AttributeDataType.LONG
          case DataSlicePropertyType.FLOAT =>
            attributeDataType = AttributeDataType.FLOAT
          case DataSlicePropertyType.DOUBLE =>
            attributeDataType = AttributeDataType.DOUBLE
          case DataSlicePropertyType.DATE =>
            attributeDataType = AttributeDataType.TIMESTAMP
          case DataSlicePropertyType.STRING =>
            attributeDataType = AttributeDataType.STRING
          case DataSlicePropertyType.BYTE =>
            attributeDataType = AttributeDataType.BYTE
          case DataSlicePropertyType.DECIMAL =>
            attributeDataType = AttributeDataType.DECIMAL
          case DataSlicePropertyType.BINARY =>
            attributeDataType = AttributeDataType.BINARY
          case DataSlicePropertyType.GEOMETRY =>
            attributeDataType = AttributeDataType.STRING
          case DataSlicePropertyType.UUID =>
            attributeDataType = AttributeDataType.STRING
        }
        val currentAttribute = new Neo4JAttributeKindImpl(null, attributeKindName, "", attributeDataType, null)
        attributeList.add(currentAttribute)
      }
    }
    attributeList
  }

  def parseSHPData(shpFile: File, fileEncode: String):SpatialDataInfo = {
    val shpDataValueList = new util.ArrayList[util.HashMap[String, Any]]
    val charsetEncode: String = if (fileEncode != null) {
      fileEncode
    }else {
      "UTF-8"
    }
    // ????????????????????????
    val dataStore: FileDataStore = FileDataStoreFinder.getDataStore(shpFile)
    dataStore.asInstanceOf[ShapefileDataStore].setCharset(Charset.forName(charsetEncode))

    val shpDataPropertyTypeMap = new util.HashMap[String,DataSlicePropertyType]

    dataStore.getSchema.getTypes.forEach( propertyType =>{
      var propertyName = propertyType.getName.toString
      //handle invalid chars and reserved words
      propertyName = propertyName.replaceAll("???", "Delta_")
      propertyName = propertyName.replaceAll("OFFSET", "OFFSET_")
      val propertyValueType = propertyType.getBinding.getName
      if(propertyValueType.equals("java.util.Date")){
        shpDataPropertyTypeMap.put(propertyName,DataSlicePropertyType.TIMESTAMP)
      }
      if(propertyValueType.equals("java.lang.String")){
        shpDataPropertyTypeMap.put(propertyName,DataSlicePropertyType.STRING)
      }
      if(propertyValueType.equals("java.lang.Integer")){
        shpDataPropertyTypeMap.put(propertyName,DataSlicePropertyType.INT)
      }
      if(propertyValueType.equals("java.lang.Long")){
        shpDataPropertyTypeMap.put(propertyName,DataSlicePropertyType.LONG)
      }
      if(propertyValueType.equals("java.lang.Double")){
        shpDataPropertyTypeMap.put(propertyName,DataSlicePropertyType.DOUBLE)
      }
      if(propertyValueType.equals("java.lang.Float")){
        shpDataPropertyTypeMap.put(propertyName,DataSlicePropertyType.FLOAT)
      }
    })

    // ??????????????????
    val simpleFeatureSource: SimpleFeatureSource = dataStore.getFeatureSource
    val simpleFeatureType: SimpleFeatureType = dataStore.getSchema

    val _CRSName: String = simpleFeatureType.getCoordinateReferenceSystem.getName.getCode
    var entityCRSAID: String = null
    var geometryContentType: String =null

    if ("GCS_WGS_1984" == _CRSName || _CRSName.contains("WGS84")) {
      entityCRSAID = "EPSG:4326"
      geometryContentType = RealmConstant._GeospatialGLGeometryContent
    }else {
      if ("CGCS_2000" == _CRSName || _CRSName.contains("CGCS2000")) {
        entityCRSAID = "EPSG:4545"
        geometryContentType = RealmConstant._GeospatialCLGeometryContent
      }else {
        val _EpsgCodeValue: Integer = CRS.lookupEpsgCode(simpleFeatureType.getCoordinateReferenceSystem, true)
        if (_EpsgCodeValue != null) {
          entityCRSAID = "EPSG:" + _EpsgCodeValue.intValue
        }
        geometryContentType = RealmConstant._GeospatialLLGeometryContent
      }
    }

    shpDataPropertyTypeMap.put(RealmConstant._GeospatialGeometryType, DataSlicePropertyType.STRING)
    shpDataPropertyTypeMap.put(geometryContentType, DataSlicePropertyType.STRING)

    // ????????????
    val simpleFeatureCollection: SimpleFeatureCollection = simpleFeatureSource.getFeatures
    // ?????????????????????
    val featureIterator: SimpleFeatureIterator = simpleFeatureCollection.features
    while ( {
      featureIterator.hasNext
    }) {
      val newEntityValueMap: util.HashMap[String, Any] = new util.HashMap[String, Any]
      // ????????????
      val feature: SimpleFeature = featureIterator.next
      // ??????????????????????????????????????????
      val propertyList= feature.getValue
      import scala.collection.JavaConversions._
      for (property <- propertyList) {
        var propertyName: String = property.getName.toString
        //handle invalid chars and reserved words
        propertyName = propertyName.replaceAll("???", "Delta_")
        propertyName = propertyName.replaceAll("OFFSET", "OFFSET_")
        val propertyValue: Any = property.getValue
        if (propertyValue != null && !propertyName.equals("the_geom")) {
          newEntityValueMap.put(propertyName, propertyValue)
        }
      }
      if (feature.getDefaultGeometry != null) {
        val geometryContent: String = feature.getDefaultGeometry.toString
        val geometryAttribute: GeometryAttribute = feature.getDefaultGeometryProperty
        val geometryType: String = geometryAttribute.getType.getName.toString
        var geometryTypeValue: String = "GEOMETRYCOLLECTION"
        if ("Point" == geometryType) {
          geometryTypeValue = "POINT"
        }
        if ("MultiPoint" == geometryType) {
          geometryTypeValue = "MULTIPOINT"
        }
        if ("LineString" == geometryType) {
          geometryTypeValue = "LINESTRING"
        }
        if ("MultiLineString" == geometryType) {
          geometryTypeValue = "MULTILINESTRING"
        }
        if ("Polygon" == geometryType) {
          geometryTypeValue = "POLYGON"
        }
        if ("MultiPolygon" == geometryType) {
          geometryTypeValue = "MULTIPOLYGON"
        }
        newEntityValueMap.put(RealmConstant._GeospatialGeometryType, geometryTypeValue)
        newEntityValueMap.put(geometryContentType, geometryContent)
      }
      shpDataValueList.add(newEntityValueMap)
    }

    dataMaintenance.SpatialDataInfo(shpDataPropertyTypeMap,shpDataValueList)
  }

  def duplicateSpatialDataInfoToDataSlice(dataServiceInvoker:DataServiceInvoker, spatialDataInfo: SpatialDataInfo,
                                          dataSliceName:String, dataSliceGroupName:String, removeExistingData:Boolean, dataSlicePrimaryKeys:Array[String]):DataSlice = {
    val spatialDataValue = spatialDataInfo.spatialDataValue
    val spatialDataPropertiesDefinition = spatialDataInfo.spatialDataPropertiesDefinition

    val primaryKeysList: util.ArrayList[String] = new util.ArrayList[String]()
    if(dataSlicePrimaryKeys != null && dataSlicePrimaryKeys.size >0){
      dataSlicePrimaryKeys.foreach(key=>{
        primaryKeysList.add(key)
      })
    }else{
      primaryKeysList.add("TGDA_AutoGeneratedPrimaryKey")
      spatialDataPropertiesDefinition.put("TGDA_AutoGeneratedPrimaryKey",DataSlicePropertyType.INT)
    }

    var targetDataSlice = dataServiceInvoker.getDataSlice(dataSliceName)

    if(targetDataSlice == null){
      targetDataSlice = dataServiceInvoker.createGridDataSlice(dataSliceName,dataSliceGroupName,spatialDataPropertiesDefinition,primaryKeysList)
    }
    if(removeExistingData){
      targetDataSlice.emptyDataSlice()
    }
    var countIndex = 0;
    spatialDataValue.forEach(dataItem =>{
      if(dataSlicePrimaryKeys == null){
        dataItem.put("TGDA_AutoGeneratedPrimaryKey",countIndex)
        countIndex = countIndex+1
      }
      targetDataSlice.addDataRecord(dataItem.asInstanceOf[util.HashMap[String,Object]])
    })
    targetDataSlice
  }

}
