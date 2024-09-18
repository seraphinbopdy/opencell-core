/*
 * (C) Copyright 2015-2020 Opencell SAS (https://opencellsoft.com/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN
 * OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS
 * IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO
 * THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE,
 * YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.
 *
 * For more information on the GNU Affero General Public License, please consult
 * <https://www.gnu.org/licenses/agpl-3.0.en.html>.
 */

package org.meveo.model;

import java.util.Date;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.meveo.model.crm.custom.CustomFieldValues;
import org.meveo.model.persistence.CustomFieldJsonDataType;

/**
 * An entity that contains custom fields
 * 
 * @author Andrius Karpavicius
 * @author Edward P. Legaspi
 * @lastModifiedVersion 5.3
 */
public interface ICustomFieldEntity {

    /**
     * Provide a transient Custom field value holder to hold converted Custom field values from JSON
     * 
     * @return A transient Custom field value holder
     */
    public CustomFieldValues getCFValuesTransient();

    /**
     * Set a transient Custom field value holder to hold converted Custom field values from JSON
     * 
     * @param cfValues A transient Custom field value holder
     */
    public void setCFValuesTransient(CustomFieldValues cfValues);

    /**
     * Get custom field values as JSON string.<br/>
     * <br/>
     * NOTE: Do not manipulate this value directly, use get/setCfValues methods instead, it will serialize into JSON string automatically
     * 
     * @return Custom field values as JSON string
     */
    public String getCfValuesAsJson();

    /**
     * Set custom field values as JSON string.<br/>
     * <br/>
     * NOTE: Do not manipulate this value directly, use get/setCfValues methods instead, it will serialize into JSON string automatically
     * 
     * @param cfValuesAsJson Custom field values as JSON string
     */
    public void setCfValuesAsJson(String cfValuesAsJson);

    /**
     * Get unique identifier.
     * 
     * @return uuid
     */
    public String getUuid();

    /**
     * Set a new UUID value.
     * 
     * @return Old UUID value
     */
    public String clearUuid();

    /**
     * Get an array of parent custom field entity in case custom field values should be inherited from a parent entity.
     * 
     * @return An entity
     */
    public default ICustomFieldEntity[] getParentCFEntities() {
        return null;
    }

    /**
     * Get a Custom field value holder from a JSON string.
     * 
     * @return Custom field values holder
     */
    public default CustomFieldValues getCfValues() {

        CustomFieldValues cfValues = getCFValuesTransient();

        if (cfValues != null) {
            return cfValues;
        }

        String json = getCfValuesAsJson();
        if (json == null) {
            return null;

        } else {
            cfValues = CustomFieldJsonDataType.INSTANCE.fromString(json);
            setCFValuesTransient(cfValues);
            return cfValues;
        }
    }

    /**
     * Set Custom field value holder and serialize it to a JSON string.
     * 
     * @param cfValuesNew Custom field values holder
     */
    public default void setCfValues(CustomFieldValues cfValuesNew) {

        if (cfValuesNew == null) {
            setCFValuesTransient(null);
            setCfValuesAsJson(null);

        } else {

            CustomFieldValues cfValuesOld = getCFValuesTransient();

            if (cfValuesOld == null) {
                setCFValuesTransient(cfValuesNew);
            } else {
                cfValuesOld.setValues(cfValuesNew.getValuesByCode());
            }
            setCfValuesAsJson(CustomFieldJsonDataType.INSTANCE.toString(cfValuesNew));
        }
    }

    /**
     * Instantiate custom field values holder if it is null (the case when entity with no CF values is retrieved from DB)
     * 
     * @return Custom field values holder
     */
    public default CustomFieldValues getCfValuesNullSafe() {
        CustomFieldValues cfValues = getCfValues();
        if (cfValues == null) {
            setCfValues(new CustomFieldValues());
            return getCfValues();
        }
        return cfValues;
    }

    /**
     * Clear custom field values
     */
    public default void clearCfValues() {
        CustomFieldValues cfValues = getCfValues();
        if (cfValues == null) {
            return;
        }
        cfValues.clearValues();
        setCfValuesAsJson(null);
    }

