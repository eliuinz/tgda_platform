package com.github.tgda.engine.core.internal.neo4j.util;

import com.github.tgda.engine.core.internal.neo4j.GraphOperationExecutor;
import com.github.tgda.engine.core.internal.neo4j.dataTransformer.DataTransformer;
import com.github.tgda.engine.core.payload.TimeScaleMoment;
import org.neo4j.driver.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

public class TimeScaleOperationUtil {

    private static Logger logger = LoggerFactory.getLogger(TimeScaleOperationUtil.class);

    public static void generateTimeFlowScaleEntities(GraphOperationExecutor workingGraphOperationExecutor, String timeFlowName, int startYear, int endYear,boolean initMinuteData){
        generateTimeFlowScaleEntities_YMD(workingGraphOperationExecutor,timeFlowName,startYear,endYear);
        generateTimeFlowScaleEntities_Hour(workingGraphOperationExecutor,timeFlowName,startYear,endYear);
        if(initMinuteData){
            generateTimeFlowScaleEntities_Minute(workingGraphOperationExecutor,timeFlowName,startYear,endYear);
        }
        linkTimeFlowScaleEntities_Year(workingGraphOperationExecutor,timeFlowName,startYear-1,endYear+1);
        linkTimeFlowScaleEntities_Month(workingGraphOperationExecutor,timeFlowName,startYear-1,endYear+1);
        linkTimeFlowScaleEntities_Day(workingGraphOperationExecutor,timeFlowName,startYear-1,endYear+1);
        linkTimeFlowScaleEntities_Hour(workingGraphOperationExecutor,timeFlowName,startYear-1,endYear+1);
        if(initMinuteData){
            linkTimeFlowScaleEntities_Minute(workingGraphOperationExecutor,timeFlowName,startYear-1,endYear+1);
        }
    }

    public static void generateTimeFlowScaleEntities(GraphOperationExecutor workingGraphOperationExecutor, String timeFlowName, int targetYear,boolean initMinuteData){
        generateTimeFlowScaleEntities_YMD(workingGraphOperationExecutor,timeFlowName,targetYear);
        generateTimeFlowScaleEntities_Hour(workingGraphOperationExecutor,timeFlowName,targetYear);
        if(initMinuteData){
            generateTimeFlowScaleEntities_Minute(workingGraphOperationExecutor,timeFlowName,targetYear);
        }
        linkTimeFlowScaleEntities_Year(workingGraphOperationExecutor,timeFlowName,targetYear-1,targetYear+1);
        linkTimeFlowScaleEntities_Month(workingGraphOperationExecutor,timeFlowName,targetYear-1,targetYear+1);
        linkTimeFlowScaleEntities_Day(workingGraphOperationExecutor,timeFlowName,targetYear-1,targetYear+1);
        linkTimeFlowScaleEntities_Hour(workingGraphOperationExecutor,timeFlowName,targetYear-1,targetYear+1);
        if(initMinuteData){
            linkTimeFlowScaleEntities_Minute(workingGraphOperationExecutor,timeFlowName,targetYear-1,targetYear+1);
        }
    }

