
package org.meveo.service.script;

import java.util.List;
import java.util.Map;

import org.meveo.admin.exception.BusinessException;

public class ScriptInterfaceImplForTest extends Script {
    private int intValue;
    private boolean booleanValue;
    private double doubleValue;
    private long longValue;
    private String stringValue;
    private Integer integerValue;
    private Boolean booleanObjectValue;
    private Double doubleObjectValue;
    private Long longObjectValue;
    private List<String> listStringValue;
    private List<Integer> listIntegerValue;
    private List<Boolean> listBooleanValue;
    private List<Double> listDoubleValue;
    private List<Long> listLongValue;
    private Map<String, String> mapStringValue;
    private Map<String, Integer> mapIntegerValue;
    private Map<String, Boolean> mapBooleanValue;
    private Map<String, Double> mapDoubleValue;
    private Map<String, Long> mapLongValue;

    public int getIntValue() {
        return intValue;
    }

    public void setIntValue(int intValue) {
        this.intValue = intValue;
    }

    public boolean isBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public long getLongValue() {
        return longValue;
    }

    public void setLongValue(long longValue) {
        this.longValue = longValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public Integer getIntegerValue() {
        return integerValue;
    }

    public void setIntegerValue(Integer integerValue) {
        this.integerValue = integerValue;
    }

    public Boolean getBooleanObjectValue() {
        return booleanObjectValue;
    }

    public void setBooleanObjectValue(Boolean booleanObjectValue) {
        this.booleanObjectValue = booleanObjectValue;
    }

    public Double getDoubleObjectValue() {
        return doubleObjectValue;
    }

    public void setDoubleObjectValue(Double doubleObjectValue) {
        this.doubleObjectValue = doubleObjectValue;
    }

    public Long getLongObjectValue() {
        return longObjectValue;
    }

    public void setLongObjectValue(Long longObjectValue) {
        this.longObjectValue = longObjectValue;
    }

    public List<String> getListStringValue() {
        return listStringValue;
    }

    public void setListStringValue(List<String> listStringValue) {
        this.listStringValue = listStringValue;
    }

    public List<Integer> getListIntegerValue() {
        return listIntegerValue;
    }

    public void setListIntegerValue(List<Integer> listIntegerValue) {
        this.listIntegerValue = listIntegerValue;
    }

    public List<Boolean> getListBooleanValue() {
        return listBooleanValue;
    }

    public void setListBooleanValue(List<Boolean> listBooleanValue) {
        this.listBooleanValue = listBooleanValue;
    }

    public List<Double> getListDoubleValue() {
        return listDoubleValue;
    }

    public void setListDoubleValue(List<Double> listDoubleValue) {
        this.listDoubleValue = listDoubleValue;
    }

    public List<Long> getListLongValue() {
        return listLongValue;
    }

    public void setListLongValue(List<Long> listLongValue) {
        this.listLongValue = listLongValue;
    }

    public Map<String, String> getMapStringValue() {
        return mapStringValue;
    }

    public void setMapStringValue(Map<String, String> mapStringValue) {
        this.mapStringValue = mapStringValue;
    }

    public Map<String, Integer> getMapIntegerValue() {
        return mapIntegerValue;
    }

    public void setMapIntegerValue(Map<String, Integer> mapIntegerValue) {
        this.mapIntegerValue = mapIntegerValue;
    }

    public Map<String, Boolean> getMapBooleanValue() {
        return mapBooleanValue;
    }

    public void setMapBooleanValue(Map<String, Boolean> mapBooleanValue) {
        this.mapBooleanValue = mapBooleanValue;
    }

    public Map<String, Double> getMapDoubleValue() {
        return mapDoubleValue;
    }

    public void setMapDoubleValue(Map<String, Double> mapDoubleValue) {
        this.mapDoubleValue = mapDoubleValue;
    }

    public Map<String, Long> getMapLongValue() {
        return mapLongValue;
    }

    public void setMapLongValue(Map<String, Long> mapLongValue) {
        this.mapLongValue = mapLongValue;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
    }
}