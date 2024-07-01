package org.meveo.service.script;

import static org.mockito.ArgumentMatchers.any;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * A class to unit test functionality in ScriptInstanceService
 */
@RunWith(MockitoJUnitRunner.class)
public class ScriptInstanceServiceTest {

    @Spy
    @InjectMocks
    private ScriptInstanceService scriptInstanceService;

    // Generate a unit test for applyParametersToScriptInstance method in ScriptInstanceService class
    // Test that given the parameters and ScriptInterfaceImplForTest class, the method will apply setters to the instance of class ScriptInterfaceImplForTest

    @Test
    public void testParameterSettingOnScript() {
        // Given
        ScriptInterfaceImplForTest scriptInterfaceImplForTest = new ScriptInterfaceImplForTest();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("intValue", 2);
        parameters.put("booleanValue", false);
        parameters.put("doubleValue", 2.0);
        parameters.put("longValue", 2L);
        parameters.put("stringValue", "string2");
        parameters.put("integerValue", 2);
        parameters.put("booleanObjectValue", false);
        parameters.put("doubleObjectValue", 2.0);
        parameters.put("longObjectValue", 2L);
        parameters.put("listStringValue", List.of("string2"));
        parameters.put("listIntegerValue", List.of(2));
        parameters.put("listBooleanValue", List.of(false));
        parameters.put("listDoubleValue", List.of(2.0));
        parameters.put("listLongValue", List.of(2L));
        parameters.put("mapStringValue", Map.of("key", "value2"));
        parameters.put("mapIntegerValue", Map.of("key", 2));
        parameters.put("mapBooleanValue", Map.of("key", false));
        parameters.put("mapDoubleValue", Map.of("key", 2.0));
        parameters.put("mapLongValue", Map.of("key", 2L));

        // Mock the method getScriptInstance and return the instance of ScriptInterfaceImplForTest
        // Use the form "doAnswer().when().method" instead of "when().thenAnswer()" because on spied object the later will call a real method at the setup time, which will fail because of null values being passed.
        Mockito.doReturn(scriptInterfaceImplForTest).when(scriptInstanceService).getScriptInstance(any());

        // When
        scriptInstanceService.execute("ScriptInterfaceImplForTest", parameters, false, false, false);
        
//        scriptInstanceService.applyParametersToScriptInstance(scriptInterfaceImplForTest, parameters);

        // Then
        // Assert that getters of the instance of ScriptInterfaceImplForTest, corresponding to parameter names, return the values set by the method applyParametersToScriptInstance
        Assert.assertEquals(2, scriptInterfaceImplForTest.getIntValue());
        Assert.assertEquals(false, scriptInterfaceImplForTest.isBooleanValue());
        Assert.assertEquals(2.0, scriptInterfaceImplForTest.getDoubleValue(), 0.0);
        Assert.assertEquals(2L, scriptInterfaceImplForTest.getLongValue());
        Assert.assertEquals("string2", scriptInterfaceImplForTest.getStringValue());
        Assert.assertEquals(Integer.valueOf(2), scriptInterfaceImplForTest.getIntegerValue());
        Assert.assertEquals(false, scriptInterfaceImplForTest.getBooleanObjectValue());
        Assert.assertEquals(2.0, scriptInterfaceImplForTest.getDoubleObjectValue(), 0.0);
        Assert.assertEquals(2L, scriptInterfaceImplForTest.getLongObjectValue().longValue());
        Assert.assertEquals(List.of("string2"), scriptInterfaceImplForTest.getListStringValue());
        Assert.assertEquals(List.of(2), scriptInterfaceImplForTest.getListIntegerValue());
        Assert.assertEquals(List.of(false), scriptInterfaceImplForTest.getListBooleanValue());
        Assert.assertEquals(List.of(2.0), scriptInterfaceImplForTest.getListDoubleValue());
        Assert.assertEquals(List.of(2L), scriptInterfaceImplForTest.getListLongValue());
        Assert.assertEquals(Map.of("key", "value2"), scriptInterfaceImplForTest.getMapStringValue());
        Assert.assertEquals(Map.of("key", 2), scriptInterfaceImplForTest.getMapIntegerValue());
        Assert.assertEquals(Map.of("key", false), scriptInterfaceImplForTest.getMapBooleanValue());
        Assert.assertEquals(Map.of("key", 2.0), scriptInterfaceImplForTest.getMapDoubleValue());
        Assert.assertEquals(Map.of("key", 2L), scriptInterfaceImplForTest.getMapLongValue());
    }
}