    private static void generateTimeFlowScaleEntities_YMD(GraphOperationExecutor workingGraphOperationExecutor, String timeFlowName, int startYear, int endYear){
        String createTimeFlowEntitiesCql = "WITH range("+startYear+", "+endYear+") AS years, range(1,12) as months\n" +
                "FOREACH(year IN years |\n" +
                "  MERGE (y:TGDA_TS_Year:TGDA_TimeScaleEntity {year:year,id:year,timeFlow:\""+timeFlowName+"\"})\n" +
                "  FOREACH(month IN months |\n" +
                "    CREATE (m:TGDA_TS_Month:TGDA_TimeScaleEntity {month: month,id:month,timeFlow:\""+timeFlowName+"\"})\n" +
                "    MERGE (y)-[:TGDA_TS_Contains]->(m)    \n" +
                "    FOREACH(month IN CASE WHEN month=1 THEN [1] ELSE [] END | \n" +
                "        MERGE (y)-[:TGDA_TS_FirstChildIs]->(m)\n" +
                "    )\n" +
                "    FOREACH(month IN CASE WHEN month=12 THEN [1] ELSE [] END | \n" +
                "        MERGE (y)-[:TGDA_TS_LastChildIs]->(m)\n" +
                "    )    \n" +
                "    FOREACH(day IN (CASE\n" +
                "                      WHEN month IN [1,3,5,7,8,10,12] THEN range(1,31) \n" +
                "                      WHEN month = 2 THEN\n" +
                "                        CASE\n" +
                "                          WHEN year % 4 <> 0 THEN range(1,28)\n" +
                "                          WHEN year % 100 <> 0 THEN range(1,29)\n" +
                "                          WHEN year % 400 = 0 THEN range(1,29)\n" +
                "                          ELSE range(1,28)\n" +
                "                        END\n" +
                "                      ELSE range(1,30)\n" +
                "                    END) |\n" +
                "        CREATE (d:TGDA_TS_Day:TGDA_TimeScaleEntity {day:day,id:day,timeFlow:\""+timeFlowName+"\"})\n" +
                "        MERGE (m)-[:TGDA_TS_Contains]->(d)        \n" +
                "        FOREACH(day IN CASE WHEN day=1 THEN [1] ELSE [] END | \n" +
                "            MERGE (m)-[:TGDA_TS_FirstChildIs]->(d)\n" +
                "        )        \n" +
                "        FOREACH(day IN CASE WHEN day=31 THEN [1] ELSE [] END | \n" +
                "            MERGE (m)-[:TGDA_TS_LastChildIs]->(d)\n" +
                "        )\n" +
                "        FOREACH(day IN CASE WHEN (day=30 and month IN [4,6,9,11]) THEN [1] ELSE [] END | \n" +
                "            MERGE (m)-[:TGDA_TS_LastChildIs]->(d)\n" +
                "        )\n" +
                "        FOREACH(day IN CASE WHEN (day=29 and month =2) THEN [1] ELSE [] END | \n" +
                "            MERGE (m)-[:TGDA_TS_LastChildIs]->(d)\n" +
                "        )\n" +
                "        FOREACH(day IN CASE WHEN (day=28 and month =2 and (year % 4 <> 0)) THEN [1] ELSE [] END | \n" +
                "            MERGE (m)-[:TGDA_TS_LastChildIs]->(d)\n" +
                "        )\n" +
                "    )\n" +
                "  )\n" +
                ")";

        logger.debug("Generated Cypher Statement: {}", createTimeFlowEntitiesCql);
        workingGraphOperationExecutor.executeWrite(getSilentOperationDataTransformer(),createTimeFlowEntitiesCql);
    }

    private static void generateTimeFlowScaleEntities_Hour(GraphOperationExecutor workingGraphOperationExecutor, String timeFlowName, int startYear, int endYear){
        String createTimeFlowEntitiesCql = "MATCH (year:TGDA_TS_Year{timeFlow:\""+timeFlowName+"\"})-[:TGDA_TS_Contains]->(month)-[:TGDA_TS_Contains]->(day:TGDA_TS_Day) WHERE year.year in range("+startYear+","+endYear+")\n"+
                "WITH range(0,23) as HOURS, day\n" +
                "FOREACH (hour in HOURS | \n" +
                "    MERGE (h:TGDA_TS_Hour:TGDA_TimeScaleEntity {hour:hour,id:hour,timeFlow:\""+timeFlowName+"\"})<-[:TGDA_TS_Contains]-(day)\n" +
                "    FOREACH(hour IN CASE WHEN hour=0 THEN [1] ELSE [] END | \n" +
                "        MERGE (h)<-[:TGDA_TS_FirstChildIs]-(day)\n" +
                "    )\n" +
                "    FOREACH(hour IN CASE WHEN hour=23 THEN [1] ELSE [] END | \n" +
                "        MERGE (h)<-[:TGDA_TS_LastChildIs]-(day)\n" +
                "    )\n" +
                ")";
        logger.debug("Generated Cypher Statement: {}", createTimeFlowEntitiesCql);
        workingGraphOperationExecutor.executeWrite(getSilentOperationDataTransformer(),createTimeFlowEntitiesCql);
    }

    private static void generateTimeFlowScaleEntities_Minute(GraphOperationExecutor workingGraphOperationExecutor, String timeFlowName, int startYear, int endYear){
        String createTimeFlowEntitiesCql = "MATCH (year:TGDA_TS_Year{timeFlow:\""+timeFlowName+"\"})-[:TGDA_TS_Contains]->(month)-[:TGDA_TS_Contains]->(day)-[:TGDA_TS_Contains]->(hour:TGDA_TS_Hour) WHERE year.year in range("+startYear+","+endYear+")\n"+
                "FOREACH (minute in range(0,59) | \n" +
                "    MERGE (m:TGDA_TS_Minute:TGDA_TimeScaleEntity {id:minute,minute:minute,timeFlow:\""+timeFlowName+"\"})<-[:TGDA_TS_Contains]-(hour)\n" +
                "    FOREACH(minute IN CASE WHEN minute=0 THEN [1] ELSE [] END | \n" +
                "        MERGE (m)<-[:TGDA_TS_FirstChildIs]-(hour)\n" +
                "    )\n" +
                "    FOREACH(minute IN CASE WHEN minute=59 THEN [1] ELSE [] END | \n" +
                "        MERGE (m)<-[:TGDA_TS_LastChildIs]-(hour)\n" +
                "    )\n" +
                ")";
        logger.debug("Generated Cypher Statement: {}", createTimeFlowEntitiesCql);
        workingGraphOperationExecutor.executeWrite(getSilentOperationDataTransformer(),createTimeFlowEntitiesCql);
    }

