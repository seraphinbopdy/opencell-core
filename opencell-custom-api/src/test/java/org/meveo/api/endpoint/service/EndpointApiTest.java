
package org.meveo.api.endpoint.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.dto.endpoint.EndpointDto;
import org.meveo.api.dto.endpoint.EndpointParameterMappingDto;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.model.endpoint.Endpoint;
import org.meveo.model.endpoint.EndpointHttpMethod;
import org.meveo.model.endpoint.EndpointParameterMapping;
import org.meveo.model.endpoint.EndpointPathParameter;
import org.meveo.model.endpoint.MimeContentTypeEnum;
import org.meveo.model.scripts.ScriptInstance;
import org.meveo.service.endpoint.EndpointService;
import org.meveo.service.script.ScriptInstanceService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

/**
 * A class to test functionality of EndpointApi
 */
@RunWith(MockitoJUnitRunner.class)
public class EndpointApiTest {

    @InjectMocks
    private EndpointApi endpointApi;

    @Mock
    private EndpointService endpointService;

    @Mock
    private ScriptInstanceService scriptInstanceService;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test(expected = EntityAlreadyExistsException.class)
    public void testCreate_WhenEndpointAlreadyExists() throws MeveoApiException, BusinessException {
        // Prepare test data
        EndpointDto endpointDto = new EndpointDto();
        endpointDto.setCode("testEndpoint");

        // Mock the behavior of endpointService
        Endpoint existingEndpoint = new Endpoint();
        Mockito.when(endpointService.findByCode(endpointDto.getCode())).thenReturn(existingEndpoint);

        // Call the method to be tested
        endpointApi.create(endpointDto);
    }

    @Test(expected = EntityDoesNotExistsException.class)
    public void testUpdate_WhenEndpointDoesNotExist() throws MeveoApiException, BusinessException {
        // Prepare test data
        EndpointDto endpointDto = new EndpointDto();
        endpointDto.setCode("testEndpoint");

        // Mock the behavior of endpointService
        Mockito.when(endpointService.findByCode(endpointDto.getCode())).thenReturn(null);

        // Call the method to be tested
        endpointApi.update(endpointDto);
    }