    /**
     * Check if entity has a non-empty value for a given custom field.
     * 
     * @param cfCode Custom field code
     * @return True if entity has a non-empty value for a given custom field
     */
    public default boolean hasCFValueNotEmpty(String cfCode) {
        CustomFieldValues cfValues = getCfValues();
        if (cfValues != null) {
            return cfValues.hasCfValueNotEmpty(cfCode);
        }
        return false;
    }

    /**
     * Check if entity has a value for a given custom field.
     * 
     * @param cfCode Custom field code
     * @return True if entity has a value for a given custom field
     */
    public default boolean hasCfValue(String cfCode) {

        CustomFieldValues cfValues = getCfValues();
        if (cfValues != null) {
            return cfValues.hasCfValue(cfCode);
        }
        return false;
    }

    /**
     * Check if entity has a value for a given custom field on a given date
     * 
     * @param cfCode Custom field code
     * @param date Date to check for
     * @return True if entity has a value for a given custom field and on a given date
     */
    public default boolean hasCfValue(String cfCode, Date date) {

        CustomFieldValues cfValues = getCfValues();
        if (cfValues != null) {
            return cfValues.hasCfValue(cfCode, date);
        }
        return false;
    }

    /**
     * Check if entity has a value for a given custom field on a given date period, strictly matching the CF value's period start/end dates
     * 
     * @param cfCode Custom field code
     * @param dateFrom Period start date
     * @param dateTo Period end date
     * @return True if entity has a value for a given custom field
     */
    public default boolean hasCfValue(String cfCode, Date dateFrom, Date dateTo) {
        CustomFieldValues cfValues = getCfValues();
        if (cfValues != null) {
            return cfValues.hasCfValue(cfCode, dateFrom, dateTo);
        }
        return false;
    }

    /**
     * Get custom field values (not CF value entity). In case of versioned values (more than one entry in CF value list) a CF value corresponding to today will be returned
     * 
     * @return A map of values with key being custom field code.
     */
    public default Map<String, Object> getCfValuesAsValues() {
        CustomFieldValues cfValues = getCfValues();
        if (cfValues != null && cfValues.getValuesByCode() != null) {
            return cfValues.getValues();
        }
        return null;
    }

    /**
     * Get a value (not CF value entity) for a given custom field. In case of versioned values (more than one entry in CF value list) a CF value corresponding to a today will be returned
     * 
     * @param cfCode Custom field code
     * @return Value
     */
    public default Object getCfValue(String cfCode) {
        CustomFieldValues cfValues = getCfValues();
        if (cfValues != null) {
            return cfValues.getValue(cfCode);
        }
        return null;
    }

    /**
     * Get a value (not CF value entity) for a given custom field for a given date
     * 
     * @param cfCode Custom field code
     * @param date Date
     * @return Value
     */
    public default Object getCfValue(String cfCode, Date date) {
        CustomFieldValues cfValues = getCfValues();
        if (cfValues != null) {
            return cfValues.getValue(cfCode, date);
        }
        return null;
    }

    /**
     * Match custom field's map's key as close as possible to the key provided and return a map value (not CF value entity). Match is performed by matching a full string and then reducing one by one symbol until a match
     * is found. In case of versioned values (more than one entry in CF value list) a CF value corresponding to a today will be returned
     * 
     * TODO can be an issue with lower/upper case mismatch
     * 
     * @param cfCode Custom field code
     * @param keyToMatch Key to match
     * @return Map value that closely matches map key
     */
    public default Object getCFValueByClosestMatch(String cfCode, String keyToMatch) {
        CustomFieldValues cfValues = getCfValues();
        if (cfValues != null) {
            Object valueMatched = cfValues.getValueByClosestMatch(cfCode, keyToMatch);
            return valueMatched;
        }
        return null;
    }

    /**
     * Match for a given date (versionable values) custom field's map's key as close as possible to the key provided and return a map value (not CF value entity). Match is performed by matching a full string and then
     * reducing one by one symbol until a match is found.
     * 
     * TODO can be an issue with lower/upper case mismatch
     * 
     * @param cfCode Custom field code
     * @param date Date to check for
     * @param keyToMatch Key to match
     * @return Map value that closely matches map key
     */
    public default Object getCFValueByClosestMatch(String cfCode, Date date, String keyToMatch) {
        CustomFieldValues cfValues = getCfValues();
        if (cfValues != null) {
            Object valueMatched = cfValues.getValueByClosestMatch(cfCode, date, keyToMatch);
            return valueMatched;
        }
        return null;
    }

