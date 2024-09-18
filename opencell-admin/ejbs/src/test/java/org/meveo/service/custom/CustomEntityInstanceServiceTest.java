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

package org.meveo.service.custom;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;

import org.junit.Test;
import org.meveo.model.customEntities.CustomEntityInstance;

public class CustomEntityInstanceServiceTest {

    private CustomEntityInstanceService sut = new CustomEntityInstanceService();

    @Test
    public void should_transform_custom_entity_instance_to_map() {
        //Given
        CustomEntityInstance instance = new CustomEntityInstance();
        //When
        HashMap<String, Object> transformedMap = sut.customEntityInstanceAsMap(instance);
        //Then
        assertThat(transformedMap).isNotNull();
        assertThat(transformedMap).hasSize(3);
        assertThat(transformedMap).containsKeys("code", "description", "id");
    }

    @Test
    public void should_transform_custom_entity_instance_to_map_with_cf_values() {
        //Given
        CustomEntityInstance instance = new CustomEntityInstance();
        //When
        HashMap<String, Object> transformedMap = sut.customEntityInstanceAsMapWithCfValues(instance);
        //Then
        assertThat(transformedMap).isNotNull();
        assertThat(transformedMap).hasSize(4);
        assertThat(transformedMap).containsKeys("cfValues");
    }
}