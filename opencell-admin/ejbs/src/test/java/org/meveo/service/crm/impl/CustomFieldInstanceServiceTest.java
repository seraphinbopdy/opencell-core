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

package org.meveo.service.crm.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.meveo.model.ICustomFieldEntity;
import org.meveo.model.crm.custom.CustomFieldValues;
import org.meveo.model.customEntities.CustomEntityTemplate;
import org.meveo.model.jobs.JobInstance;

public class CustomFieldInstanceServiceTest {
    private CustomFieldInstanceService sut = new CustomFieldInstanceService();

    @Test
    public void should_return_class_if_className_contains_custom_table_ref() throws ClassNotFoundException {
        // Given
        String className = "org.meveo.model.customEntities.CustomEntityTemplate - TABLE_1";
        // When
        Class<?> clazz = sut.trimTableNameAndGetClass(className);
        // Then
        assertThat(clazz).isEqualTo(CustomEntityTemplate.class);
    }

    @Test
    public void should_return_class_if_className_is_right() throws ClassNotFoundException {
        // Given
        String className = "org.meveo.model.customEntities.CustomEntityTemplate";
        // When
        Class<?> clazz = sut.trimTableNameAndGetClass(className);
        // Then
        assertThat(clazz).isEqualTo(CustomEntityTemplate.class);
    }

    @Test
    public void testGetCfValuesNullSafe_WhenCurrentInstanceIsNull_ShouldReturnNewInstance() {
        ICustomFieldEntity customFieldEntityUnderTest = new JobInstance();

        // Given
        customFieldEntityUnderTest.setCfValuesAsJson(null);

        assertNull(customFieldEntityUnderTest.getCfValues());

        // When
        CustomFieldValues cfValues = customFieldEntityUnderTest.getCfValuesNullSafe();

        // Then
        assertNotNull(cfValues);
        assertSame(cfValues, customFieldEntityUnderTest.getCfValues());
    }

    @Test
    public void testGetCfValuesNullSafe_WhenCurrentInstanceIsNotNull_ShouldReturnSameInstance() {

        ICustomFieldEntity customFieldEntityUnderTest = new JobInstance();

        // Given
        customFieldEntityUnderTest.setCfValuesAsJson("{}");

        assertNotNull(customFieldEntityUnderTest.getCfValues());

        // When
        CustomFieldValues cfValuesNullSafe = customFieldEntityUnderTest.getCfValuesNullSafe();
        CustomFieldValues cfValues = customFieldEntityUnderTest.getCfValues();

        // Then
        assertNotNull(cfValuesNullSafe);
        assertNotNull(cfValues);
        assertSame(cfValuesNullSafe, cfValues);
    }

    @Test
    public void testEncryptCfValues_WhenCurrentInstanceIsNotNull_ShouldEncryptValues() {

        ICustomFieldEntity customFieldEntityUnderTest = new JobInstance();

        // Given
        customFieldEntityUnderTest.setCfValuesAsJson("{}");

        // When
        customFieldEntityUnderTest.encryptCfValues();

        // Then
        CustomFieldValues cfValues = customFieldEntityUnderTest.getCfValuesNullSafe();
        assertTrue(cfValues.isEncrypted());
    }

    @Test
    public void shouldSetCFValueUsingSetCFValueMethod() {
        // Arrange
        JobInstance jobInstance = new JobInstance();

        // Act
        jobInstance.setCfValue("description", "nice person");
        jobInstance.setCfValue("height", 190D);

        assertEquals(jobInstance.getCfValuesAsJson(), "{\"description\":[{\"string\":\"nice person\"}],\"height\":[{\"double\":190.0}]}");

        // Assert
        CustomFieldValues cfValuesNullSafe = jobInstance.getCfValuesNullSafe();
        assertEquals("nice person", cfValuesNullSafe.getValue("description"));
        assertEquals(190D, cfValuesNullSafe.getValue("height"));
    }
}