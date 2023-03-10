package com.github.tgda.engine.core.internal.neo4j.dataTransformer;

import com.github.tgda.engine.core.internal.neo4j.CypherBuilder;
import com.github.tgda.engine.core.payload.EntityValue;
import com.github.tgda.engine.core.term.Attribute;
import com.github.tgda.engine.core.term.AttributeDataType;

import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.types.Node;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

public class GetListEntityValueTransformer implements DataTransformer<List<EntityValue>>{

    private List<Attribute> containsAttributeList;
    private List<String> returnedAttributeList;
    private Map<String,AttributeDataType> attributeDataTypeMap;
    private boolean useIDMatchLogic = true;

    public GetListEntityValueTransformer(List<String> returnedAttributeList){
        this.containsAttributeList = new ArrayList<>();
        this.returnedAttributeList = returnedAttributeList;
        this.setUseIDMatchLogic(false);
        this.attributeDataTypeMap = new HashMap<>();
        for(Attribute currentAttribute :this.containsAttributeList){
            String attributeName = currentAttribute.getAttributeKindName();
            AttributeDataType attributeDataType = currentAttribute.getAttributeDataType();
            this.attributeDataTypeMap.put(attributeName,attributeDataType);
        }
    }

    public GetListEntityValueTransformer(List<String> returnedAttributeList, List<Attribute> containsAttributeList){
        this.containsAttributeList = containsAttributeList;
        this.returnedAttributeList = returnedAttributeList;

        this.attributeDataTypeMap = new HashMap<>();
        for(Attribute currentAttribute :this.containsAttributeList){
            String attributeName = currentAttribute.getAttributeKindName();
            AttributeDataType attributeDataType = currentAttribute.getAttributeDataType();
            this.attributeDataTypeMap.put(attributeName,attributeDataType);
        }
    }

    @Override
    public List<EntityValue> transformResult(Result result) {
        List<EntityValue> entityValueList = new ArrayList<>();
        while(result.hasNext()){
            Record nodeRecord = result.next();
            String conceptionEntityUID;
            Map<String,Object> valueMap;
            if(this.isUseIDMatchLogic()){
                valueMap = nodeRecord.asMap();
                String idKey = "id("+CypherBuilder.operationResultName+")";
                Long uidValue = (Long)valueMap.get(idKey);
                conceptionEntityUID = ""+uidValue.longValue();
            }else{
                Node resultNode = nodeRecord.get(CypherBuilder.operationResultName).asNode();
                long nodeUID = resultNode.id();
                conceptionEntityUID = ""+nodeUID;
                valueMap = resultNode.asMap();
            }

            Map<String,Object> entityAttributesValue = new HashMap<>();
            EntityValue currentEntityValue = new EntityValue(conceptionEntityUID,entityAttributesValue);
            entityValueList.add(currentEntityValue);
            if(this.isUseIDMatchLogic()){
                for(String currentAttributeName:returnedAttributeList){
                    String entityAttributeName = CypherBuilder.operationResultName+"."+currentAttributeName;
                    Object objectValue = valueMap.get(entityAttributeName);
                    if(objectValue == null){
                        //if attribute contains space for example : attribute a, will returned in `` such as .`attribute a`
                        entityAttributeName = CypherBuilder.operationResultName+".`"+currentAttributeName+"`";
                        objectValue = valueMap.get(entityAttributeName);
                    }
                    Object resultAttributeValue = getFormattedValue(currentAttributeName,objectValue);
                    if(resultAttributeValue != null){
                        entityAttributesValue.put(currentAttributeName,resultAttributeValue);
                    }
                }
            }else{
                for(String currentAttributeName:returnedAttributeList){
                    Object objectValue = valueMap.get(currentAttributeName);
                    Object resultAttributeValue = getFormattedValue(currentAttributeName,objectValue);
                    if(resultAttributeValue != null){
                        entityAttributesValue.put(currentAttributeName,resultAttributeValue);
                    }
                }
            }
        }
        return entityValueList;
    }

