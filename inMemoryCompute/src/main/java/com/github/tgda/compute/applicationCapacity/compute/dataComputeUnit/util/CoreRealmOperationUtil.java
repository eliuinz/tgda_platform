package com.github.tgda.compute.applicationCapacity.compute.dataComputeUnit.util;

import com.github.tgda.engine.core.payload.EntityValue;
import com.github.tgda.engine.core.payload.RelationshipEntityValue;
import com.google.common.collect.Lists;
import com.github.tgda.engine.core.analysis.query.QueryParameters;
import com.github.tgda.engine.core.exception.EngineServiceEntityExploreException;
import com.github.tgda.engine.core.internal.neo4j.CypherBuilder;
import com.github.tgda.engine.core.internal.neo4j.GraphOperationExecutor;
import com.github.tgda.engine.core.internal.neo4j.dataTransformer.GetListEntityValueTransformer;
import com.github.tgda.engine.core.payload.EntitiesAttributesRetrieveResult;
import com.github.tgda.engine.core.payload.RelationEntitiesAttributesRetrieveResult;
import com.github.tgda.engine.core.payload.spi.common.payloadImpl.CommonEntitiesAttributesRetrieveResultImpl;
import com.github.tgda.coreRealm.realmServiceCore.term.*;
import com.github.tgda.engine.core.util.factory.EngineFactory;
import com.github.tgda.compute.applicationCapacity.compute.dataComputeUnit.dataService.DataServiceInvoker;
import com.github.tgda.compute.applicationCapacity.compute.dataComputeUnit.dataService.DataSlice;
import com.github.tgda.compute.applicationCapacity.compute.dataComputeUnit.dataService.DataSliceMetaInfo;
import com.github.tgda.compute.applicationCapacity.compute.dataComputeUnit.dataService.DataSlicePropertyType;
import com.github.tgda.compute.applicationCapacity.compute.dataComputeUnit.dataService.result.DataSliceOperationResult;
import com.github.tgda.compute.applicationCapacity.compute.exception.ComputeGridNotActiveException;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CoreRealmOperationUtil {

    public final static String RealmGlobalUID = "RealmGlobalUID";
    public final static String RelationFromEntityUID = "RelationFromEntityUID";
    public final static String RelationToEntityUID = "RelationToEntityUID";
    public final static String defaultSliceGroup = "DefaultSliceGroup";
    private final static int defaultResultNumber = 100000000;

    public static DataSliceOperationResult syncConceptionKindToDataSlice(String conceptionKindName,String dataSliceName,String dataSliceGroup,Map<String, DataSlicePropertyType> dataSlicePropertyMap,QueryParameters queryParameters){

        String dataSliceRealName = dataSliceName != null ? dataSliceName : conceptionKindName;
        String dataSliceRealGroup = dataSliceGroup != null ? dataSliceGroup : defaultSliceGroup;
        List<String> conceptionKindPropertiesList = new ArrayList<>();

        if(dataSlicePropertyMap != null && dataSlicePropertyMap.size() >0){

            Set<String> propertiesNameSet = dataSlicePropertyMap.keySet();
            conceptionKindPropertiesList.addAll(propertiesNameSet);
            dataSlicePropertyMap.put(CoreRealmOperationUtil.RealmGlobalUID,DataSlicePropertyType.STRING);

            // use this logic to avoid create ready exist ignite nodes has same name
            IgniteConfiguration igniteConfiguration= new IgniteConfiguration();
            igniteConfiguration.setClientMode(true);
            igniteConfiguration.setIgniteInstanceName("DataSliceCreateThread_"+new Date().getTime());
            Ignite invokerIgnite =Ignition.start(igniteConfiguration);
            try(DataServiceInvoker dataServiceInvoker = DataServiceInvoker.getInvokerInstance(invokerIgnite)){
                DataSlice targetDataSlice = dataServiceInvoker.getDataSlice(dataSliceRealName);
                if(targetDataSlice == null){
                    List<String> pkList = new ArrayList<>();
                    pkList.add(CoreRealmOperationUtil.RealmGlobalUID);
                    dataServiceInvoker.createGridDataSlice(dataSliceRealName,dataSliceRealGroup,dataSlicePropertyMap,pkList);
                }
            } catch (ComputeGridNotActiveException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(conceptionKindPropertiesList.size() > 0){
                QueryParameters executedQueryParameters;
                if(queryParameters != null){
                    executedQueryParameters = queryParameters;
                }else{
                    executedQueryParameters = new QueryParameters();
                    executedQueryParameters.setResultNumber(defaultResultNumber);
                }
                int processorNumber = Runtime.getRuntime().availableProcessors();
                int degreeOfParallelism = (processorNumber/2) >=4 ? (processorNumber/2) : 4;
                return loadConceptionKindEntitiesToDataSlice(conceptionKindName,conceptionKindPropertiesList,executedQueryParameters,dataSliceRealName,true,degreeOfParallelism);
            }else{
                return null;
            }
        }else{
            return null;
        }
    }

    public static DataSliceOperationResult refreshDataSliceAndLoadDataFromConceptionKind(String dataSliceGroupName,String dataSliceName,Map<String, DataSlicePropertyType> dataSlicePropertyMap,String conceptionKindName,QueryParameters queryParameters,int degreeOfParallelism){
        if(dataSlicePropertyMap == null ||dataSlicePropertyMap.size() == 0){
            return null;
        }
        Set<String> propertyNameSet = dataSlicePropertyMap.keySet();
        List<String> conceptionKindPropertiesList = new ArrayList<>();
        conceptionKindPropertiesList.addAll(propertyNameSet);

        try(DataServiceInvoker dataServiceInvoker = DataServiceInvoker.getInvokerInstance()){
            DataSlice targetDataSlice = dataServiceInvoker.getDataSlice(dataSliceName);
            if(targetDataSlice != null){
                dataServiceInvoker.eraseDataSlice(dataSliceName);
            }
            dataSlicePropertyMap.put(CoreRealmOperationUtil.RealmGlobalUID,DataSlicePropertyType.STRING);
            if(targetDataSlice == null){
                List<String> pkList = new ArrayList<>();
                pkList.add(CoreRealmOperationUtil.RealmGlobalUID);
                dataServiceInvoker.createGridDataSlice(dataSliceName,dataSliceGroupName+"_CONCEPTION",dataSlicePropertyMap,pkList);
            }
        } catch (ComputeGridNotActiveException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return loadConceptionKindEntitiesToDataSlice(conceptionKindName,conceptionKindPropertiesList,queryParameters,dataSliceName,true,degreeOfParallelism);
    }

    public static DataSliceOperationResult loadConceptionKindEntitiesToDataSlice(String conceptionKindName, List<String> attributeNamesList,QueryParameters queryParameters, String dataSliceName,boolean useEntityUIDAsPK,int degreeOfParallelism) {
        DataSliceOperationResult dataSliceOperationResult = new DataSliceOperationResult();

        CoreRealm coreRealm = EngineFactory.getDefaultEngine();
        ConceptionKind targetConceptionKind = coreRealm.getConceptionKind(conceptionKindName);
        int totalResultConceptionEntitiesCount = 0;

        try {
            EntitiesAttributesRetrieveResult conceptionEntitiesAttributeResult =  targetConceptionKind.getSingleValueEntityAttributesByAttributeNames(attributeNamesList,queryParameters);
            List<EntityValue> entityValueList = conceptionEntitiesAttributeResult.getEntityValues();
            totalResultConceptionEntitiesCount = entityValueList.size();

            int singlePartitionSize = (entityValueList.size()/degreeOfParallelism)+1;
            List<List<EntityValue>> rsList = Lists.partition(entityValueList, singlePartitionSize);

            if(useEntityUIDAsPK){
                attributeNamesList.add(RealmGlobalUID);
            }

            ExecutorService executor = Executors.newFixedThreadPool(rsList.size());
            for(int i = 0;i < rsList.size(); i++){
                List<EntityValue> currentEntityValueList = rsList.get(i);
                DataSliceInsertDataThread dataSliceInsertDataThread = new DataSliceInsertDataThread(i,dataSliceName,attributeNamesList, currentEntityValueList,useEntityUIDAsPK);
                executor.execute(dataSliceInsertDataThread);
            }
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE,TimeUnit.NANOSECONDS);
        } catch (EngineServiceEntityExploreException | InterruptedException e) {
            e.printStackTrace();
        }

        // use this logic to avoid create ready exist ignite nodes has same name
        IgniteConfiguration igniteConfiguration= new IgniteConfiguration();
        igniteConfiguration.setClientMode(true);
        igniteConfiguration.setIgniteInstanceName("DataSliceConfirmThread_"+new Date().getTime());
        Ignite invokerIgnite =Ignition.start(igniteConfiguration);
        try(DataServiceInvoker dataServiceInvoker = DataServiceInvoker.getInvokerInstance(invokerIgnite)){
            DataSlice targetDataSlice = dataServiceInvoker.getDataSlice(dataSliceName);
            DataSliceMetaInfo dataSliceMetaInfo = targetDataSlice.getDataSliceMetaInfo();
            int successDataCount = dataSliceMetaInfo.getPrimaryDataCount() + dataSliceMetaInfo.getBackupDataCount();
            dataSliceOperationResult.setSuccessItemsCount(successDataCount);
            dataSliceOperationResult.setFailItemsCount(totalResultConceptionEntitiesCount-successDataCount);
        } catch (ComputeGridNotActiveException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        dataSliceOperationResult.finishOperation();
        dataSliceOperationResult.setOperationSummary("Load ConceptionKind Entities To DataSlice Operation");
        return dataSliceOperationResult;
    }

    public static DataSliceOperationResult loadConceptionKindEntitiesToDataSlice(DataServiceInvoker dataServiceInvoker,String conceptionKindName, List<String> attributeNamesList,QueryParameters queryParameters, String dataSliceName,boolean useEntityUIDAsPK,int degreeOfParallelism) {
        DataSliceOperationResult dataSliceOperationResult = new DataSliceOperationResult();

        CoreRealm coreRealm = EngineFactory.getDefaultEngine();
        ConceptionKind targetConceptionKind = coreRealm.getConceptionKind(conceptionKindName);
        if(targetConceptionKind == null){
            return null;
        }
        int totalResultConceptionEntitiesCount = 0;

        try {
            EntitiesAttributesRetrieveResult conceptionEntitiesAttributeResult =  targetConceptionKind.getSingleValueEntityAttributesByAttributeNames(attributeNamesList,queryParameters);
            List<EntityValue> entityValueList = conceptionEntitiesAttributeResult.getEntityValues();
            totalResultConceptionEntitiesCount = entityValueList.size();

            int singlePartitionSize = (entityValueList.size()/degreeOfParallelism)+1;
            List<List<EntityValue>> rsList = Lists.partition(entityValueList, singlePartitionSize);

            if(useEntityUIDAsPK){
                attributeNamesList.add(RealmGlobalUID);
            }

            ExecutorService executor = Executors.newFixedThreadPool(rsList.size());
            for(int i = 0;i < rsList.size(); i++){
                List<EntityValue> currentEntityValueList = rsList.get(i);
                DataSliceInsertDataThread dataSliceInsertDataThread = new DataSliceInsertDataThread(i,dataSliceName,attributeNamesList, currentEntityValueList,useEntityUIDAsPK);
                executor.execute(dataSliceInsertDataThread);
            }
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE,TimeUnit.NANOSECONDS);
        } catch (EngineServiceEntityExploreException | InterruptedException e) {
            e.printStackTrace();
        }

        DataSlice targetDataSlice = dataServiceInvoker.getDataSlice(dataSliceName);
        DataSliceMetaInfo dataSliceMetaInfo = targetDataSlice.getDataSliceMetaInfo();
        int successDataCount = dataSliceMetaInfo.getPrimaryDataCount() + dataSliceMetaInfo.getBackupDataCount();
        dataSliceOperationResult.setSuccessItemsCount(successDataCount);
        dataSliceOperationResult.setFailItemsCount(totalResultConceptionEntitiesCount-successDataCount);

        dataSliceOperationResult.finishOperation();
        dataSliceOperationResult.setOperationSummary("Load ConceptionKind Entities To DataSlice Operation");
        return dataSliceOperationResult;
    }

    public static DataSliceOperationResult syncInnerDataKindEntitiesToDataSlice(DataServiceInvoker dataServiceInvoker,
                                                                                String innerDataKindName,String dataSliceGroup,
                                                                                List<AttributeKind> containsAttributesKinds,
                                                                                QueryParameters queryParameters, String dataSliceName,boolean useEntityUIDAsPK,int degreeOfParallelism) {
        DataSliceOperationResult dataSliceOperationResult = new DataSliceOperationResult();
        int totalResultConceptionEntitiesCount = 0;

        Map<String, DataSlicePropertyType> dataSlicePropertyMap = new HashMap<>();
        dataSlicePropertyMap.put(CoreRealmOperationUtil.RealmGlobalUID,DataSlicePropertyType.STRING);
        List<String> attributeNamesList = new ArrayList<>();

        if(containsAttributesKinds != null && containsAttributesKinds.size() >0){
            for(AttributeKind currentAttributeKind :containsAttributesKinds){
                String currentAttributeKindName = currentAttributeKind.getAttributeKindName();
                AttributeDataType currentAttributeDataType = currentAttributeKind.getAttributeDataType();
                switch (currentAttributeDataType){
                    case DECIMAL:
                        dataSlicePropertyMap.put(currentAttributeKindName,DataSlicePropertyType.DECIMAL);
                        break;
                    case BOOLEAN:
                        dataSlicePropertyMap.put(currentAttributeKindName,DataSlicePropertyType.BOOLEAN);
                        break;
                    case STRING:
                        dataSlicePropertyMap.put(currentAttributeKindName,DataSlicePropertyType.STRING);
                        break;
                    case DOUBLE:
                        dataSlicePropertyMap.put(currentAttributeKindName,DataSlicePropertyType.DOUBLE);
                        break;
                    case BINARY:
                        dataSlicePropertyMap.put(currentAttributeKindName,DataSlicePropertyType.BINARY);
                        break;
                    case SHORT:
                        dataSlicePropertyMap.put(currentAttributeKindName,DataSlicePropertyType.SHORT);
                        break;
                    case FLOAT:
                        dataSlicePropertyMap.put(currentAttributeKindName,DataSlicePropertyType.FLOAT);
                        break;
                    case TIMESTAMP:
                        dataSlicePropertyMap.put(currentAttributeKindName,DataSlicePropertyType.TIMESTAMP);
                        break;
                    case LONG:
                        dataSlicePropertyMap.put(currentAttributeKindName,DataSlicePropertyType.LONG);
                        break;
                    case BYTE:
                        dataSlicePropertyMap.put(currentAttributeKindName,DataSlicePropertyType.BYTE);
                        break;
                    case INT:
                        dataSlicePropertyMap.put(currentAttributeKindName,DataSlicePropertyType.INT);
                        break;
                    case DATE:
                        dataSlicePropertyMap.put(currentAttributeKindName,DataSlicePropertyType.DATE);
                        break;
                    case TIME:
                        dataSlicePropertyMap.put(currentAttributeKindName,DataSlicePropertyType.TIME);
                        break;
                    case DATETIME:
                        dataSlicePropertyMap.put(currentAttributeKindName,DataSlicePropertyType.TIMESTAMP);
                        break;
                }
                attributeNamesList.add(currentAttributeKindName);
            }
        }

        // use this logic to avoid create ready exist ignite nodes has same name
        IgniteConfiguration igniteConfiguration= new IgniteConfiguration();
        igniteConfiguration.setClientMode(true);
        igniteConfiguration.setIgniteInstanceName("DataSliceCreateThread_"+new Date().getTime());
        Ignite invokerIgnite =Ignition.start(igniteConfiguration);
        try(DataServiceInvoker dataServiceInvokerForCreate = DataServiceInvoker.getInvokerInstance(invokerIgnite)){
            DataSlice targetDataSlice = dataServiceInvokerForCreate.getDataSlice(dataSliceName);
            if(targetDataSlice == null){
                List<String> pkList = new ArrayList<>();
                pkList.add(CoreRealmOperationUtil.RealmGlobalUID);
                dataServiceInvokerForCreate.createGridDataSlice(dataSliceName,dataSliceGroup,dataSlicePropertyMap,pkList);
            }
        } catch (ComputeGridNotActiveException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try{
            CommonEntitiesAttributesRetrieveResultImpl commonConceptionEntitiesAttributesRetrieveResultImpl
                    = new CommonEntitiesAttributesRetrieveResultImpl();
            commonConceptionEntitiesAttributesRetrieveResultImpl.getOperationStatistics().setQueryParameters(queryParameters);
            GraphOperationExecutor workingGraphOperationExecutor = new GraphOperationExecutor();
            try {
                String queryCql = CypherBuilder.matchAttributesWithQueryParameters(innerDataKindName,queryParameters,attributeNamesList);
                GetListEntityValueTransformer getListEntityValueTransformer =
                        new GetListEntityValueTransformer(attributeNamesList,containsAttributesKinds);
                Object resEntityRes = workingGraphOperationExecutor.executeRead(getListEntityValueTransformer, queryCql);
                if(resEntityRes != null){
                    List<EntityValue> resultEntitiesValues = (List<EntityValue>)resEntityRes;
                    commonConceptionEntitiesAttributesRetrieveResultImpl.addConceptionEntitiesAttributes(resultEntitiesValues);
                    commonConceptionEntitiesAttributesRetrieveResultImpl.getOperationStatistics().setResultEntitiesCount(resultEntitiesValues.size());
                }
            }finally {
                workingGraphOperationExecutor.close();
            }
            commonConceptionEntitiesAttributesRetrieveResultImpl.finishEntitiesRetrieving();
            EntitiesAttributesRetrieveResult conceptionEntitiesAttributeResult = commonConceptionEntitiesAttributesRetrieveResultImpl;
            List<EntityValue> entityValueList = conceptionEntitiesAttributeResult.getEntityValues();
            totalResultConceptionEntitiesCount = entityValueList.size();

            int singlePartitionSize = (entityValueList.size()/degreeOfParallelism)+1;
            List<List<EntityValue>> rsList = Lists.partition(entityValueList, singlePartitionSize);

            if(useEntityUIDAsPK){
                attributeNamesList.add(RealmGlobalUID);
            }

            ExecutorService executor = Executors.newFixedThreadPool(rsList.size());
            for(int i = 0;i < rsList.size(); i++){
                List<EntityValue> currentEntityValueList = rsList.get(i);
                DataSliceInsertDataThread dataSliceInsertDataThread = new DataSliceInsertDataThread(i,dataSliceName,attributeNamesList, currentEntityValueList,useEntityUIDAsPK);
                executor.execute(dataSliceInsertDataThread);
            }
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE,TimeUnit.NANOSECONDS);

        } catch (EngineServiceEntityExploreException | InterruptedException e) {
            e.printStackTrace();
        }

        DataSlice targetDataSlice = dataServiceInvoker.getDataSlice(dataSliceName);
        DataSliceMetaInfo dataSliceMetaInfo = targetDataSlice.getDataSliceMetaInfo();
        int successDataCount = dataSliceMetaInfo.getPrimaryDataCount() + dataSliceMetaInfo.getBackupDataCount();
        dataSliceOperationResult.setSuccessItemsCount(successDataCount);
        dataSliceOperationResult.setFailItemsCount(totalResultConceptionEntitiesCount-successDataCount);
        dataSliceOperationResult.finishOperation();
        dataSliceOperationResult.setOperationSummary("Sync Inner DataKind Entities To DataSlice Operation");
        return dataSliceOperationResult;
    }

    public static DataSliceOperationResult loadInnerDataKindEntitiesToDataSlice(DataServiceInvoker dataServiceInvoker,String innerDataKindName, List<AttributeKind> containsAttributesKinds,QueryParameters queryParameters, String dataSliceName,boolean useEntityUIDAsPK,int degreeOfParallelism) {
        DataSliceOperationResult dataSliceOperationResult = new DataSliceOperationResult();
        int totalResultConceptionEntitiesCount = 0;
        try {
            List<String> attributeNamesList = new ArrayList<>();
            for(AttributeKind currentAttributeKind : containsAttributesKinds){
                attributeNamesList.add(currentAttributeKind.getAttributeKindName());
            }

            CommonEntitiesAttributesRetrieveResultImpl commonConceptionEntitiesAttributesRetrieveResultImpl
                    = new CommonEntitiesAttributesRetrieveResultImpl();
            commonConceptionEntitiesAttributesRetrieveResultImpl.getOperationStatistics().setQueryParameters(queryParameters);
            GraphOperationExecutor workingGraphOperationExecutor = new GraphOperationExecutor();
            try {
                String queryCql = CypherBuilder.matchAttributesWithQueryParameters(innerDataKindName,queryParameters,attributeNamesList);
                GetListEntityValueTransformer getListEntityValueTransformer =
                        new GetListEntityValueTransformer(attributeNamesList,containsAttributesKinds);
                Object resEntityRes = workingGraphOperationExecutor.executeRead(getListEntityValueTransformer, queryCql);
                if(resEntityRes != null){
                    List<EntityValue> resultEntitiesValues = (List<EntityValue>)resEntityRes;
                    commonConceptionEntitiesAttributesRetrieveResultImpl.addConceptionEntitiesAttributes(resultEntitiesValues);
                    commonConceptionEntitiesAttributesRetrieveResultImpl.getOperationStatistics().setResultEntitiesCount(resultEntitiesValues.size());
                }
            }finally {
                workingGraphOperationExecutor.close();
            }
            commonConceptionEntitiesAttributesRetrieveResultImpl.finishEntitiesRetrieving();
            EntitiesAttributesRetrieveResult conceptionEntitiesAttributeResult = commonConceptionEntitiesAttributesRetrieveResultImpl;
            List<EntityValue> entityValueList = conceptionEntitiesAttributeResult.getEntityValues();
            totalResultConceptionEntitiesCount = entityValueList.size();

            int singlePartitionSize = (entityValueList.size()/degreeOfParallelism)+1;
            List<List<EntityValue>> rsList = Lists.partition(entityValueList, singlePartitionSize);

            if(useEntityUIDAsPK){
                attributeNamesList.add(RealmGlobalUID);
            }

            ExecutorService executor = Executors.newFixedThreadPool(rsList.size());
            for(int i = 0;i < rsList.size(); i++){
                List<EntityValue> currentEntityValueList = rsList.get(i);
                DataSliceInsertDataThread dataSliceInsertDataThread = new DataSliceInsertDataThread(i,dataSliceName,attributeNamesList, currentEntityValueList,useEntityUIDAsPK);
                executor.execute(dataSliceInsertDataThread);
            }
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE,TimeUnit.NANOSECONDS);
        } catch (EngineServiceEntityExploreException | InterruptedException e) {
            e.printStackTrace();
        }

        DataSlice targetDataSlice = dataServiceInvoker.getDataSlice(dataSliceName);
        DataSliceMetaInfo dataSliceMetaInfo = targetDataSlice.getDataSliceMetaInfo();
        int successDataCount = dataSliceMetaInfo.getPrimaryDataCount() + dataSliceMetaInfo.getBackupDataCount();
        dataSliceOperationResult.setSuccessItemsCount(successDataCount);
        dataSliceOperationResult.setFailItemsCount(totalResultConceptionEntitiesCount-successDataCount);

        dataSliceOperationResult.finishOperation();
        dataSliceOperationResult.setOperationSummary("Load ConceptionKind Entities To DataSlice Operation");
        return dataSliceOperationResult;
    }

    public static DataSliceOperationResult loadRelationKindEntitiesToDataSlice(DataServiceInvoker dataServiceInvoker,String relationKindName, List<String> attributeNamesList,QueryParameters queryParameters, String dataSliceName,boolean useEntityUIDAsPK,int degreeOfParallelism) {
        DataSliceOperationResult dataSliceOperationResult = new DataSliceOperationResult();

        CoreRealm coreRealm = EngineFactory.getDefaultEngine();
        RelationKind targetRelationKind = coreRealm.getRelationKind(relationKindName);
        if(targetRelationKind == null){
            return null;
        }
        int totalResultConceptionEntitiesCount = 0;
        try {
            RelationEntitiesAttributesRetrieveResult relationEntitiesAttributesRetrieveResult = targetRelationKind.getEntityAttributesByAttributeNames(attributeNamesList,queryParameters);
            List<RelationshipEntityValue> conceptionEntityValueList = relationEntitiesAttributesRetrieveResult.getRelationshipEntityValues();
            totalResultConceptionEntitiesCount = conceptionEntityValueList.size();

            int singlePartitionSize = (conceptionEntityValueList.size()/degreeOfParallelism)+1;
            List<List<RelationshipEntityValue>> rsList = Lists.partition(conceptionEntityValueList, singlePartitionSize);

            if(useEntityUIDAsPK){
                attributeNamesList.add(RealmGlobalUID);
            }

            ExecutorService executor = Executors.newFixedThreadPool(rsList.size());
            for(int i = 0;i < rsList.size(); i++){
                List<RelationshipEntityValue> currentEntityValueList = rsList.get(i);
                DataSliceInsertRelationThread dataSliceInsertRelationThread = new DataSliceInsertRelationThread(i,dataSliceName,attributeNamesList,currentEntityValueList,useEntityUIDAsPK);
                executor.execute(dataSliceInsertRelationThread);
            }
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE,TimeUnit.NANOSECONDS);
        } catch (EngineServiceEntityExploreException | InterruptedException e) {
            e.printStackTrace();
        }

        DataSlice targetDataSlice = dataServiceInvoker.getDataSlice(dataSliceName);
        DataSliceMetaInfo dataSliceMetaInfo = targetDataSlice.getDataSliceMetaInfo();
        int successDataCount = dataSliceMetaInfo.getPrimaryDataCount() + dataSliceMetaInfo.getBackupDataCount();
        dataSliceOperationResult.setSuccessItemsCount(successDataCount);
        dataSliceOperationResult.setFailItemsCount(totalResultConceptionEntitiesCount-successDataCount);

        dataSliceOperationResult.finishOperation();
        dataSliceOperationResult.setOperationSummary("Load RelationKind Entities To DataSlice Operation");
        return dataSliceOperationResult;
    }

    private static class DataSliceInsertDataThread implements Runnable{
        private String dataSliceName;
        private List<String> sliceDataProperties;
        private List<EntityValue> sliceDataRows;
        private boolean useEntityUIDAsPK;
        private int threadId;

        public DataSliceInsertDataThread(int threadId, String dataSliceName, List<String> sliceDataProperties, List<EntityValue> sliceDataRows, boolean useEntityUIDAsPK){
            this.dataSliceName = dataSliceName;
            this.sliceDataProperties = sliceDataProperties;
            this.sliceDataRows = sliceDataRows;
            this.useEntityUIDAsPK = useEntityUIDAsPK;
            this.threadId = threadId;
        }

        @Override
        public void run() {
            List<Map<String,Object>> sliceDataRowsDataList = new ArrayList<>();
            for(EntityValue currentEntityValue :this.sliceDataRows){
                Map<String,Object> currentDataMap = currentEntityValue.getEntityAttributesValue();
                if(useEntityUIDAsPK){
                    currentDataMap.put(RealmGlobalUID, currentEntityValue.getEntityUID());
                }
                sliceDataRowsDataList.add(currentDataMap);
            }

            IgniteConfiguration igniteConfiguration= new IgniteConfiguration();
            igniteConfiguration.setClientMode(true);
            igniteConfiguration.setIgniteInstanceName("DataSliceInsertDataThread_"+threadId);
            Ignite invokerIgnite =Ignition.start(igniteConfiguration);

            try(DataServiceInvoker dataServiceInvoker = DataServiceInvoker.getInvokerInstance(invokerIgnite)){
                DataSlice targetDataSlice = dataServiceInvoker.getDataSlice(this.dataSliceName);
                DataSliceOperationResult dataSliceOperationResult = targetDataSlice.addDataRecords(this.sliceDataProperties,sliceDataRowsDataList);
                System.out.println("--------------------------------------");
                System.out.println("Execution result of : "+"DataSliceInsertDataThread_"+threadId);
                System.out.println(dataSliceOperationResult.getOperationSummary());
                System.out.println(dataSliceOperationResult.getStartTime());
                System.out.println(dataSliceOperationResult.getFinishTime());
                System.out.println(dataSliceOperationResult.getSuccessItemsCount());
                System.out.println(dataSliceOperationResult.getFailItemsCount());
                System.out.println("--------------------------------------");
            } catch (ComputeGridNotActiveException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class DataSliceInsertRelationThread implements Runnable{
        private String dataSliceName;
        private List<String> sliceDataProperties;
        private List<RelationshipEntityValue> sliceDataRows;
        private boolean useRelationshipEntityUIDAsPK;
        private int threadId;

        public DataSliceInsertRelationThread(int threadId, String dataSliceName, List<String> sliceDataProperties, List<RelationshipEntityValue> sliceDataRows, boolean useRelationshipEntityUIDAsPK){
            this.dataSliceName = dataSliceName;
            this.sliceDataProperties = sliceDataProperties;
            this.sliceDataRows = sliceDataRows;
            this.useRelationshipEntityUIDAsPK = useRelationshipEntityUIDAsPK;
            this.threadId = threadId;
        }

        @Override
        public void run() {
            List<Map<String,Object>> sliceDataRowsDataList = new ArrayList<>();
            for(RelationshipEntityValue currentRelationshipEntityValue :this.sliceDataRows){
                Map<String,Object> currentDataMap = currentRelationshipEntityValue.getEntityAttributesValue();
                if(useRelationshipEntityUIDAsPK){
                    currentDataMap.put(RealmGlobalUID, currentRelationshipEntityValue.getRelationshipEntityUID());
                }
                currentDataMap.put(RelationFromEntityUID, currentRelationshipEntityValue.getFromEntityUID());
                currentDataMap.put(RelationToEntityUID, currentRelationshipEntityValue.getToEntityUID());
                sliceDataRowsDataList.add(currentDataMap);
            }

            IgniteConfiguration igniteConfiguration= new IgniteConfiguration();
            igniteConfiguration.setClientMode(true);
            igniteConfiguration.setIgniteInstanceName("DataSliceInsertRelationThread_"+threadId);
            Ignite invokerIgnite =Ignition.start(igniteConfiguration);

            try(DataServiceInvoker dataServiceInvoker = DataServiceInvoker.getInvokerInstance(invokerIgnite)){
                DataSlice targetDataSlice = dataServiceInvoker.getDataSlice(this.dataSliceName);
                DataSliceOperationResult dataSliceOperationResult = targetDataSlice.addDataRecords(this.sliceDataProperties,sliceDataRowsDataList);
                System.out.println("--------------------------------------");
                System.out.println("Execution result of : "+"DataSliceInsertRelationThread_"+threadId);
                System.out.println(dataSliceOperationResult.getOperationSummary());
                System.out.println(dataSliceOperationResult.getStartTime());
                System.out.println(dataSliceOperationResult.getFinishTime());
                System.out.println(dataSliceOperationResult.getSuccessItemsCount());
                System.out.println(dataSliceOperationResult.getFailItemsCount());
                System.out.println("--------------------------------------");
            } catch (ComputeGridNotActiveException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