    @Test
    public void testCreate_WhenEndpointDoesNotExist() throws MeveoApiException, BusinessException {

        // Prepare test data
        EndpointDto endpointDto = new EndpointDto();
        endpointDto.setCode("testCode");
        endpointDto.setSecured(true);
        endpointDto.setSerializeResult(true);
        endpointDto.setReturnedVariableName("testReturnedVariableName");
        endpointDto.setJsonataTransformer("testJsonataTransformer");
        endpointDto.setServiceCode("testServiceCode");
        endpointDto.setContentType("application/json");
        endpointDto.setPath("testPath");
        endpointDto.setBasePath("basePath");
        endpointDto.setMethod(EndpointHttpMethod.POST);
        endpointDto.setDescription("testDescription");
        endpointDto.setReturnedValueExample("return example");
        endpointDto.setReturnedVariableName("testReturnedVariableName");
        endpointDto.setPathParameters(new ArrayList<>());
        endpointDto.getPathParameters().add("testPathParameter1");
        endpointDto.getPathParameters().add("testPathParameter2");

        endpointDto.setParameterMappings(new ArrayList<>());
        EndpointParameterMappingDto endpointParameterMappingDto = new EndpointParameterMappingDto();
        endpointParameterMappingDto.setParameterName("testParameterName");
        endpointParameterMappingDto.setDefaultValue("testDefaultValue");
        endpointParameterMappingDto.setMultivalued(true);
        endpointParameterMappingDto.setScriptParameter("testScriptParameter");
        endpointParameterMappingDto.setValueRequired(true);
        endpointParameterMappingDto.setDescription("testDescription");
        endpointParameterMappingDto.setExample("testExample");
        endpointDto.getParameterMappings().add(endpointParameterMappingDto);

        // Mock the behavior of endpointService
        Mockito.when(endpointService.findByCode(endpointDto.getCode())).thenReturn(null);
        // Mock the behavior of scriptInstanceService
        ScriptInstance scriptInstance = new ScriptInstance();
        scriptInstance.setCode(endpointDto.getServiceCode());
        Mockito.when(scriptInstanceService.findByCode(endpointDto.getServiceCode())).thenReturn(scriptInstance);

        // Call the method to be tested
        Endpoint createdEndpoint = endpointApi.create(endpointDto);

        // Assert the result
        Assert.assertNotNull(createdEndpoint);
        Assert.assertEquals(endpointDto.getCode(), createdEndpoint.getCode());
        Assert.assertEquals(endpointDto.getSecured(), createdEndpoint.isSecured());
        Assert.assertEquals(endpointDto.getSerializeResult(), createdEndpoint.isSerializeResult());
        Assert.assertEquals(endpointDto.getReturnedVariableName(), createdEndpoint.getReturnedVariableName());
        Assert.assertEquals(endpointDto.getJsonataTransformer(), createdEndpoint.getJsonataTransformer());
        Assert.assertEquals(endpointDto.getServiceCode(), createdEndpoint.getService().getCode());
        Assert.assertEquals(endpointDto.getContentType(), createdEndpoint.getContentType().getValue());
        Assert.assertEquals(endpointDto.getPath(), createdEndpoint.getPath());
        Assert.assertEquals(endpointDto.getBasePath(), createdEndpoint.getBasePath());
        Assert.assertEquals(endpointDto.getMethod(), createdEndpoint.getMethod());
        Assert.assertEquals(endpointDto.getDescription(), createdEndpoint.getDescription());
        Assert.assertEquals(endpointDto.getReturnedValueExample(), createdEndpoint.getReturnedValueExample());
        Assert.assertEquals(endpointDto.getPathParameters().size(), createdEndpoint.getPathParameters().size());
        Assert.assertEquals(endpointDto.getPathParameters().get(0), createdEndpoint.getPathParameters().get(0).getScriptParameter());
        Assert.assertEquals((String) endpointDto.getPathParameters().get(1), createdEndpoint.getPathParameters().get(1).getScriptParameter());
        Assert.assertEquals(endpointDto.getParameterMappings().size(), createdEndpoint.getParametersMapping().size());
        Assert.assertEquals(endpointDto.getParameterMappings().get(0).getParameterName(), createdEndpoint.getParametersMapping().get(0).getParameterName());
        Assert.assertEquals(endpointDto.getParameterMappings().get(0).getDefaultValue(), createdEndpoint.getParametersMapping().get(0).getDefaultValue());
        Assert.assertEquals(endpointDto.getParameterMappings().get(0).getMultivalued(), createdEndpoint.getParametersMapping().get(0).isMultivaluedAsBoolean());
        Assert.assertEquals(endpointDto.getParameterMappings().get(0).getScriptParameter(), createdEndpoint.getParametersMapping().get(0).getScriptParameter());
        Assert.assertEquals(endpointDto.getParameterMappings().get(0).getValueRequired(), createdEndpoint.getParametersMapping().get(0).isValueRequiredAsBoolean());
        Assert.assertEquals(endpointDto.getParameterMappings().get(0).getDescription(), createdEndpoint.getParametersMapping().get(0).getDescription());
        Assert.assertEquals(endpointDto.getParameterMappings().get(0).getExample(), createdEndpoint.getParametersMapping().get(0).getExample());

    }

