package com.github.tgda.sceneDataPrepare;

import com.github.tgda.engine.core.analysis.query.QueryParameters;
import com.github.tgda.engine.core.exception.EngineServiceEntityExploreException;
import com.github.tgda.engine.core.exception.EngineServiceRuntimeException;
import com.github.tgda.engine.core.internal.neo4j.util.BatchDataOperationUtil;
import com.github.tgda.engine.core.payload.EntitiesAttributesRetrieveResult;
import com.github.tgda.engine.core.payload.EntityValue;
import com.github.tgda.engine.core.term.Type;
import com.github.tgda.engine.core.term.Engine;
import com.github.tgda.engine.core.term.TimeFlow;
import com.github.tgda.engine.core.util.Constant;
import com.github.tgda.engine.core.util.factory.EngineFactory;
import com.github.tgda.engine.core.util.geospatial.GeospatialOperationUtil;
import com.github.tgda.example.generator.SeattleRealTimeFire911Calls_Realm_Generator;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class TimeAndGeoSceneDataGenerator {

    private static boolean loadHugeDataset = true;

    public static void main(String[] args) throws EngineServiceRuntimeException, EngineServiceEntityExploreException {
        //initTimeline
        generateTimelineData();

        //load data
        SeattleRealTimeFire911Calls_Realm_Generator.main(null);
        generateFileViolationsData();
        generateNoiseReportsData();
        generatePaidParkingTransactionData();
        generateSPDCrimeData();
        generateFireDepartmentCallsForServiceData();
        generatePoliceDepartmentIncidentReportsData();
        generateFireIncidentsData();
    }

    private static void generateTimelineData() throws EngineServiceRuntimeException {
        Engine coreRealm = EngineFactory.getDefaultEngine();
        TimeFlow defaultTimeFlow = coreRealm.getOrCreateTimeFlow();
        defaultTimeFlow.createTimeSpanEntities(2000,2005,true);
        defaultTimeFlow.createTimeSpanEntities(2006,2010,true);
        defaultTimeFlow.createTimeSpanEntities(2011,2015,true);
        defaultTimeFlow.createTimeSpanEntities(2016,2022,true);
    }

    private static void generateFileViolationsData() throws EngineServiceRuntimeException, EngineServiceEntityExploreException {
        initConceptionKind("FireViolation","????????????");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        BatchDataOperationUtil.EntityAttributesProcess conceptionEntityAttributesProcess = new BatchDataOperationUtil.EntityAttributesProcess(){
            @Override
            public void doEntityAttributesProcess(Map<String, Object> entityValueMap) {
                if(entityValueMap != null && entityValueMap.containsKey("Location")){
                    String locationPoint = entityValueMap.get("Location").toString();
                    entityValueMap.put(Constant._GeospatialGLGeometryContent,locationPoint);
                    entityValueMap.put(Constant._GeospatialGeometryType,"POINT");
                    entityValueMap.put(Constant._GeospatialGlobalCRSAID,"EPSG:4326");
                }
                if(entityValueMap.containsKey("violation date") && !entityValueMap.get("violation date").toString().equals("")){
                    try {
                        Date date = sdf.parse(entityValueMap.get("violation date").toString());
                        entityValueMap.put("violationDate",date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(entityValueMap.containsKey("close date") && !entityValueMap.get("close date").toString().equals("")){
                    try {
                        Date date = sdf.parse(entityValueMap.get("close date").toString());
                        entityValueMap.put("closeDate",date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        BatchDataOperationUtil.importConceptionEntitiesFromExternalCSV("realmExampleData/time_and_geo_scene_data/Fire_Violations.csv","FireViolation",conceptionEntityAttributesProcess);
        linkDateAttribute("FireViolation","violationDate","Fire Violation occurred at",null,TimeFlow.TimeScaleGrade.DAY);
        linkDateAttribute("FireViolation","closeDate","Fire Violation closed at",null,TimeFlow.TimeScaleGrade.DAY);
    }

    private static void generateNoiseReportsData() throws EngineServiceRuntimeException, EngineServiceEntityExploreException {
        initConceptionKind("NoiseReport","????????????");
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss aa");
        BatchDataOperationUtil.EntityAttributesProcess conceptionEntityAttributesProcess = new BatchDataOperationUtil.EntityAttributesProcess(){
            @Override
            public void doEntityAttributesProcess(Map<String, Object> entityValueMap){
                if(entityValueMap != null && entityValueMap.containsKey("Point")&& entityValueMap.containsKey("Source")){
                    String longitude = entityValueMap.get("Source").toString().replace(")\"","");
                    String latitude = entityValueMap.get("Point").toString().replace("\"(","");
                    String locationPoint = "POINT ("+longitude+" "+latitude+")";
                    if(!locationPoint.equals("POINT ( 0.0 0.0)")){
                        entityValueMap.put(Constant._GeospatialGLGeometryContent, locationPoint);
                        entityValueMap.put(Constant._GeospatialGeometryType, "POINT");
                        entityValueMap.put(Constant._GeospatialGlobalCRSAID, "EPSG:4326");
                    }
                }
                if(entityValueMap.containsKey("Opened") && !entityValueMap.get("Opened").toString().equals("")){
                    Date date = null;
                    try {
                        date = sdf.parse(entityValueMap.get("Opened").toString());
                        entityValueMap.put("openedDate",date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                if(entityValueMap.containsKey("Closed") && !entityValueMap.get("Closed").toString().equals("")){
                    Date date = null;
                    try {
                        date = sdf.parse(entityValueMap.get("Closed").toString());
                        entityValueMap.put("closedDate",date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                if(entityValueMap.containsKey("Updated") && !entityValueMap.get("Updated").toString().equals("")){
                    Date date = null;
                    try {
                        date = sdf.parse(entityValueMap.get("Updated").toString());
                        entityValueMap.put("updatedDate",date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        BatchDataOperationUtil.importConceptionEntitiesFromExternalCSV("realmExampleData/time_and_geo_scene_data/Noise_Reports.csv","NoiseReport",conceptionEntityAttributesProcess);
        linkDateAttribute("NoiseReport","openedDate","Noise Report opened at",null,TimeFlow.TimeScaleGrade.MINUTE);
        linkDateAttribute("NoiseReport","closedDate","Noise Report closed at",null,TimeFlow.TimeScaleGrade.MINUTE);
        linkDateAttribute("NoiseReport","updatedDate","Noise Report updated at",null,TimeFlow.TimeScaleGrade.MINUTE);
    }

    private static void generatePaidParkingTransactionData() throws EngineServiceRuntimeException, EngineServiceEntityExploreException {
        initConceptionKind("PaidParkingTransaction","??????????????????");
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss aa");
        BatchDataOperationUtil.EntityAttributesProcess conceptionEntityAttributesProcess = new BatchDataOperationUtil.EntityAttributesProcess(){
            @Override
            public void doEntityAttributesProcess(Map<String, Object> entityValueMap) {
                if(entityValueMap != null && entityValueMap.containsKey("Latitude") && entityValueMap.containsKey("Longitude")){
                    String locationPoint = "POINT ("+entityValueMap.get("Longitude")+" "+entityValueMap.get("Latitude")+")";
                    entityValueMap.put(Constant._GeospatialGLGeometryContent,locationPoint);
                    entityValueMap.put(Constant._GeospatialGeometryType,"POINT");
                    entityValueMap.put(Constant._GeospatialGlobalCRSAID,"EPSG:4326");
                }
                if(entityValueMap.containsKey("Transaction DateTime") && !entityValueMap.get("Transaction DateTime").toString().equals("")){
                    Date date = null;
                    try {
                        date = sdf.parse(entityValueMap.get("Transaction DateTime").toString());
                        entityValueMap.put("transactionDate",date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        BatchDataOperationUtil.importConceptionEntitiesFromExternalCSV("realmExampleData/time_and_geo_scene_data/Paid_Parking_Transaction_Data.csv","PaidParkingTransaction",conceptionEntityAttributesProcess);
        linkDateAttribute("PaidParkingTransaction","transactionDate","Payment transaction occurred at",null,TimeFlow.TimeScaleGrade.MINUTE);
    }

    private static void generateSPDCrimeData() throws EngineServiceRuntimeException, EngineServiceEntityExploreException {
        initConceptionKind("SPDCrimeReport","SPD????????????");
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss aa");
        BatchDataOperationUtil.EntityAttributesProcess conceptionEntityAttributesProcess = new BatchDataOperationUtil.EntityAttributesProcess(){
            @Override
            public void doEntityAttributesProcess(Map<String, Object> entityValueMap) {
                if(entityValueMap != null && entityValueMap.containsKey("Latitude") && entityValueMap.containsKey("Longitude")){
                    String locationPoint = "POINT ("+entityValueMap.get("Longitude")+" "+entityValueMap.get("Latitude")+")";
                    entityValueMap.put(Constant._GeospatialGLGeometryContent,locationPoint);
                    entityValueMap.put(Constant._GeospatialGeometryType,"POINT");
                    entityValueMap.put(Constant._GeospatialGlobalCRSAID,"EPSG:4326");
                }
                if(entityValueMap.containsKey("Offense End DateTime") && !entityValueMap.get("Offense End DateTime").toString().equals("")){
                    Date date = null;
                    try {
                        date = sdf.parse(entityValueMap.get("Offense End DateTime").toString());
                        entityValueMap.put("offenseEndDate",date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                if(entityValueMap.containsKey("Offense Start DateTime") && !entityValueMap.get("Offense Start DateTime").toString().equals("")){
                    Date date = null;
                    try {
                        date = sdf.parse(entityValueMap.get("Offense Start DateTime").toString());
                        entityValueMap.put("offenseStartDate",date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                if(entityValueMap.containsKey("Report DateTime") && !entityValueMap.get("Report DateTime").toString().equals("")){
                    Date date = null;
                    try {
                        date = sdf.parse(entityValueMap.get("Report DateTime").toString());
                        entityValueMap.put("crimeReportDate",date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        if(loadHugeDataset){
            BatchDataOperationUtil.importConceptionEntitiesFromExternalCSV("realmExampleData/spd_crime_data/SPD_Crime_Data__2008-Present_huge.csv","SPDCrimeReport",conceptionEntityAttributesProcess);

        }else{
            BatchDataOperationUtil.importConceptionEntitiesFromExternalCSV("realmExampleData/spd_crime_data/SPD_Crime_Data__2008-Present.csv","SPDCrimeReport",conceptionEntityAttributesProcess);
        }
        linkDateAttribute("SPDCrimeReport","offenseEndDate","Offense ended at",null,TimeFlow.TimeScaleGrade.MINUTE);
        linkDateAttribute("SPDCrimeReport","offenseStartDate","Offense started at",null,TimeFlow.TimeScaleGrade.MINUTE);
        linkDateAttribute("SPDCrimeReport","crimeReportDate","Crime report at",null,TimeFlow.TimeScaleGrade.MINUTE);
    }

    private static void generateFireDepartmentCallsForServiceData() throws EngineServiceRuntimeException, EngineServiceEntityExploreException {
        initConceptionKind("FireDepartmentCall","??????????????????");
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss aa");
        SimpleDateFormat sdf2 = new SimpleDateFormat("MM/dd/yyyy");
        BatchDataOperationUtil.EntityAttributesProcess conceptionEntityAttributesProcess = new BatchDataOperationUtil.EntityAttributesProcess(){
            @Override
            public void doEntityAttributesProcess(Map<String, Object> entityValueMap) {
                if(entityValueMap.containsKey("Available DtTm") && !entityValueMap.get("Available DtTm").toString().equals("")){
                    Date date = null;
                    try {
                        date = sdf.parse(entityValueMap.get("Available DtTm").toString());
                        entityValueMap.put("availableDate",date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                if(entityValueMap.containsKey("Dispatch DtTm") && !entityValueMap.get("Dispatch DtTm").toString().equals("")){
                    Date date = null;
                    try {
                        date = sdf.parse(entityValueMap.get("Dispatch DtTm").toString());
                        entityValueMap.put("dispatchDate",date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                if(entityValueMap.containsKey("Entry DtTm") && !entityValueMap.get("Entry DtTm").toString().equals("")){
                    Date date = null;
                    try {
                        date = sdf.parse(entityValueMap.get("Entry DtTm").toString());
                        entityValueMap.put("entryDate",date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                if(entityValueMap.containsKey("On Scene DtTm") && !entityValueMap.get("On Scene DtTm").toString().equals("")){
                    Date date = null;
                    try {
                        date = sdf.parse(entityValueMap.get("On Scene DtTm").toString());
                        entityValueMap.put("onSceneDate",date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                if(entityValueMap.containsKey("Received DtTm") && !entityValueMap.get("Received DtTm").toString().equals("")){
                    Date date = null;
                    try {
                        date = sdf.parse(entityValueMap.get("Received DtTm").toString());
                        entityValueMap.put("receivedDate",date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                if(entityValueMap.containsKey("Response DtTm") && !entityValueMap.get("Response DtTm").toString().equals("")){
                    Date date = null;
                    try {
                        date = sdf.parse(entityValueMap.get("Response DtTm").toString());
                        entityValueMap.put("responseDate",date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                if(entityValueMap.containsKey("Call Date") && !entityValueMap.get("Call Date").toString().equals("")){
                    Date date = null;
                    try {
                        date = sdf2.parse(entityValueMap.get("Call Date").toString());
                        entityValueMap.put("callDate",date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                if(entityValueMap.containsKey("Watch Date") && !entityValueMap.get("Watch Date").toString().equals("")){
                    Date date = null;
                    try {
                        date = sdf2.parse(entityValueMap.get("Watch Date").toString());
                        entityValueMap.put("watchDate",date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        if(loadHugeDataset){
            BatchDataOperationUtil.importConceptionEntitiesFromExternalCSV("realmExampleData/fire_department_calls_for_service/Fire_Department_Calls_For_Service__2016_huge.csv","FireDepartmentCall",conceptionEntityAttributesProcess);
        }else{
            BatchDataOperationUtil.importConceptionEntitiesFromExternalCSV("realmExampleData/fire_department_calls_for_service/Fire_Department_Calls_For_Service__2016.csv","FireDepartmentCall",conceptionEntityAttributesProcess);
        }
        linkDateAttribute("FireDepartmentCall","availableDate","Call available at",null,TimeFlow.TimeScaleGrade.MINUTE);
        linkDateAttribute("FireDepartmentCall","dispatchDate","Call dispatch at",null,TimeFlow.TimeScaleGrade.MINUTE);
        linkDateAttribute("FireDepartmentCall","entryDate","Call entry at",null,TimeFlow.TimeScaleGrade.MINUTE);
        linkDateAttribute("FireDepartmentCall","onSceneDate","Call on scene at",null,TimeFlow.TimeScaleGrade.MINUTE);
        linkDateAttribute("FireDepartmentCall","receivedDate","Call received at",null,TimeFlow.TimeScaleGrade.MINUTE);
        linkDateAttribute("FireDepartmentCall","responseDate","Call response at",null,TimeFlow.TimeScaleGrade.MINUTE);
        linkDateAttribute("FireDepartmentCall","callDate","Call at",null,TimeFlow.TimeScaleGrade.DAY);
        linkDateAttribute("FireDepartmentCall","watchDate","Call watch at",null,TimeFlow.TimeScaleGrade.DAY);
    }

    private static void generatePoliceDepartmentIncidentReportsData() throws EngineServiceRuntimeException, EngineServiceEntityExploreException {
        initConceptionKind("PoliceDepartmentIncidentReport","????????????????????????");
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss aa");
        BatchDataOperationUtil.EntityAttributesProcess conceptionEntityAttributesProcess1 = new BatchDataOperationUtil.EntityAttributesProcess(){
            @Override
            public void doEntityAttributesProcess(Map<String, Object> entityValueMap) {
                if(entityValueMap != null && entityValueMap.containsKey("Point")){
                    String locationPoint = entityValueMap.get("Point").toString();
                    entityValueMap.put(Constant._GeospatialGLGeometryContent,locationPoint);
                    entityValueMap.put(Constant._GeospatialGeometryType,"POINT");
                    entityValueMap.put(Constant._GeospatialGlobalCRSAID,"EPSG:4326");
                }
                if(entityValueMap.containsKey("Incident Datetime") && !entityValueMap.get("Incident Datetime").toString().equals("")){
                    Date date = null;
                    try {
                        date = sdf.parse(entityValueMap.get("Incident Datetime").toString());
                        entityValueMap.put("incidentDate",date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                if(entityValueMap.containsKey("Report Datetime") && !entityValueMap.get("Report Datetime").toString().equals("")){
                    Date date = null;
                    try {
                        date = sdf.parse(entityValueMap.get("Report Datetime").toString());
                        entityValueMap.put("reportDate",date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        if(loadHugeDataset){
            BatchDataOperationUtil.importConceptionEntitiesFromExternalCSV("realmExampleData/police_department_incident_reports/Police_Department_Incident_Reports__2018_to_Present_huge.csv","PoliceDepartmentIncidentReport",conceptionEntityAttributesProcess1);
        }else{
            BatchDataOperationUtil.importConceptionEntitiesFromExternalCSV("realmExampleData/police_department_incident_reports/Police_Department_Incident_Reports__2018_to_Present.csv","PoliceDepartmentIncidentReport",conceptionEntityAttributesProcess1);
        }

        SimpleDateFormat sdf2 = new SimpleDateFormat("MM/dd/yyyy HH:mm");
        BatchDataOperationUtil.EntityAttributesProcess conceptionEntityAttributesProcess2 = new BatchDataOperationUtil.EntityAttributesProcess(){
            @Override
            public void doEntityAttributesProcess(Map<String, Object> entityValueMap) {
                if(entityValueMap != null && entityValueMap.containsKey("Current Police Districts 2 2")){
                    String locationPoint = entityValueMap.get("Current Police Districts 2 2").toString();
                    entityValueMap.put(Constant._GeospatialGLGeometryContent,locationPoint);
                    entityValueMap.put(Constant._GeospatialGeometryType,"POINT");
                    entityValueMap.put(Constant._GeospatialGlobalCRSAID,"EPSG:4326");
                }
                if(entityValueMap != null && entityValueMap.containsKey("Time") && entityValueMap.containsKey("PdDistrict")){
                    String dateString = entityValueMap.get("Time")+" "+entityValueMap.get("PdDistrict");
                    Date date = null;
                    try {
                        date = sdf2.parse(dateString);
                        entityValueMap.put("incidentDate",date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        if(loadHugeDataset){
            BatchDataOperationUtil.importConceptionEntitiesFromExternalCSV("realmExampleData/police_department_incident_reports/Police_Department_Incident_Reports__Historical_2003_to_May_2018_huge.csv","PoliceDepartmentIncidentReport",conceptionEntityAttributesProcess2);
        }else{
            BatchDataOperationUtil.importConceptionEntitiesFromExternalCSV("realmExampleData/police_department_incident_reports/Police_Department_Incident_Reports__Historical_2003_to_May_2018.csv","PoliceDepartmentIncidentReport",conceptionEntityAttributesProcess2);
        }
        linkDateAttribute("PoliceDepartmentIncidentReport","incidentDate","Incident occurred at",null,TimeFlow.TimeScaleGrade.MINUTE);
        linkDateAttribute("PoliceDepartmentIncidentReport","reportDate","Incident reported at",null,TimeFlow.TimeScaleGrade.MINUTE);
    }

    private static void generateFireIncidentsData() throws EngineServiceRuntimeException, EngineServiceEntityExploreException {
        //initConceptionKind("FireIncidentRecord","????????????");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        GeospatialOperationUtil.EntityAttributesProcess conceptionEntityAttributesProcess = new GeospatialOperationUtil.EntityAttributesProcess(){
            @Override
            public void doEntityAttributesProcess(Map<String, Object> entityValueMap) {
                if(entityValueMap.containsKey("alarm_dttm") && !entityValueMap.get("alarm_dttm").toString().equals("")){
                    Date date = null;
                    try {
                        date = sdf.parse(entityValueMap.get("alarm_dttm").toString());
                        entityValueMap.put("alarmDate",date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                if(entityValueMap.containsKey("arrival_dt") && !entityValueMap.get("arrival_dt").toString().equals("")){
                    Date date = null;
                    try {
                        date = sdf.parse(entityValueMap.get("arrival_dt").toString());
                        entityValueMap.put("arrivalDate",date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                if(entityValueMap.containsKey("close_dttm") && !entityValueMap.get("close_dttm").toString().equals("")){
                    Date date = null;
                    try {
                        date = sdf.parse(entityValueMap.get("close_dttm").toString());
                        entityValueMap.put("closeDate",date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        try {
            GeospatialOperationUtil.importSHPDataDirectlyToConceptionKind("FireIncidentRecord",true,
                    new File("/home/wangychu/Desktop/timeAndGeo/fire_incidents/geo_export_9ada77e3-70c4-4b65-a9aa-c1f02359540f.shp"),null,conceptionEntityAttributesProcess);

            linkDateAttribute("FireIncidentRecord","alarmDate","Incident alarmed at",null,TimeFlow.TimeScaleGrade.MINUTE);
            linkDateAttribute("FireIncidentRecord","arrivalDate","Rescue arrived at",null,TimeFlow.TimeScaleGrade.MINUTE);
            linkDateAttribute("FireIncidentRecord","closeDate","Incident closed at",null,TimeFlow.TimeScaleGrade.MINUTE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void initConceptionKind(String conceptionKindName,String conceptionKindDesc) throws EngineServiceRuntimeException {
        Engine coreRealm = EngineFactory.getDefaultEngine();

        Type targetType = coreRealm.getType(conceptionKindName);
        if(targetType != null){
            coreRealm.removeType(targetType.getConceptionKindName(),true);
        }
        targetType = coreRealm.getType(conceptionKindName);
        if(targetType == null){
            targetType = coreRealm.createType(conceptionKindName,conceptionKindDesc);
        }
    }

    private static void linkDateAttribute(String conceptionKindName,String dateAttributeName,String eventComment,Map<String,Object> globalEventData,TimeFlow.TimeScaleGrade timeScaleGrade) throws EngineServiceRuntimeException, EngineServiceEntityExploreException {
        Engine coreRealm = EngineFactory.getDefaultEngine();
        Type type = coreRealm.getType(conceptionKindName);
        QueryParameters queryParameters = new QueryParameters();
        queryParameters.setResultNumber(10000000);
        List<String> attributeNamesList = new ArrayList<>();
        attributeNamesList.add(dateAttributeName);
        EntitiesAttributesRetrieveResult conceptionEntitiesAttributeResult =  type.getSingleValueEntityAttributesByAttributeNames(attributeNamesList,queryParameters);
        List<EntityValue> entityValueList = conceptionEntitiesAttributeResult.getEntityValues();
        BatchDataOperationUtil.batchAttachTimeScaleEvents(entityValueList,dateAttributeName,eventComment,globalEventData, timeScaleGrade, BatchDataOperationUtil.CPUUsageRate.High);
    }
}