    private Object getFormattedValue(String attributeName,Object attributeValue){
        if(attributeValue != null) {
            AttributeDataType targetAttributeDataType = this.attributeDataTypeMap.get(attributeName);
            if(attributeValue instanceof Boolean || attributeValue instanceof String) {
                return attributeValue;
            }else if (attributeValue instanceof ZonedDateTime) {
                ZonedDateTime targetZonedDateTime = (ZonedDateTime) attributeValue;
                Date currentDate = Date.from(targetZonedDateTime.toInstant());
                return currentDate;
            }else if (attributeValue instanceof Long) {
                if(targetAttributeDataType != null) {
                    switch (targetAttributeDataType) {
                        case INT:
                            return ((Long) attributeValue).intValue();
                        case BYTE:
                            return ((Long) attributeValue).byteValue();
                        case LONG:
                            return ((Long) attributeValue).longValue();
                        case SHORT:
                            return ((Long) attributeValue).shortValue();
                    }
                }else {
                    return attributeValue;
                }
            }else if (attributeValue instanceof Double) {
                if(targetAttributeDataType != null) {
                    switch (targetAttributeDataType) {
                        case FLOAT:
                            return ((Double) attributeValue).floatValue();
                        case DOUBLE:
                            return ((Double) attributeValue).doubleValue();
                        case DECIMAL:
                            return new BigDecimal((Double) attributeValue);
                    }
                }else {
                    return attributeValue;
                }
            }else if (attributeValue instanceof List && ((List<?>) attributeValue).size() > 0) {
                Object firstAttributeValue = ((List<?>) attributeValue).get(0);
                if (firstAttributeValue instanceof Boolean) {
                    List<Boolean> booleanValueList = (List<Boolean>) attributeValue;
                    Boolean[] returnBooleanValueArray = booleanValueList.toArray(new Boolean[booleanValueList.size()]);
                    return returnBooleanValueArray;
                }else if (firstAttributeValue instanceof ZonedDateTime) {
                    List<ZonedDateTime> valueList = (List<ZonedDateTime>) attributeValue;
                    Date[] returnDateValueArray = new Date[valueList.size()];
                    for (int i = 0; i < valueList.size(); i++) {
                        returnDateValueArray[i] = Date.from(valueList.get(i).toInstant());
                    }
                    return returnDateValueArray;
                }else if (firstAttributeValue instanceof String) {
                    List<String> stringValueList = (List<String>) attributeValue;
                    String[] returnStringValueArray = stringValueList.toArray(new String[stringValueList.size()]);
                    return returnStringValueArray;
                }else if (firstAttributeValue instanceof Long) {
                    List<Long> longValueList = (List<Long>) attributeValue;
                    if (targetAttributeDataType != null) {
                        switch (targetAttributeDataType) {
                            case INT_ARRAY:
                                Integer[] returnIntValueArray = new Integer[longValueList.size()];
                                for (int i = 0; i < longValueList.size(); i++) {
                                    returnIntValueArray[i] = longValueList.get(i).intValue();
                                }
                                return returnIntValueArray;
                            case LONG_ARRAY:
                                Long[] returnLongValueArray = new Long[longValueList.size()];
                                for (int i = 0; i < longValueList.size(); i++) {
                                    returnLongValueArray[i] = longValueList.get(i).longValue();
                                }
                                return returnLongValueArray;
                            case SHORT_ARRAY:
                                Short[] returnShortValueArray = new Short[longValueList.size()];
                                for (int i = 0; i < longValueList.size(); i++) {
                                    returnShortValueArray[i] = longValueList.get(i).shortValue();
                                }
                                return returnShortValueArray;
                            case BYTE_ARRAY:
                                Byte[] returnByteValueArray = new Byte[longValueList.size()];
                                for (int i = 0; i < longValueList.size(); i++) {
                                    returnByteValueArray[i] = longValueList.get(i).byteValue();
                                }
                                return returnByteValueArray;
                            case BINARY:
                                byte[] returnBinaryValueArray = new byte[longValueList.size()];
                                for (int i = 0; i < longValueList.size(); i++) {
                                    returnBinaryValueArray[i] = longValueList.get(i).byteValue();
                                }
                                return returnBinaryValueArray;
                        }
                    }else {
                        Long[] returnLongValueArray = new Long[longValueList.size()];
                        for (int i = 0; i < longValueList.size(); i++) {
                            returnLongValueArray[i] = longValueList.get(i).longValue();
                        }
                        return returnLongValueArray;
                    }
                }else if (firstAttributeValue instanceof Double) {
                    List<Double> doubleValueList = (List<Double>) attributeValue;
                    if (targetAttributeDataType != null) {
                        switch (targetAttributeDataType) {
                            case FLOAT_ARRAY:
                                Float[] returnFloatValueArray = new Float[doubleValueList.size()];
                                for (int i = 0; i < doubleValueList.size(); i++) {
                                    returnFloatValueArray[i] = doubleValueList.get(i).floatValue();
                                }
                                return returnFloatValueArray;
                            case DOUBLE_ARRAY:
                                Double[] returnDoubleValueArray = new Double[doubleValueList.size()];
                                for (int i = 0; i < doubleValueList.size(); i++) {
                                    returnDoubleValueArray[i] = doubleValueList.get(i).doubleValue();
                                }
                                return returnDoubleValueArray;
                            case DECIMAL_ARRAY:
                                BigDecimal[] returnDecimalValueArray = new BigDecimal[doubleValueList.size()];
                                for (int i = 0; i < doubleValueList.size(); i++) {
                                    returnDecimalValueArray[i] = new BigDecimal(doubleValueList.get(i).doubleValue());
                                }
                                return returnDecimalValueArray;
                        }
                    }else {
                        Double[] returnDoubleValueArray = new Double[doubleValueList.size()];
                        for (int i = 0; i < doubleValueList.size(); i++) {
                            returnDoubleValueArray[i] = doubleValueList.get(i).doubleValue();
                        }
                        return returnDoubleValueArray;
                    }
                }
            }else {
                return attributeValue;
            }
        }
        return null;
    }

    public boolean isUseIDMatchLogic() {
        return useIDMatchLogic;
    }

    public void setUseIDMatchLogic(boolean useIDMatchLogic) {
        this.useIDMatchLogic = useIDMatchLogic;
    }
}