    @Test
    public void testCreate_EndpointPathAndBasePath() throws MeveoApiException, BusinessException {

        // Prepare test data
        EndpointDto endpointDto = new EndpointDto();
        endpointDto.setCode("testCode");
        endpointDto.setServiceCode("testServiceCode");
        endpointDto.setContentType("application/json");
        endpointDto.setMethod(EndpointHttpMethod.POST);
        endpointDto.setDescription("testDescription");
        endpointDto.setPathParameters(new ArrayList<>());
        endpointDto.getPathParameters().add("testPathParameter1");
        endpointDto.getPathParameters().add("testPathParameter2");

        endpointDto.setParameterMappings(new ArrayList<>());
        EndpointParameterMappingDto endpointParameterMappingDto = new EndpointParameterMappingDto();
        endpointParameterMappingDto.setParameterName("testParameterName");
        endpointParameterMappingDto.setDefaultValue("testDefaultValue");
        endpointParameterMappingDto.setMultivalued(true);
        endpointParameterMappingDto.setScriptParameter("testScriptParameter");
        endpointParameterMappingDto.setValueRequired(true);
        endpointParameterMappingDto.setDescription("testDescription");
        endpointParameterMappingDto.setExample("testExample");
        endpointDto.getParameterMappings().add(endpointParameterMappingDto);

        // Mock the behavior of endpointService
        Mockito.when(endpointService.findByCode(endpointDto.getCode())).thenReturn(null);
        // Mock the behavior of scriptInstanceService
        ScriptInstance scriptInstance = new ScriptInstance();
        scriptInstance.setCode(endpointDto.getServiceCode());
        Mockito.when(scriptInstanceService.findByCode(endpointDto.getServiceCode())).thenReturn(scriptInstance);

        // Call the method to be tested
        Endpoint createdEndpoint = endpointApi.create(endpointDto);

        // Assert the result
        Assert.assertNotNull(createdEndpoint);
        Assert.assertEquals(endpointDto.getCode(), createdEndpoint.getCode());
        Assert.assertEquals(endpointDto.getServiceCode(), createdEndpoint.getService().getCode());
        Assert.assertEquals(endpointDto.getContentType(), createdEndpoint.getContentType().getValue());
        Assert.assertEquals("/{" + createdEndpoint.getPathParameters().get(0).getScriptParameter() + "}/{" + createdEndpoint.getPathParameters().get(1).getScriptParameter() + "}", createdEndpoint.getPath());
        Assert.assertEquals(createdEndpoint.getCode().toLowerCase(), createdEndpoint.getBasePath());
        Assert.assertEquals(endpointDto.getMethod(), createdEndpoint.getMethod());
        Assert.assertEquals(endpointDto.getPathParameters().size(), createdEndpoint.getPathParameters().size());
        Assert.assertEquals(endpointDto.getPathParameters().get(0), createdEndpoint.getPathParameters().get(0).getScriptParameter());
        Assert.assertEquals((String) endpointDto.getPathParameters().get(1), createdEndpoint.getPathParameters().get(1).getScriptParameter());
        Assert.assertEquals(endpointDto.getParameterMappings().size(), createdEndpoint.getParametersMapping().size());
        Assert.assertEquals(endpointDto.getParameterMappings().get(0).getParameterName(), createdEndpoint.getParametersMapping().get(0).getParameterName());
        Assert.assertEquals(endpointDto.getParameterMappings().get(0).getDefaultValue(), createdEndpoint.getParametersMapping().get(0).getDefaultValue());
        Assert.assertEquals(endpointDto.getParameterMappings().get(0).getMultivalued(), createdEndpoint.getParametersMapping().get(0).isMultivaluedAsBoolean());
        Assert.assertEquals(endpointDto.getParameterMappings().get(0).getScriptParameter(), createdEndpoint.getParametersMapping().get(0).getScriptParameter());
        Assert.assertEquals(endpointDto.getParameterMappings().get(0).getValueRequired(), createdEndpoint.getParametersMapping().get(0).isValueRequiredAsBoolean());
        Assert.assertEquals(endpointDto.getParameterMappings().get(0).getDescription(), createdEndpoint.getParametersMapping().get(0).getDescription());
        Assert.assertEquals(endpointDto.getParameterMappings().get(0).getExample(), createdEndpoint.getParametersMapping().get(0).getExample());

    }

    @Test(expected = NullPointerException.class)
    public void testCreate_WhenEndpointDtoIsNull() throws MeveoApiException, BusinessException {
        // Call the method to be tested with null
        endpointApi.create(null);
    }

    @Test(expected = NullPointerException.class)
    public void testUpdate_WhenEndpointDtoIsNull() throws MeveoApiException, BusinessException {
        // Call the method to be tested with null
        endpointApi.update(null);
    }