    private static void generateTimeFlowScaleEntities_YMD(GraphOperationExecutor workingGraphOperationExecutor, String timeFlowName, int targetYear){
        String createTimeFlowEntitiesCql = "WITH range(1,12) as months, "+targetYear+" as year\n" +
                "MERGE (y:TGDA_TS_Year:TGDA_TimeScaleEntity {year:year,id:year,timeFlow:\""+timeFlowName+"\"})\n" +
                "  FOREACH(month IN months |\n" +
                "    CREATE (m:TGDA_TS_Month:TGDA_TimeScaleEntity {month: month,id:month,timeFlow:\""+timeFlowName+"\"})\n" +
                "    MERGE (y)-[:TGDA_TS_Contains]->(m)\n" +
                "    FOREACH(month IN CASE WHEN month=1 THEN [1] ELSE [] END |\n" +
                "        MERGE (y)-[:TGDA_TS_FirstChildIs]->(m)\n" +
                "    )\n" +
                "    FOREACH(month IN CASE WHEN month=12 THEN [1] ELSE [] END |\n" +
                "        MERGE (y)-[:TGDA_TS_LastChildIs]->(m)\n" +
                "    )\n" +
                "    FOREACH(day IN (CASE\n" +
                "                      WHEN month IN [1,3,5,7,8,10,12] THEN range(1,31)\n" +
                "                      WHEN month = 2 THEN\n" +
                "                        CASE\n" +
                "                          WHEN year % 4 <> 0 THEN range(1,28)\n" +
                "                          WHEN year % 100 <> 0 THEN range(1,29)\n" +
                "                          WHEN year % 400 = 0 THEN range(1,29)\n" +
                "                          ELSE range(1,28)\n" +
                "                        END\n" +
                "                      ELSE range(1,30)\n" +
                "                    END) |\n" +
                "        CREATE (d:TGDA_TS_Day:TGDA_TimeScaleEntity {day:day,id:day,timeFlow:\""+timeFlowName+"\"})\n" +
                "        MERGE (m)-[:TGDA_TS_Contains]->(d)\n" +
                "        FOREACH(day IN CASE WHEN day=1 THEN [1] ELSE [] END |\n" +
                "            MERGE (m)-[:TGDA_TS_FirstChildIs]->(d)\n" +
                "        )\n" +
                "        FOREACH(day IN CASE WHEN day=31 THEN [1] ELSE [] END |\n" +
                "            MERGE (m)-[:TGDA_TS_LastChildIs]->(d)\n" +
                "        )\n" +
                "        FOREACH(day IN CASE WHEN (day=30 and month IN [4,6,9,11]) THEN [1] ELSE [] END |\n" +
                "            MERGE (m)-[:TGDA_TS_LastChildIs]->(d)\n" +
                "        )\n" +
                "        FOREACH(day IN CASE WHEN (day=29 and month =2) THEN [1] ELSE [] END |\n" +
                "            MERGE (m)-[:TGDA_TS_LastChildIs]->(d)\n" +
                "        )\n" +
                "        FOREACH(day IN CASE WHEN (day=28 and month =2 and (year % 4 <> 0)) THEN [1] ELSE [] END |\n" +
                "            MERGE (m)-[:TGDA_TS_LastChildIs]->(d)\n" +
                "        )\n" +
                "    )\n" +
                "  )" ;
        logger.debug("Generated Cypher Statement: {}", createTimeFlowEntitiesCql);
        workingGraphOperationExecutor.executeWrite(getSilentOperationDataTransformer(),createTimeFlowEntitiesCql);
    }