    /**
     * Remove custom field values
     * 
     * @param cfCode Custom field code
     */
    public default void removeCfValue(String cfCode) {
        CustomFieldValues cfValues = getCfValues();
        if (cfValues != null) {
            cfValues.removeValue(cfCode);
            setCfValuesAsJson(CustomFieldJsonDataType.INSTANCE.toString(cfValues));
        }
    }

    /**
     * Remove custom field values for a given date
     * 
     * @param cfCode Custom field code
     * @param date Date
     */
    public default void removeCfValue(String cfCode, Date date) {
        CustomFieldValues cfValues = getCfValues();
        if (cfValues != null) {
            cfValues.removeValue(cfCode, date);
            setCfValuesAsJson(CustomFieldJsonDataType.INSTANCE.toString(cfValues));
        }
    }

    /**
     * Remove custom field values for a given date period, strictly matching custom field value's period start and end dates
     * 
     * @param cfCode Custom field code
     * @param dateFrom Period start date
     * @param dateTo Period end date
     */
    public default void removeCfValue(String cfCode, Date dateFrom, Date dateTo) {
        CustomFieldValues cfValues = getCfValues();
        if (cfValues != null) {
            cfValues.removeValue(cfCode, dateFrom, dateTo);
            setCfValuesAsJson(CustomFieldJsonDataType.INSTANCE.toString(cfValues));
        }
    }

    /**
     * Set custom field value. A raw implementation. Consider using CustomFieldInstanceService.setCFValue() for more controlled logic.
     * 
     * @param cfCode Custom field code
     * @param value Value to set. If value is null, it will store a NULL value - consider using removeCfValue() instead if you want to remove CF value if it is null.
     */
    public default void setCfValue(String cfCode, Object value) {
        CustomFieldValues cfValues = getCfValuesNullSafe();
        cfValues.setValue(cfCode, value);
        setCfValuesAsJson(CustomFieldJsonDataType.INSTANCE.toString(cfValues));
    }

    /**
     * Set custom field value for a given period. A raw implementation. Consider using CustomFieldInstanceService.setCFValue() for more controlled logic.
     * 
     * @param cfCode Custom field code
     * @param period Period
     * @param priority Priority. Will default to 0 if passed null, will default to next value if passed as -1, will be set otherwise.
     * @param value Value to set. If value is null, it will store a NULL value - consider using removeCfValue() instead if you want to remove CF value if it is null.
     */
    public default void setCfValue(String cfCode, DatePeriod period, Integer priority, Object value) {
        CustomFieldValues cfValues = getCfValuesNullSafe();
        cfValues.setValue(cfCode, period, priority, value);
        setCfValuesAsJson(CustomFieldJsonDataType.INSTANCE.toString(cfValues));
    }