    @Test
    public void testUpdate_WhenEndpointExists() throws MeveoApiException, BusinessException {

        // Prepare test data
        EndpointDto endpointDto = new EndpointDto();
        endpointDto.setCode("testCode");
        endpointDto.setSecured(true);
        endpointDto.setSerializeResult(true);
        endpointDto.setReturnedVariableName("testReturnedVariableName");
        endpointDto.setJsonataTransformer("testJsonataTransformer");
        endpointDto.setServiceCode("testServiceCode");
        endpointDto.setContentType("application/json");
        endpointDto.setPath("testPath");
        endpointDto.setBasePath("basePath");
        endpointDto.setMethod(EndpointHttpMethod.POST);
        endpointDto.setDescription("testDescription");
        endpointDto.setReturnedValueExample("return example");
        endpointDto.setReturnedVariableName("testReturnedVariableName");
        endpointDto.setPathParameters(new ArrayList<>());
        endpointDto.getPathParameters().add("testPathParameter1");
        endpointDto.getPathParameters().add("testPathParameter2");

        endpointDto.setParameterMappings(new ArrayList<>());
        EndpointParameterMappingDto endpointParameterMappingDto = new EndpointParameterMappingDto();
        endpointParameterMappingDto.setParameterName("testParameterName");
        endpointParameterMappingDto.setDefaultValue("testDefaultValue");
        endpointParameterMappingDto.setMultivalued(true);
        endpointParameterMappingDto.setScriptParameter("testScriptParameter");
        endpointParameterMappingDto.setValueRequired(true);
        endpointParameterMappingDto.setDescription("testDescription");
        endpointParameterMappingDto.setExample("testExample");
        endpointDto.getParameterMappings().add(endpointParameterMappingDto);

        // Mock the behavior of endpointService
        Endpoint existingEndpoint = new Endpoint();
        existingEndpoint.setCode(endpointDto.getCode());
        existingEndpoint.setSecured(false);
        existingEndpoint.setSerializeResult(false);
        existingEndpoint.setReturnedVariableName("existingReturnedVariableName");
        existingEndpoint.setJsonataTransformer("existingJsonataTransformer");
        existingEndpoint.setService(new ScriptInstance());
        existingEndpoint.getService().setCode("existingServiceCode");
        existingEndpoint.setContentType(MimeContentTypeEnum.APPLICATION_XML);
        existingEndpoint.setPath("existingPath");
        existingEndpoint.setBasePath("existingBasePath");
        existingEndpoint.setMethod(EndpointHttpMethod.GET);
        existingEndpoint.setDescription("existingDescription");
        existingEndpoint.setReturnedValueExample("existingReturnedValueExample");

        existingEndpoint.setPathParameters(new ArrayList<>());

        EndpointPathParameter existingPathParameter1 = new EndpointPathParameter();
        existingEndpoint.getPathParameters().add(existingPathParameter1);
        existingPathParameter1.setScriptParameter("existingPathParameter1");

        EndpointPathParameter existingPathParameter2 = new EndpointPathParameter();
        existingEndpoint.getPathParameters().add(existingPathParameter2);
        existingPathParameter2.setScriptParameter("existingPathParameter2");

        existingEndpoint.setParametersMapping(new ArrayList<>());

        EndpointParameterMapping existingEndpointParameterMapping1 = new EndpointParameterMapping();
        existingEndpoint.getParametersMapping().add(existingEndpointParameterMapping1);
        existingEndpointParameterMapping1.setParameterName("existingParameterName");
        existingEndpointParameterMapping1.setDefaultValue("existingDefaultValue");
        existingEndpointParameterMapping1.setMultivaluedAsBoolean(false);
        existingEndpointParameterMapping1.setScriptParameter("existingScriptParameter");
        existingEndpointParameterMapping1.setValueRequiredAsBoolean(false);
        existingEndpointParameterMapping1.setDescription("existingDescription");
        existingEndpointParameterMapping1.setExample("existingExample");

        EndpointParameterMapping existingEndpointParameterMapping2 = new EndpointParameterMapping();
        existingEndpoint.getParametersMapping().add(existingEndpointParameterMapping2);
        existingEndpointParameterMapping2.setParameterName("existingParameterName2");
        existingEndpointParameterMapping2.setDefaultValue("existingDefaultValue2");
        existingEndpointParameterMapping2.setMultivaluedAsBoolean(true);
        existingEndpointParameterMapping2.setScriptParameter("existingScriptParameter2");
        existingEndpointParameterMapping2.setValueRequiredAsBoolean(true);
        existingEndpointParameterMapping2.setDescription("existingDescription2");
        existingEndpointParameterMapping2.setExample("existingExample2");

        // Mock the behavior of endpointService
        Mockito.when(endpointService.findByCode(endpointDto.getCode())).thenReturn(existingEndpoint);

        doAnswer(new Answer<Endpoint>() {
            public Endpoint answer(InvocationOnMock invocation) throws Throwable {
                return ((Endpoint) invocation.getArguments()[0]);
            }
        }).when(endpointService).update(any());

        // Mock the behavior of scriptInstanceService
        ScriptInstance scriptInstance = new ScriptInstance();
        scriptInstance.setCode(endpointDto.getServiceCode());
        Mockito.when(scriptInstanceService.findByCode(endpointDto.getServiceCode())).thenReturn(scriptInstance);

        // Call the method to be tested
        Endpoint updatedEndpoint = endpointApi.update(endpointDto);

        // Assert the result
        Assert.assertNotNull(updatedEndpoint);

        Assert.assertEquals(endpointDto.getCode(), updatedEndpoint.getCode());
        Assert.assertEquals(endpointDto.getSecured(), updatedEndpoint.isSecured());
        Assert.assertEquals(endpointDto.getSerializeResult(), updatedEndpoint.isSerializeResult());
        Assert.assertEquals(endpointDto.getReturnedVariableName(), updatedEndpoint.getReturnedVariableName());
        Assert.assertEquals(endpointDto.getJsonataTransformer(), updatedEndpoint.getJsonataTransformer());
        Assert.assertEquals(endpointDto.getServiceCode(), updatedEndpoint.getService().getCode());
        Assert.assertEquals(endpointDto.getContentType(), updatedEndpoint.getContentType().getValue());
        Assert.assertEquals(endpointDto.getPath(), updatedEndpoint.getPath());
        Assert.assertEquals(endpointDto.getBasePath(), updatedEndpoint.getBasePath());
        Assert.assertEquals(endpointDto.getMethod(), updatedEndpoint.getMethod());
        Assert.assertEquals(endpointDto.getDescription(), updatedEndpoint.getDescription());
        Assert.assertEquals(endpointDto.getReturnedValueExample(), updatedEndpoint.getReturnedValueExample());
        Assert.assertEquals(endpointDto.getPathParameters().get(0), updatedEndpoint.getPathParameters().get(0).getScriptParameter());
        Assert.assertEquals(endpointDto.getPathParameters().get(1), updatedEndpoint.getPathParameters().get(1).getScriptParameter());
        Assert.assertEquals(endpointDto.getParameterMappings().size(), updatedEndpoint.getParametersMapping().size());
        Assert.assertEquals(endpointDto.getParameterMappings().get(0).getParameterName(), updatedEndpoint.getParametersMapping().get(0).getParameterName());
        Assert.assertEquals(endpointDto.getParameterMappings().get(0).getDefaultValue(), updatedEndpoint.getParametersMapping().get(0).getDefaultValue());
        Assert.assertEquals(endpointDto.getParameterMappings().get(0).getMultivalued(), updatedEndpoint.getParametersMapping().get(0).isMultivaluedAsBoolean());
        Assert.assertEquals(endpointDto.getParameterMappings().get(0).getScriptParameter(), updatedEndpoint.getParametersMapping().get(0).getScriptParameter());
        Assert.assertEquals(endpointDto.getParameterMappings().get(0).getValueRequired(), updatedEndpoint.getParametersMapping().get(0).isValueRequiredAsBoolean());
        Assert.assertEquals(endpointDto.getParameterMappings().get(0).getDescription(), updatedEndpoint.getParametersMapping().get(0).getDescription());
        Assert.assertEquals(endpointDto.getParameterMappings().get(0).getExample(), updatedEndpoint.getParametersMapping().get(0).getExample());
    }

