package com.github.tgda.knowledgeManage.applicationCapacity.dataSlicesSynchronization;

import com.github.tgda.compute.applicationCapacity.compute.dataComputeUnit.dataService.DataServiceInvoker;
import com.github.tgda.compute.applicationCapacity.compute.exception.ComputeGridNotActiveException;
import com.github.tgda.knowledgeManage.applicationCapacity.dataSlicesSynchronization.commandProcessor.DataSlicesSyncCommandProcessorFactory;
import com.github.tgda.knowledgeManage.applicationCapacity.dataSlicesSynchronization.dataSlicesSync.DataSliceSyncUtil;
import com.github.tgda.knowledgeManage.applicationCapacity.dataSlicesSynchronization.dataSlicesSync.GeneralDataSliceEntityValueOperationsMessageHandler;
import com.github.tgda.knowledgeManage.applicationService.eventStreaming.kafka.exception.ConfigurationErrorException;
import com.github.tgda.knowledgeManage.applicationService.eventStreaming.kafka.exception.MessageHandleErrorException;
import com.github.tgda.knowledgeManage.applicationService.eventStreaming.kafka.receiver.CommonObjectsMessageReceiver;
import com.github.tgda.knowledgeManage.consoleApplication.feature.BaseApplication;
import com.github.tgda.knowledgeManage.consoleApplication.feature.BaseCommandProcessor;
import com.github.tgda.knowledgeManage.consoleApplication.util.ApplicationLauncherUtil;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataSlicesSynchronizationApplication implements BaseApplication {

    public static final String APPLICATION_START_TIME = "APPLICATION_START_TIME";
    public static final String SYNC_LISTENING_START_TIME = "SYNC_LISTENING_START_TIME";

    private ExecutorService executorService;
    private Map<Object,Object> commandContextDataMap;
    private CommonObjectsMessageReceiver commonObjectsMessageReceiver;
    private DataServiceInvoker dataServiceInvoker;

    @Override
    public boolean isDaemonApplication() {
        return true;
    }

    @Override
    public void executeDaemonLogic() {
        this.commandContextDataMap.put(SYNC_LISTENING_START_TIME,new Date());
        String syncGeospatialDataFlag = ApplicationLauncherUtil.getApplicationInfoPropertyValue("DataSlicesSynchronization.syncGeospatialData");
        if(Boolean.parseBoolean(syncGeospatialDataFlag)){
            DataSliceSyncUtil.syncGeospatialData(dataServiceInvoker);
        }

        String launchSyncAtStartupFlag = ApplicationLauncherUtil.getApplicationInfoPropertyValue("DataSlicesSynchronization.launchSyncAtStartup");
        if(Boolean.parseBoolean(launchSyncAtStartupFlag)){
            //Batch load data into data slices by per defined rules
            DataSliceSyncUtil.batchSyncPerDefinedDataSlices(dataServiceInvoker);
        }
        //Start real time data sync process
        GeneralDataSliceEntityValueOperationsMessageHandler generalDataSliceEntityValueOperationsMessageHandler = new GeneralDataSliceEntityValueOperationsMessageHandler(this.commandContextDataMap,this.dataServiceInvoker);
        try {
            commonObjectsMessageReceiver = new CommonObjectsMessageReceiver(generalDataSliceEntityValueOperationsMessageHandler);
        } catch (ConfigurationErrorException e) {
            e.printStackTrace();
        }

        executorService = Executors.newFixedThreadPool(1);
        executorService.submit(new Runnable() {
            public void run() {
                try {
                    String defaultMessageReceiverTopicName = ApplicationLauncherUtil.getApplicationInfoPropertyValue("DataSlicesSynchronization.MessageReceiver.defaultTopicName");
                    commonObjectsMessageReceiver.startMessageReceive(new String[]{defaultMessageReceiverTopicName});
                } catch (ConfigurationErrorException e) {
                    e.printStackTrace();
                } catch (MessageHandleErrorException e) {
                    e.printStackTrace();
                }
            }
        });

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ApplicationLauncherUtil.printApplicationConsoleBanner();
    }

    @Override
    public boolean initApplication() {
        this.commandContextDataMap = new ConcurrentHashMap<>();
        this.commandContextDataMap.put(APPLICATION_START_TIME,new Date());
        try {
            this.dataServiceInvoker = DataServiceInvoker.getInvokerInstance();
        } catch (ComputeGridNotActiveException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean shutdownApplication() {
        if(commonObjectsMessageReceiver != null){
            commonObjectsMessageReceiver.stopMessageReceive();
        }
        if(executorService != null){
            executorService.shutdown();
        }
        if(dataServiceInvoker != null){
            try {
                dataServiceInvoker.close();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    @Override
    public void executeConsoleCommand(String consoleCommand) {
        if(consoleCommand != null){
            String[] commandOptions = consoleCommand.split(" ");
            if(commandOptions.length>0){
                String command = commandOptions[0];
                if(command.startsWith("-")||command.startsWith("--")){
                    System.out.println("Please input valid command and options");
                }else{
                    String[] options = Arrays.copyOfRange(commandOptions,1,commandOptions.length);
                    BaseCommandProcessor commandProcessor = DataSlicesSyncCommandProcessorFactory.getCommandProcessor(command,this.commandContextDataMap);
                    if(commandProcessor!=null){
                        commandProcessor.processCommand(command,options);
                    }
                }
            }
        }
    }
}