    private static void generateTimeFlowScaleEntities_Hour(GraphOperationExecutor workingGraphOperationExecutor, String timeFlowName, int targetYear){
        String createTimeFlowEntitiesCql = "MATCH (year:TGDA_TS_Year{timeFlow:\""+timeFlowName+"\",year:"+targetYear+"})-[:TGDA_TS_Contains]->(month)-[:TGDA_TS_Contains]->(day:TGDA_TS_Day)\n" +
                "WITH range(0,23) as HOURS, day\n" +
                "FOREACH (hour in HOURS |\n" +
                "    MERGE (h:TGDA_TS_Hour:TGDA_TimeScaleEntity {hour:hour,id:hour,timeFlow:\""+timeFlowName+"\"})<-[:TGDA_TS_Contains]-(day)\n" +
                "    FOREACH(hour IN CASE WHEN hour=0 THEN [1] ELSE [] END |\n" +
                "        MERGE (h)<-[:TGDA_TS_FirstChildIs]-(day)\n" +
                "    )\n" +
                "    FOREACH(hour IN CASE WHEN hour=23 THEN [1] ELSE [] END |\n" +
                "        MERGE (h)<-[:TGDA_TS_LastChildIs]-(day)\n" +
                "    )\n" +
                ")";
        logger.debug("Generated Cypher Statement: {}", createTimeFlowEntitiesCql);
        workingGraphOperationExecutor.executeWrite(getSilentOperationDataTransformer(),createTimeFlowEntitiesCql);
    }

    private static void generateTimeFlowScaleEntities_Minute(GraphOperationExecutor workingGraphOperationExecutor, String timeFlowName, int targetYear){
        String createTimeFlowEntitiesCql = "MATCH (year:TGDA_TS_Year{timeFlow:\""+timeFlowName+"\",year:"+targetYear+"})-[:TGDA_TS_Contains]->(month)-[:TGDA_TS_Contains]->(day)-[:TGDA_TS_Contains]->(hour:TGDA_TS_Hour)\n" +
                "FOREACH (minute in range(0,59) |\n" +
                "    MERGE (m:TGDA_TS_Minute:TGDA_TimeScaleEntity {id:minute,minute:minute,timeFlow:\""+timeFlowName+"\"})<-[:TGDA_TS_Contains]-(hour)\n" +
                "    FOREACH(minute IN CASE WHEN minute=0 THEN [1] ELSE [] END |\n" +
                "        MERGE (m)<-[:TGDA_TS_FirstChildIs]-(hour)\n" +
                "    )\n" +
                "    FOREACH(minute IN CASE WHEN minute=59 THEN [1] ELSE [] END |\n" +
                "        MERGE (m)<-[:TGDA_TS_LastChildIs]-(hour)\n" +
                "    )\n" +
                ")";
        logger.debug("Generated Cypher Statement: {}", createTimeFlowEntitiesCql);
        workingGraphOperationExecutor.executeWrite(getSilentOperationDataTransformer(),createTimeFlowEntitiesCql);
    }

    private static void linkTimeFlowScaleEntities_Year(GraphOperationExecutor workingGraphOperationExecutor, String timeFlowName, int startYear, int endYear){
        String linkTimeFlowEntitiesCql ="MATCH (year:TGDA_TS_Year{timeFlow:\""+timeFlowName+"\"}) WHERE year.year in range("+startYear+","+endYear+")\n"+
                "WITH year\n" +
                "ORDER BY year.year\n" +
                "WITH collect(year) as years\n" +
                "FOREACH(i in RANGE(0, size(years)-2) |\n" +
                "    FOREACH(year1 in [years[i]] |\n" +
                "        FOREACH(year2 in [years[i+1]] |\n" +
                "            MERGE (year1)-[:TGDA_TS_NextIs]->(year2))))";
        logger.debug("Generated Cypher Statement: {}", linkTimeFlowEntitiesCql);
        workingGraphOperationExecutor.executeWrite(getSilentOperationDataTransformer(),linkTimeFlowEntitiesCql);
    }

    private static void linkTimeFlowScaleEntities_Month(GraphOperationExecutor workingGraphOperationExecutor, String timeFlowName, int startYear, int endYear){
        String linkTimeFlowEntitiesCql = "MATCH (year:TGDA_TS_Year{timeFlow:\""+timeFlowName+"\"})-[:TGDA_TS_Contains]->(month) WHERE year.year in range("+startYear+","+endYear+")\n"+
                "WITH year,month\n" +
                "ORDER BY year.year, month.month\n" +
                "WITH collect(month) as months\n" +
                "FOREACH(i in RANGE(0, size(months)-2) |\n" +
                "    FOREACH(month1 in [months[i]] |\n" +
                "        FOREACH(month2 in [months[i+1]] |\n" +
                "            MERGE (month1)-[:TGDA_TS_NextIs]->(month2))))";
        workingGraphOperationExecutor.executeWrite(getSilentOperationDataTransformer(),linkTimeFlowEntitiesCql);
    }