    @Test
    public void testUpdate_WhenEndpointExists_noChange() throws MeveoApiException, BusinessException {

        // Prepare test data
        EndpointDto endpointDto = new EndpointDto();
        endpointDto.setCode("testCode");

        // Mock the behavior of endpointService
        Endpoint existingEndpoint = new Endpoint();
        existingEndpoint.setCode(endpointDto.getCode());
        existingEndpoint.setSecured(false);
        existingEndpoint.setSynchronous(false);
        existingEndpoint.setSerializeResult(true);
        existingEndpoint.setReturnedVariableName("existingReturnedVariableName");
        existingEndpoint.setJsonataTransformer("existingJsonataTransformer");
        existingEndpoint.setService(new ScriptInstance());
        existingEndpoint.getService().setCode("existingServiceCode");
        existingEndpoint.setContentType(MimeContentTypeEnum.APPLICATION_XML);
        existingEndpoint.setPath("existingPath");
        existingEndpoint.setBasePath("existingBasePath");
        existingEndpoint.setMethod(EndpointHttpMethod.GET);
        existingEndpoint.setDescription("existingDescription");
        existingEndpoint.setReturnedValueExample("existingReturnedValueExample");

        existingEndpoint.setPathParameters(new ArrayList<>());

        EndpointPathParameter existingPathParameter1 = new EndpointPathParameter();
        existingEndpoint.getPathParameters().add(existingPathParameter1);
        existingPathParameter1.setScriptParameter("existingPathParameter1");

        EndpointPathParameter existingPathParameter2 = new EndpointPathParameter();
        existingEndpoint.getPathParameters().add(existingPathParameter2);
        existingPathParameter2.setScriptParameter("existingPathParameter2");

        existingEndpoint.setParametersMapping(new ArrayList<>());

        EndpointParameterMapping existingEndpointParameterMapping = new EndpointParameterMapping();
        existingEndpoint.getParametersMapping().add(existingEndpointParameterMapping);
        existingEndpointParameterMapping.setParameterName("existingParameterName");
        existingEndpointParameterMapping.setDefaultValue("existingDefaultValue");
        existingEndpointParameterMapping.setMultivaluedAsBoolean(true);
        existingEndpointParameterMapping.setScriptParameter("existingScriptParameter");
        existingEndpointParameterMapping.setValueRequiredAsBoolean(true);
        existingEndpointParameterMapping.setDescription("existingDescription");
        existingEndpointParameterMapping.setExample("existingExample");

        // Mock the behavior of endpointService
        Mockito.when(endpointService.findByCode(endpointDto.getCode())).thenReturn(existingEndpoint);

        doAnswer(new Answer<Endpoint>() {
            public Endpoint answer(InvocationOnMock invocation) throws Throwable {
                return ((Endpoint) invocation.getArguments()[0]);
            }
        }).when(endpointService).update(any());

        // Call the method to be tested
        Endpoint updatedEndpoint = endpointApi.update(endpointDto);

        // Assert the result
        Assert.assertNotNull(updatedEndpoint);

        Assert.assertEquals("testCode", updatedEndpoint.getCode());
        Assert.assertEquals(false, updatedEndpoint.isSecured());
        Assert.assertEquals(false, updatedEndpoint.isSynchronous());
        Assert.assertEquals(true, updatedEndpoint.isSerializeResult());
        Assert.assertEquals("existingReturnedVariableName", updatedEndpoint.getReturnedVariableName());
        Assert.assertEquals("existingJsonataTransformer", updatedEndpoint.getJsonataTransformer());
        Assert.assertEquals("existingServiceCode", updatedEndpoint.getService().getCode());
        Assert.assertEquals("application/xml", updatedEndpoint.getContentType().getValue());
        Assert.assertEquals("existingPath", updatedEndpoint.getPath());
        Assert.assertEquals("existingBasePath", updatedEndpoint.getBasePath());
        Assert.assertEquals(EndpointHttpMethod.GET, updatedEndpoint.getMethod());
        Assert.assertEquals("existingDescription", updatedEndpoint.getDescription());
        Assert.assertEquals("existingReturnedValueExample", updatedEndpoint.getReturnedValueExample());
        Assert.assertEquals(2, updatedEndpoint.getPathParameters().size());
        Assert.assertEquals("existingPathParameter1", updatedEndpoint.getPathParameters().get(0).getScriptParameter());
        Assert.assertEquals("existingPathParameter2", updatedEndpoint.getPathParameters().get(1).getScriptParameter());
        Assert.assertEquals(1, updatedEndpoint.getParametersMapping().size());
        Assert.assertEquals("existingParameterName", updatedEndpoint.getParametersMapping().get(0).getParameterName());
        Assert.assertEquals("existingDefaultValue", updatedEndpoint.getParametersMapping().get(0).getDefaultValue());
        Assert.assertEquals(true, updatedEndpoint.getParametersMapping().get(0).isMultivaluedAsBoolean());
        Assert.assertEquals("existingScriptParameter", updatedEndpoint.getParametersMapping().get(0).getScriptParameter());
        Assert.assertEquals(true, updatedEndpoint.getParametersMapping().get(0).isValueRequiredAsBoolean());
        Assert.assertEquals("existingDescription", updatedEndpoint.getParametersMapping().get(0).getDescription());
        Assert.assertEquals("existingExample", updatedEndpoint.getParametersMapping().get(0).getExample());
    }

}