    // Accumulated Custom field value functionality is currently not used

//    /**
//     * @return Accumulated Custom field values holder
//     */
//    public default CustomFieldValues getCfAccumulatedValues() {
//        return null;
//    }
//
//    /**
//     * Instantiate Accumulated custom field values holder if it is null (the case when entity with no CF values is retrieved from DB)
//     * 
//     * @return Custom field values holder
//     */
//    public default CustomFieldValues getCfAccumulatedValuesNullSafe() {
//        CustomFieldValues cfValues = getCfAccumulatedValues();
//        if (cfValues == null) {
//            setCfAccumulatedValues(new CustomFieldValues());
//            return getCfAccumulatedValues();
//        }
//        return cfValues;
//    }
//
//    /**
//     * @param cfValues Accumulated Custom field values holder
//     */
//    public void setCfAccumulatedValues(CustomFieldValues cfValues);
//
//    /**
//     * Get an accumulated value (not CF value entity) for a given custom field. In case of versioned values (more than one entry in CF value list) a CF value corresponding to a today will be returned
//     * 
//     * @param cfCode Custom field code
//     * @return Accumulated field value
//     */
//    public default Object getCfAccumulatedValue(String cfCode) {
//        CustomFieldValues cfValues = getCfAccumulatedValues();
//        if (cfValues != null) {
//            return cfValues.getValue(cfCode);
//        }
//        return null;
//    }
//
//    /**
//     * Get an accumulated value (not CF value entity) for a given custom field for a given date
//     * 
//     * @param cfCode Custom field code
//     * @param date Date
//     * @return Accumulated field value
//     */
//    public default Object getCfAccumulatedValue(String cfCode, Date date) {
//        CustomFieldValues cfValues = getCfAccumulatedValues();
//        if (cfValues != null) {
//            return cfValues.getValue(cfCode, date);
//        }
//        return null;
//    }
//
//    /**
//     * Match custom field's map's key as close as possible to the key provided and return a map value (not CF value entity). Match is performed by matching a full string and then reducing one by one symbol until a match
//     * is found. In case of versioned values (more than one entry in CF value list) a CF value corresponding to a today will be returned
//     * 
//     * TODO can be an issue with lower/upper case mismatch
//     * 
//     * @param cfCode Custom field code
//     * @param keyToMatch Key to match
//     * @return Map value that closely matches map key
//     */
//    public default Object getCFAccumulatedValueByClosestMatch(String cfCode, String keyToMatch) {
//        CustomFieldValues cfValues = getCfAccumulatedValues();
//        if (cfValues != null) {
//            Object valueMatched = cfValues.getValueByClosestMatch(cfCode, keyToMatch);
//            return valueMatched;
//        }
//        return null;
//    }
//
//    /**
//     * Match for a given date (versionable values) custom field's map's key as close as possible to the key provided and return a map value (not CF value entity). Match is performed by matching a full string and then
//     * reducing one by one symbol until a match is found.
//     * 
//     * TODO can be an issue with lower/upper case mismatch
//     * 
//     * @param cfCode Custom field code
//     * @param date Date to check for
//     * @param keyToMatch Key to match
//     * @return Map value that closely matches map key
//     */
//    public default Object getCFAccumulatedValueByClosestMatch(String cfCode, Date date, String keyToMatch) {
//        CustomFieldValues cfValues = getCfAccumulatedValues();
//        if (cfValues != null) {
//            Object valueMatched = cfValues.getValueByClosestMatch(cfCode, date, keyToMatch);
//            return valueMatched;
//        }
//        return null;
//    }

    /**
     * Match as close as possible map's key to the key provided and return a map value. Match is performed by matching a full string and then reducing one by one symbol untill a match is found.
     * 
     * TODO can be an issue with lower/upper case mismatch
     *
     * @param value Value to inspect
     * @param keyToMatch Key to match
     * @return Map value that closely matches map key
     */
    @SuppressWarnings("unchecked")
    public static Object matchClosestValue(Object value, String keyToMatch) {
        if (value == null || !(value instanceof Map) || StringUtils.isEmpty(keyToMatch)) {
            return null;
        }
        // Logger log = LoggerFactory.getLogger(ICustomFieldEntity.class);
        Object valueFound = null;
        Map<String, Object> mapValue = (Map<String, Object>) value;
        // log.trace("matchClosestValue keyToMatch: {} in {}", keyToMatch, mapValue);
        for (int i = keyToMatch.length(); i > 0; i--) {
            valueFound = mapValue.get(keyToMatch.substring(0, i));
            if (valueFound != null) {
                // log.trace("matchClosestValue found value: {} for key: {}", valueFound, keyToMatch.substring(0, i));
                return valueFound;
            }
        }

        return null;
    }

    /**
     * @return
     */
    public default Boolean isDirtyCF() {
        return getCfValues() != null && (CollectionUtils.isNotEmpty(getCfValues().getDirtyCfPeriods()) || CollectionUtils.isNotEmpty(getCfValues().getDirtyCfValues()));
    }

    /**
     * Copy Custom field values omitting all GUI related and transient fields
     * 
     * @return Custom field values
     */
    public default CustomFieldValues getCFValuesCopy() {
        CustomFieldValues cfValues = getCfValues();
        if (cfValues == null) {
            return null;
        }
        return cfValues.clone();
    }

    /**
     * Encrypt custom field values
     */
    public default void encryptCfValues() {
        CustomFieldValues cfValues = getCfValues();
        if (cfValues != null) {
            cfValues.setEncrypted(true);
            setCfValuesAsJson(CustomFieldJsonDataType.INSTANCE.toString(cfValues));
        }
    }
}