    private static void linkTimeFlowScaleEntities_Day(GraphOperationExecutor workingGraphOperationExecutor, String timeFlowName, int startYear, int endYear){
        String linkTimeFlowEntitiesCql = "MATCH (year:TGDA_TS_Year{timeFlow:\""+timeFlowName+"\"})-[:TGDA_TS_Contains]->(month)-[:TGDA_TS_Contains]->(day) WHERE year.year in range("+startYear+","+endYear+")\n"+
                "WITH year,month,day\n" +
                "ORDER BY year.year, month.month, day.day\n" +
                "WITH collect(year) as years,collect(month) as months,collect(day) as days\n" +
                "FOREACH(i in RANGE(0, size(days)-2) |\n" +
                "    FOREACH(day1 in [days[i]] |\n" +
                "        FOREACH(day2 in [days[i+1]] |\n" +
                "            MERGE (day1)-[:TGDA_TS_NextIs]->(day2))))";
        workingGraphOperationExecutor.executeWrite(getSilentOperationDataTransformer(),linkTimeFlowEntitiesCql);
    }

    private static void linkTimeFlowScaleEntities_Hour(GraphOperationExecutor workingGraphOperationExecutor, String timeFlowName, int startYear, int endYear){
        String linkTimeFlowEntitiesCql = "MATCH (year:TGDA_TS_Year{timeFlow:\""+timeFlowName+"\"})-[:TGDA_TS_Contains]->(month)-[:TGDA_TS_Contains]->(day)-[:TGDA_TS_Contains]->(hour) WHERE year.year in range("+startYear+","+endYear+")\n"+
                "WITH year,month,day,hour\n" +
                "ORDER BY year.year, month.month, day.day, hour.hour\n" +
                "WITH collect(hour) as hours,size(collect(hour)) as hourCount\n" +
                "FOREACH(i in RANGE(0, hourCount-2) |\n" +
                "    FOREACH(hour1 in [hours[i]] |\n" +
                "        FOREACH(hour2 in [hours[i+1]] |\n" +
                "            MERGE (hour1)-[:TGDA_TS_NextIs]->(hour2))))";
        workingGraphOperationExecutor.executeWrite(getSilentOperationDataTransformer(),linkTimeFlowEntitiesCql);
    }

    private static void linkTimeFlowScaleEntities_Minute(GraphOperationExecutor workingGraphOperationExecutor, String timeFlowName, int startYear, int endYear){
        String linkTimeFlowEntitiesCql = "MATCH (year:TGDA_TS_Year{timeFlow:\""+timeFlowName+"\"})-[:TGDA_TS_Contains]->(month)-[:TGDA_TS_Contains]->(day)-[:TGDA_TS_Contains]->(hour)-[:TGDA_TS_Contains]->(minute) WHERE year.year in range("+startYear+","+endYear+")\n"+
                "WITH year,month,day,hour,minute\n" +
                "ORDER BY year.year, month.month, day.day, hour.hour, minute.minute\n" +
                "WITH collect(minute) as minutes,size(collect(minute)) as minuteCount\n" +
                "FOREACH(i in RANGE(0, minuteCount-2) |\n" +
                "    FOREACH(minute1 in [minutes[i]] |\n" +
                "        FOREACH(minute2 in [minutes[i+1]] |\n" +
                "            MERGE (minute1)-[:TGDA_TS_NextIs]->(minute2))))";
        workingGraphOperationExecutor.executeWrite(getSilentOperationDataTransformer(),linkTimeFlowEntitiesCql);
    }

    private static DataTransformer getSilentOperationDataTransformer(){
        DataTransformer dataTransformer = new DataTransformer() {
            @Override
            public Object transformResult(Result result) {
                return null;
            }
        };
        return dataTransformer;
    }

    public static LinkedList<TimeScaleMoment> getMonths(int startYear, int startMonth, int endYear, int endMonth) throws ParseException {
        Date d1 = new SimpleDateFormat("yyyy-MM").parse(startYear+"-"+startMonth);
        Date d2 = new SimpleDateFormat("yyyy-MM").parse(endYear+"-"+endMonth);
        LinkedList<TimeScaleMoment> timeScaleMomentLinkedList = new LinkedList<>();
        Calendar dd = Calendar.getInstance();
        dd.setTime(d1);
        while(dd.getTime().before(d2)){
            timeScaleMomentLinkedList.add(new TimeScaleMoment(dd.get(Calendar.YEAR),(dd.get(Calendar.MONTH)+1)));
            dd.add(Calendar.MONTH, 1);
        }
        timeScaleMomentLinkedList.add(new TimeScaleMoment(endYear,endMonth));
        return timeScaleMomentLinkedList;
    }

}
