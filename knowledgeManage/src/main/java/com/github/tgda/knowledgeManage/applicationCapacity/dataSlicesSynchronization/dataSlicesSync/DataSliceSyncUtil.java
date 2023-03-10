package com.github.tgda.knowledgeManage.applicationCapacity.dataSlicesSynchronization.dataSlicesSync;

import com.github.tgda.engine.core.analysis.query.QueryParameters;
import com.github.tgda.engine.core.term.Attribute;
import com.github.tgda.engine.core.term.AttributeDataType;
import com.github.tgda.engine.core.term.spi.neo4j.termImpl.Neo4JAttributeImpl;
import com.github.tgda.engine.core.util.Constant;
import com.github.tgda.compute.applicationCapacity.compute.dataComputeUnit.dataService.DataServiceInvoker;
import com.github.tgda.compute.applicationCapacity.compute.dataComputeUnit.dataService.DataSlice;
import com.github.tgda.compute.applicationCapacity.compute.dataComputeUnit.dataService.DataSlicePropertyType;
import com.github.tgda.compute.applicationCapacity.compute.dataComputeUnit.util.CoreRealmOperationUtil;
import com.github.tgda.compute.applicationCapacity.compute.exception.DataSliceDataException;
import com.github.tgda.compute.applicationCapacity.compute.exception.DataSliceExistException;
import com.github.tgda.compute.applicationCapacity.compute.exception.DataSlicePropertiesStructureException;
import com.github.tgda.knowledgeManage.consoleApplication.util.ApplicationLauncherUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class DataSliceSyncUtil {

    public static void syncGeospatialData(DataServiceInvoker dataServiceInvoker){
        String dataSliceGroupName = ApplicationLauncherUtil.getApplicationInfoPropertyValue("DataSlicesSynchronization.dataSliceGroup");
        String dataSyncPerLoadResultNumber = ApplicationLauncherUtil.getApplicationInfoPropertyValue("DataSlicesSynchronization.dataSyncPerLoadResultNumber");
        String degreeOfParallelismNumber = ApplicationLauncherUtil.getApplicationInfoPropertyValue("DataSlicesSynchronization.degreeOfParallelism");
        int dataSyncPerLoadResultNum = dataSyncPerLoadResultNumber != null ? Integer.parseInt(dataSyncPerLoadResultNumber) : 100000000;
        int degreeOfParallelismNum = degreeOfParallelismNumber != null ? Integer.parseInt(degreeOfParallelismNumber) : 5;
        List<String> pkList = new ArrayList<>();
        pkList.add(CoreRealmOperationUtil.RealmGlobalUID);
        try {
            DataSlice targetCountryRegionDataSlice = dataServiceInvoker.getDataSlice(Constant.GeospatialScaleCountryRegionEntityClass);
            Map<String, DataSlicePropertyType> dataSlicePropertyMap = new HashMap<>();
            dataSlicePropertyMap.put("Alpha_2Code",DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put("Alpha_3Code",DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put("NumericCode",DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put("ISO3166_2Code",DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant.GeospatialEnglishNameProperty,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant.GeospatialChineseNameProperty,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put("belongedContinent",DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put("capitalChineseName",DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put("capitalEnglishName",DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant.GeospatialCodeProperty,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant.GeospatialProperty,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant.GeospatialScaleGradeProperty, DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant._GeospatialGeometryType, DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant._GeospatialGlobalCRSAID, DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant._GeospatialGLGeometryContent, DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(CoreRealmOperationUtil.RealmGlobalUID, DataSlicePropertyType.STRING);
            if (targetCountryRegionDataSlice == null) {
                dataServiceInvoker.createGridDataSlice(Constant.GeospatialScaleCountryRegionEntityClass, dataSliceGroupName, dataSlicePropertyMap, pkList);
            }
            QueryParameters queryParameters = new QueryParameters();
            queryParameters.setResultNumber(dataSyncPerLoadResultNum);
            List<Attribute> containsAttributesKinds = buildAttributeKindList(dataSlicePropertyMap);
            CoreRealmOperationUtil.loadInnerDataKindEntitiesToDataSlice(dataServiceInvoker, Constant.GeospatialScaleCountryRegionEntityClass,containsAttributesKinds,
                    queryParameters, Constant.GeospatialScaleCountryRegionEntityClass,true,degreeOfParallelismNum);

            DataSlice targetProvinceDataSlice = dataServiceInvoker.getDataSlice(Constant.GeospatialScaleProvinceEntityClass);
            dataSlicePropertyMap = new HashMap<>();
            dataSlicePropertyMap.put("ISO3166_1Alpha_2Code",DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put("ISO3166_2SubDivisionCode",DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put("ISO3166_2SubdivisionName",DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put("ISO3166_2SubdivisionCategory",DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant.GeospatialCodeProperty,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant.GeospatialProperty,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant.GeospatialScaleGradeProperty, DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put("DivisionCategory_EN",DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put("DivisionCategory_CH",DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant.GeospatialEnglishNameProperty,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant.GeospatialChineseNameProperty,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant._GeospatialGLGeometryPOI,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant._GeospatialGlobalCRSAID,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant._GeospatialGeometryType,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant._GeospatialGLGeometryContent,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put("ChinaDivisionCode",DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant._GeospatialCLGeometryPOI,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant._GeospatialCountryCRSAID,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant._GeospatialCLGeometryContent,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(CoreRealmOperationUtil.RealmGlobalUID, DataSlicePropertyType.STRING);
            if (targetProvinceDataSlice == null) {
                dataServiceInvoker.createGridDataSlice(Constant.GeospatialScaleProvinceEntityClass, dataSliceGroupName, dataSlicePropertyMap, pkList);
            }
            containsAttributesKinds = buildAttributeKindList(dataSlicePropertyMap);
            CoreRealmOperationUtil.loadInnerDataKindEntitiesToDataSlice(dataServiceInvoker, Constant.GeospatialScaleProvinceEntityClass,containsAttributesKinds,
                    queryParameters, Constant.GeospatialScaleProvinceEntityClass,true,degreeOfParallelismNum);

            DataSlice targetPrefectureDataSlice = dataServiceInvoker.getDataSlice(Constant.GeospatialScalePrefectureEntityClass);
            dataSlicePropertyMap = new HashMap<>();
            dataSlicePropertyMap.put("ChinaParentDivisionCode",DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put("ChinaDivisionCode",DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put("ChinaProvinceName",DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant.GeospatialCodeProperty,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant.GeospatialProperty,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant._GeospatialGeometryType,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant._GeospatialGlobalCRSAID,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant._GeospatialGLGeometryContent,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant._GeospatialCountryCRSAID,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant._GeospatialCLGeometryContent,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant._GeospatialGLGeometryPOI,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant._GeospatialCLGeometryPOI,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant._GeospatialGLGeometryBorder,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant._GeospatialCLGeometryBorder,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant.GeospatialScaleGradeProperty,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(Constant.GeospatialChineseNameProperty,DataSlicePropertyType.STRING);
            dataSlicePropertyMap.put(CoreRealmOperationUtil.RealmGlobalUID, DataSlicePropertyType.STRING);
            if (targetPrefectureDataSlice == null) {
                dataServiceInvoker.createGridDataSlice(Constant.GeospatialScalePrefectureEntityClass, dataSliceGroupName, dataSlicePropertyMap, pkList);
            }
            containsAttributesKinds = buildAttributeKindList(dataSlicePropertyMap);
            CoreRealmOperationUtil.loadInnerDataKindEntitiesToDataSlice(dataServiceInvoker, Constant.GeospatialScalePrefectureEntityClass,containsAttributesKinds,
                    queryParameters, Constant.GeospatialScalePrefectureEntityClass,true,degreeOfParallelismNum);

            DataSlice targetCountyDataSlice = dataServiceInvoker.getDataSlice(Constant.GeospatialScaleCountyEntityClass);
            dataSlicePropertyMap.put("ChinaPrefectureName",DataSlicePropertyType.STRING);
            if (targetCountyDataSlice == null) {
                dataServiceInvoker.createGridDataSlice(Constant.GeospatialScaleCountyEntityClass, dataSliceGroupName, dataSlicePropertyMap, pkList);
            }
            containsAttributesKinds = buildAttributeKindList(dataSlicePropertyMap);
            CoreRealmOperationUtil.loadInnerDataKindEntitiesToDataSlice(dataServiceInvoker, Constant.GeospatialScaleCountyEntityClass,containsAttributesKinds,
                    queryParameters, Constant.GeospatialScaleCountyEntityClass,true,degreeOfParallelismNum);

            DataSlice targetTownshipDataSlice = dataServiceInvoker.getDataSlice(Constant.GeospatialScaleTownshipEntityClass);
            dataSlicePropertyMap.put("ChinaCountyName",DataSlicePropertyType.STRING);
            if (targetTownshipDataSlice == null) {
                dataServiceInvoker.createGridDataSlice(Constant.GeospatialScaleTownshipEntityClass, dataSliceGroupName, dataSlicePropertyMap, pkList);
            }
            containsAttributesKinds = buildAttributeKindList(dataSlicePropertyMap);
            CoreRealmOperationUtil.loadInnerDataKindEntitiesToDataSlice(dataServiceInvoker, Constant.GeospatialScaleTownshipEntityClass,containsAttributesKinds,
                    queryParameters, Constant.GeospatialScaleTownshipEntityClass,true,degreeOfParallelismNum);

            DataSlice targetVillageDataSlice = dataServiceInvoker.getDataSlice(Constant.GeospatialScaleVillageEntityClass);
            dataSlicePropertyMap.put("ChinaTownshipName",DataSlicePropertyType.STRING);
            if (targetVillageDataSlice == null) {
                dataServiceInvoker.createGridDataSlice(Constant.GeospatialScaleVillageEntityClass, dataSliceGroupName, dataSlicePropertyMap, pkList);
            }
            containsAttributesKinds = buildAttributeKindList(dataSlicePropertyMap);
            CoreRealmOperationUtil.loadInnerDataKindEntitiesToDataSlice(dataServiceInvoker, Constant.GeospatialScaleVillageEntityClass,containsAttributesKinds,
                    queryParameters, Constant.GeospatialScaleVillageEntityClass,true,degreeOfParallelismNum);
        } catch (DataSliceExistException e) {
            e.printStackTrace();
        } catch (DataSlicePropertiesStructureException e) {
            e.printStackTrace();
        }
    }

    public static void batchSyncPerDefinedDataSlices(DataServiceInvoker dataServiceInvoker) {
        String dataSliceGroupName = ApplicationLauncherUtil.getApplicationInfoPropertyValue("DataSlicesSynchronization.dataSliceGroup");
        String dataSyncPerLoadResultNumber = ApplicationLauncherUtil.getApplicationInfoPropertyValue("DataSlicesSynchronization.dataSyncPerLoadResultNumber");
        String degreeOfParallelismNumber = ApplicationLauncherUtil.getApplicationInfoPropertyValue("DataSlicesSynchronization.degreeOfParallelism");
        int dataSyncPerLoadResultNum = dataSyncPerLoadResultNumber != null ? Integer.parseInt(dataSyncPerLoadResultNumber) : 100000000;
        int degreeOfParallelismNum = degreeOfParallelismNumber != null ? Integer.parseInt(degreeOfParallelismNumber) : 5;

        Map<String,List<DataPropertyInfo>> conceptionKindDataPropertiesMap = new HashMap<>();
        Map<String,List<DataPropertyInfo>> relationKindDataPropertiesMap = new HashMap<>();
        String lastConceptionKindName = null;
        String lastRelationKindName = null;
        String currentHandleType = "ConceptionKind";

        File file = new File("DataSlicesSyncKindList");
        if(file.exists() && file.isFile()){
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                String tempStr;
                while ((tempStr = reader.readLine()) != null) {
                    String currentLine = tempStr.trim();
                    if(currentLine.startsWith("ConceptionKind.")){
                        //handle ConceptionKind define
                        currentHandleType = "ConceptionKind";
                        String currentConceptionKindName = currentLine.replace("ConceptionKind.","");
                        lastConceptionKindName = currentConceptionKindName;
                    }else if(currentLine.startsWith("RelationKind.")){
                        //handle ConceptionKind define
                        currentHandleType = "RelationKind";
                        String currentRelationKindName = currentLine.replace("RelationKind.","");
                        lastRelationKindName = currentRelationKindName;
                    }else{
                        String[] propertyDefineArray = currentLine.split("    ");
                        String propertyName = propertyDefineArray[0];
                        String propertyType = propertyDefineArray[1];
                        if(currentHandleType.equals("ConceptionKind")){
                            initKindPropertyDefine(conceptionKindDataPropertiesMap,lastConceptionKindName,propertyName,propertyType);
                        }
                        if(currentHandleType.equals("RelationKind")){
                            initKindPropertyDefine(relationKindDataPropertiesMap,lastRelationKindName,propertyName,propertyType);
                        }
                    }
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }

        //handle conceptionKinds data
        Set<String> conceptionKindsSet = conceptionKindDataPropertiesMap.keySet();
        try {
            for(String currentConceptionKind : conceptionKindsSet){
                DataSlice targetDataSlice = dataServiceInvoker.getDataSlice(currentConceptionKind);
                if (targetDataSlice == null) {
                    List<DataPropertyInfo> kindDataPropertyInfoList = conceptionKindDataPropertiesMap.get(currentConceptionKind);
                    Map<String, DataSlicePropertyType> dataSlicePropertyMap = new HashMap<>();
                    if(kindDataPropertyInfoList != null) {
                        for (DataPropertyInfo currentDataPropertyInfo : kindDataPropertyInfoList) {
                            dataSlicePropertyMap.put(currentDataPropertyInfo.getPropertyName(), currentDataPropertyInfo.getPropertyType());
                        }
                    }
                    dataSlicePropertyMap.put(CoreRealmOperationUtil.RealmGlobalUID, DataSlicePropertyType.STRING);
                    List<String> pkList = new ArrayList<>();
                    pkList.add(CoreRealmOperationUtil.RealmGlobalUID);
                    dataServiceInvoker.createGridDataSlice(currentConceptionKind, dataSliceGroupName+"_CONCEPTION", dataSlicePropertyMap, pkList);
                }
            }

            for(String currentConceptionKind : conceptionKindsSet){
                List<DataPropertyInfo> kindDataPropertyInfoList = conceptionKindDataPropertiesMap.get(currentConceptionKind);
                List<String> conceptionKindPropertiesList = new ArrayList<>();
                if(kindDataPropertyInfoList != null){
                    for(DataPropertyInfo currentDataPropertyInfo : kindDataPropertyInfoList){
                        conceptionKindPropertiesList.add(currentDataPropertyInfo.getPropertyName());
                    }
                }
                QueryParameters queryParameters = new QueryParameters();
                queryParameters.setResultNumber(dataSyncPerLoadResultNum);
                CoreRealmOperationUtil.loadConceptionKindEntitiesToDataSlice(dataServiceInvoker,currentConceptionKind, conceptionKindPropertiesList,
                        queryParameters, currentConceptionKind, true, degreeOfParallelismNum);
            }
        } catch (DataSliceExistException e) {
            e.printStackTrace();
        } catch (DataSlicePropertiesStructureException e) {
            e.printStackTrace();
        }
        //handle relationKinds data
        Set<String> relationKindsSet = relationKindDataPropertiesMap.keySet();
        try {
            for(String currentRelationKind : relationKindsSet){
                DataSlice targetDataSlice = dataServiceInvoker.getDataSlice(currentRelationKind);
                if (targetDataSlice == null) {
                    List<DataPropertyInfo> kindDataPropertyInfoList = relationKindDataPropertiesMap.get(currentRelationKind);
                    Map<String, DataSlicePropertyType> dataSlicePropertyMap = new HashMap<>();
                    if(kindDataPropertyInfoList != null) {
                        for (DataPropertyInfo currentDataPropertyInfo : kindDataPropertyInfoList) {
                            dataSlicePropertyMap.put(currentDataPropertyInfo.getPropertyName(), currentDataPropertyInfo.getPropertyType());
                        }
                    }
                    dataSlicePropertyMap.put(CoreRealmOperationUtil.RealmGlobalUID, DataSlicePropertyType.STRING);
                    dataSlicePropertyMap.put(CoreRealmOperationUtil.RelationFromEntityUID, DataSlicePropertyType.STRING);
                    dataSlicePropertyMap.put(CoreRealmOperationUtil.RelationToEntityUID, DataSlicePropertyType.STRING);
                    List<String> pkList = new ArrayList<>();
                    pkList.add(CoreRealmOperationUtil.RealmGlobalUID);
                    dataServiceInvoker.createGridDataSlice(currentRelationKind, dataSliceGroupName+"_RELATION", dataSlicePropertyMap, pkList);
                }
            }

            for(String currentRelationKind : relationKindsSet){
                List<DataPropertyInfo> kindDataPropertyInfoList = relationKindDataPropertiesMap.get(currentRelationKind);
                List<String> relationKindPropertiesList = new ArrayList<>();
                if(kindDataPropertyInfoList != null){
                    for(DataPropertyInfo currentDataPropertyInfo : kindDataPropertyInfoList){
                        relationKindPropertiesList.add(currentDataPropertyInfo.getPropertyName());
                    }
                }
                QueryParameters queryParameters = new QueryParameters();
                queryParameters.setResultNumber(dataSyncPerLoadResultNum);
                CoreRealmOperationUtil.loadRelationKindEntitiesToDataSlice(dataServiceInvoker,currentRelationKind, relationKindPropertiesList,
                        queryParameters, currentRelationKind, true, degreeOfParallelismNum);
            }
        } catch (DataSliceExistException e) {
            e.printStackTrace();
        } catch (DataSlicePropertiesStructureException e) {
            e.printStackTrace();
        }
    }

    public static void initKindPropertyDefine(Map<String,List<DataPropertyInfo>> kindDataPropertiesMap,String KindName,String propertyName,String propertyType){
        if(propertyName.startsWith("Attribute.")){
            String propertyRealName = propertyName.replace("Attribute.","");
            DataPropertyInfo currentDataPropertyInfo = null;
            switch(propertyType){
                case "BOOLEAN" :
                    currentDataPropertyInfo = new DataPropertyInfo(propertyRealName,DataSlicePropertyType.BOOLEAN);
                    break;
                case "INT" :
                    currentDataPropertyInfo = new DataPropertyInfo(propertyRealName,DataSlicePropertyType.INT);
                    break;
                case "SHORT" :
                    currentDataPropertyInfo = new DataPropertyInfo(propertyRealName,DataSlicePropertyType.SHORT);
                    break;
                case "LONG" :
                    currentDataPropertyInfo = new DataPropertyInfo(propertyRealName,DataSlicePropertyType.LONG);
                    break;
                case "FLOAT" :
                    currentDataPropertyInfo = new DataPropertyInfo(propertyRealName,DataSlicePropertyType.FLOAT);
                    break;
                case "DOUBLE" :
                    currentDataPropertyInfo = new DataPropertyInfo(propertyRealName,DataSlicePropertyType.DOUBLE);
                    break;
                case "DATE" :
                    currentDataPropertyInfo = new DataPropertyInfo(propertyRealName,DataSlicePropertyType.DATE);
                    break;
                case "STRING" :
                    currentDataPropertyInfo = new DataPropertyInfo(propertyRealName,DataSlicePropertyType.STRING);
                    break;
                case "BYTE" :
                    currentDataPropertyInfo = new DataPropertyInfo(propertyRealName,DataSlicePropertyType.BYTE);
                    break;
                case "DECIMAL" :
                    currentDataPropertyInfo = new DataPropertyInfo(propertyRealName,DataSlicePropertyType.DECIMAL);
                    break;
                case "BINARY" :
                    currentDataPropertyInfo = new DataPropertyInfo(propertyRealName,DataSlicePropertyType.BINARY);
                    break;
                case "GEOMETRY" :
                    currentDataPropertyInfo = new DataPropertyInfo(propertyRealName,DataSlicePropertyType.GEOMETRY);
                    break;
                case "UUID" :
                    currentDataPropertyInfo = new DataPropertyInfo(propertyRealName,DataSlicePropertyType.UUID);
                    break;
            }
            if(currentDataPropertyInfo != null){
                if(!kindDataPropertiesMap.containsKey(KindName)){
                    kindDataPropertiesMap.put(KindName,new ArrayList<>());
                }
                kindDataPropertiesMap.get(KindName).add(currentDataPropertyInfo);
            }
        }
    }

    public static void deleteDataFromSlice(DataServiceInvoker dataServiceInvoker,String dataSliceName,String dataPK){
        DataSlice targetDataSlice = dataServiceInvoker.getDataSlice(dataSliceName);
        if(targetDataSlice != null){
            try {
                Map<String,Object> dataPKPropertiesValue = new HashMap<>();
                dataPKPropertiesValue.put(CoreRealmOperationUtil.RealmGlobalUID,dataPK);
                targetDataSlice.deleteDataRecord(dataPKPropertiesValue);
            } catch (DataSlicePropertiesStructureException e) {
                e.printStackTrace();
            } catch (DataSliceDataException e) {
                e.printStackTrace();
            }
        }
    }

    public static void updateDataInSlice(DataServiceInvoker dataServiceInvoker,String dataSliceName,String dataPK,Map<String,Object> entityProperties){
        DataSlice targetDataSlice = dataServiceInvoker.getDataSlice(dataSliceName);
        if(targetDataSlice != null && entityProperties!= null){
            try {
                entityProperties.put(CoreRealmOperationUtil.RealmGlobalUID,dataPK);
                targetDataSlice.updateDataRecord(entityProperties);
            } catch (DataSlicePropertiesStructureException e) {
                e.printStackTrace();
            } catch (DataSliceDataException e) {
                e.printStackTrace();
            }
        }
    }

    public static void createDataInSlice(DataServiceInvoker dataServiceInvoker,String dataSliceName,String dataPK,
                                         Map<String,Object> entityProperties,Map<String, List<DataPropertyInfo>> kindDataPropertiesMap,String dataSliceType){
        DataSlice targetDataSlice = dataServiceInvoker.getDataSlice(dataSliceName);
        try {
            if(targetDataSlice == null){
                String dataSliceGroupName = ApplicationLauncherUtil.getApplicationInfoPropertyValue("DataSlicesSynchronization.dataSliceGroup");
                List<DataPropertyInfo> kindDataPropertyInfoList = kindDataPropertiesMap.get(dataSliceName);
                Map<String, DataSlicePropertyType> dataSlicePropertyMap = new HashMap<>();
                if(kindDataPropertyInfoList != null) {
                    for (DataPropertyInfo currentDataPropertyInfo : kindDataPropertyInfoList) {
                        dataSlicePropertyMap.put(currentDataPropertyInfo.getPropertyName(), currentDataPropertyInfo.getPropertyType());
                    }
                }
                dataSlicePropertyMap.put(CoreRealmOperationUtil.RealmGlobalUID, DataSlicePropertyType.STRING);
                List<String> pkList = new ArrayList<>();
                pkList.add(CoreRealmOperationUtil.RealmGlobalUID);
                dataServiceInvoker.createGridDataSlice(dataSliceName, dataSliceGroupName+dataSliceType, dataSlicePropertyMap, pkList);
            }
            if(entityProperties!= null){
                entityProperties.put(CoreRealmOperationUtil.RealmGlobalUID,dataPK);
                targetDataSlice.addDataRecord(entityProperties);
            }
        } catch (DataSlicePropertiesStructureException e) {
            e.printStackTrace();
        } catch (DataSliceDataException | DataSliceExistException e) {
            e.printStackTrace();
        }
    }

    private static List<Attribute> buildAttributeKindList(Map<String, DataSlicePropertyType> dataSlicePropertyMap){
        List<Attribute> attributeList = new ArrayList<>();
        for (Map.Entry<String, DataSlicePropertyType> entry : dataSlicePropertyMap.entrySet()) {
            String attributeKindName = entry.getKey();
            if(!attributeKindName.equals(CoreRealmOperationUtil.RealmGlobalUID)){
                DataSlicePropertyType mapValue = entry.getValue();
                AttributeDataType attributeDataType = null;
                switch(mapValue){
                    case BOOLEAN:
                        attributeDataType = AttributeDataType.BOOLEAN;
                        break;
                    case INT:
                        attributeDataType = AttributeDataType.INT;
                        break;
                    case SHORT:
                        attributeDataType = AttributeDataType.SHORT;
                        break;
                    case LONG:
                        attributeDataType = AttributeDataType.LONG;
                        break;
                    case FLOAT:
                        attributeDataType = AttributeDataType.FLOAT;
                        break;
                    case DOUBLE:
                        attributeDataType = AttributeDataType.DOUBLE;
                        break;
                    case DATE:
                        attributeDataType = AttributeDataType.TIMESTAMP;
                        break;
                    case STRING:
                        attributeDataType = AttributeDataType.STRING;
                        break;
                    case BYTE:
                        attributeDataType = AttributeDataType.BYTE;
                        break;
                    case DECIMAL:
                        attributeDataType = AttributeDataType.DECIMAL;
                        break;
                    case BINARY:
                        attributeDataType = AttributeDataType.BINARY;
                        break;
                    case GEOMETRY:
                        attributeDataType = AttributeDataType.STRING;
                        break;
                    case UUID:
                        attributeDataType = AttributeDataType.STRING;
                        break;
                }
                Attribute currentAttribute = new Neo4JAttributeImpl(null,attributeKindName,"",attributeDataType,null);
                attributeList.add(currentAttribute);
            }
        }
        return attributeList;
    }
}