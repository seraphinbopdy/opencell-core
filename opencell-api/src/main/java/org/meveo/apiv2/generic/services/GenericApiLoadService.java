package org.meveo.apiv2.generic.services;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.meveo.apiv2.generic.ValidationUtils.checkId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.api.restful.util.GenericPagingAndFilteringUtils;
import org.meveo.apiv2.JaxRsActivatorApiV2;
import org.meveo.apiv2.generic.GenericFieldDetails;
import org.meveo.apiv2.generic.ImmutableGenericPaginatedResource;
import org.meveo.apiv2.generic.common.ExcelExportConfiguration;
import org.meveo.apiv2.generic.core.mapper.JsonGenericMapper;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.IEntity;
import org.meveo.model.persistence.CustomFieldJsonDataType;
import org.meveo.service.base.NativePersistenceService;
import org.meveo.service.base.ValueExpressionWrapper;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Stateless
public class GenericApiLoadService {

    @Inject
    JaxRsActivatorApiV2 genericOpencellRestful;

    @Inject
    @Named
    private NativePersistenceService nativePersistenceService;

    @Inject
    private GenericApiPersistenceDelegate persistenceDelegate;
    
    @Inject
    private GenericFileExportManager genericExportManager;

    @Inject
    protected ParamBeanFactory paramBeanFactory;

    @Inject
    private GenericPagingAndFilteringUtils genericPagingAndFilteringUtils;

    public Long count(Class entityClass, PaginationConfiguration searchConfig) {
        searchConfig.setForceCount(true);
        return persistenceDelegate.count(entityClass, searchConfig);
    }

    public String findPaginatedRecords(Boolean extractList, Class entityClass, PaginationConfiguration searchConfig, Set<String> genericFields, Set<String> fetchFields, Long nestedDepth, Long id, Set<String> excludedFields) {

        if(genericFields != null && isAggregationQueries(genericFields)){
            searchConfig.setFetchFields(PaginationConfiguration.migrateOldFieldNames(new ArrayList<>(genericFields)));
            List<List<Object>> list = (List<List<Object>>) nativePersistenceService.getAggregateQuery(entityClass.getCanonicalName(), searchConfig, id)
                    .find(nativePersistenceService.getEntityManager()).stream()
                    .map(Arrays::asList)
                    .collect(toList());

            List<Map<String, Object>> mapResult = list.stream()
                    .map(line -> addResultLine(line, genericFields.iterator()))
                    .collect(toList());
            Map<String, Object> results = new LinkedHashMap<>();
            results.put("total", list.size());
            results.put("limit", genericPagingAndFilteringUtils.getLimit(searchConfig.getLimit()));
            results.put("offset", Long.valueOf(searchConfig.getFirstRow()));
            results.put("data", mapResult);

            return serializeResults(results);
        }else if(genericFields != null &&  isCustomFieldQuery(genericFields)){
        	// get specific custom fields with meta data
        	SearchResult searchResult = persistenceDelegate.list(entityClass, searchConfig);
            searchConfig.setFetchFields(PaginationConfiguration.migrateOldFieldNames(new ArrayList<>(genericFields)));
            List<List<Object>> list = (List<List<Object>>) nativePersistenceService.getQuery(entityClass.getCanonicalName(), searchConfig, id, Boolean.FALSE)
                    .find(nativePersistenceService.getEntityManager()).stream()
                    .map(ObjectArrays -> Arrays.asList(ObjectArrays))
                    .collect(toList());
            List<Map<String, Object>> mapResult = list.stream()
            .map(line -> addResultLine(line, genericFields.iterator()))
            .collect(toList());
            Map<String, Object> results = new LinkedHashMap<String, Object>();
            results.put("total", searchResult.getCount());
            results.put("limit", genericPagingAndFilteringUtils.getLimit(searchConfig.getLimit()));
            results.put("offset", Long.valueOf(searchConfig.getFirstRow()));
            results.put("data", mapResult);
            return serializeResults(results);
        }else{
            SearchResult searchResult = persistenceDelegate.list(entityClass, searchConfig);
            ImmutableGenericPaginatedResource.Builder builder = ImmutableGenericPaginatedResource.builder()
                                                                                                 .data(searchResult.getEntityList())
                                                                                                 .limit(genericPagingAndFilteringUtils.getLimit(searchConfig.getLimit()))
                                                                                                 .offset(Long.valueOf(searchConfig.getFirstRow()))
                                                                                                 .filters(searchConfig.getFilters());
            builder.total(searchResult.getCount());
            ImmutableGenericPaginatedResource genericPaginatedResource = builder.build();
            return JsonGenericMapper.Builder.getBuilder()
                    .withExtractList(Objects.nonNull(extractList) ? extractList : genericOpencellRestful.shouldExtractList())
                    .withNestedEntities(fetchFields)
                    .withNestedDepth(nestedDepth)
                    .build()
                    .toJson(genericFields, entityClass, genericPaginatedResource, excludedFields);
        }
    }


    public List<Map<String, Object>> findAggregatedPaginatedRecords(Class entityClass, PaginationConfiguration searchConfig, Set<String> genericFieldsAlias) {
		List<List<Object>> list = (List<List<Object>>) nativePersistenceService.getQueryWithoutDependencies(entityClass.getCanonicalName(), searchConfig, null)
				.addPaginationConfiguration(searchConfig, "a").find(nativePersistenceService.getEntityManager()).stream().map(ObjectArrays -> Arrays.asList(ObjectArrays)).collect(toList());
		return list.stream().map(line -> addResultLine(line, genericFieldsAlias != null ? genericFieldsAlias.iterator() : searchConfig.getFetchFields().iterator())).collect(toList());
	}

    public String findAggregatedPaginatedRecordsAsString(Class entityClass, String defaultLeftJoinWithAlias, PaginationConfiguration searchConfig) {
        return nativePersistenceService.getQuery(entityClass.getCanonicalName(), defaultLeftJoinWithAlias, searchConfig, null)
                .addPaginationConfiguration(searchConfig, "a").getQueryAsString();
    }

	public int getAggregatedRecordsCount(Class entityClass, PaginationConfiguration searchConfig) {
		return nativePersistenceService.getQueryWithoutDependencies(entityClass.getCanonicalName(), searchConfig, null)
				.find(nativePersistenceService.getEntityManager()).size();
	}
	private Map<String, Object> addResultLine(List<Object> line, Iterator<String> iterator) {
	    
	    List<Object> valueList = line.stream()
                .flatMap(array -> array instanceof Object[] ? flatten((Object[])array) : Stream.of(array))
                .map(l -> Objects.isNull(l) ? "" : l)
                .collect(Collectors.toList());

        Map<String, Object> valueMap = new LinkedHashMap<>();

        for (Object value : valueList) {
            String fieldName = iterator.next();

            if (fieldName.endsWith("cfValues")) {
                value = CustomFieldJsonDataType.INSTANCE.fromString((String) value);
            }

            valueMap.put(fieldName, value);
        }

        return valueMap;
    }
	
    private static Stream<Object> flatten(Object[] array) {
        return Arrays.stream(array)
                .flatMap(o -> o instanceof Object[]? flatten((Object[])o): Stream.of(o));
    }
    private String serializeResult(List<Map<String, Object>> mapResult) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(mapResult);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("json formatting exception", e);
        }
    }
    
    private String serializeResults(Map<String, Object> results) {
               
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY); // AK added, so that in case of immutable classes, private fields, that do not have setters, are also serialized
        mapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true);
        mapper.configure(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS, true); // AK Do not change to false, as it will serialize all getXXX() methods, which might not be related to actual fields
        try {
            return mapper.writeValueAsString(results);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("json formatting exception", e);
        }
    }

    private boolean isAggregationField(String field) {
        return field.startsWith("SUM(") || field.startsWith("COUNT(") || field.startsWith("AVG(")
                || field.startsWith("MAX(") || field.startsWith("MIN(") || field.startsWith("COALESCE(SUM(");
    }
    
    private boolean isCustomField(String field) {
        return field.contains("->>");
    }
    
    public boolean isCustomFieldQuery(Set<String> genericFields) {
        return genericFields.stream()
                .filter(genericField -> isCustomField(genericField))
                .findFirst()
                .isPresent();
    }

    private boolean isAggregationQueries(Set<String> genericFields) {
        return genericFields.stream()
                .filter(genericField -> isAggregationField(genericField))
                .findFirst()
                .isPresent();
    }

    public Optional<String> findByClassNameAndId(Boolean extractList, Class entityClass, Long id, PaginationConfiguration searchConfig, Set<String> genericFields, Set<String> nestedEntities, Long nestedDepth, Set<String> excludedFields) {
        checkId(id);

        IEntity iEntity = persistenceDelegate.findByIdIgnoringCache(entityClass, id, searchConfig.getFetchFields());

        return Optional
                .ofNullable(iEntity)
                .map(entity -> JsonGenericMapper.Builder.getBuilder()
                        .withExtractList(Objects.nonNull(extractList) ? extractList : genericOpencellRestful.shouldExtractList())
                        .withNestedEntities(nestedEntities)
                        .withNestedDepth(nestedDepth)
                        .build()
                        .toJson(genericFields, entityClass, Collections.singletonMap("data", entity), excludedFields));
    }

    public String export(Class entityClass, PaginationConfiguration searchConfig, Set<String> genericFields,
                         List<GenericFieldDetails> genericFieldDetails, String fileFormat, String entityName, String locale,
                         String fieldsSeparator, String decimalSeparator, String fileNameExtension) {
        return export(entityClass, searchConfig, genericFields, genericFieldDetails, fileFormat, entityName, locale, fieldsSeparator, decimalSeparator, fileNameExtension, null);
    }

	public String export(Class entityClass, PaginationConfiguration searchConfig, Set<String> genericFields,
                         List<GenericFieldDetails> genericFieldDetails, String fileFormat, String entityName, String locale,
                         String fieldsSeparator, String decimalSeparator, String fileNameExtension, ExcelExportConfiguration excelExportConfiguration) {

        List<List<Object>> list = extractDataForExport(entityClass, searchConfig, genericFields, genericFieldDetails);

        Map<String, GenericFieldDetails> fieldDetails = new HashMap<>();
        if(CollectionUtils.isNotEmpty(genericFieldDetails)) {
            fieldDetails = genericFieldDetails.stream().collect(toMap(this::nameOrHeader, Function.identity()));
        }
        
        List<GenericFieldDetails> formulaFields = fieldDetails.values().stream()
                .filter(x -> !StringUtils.isEmpty(x.getFormula()))
                .collect(toList());

        Map<String, GenericFieldDetails> finalFieldDetails = fieldDetails;
        Function<List<Object>, Map<String, Object>> originalLine = line -> {
            Map<String, Object> resultLines = new HashMap<>();
			Map<String, Object> inputs = addResultLine(line, searchConfig.getFetchFields().iterator());
			Map<Object, Object> vals = new TreeMap<>(inputs);
			formulaFields.stream()
                    .forEach(x -> resultLines.put(nameOrHeader(x), ValueExpressionWrapper.evaluateExpression(x.getFormula(), vals, Object.class)));
            finalFieldDetails.keySet().stream()
                    .filter(key -> Objects.isNull(finalFieldDetails.get(key).getFormula()))
                    .forEach(key -> resultLines.put(nameOrHeader(finalFieldDetails.get(key)), inputs.get(key)));
			return resultLines;
		};

		return genericExportManager.export(entityName, list.stream().map(originalLine).collect(toList()), fileFormat, fieldDetails,
                genericFieldDetails.stream().map(GenericFieldDetails::getName).collect(Collectors.toList()), locale, fieldsSeparator, decimalSeparator, fileNameExtension, excelExportConfiguration);
	}

    public List<List<Object>> extractDataForExport(Class entityClass, PaginationConfiguration searchConfig, Set<String> genericFields, List<GenericFieldDetails> genericFieldDetails) {
        if (CollectionUtils.isNotEmpty(genericFields)) {
            searchConfig.setFetchFields(new ArrayList<>(genericFields));
        } else if (CollectionUtils.isNotEmpty(genericFieldDetails)) {
            searchConfig.setFetchFields(genericFieldDetails.stream()
                                                           .filter(x -> StringUtils.isEmpty(x.getFormula()))
                                                           .map(this::nameOrHeader)
                                                           .collect(toList()));
            searchConfig.getFetchFields()
                        .addAll(genericFieldDetails.stream()
                                                   .filter(x -> !StringUtils.isEmpty(x.getFormulaInputs()))
                                                   .map(x -> Arrays.asList(x.getFormulaInputs().split(",")))
                                                   .flatMap(List::stream)
                                                   .map(String::trim)
                                                   .collect(Collectors.toList()));
        }
        
        return (List<List<Object>>) nativePersistenceService.getQuery(entityClass.getCanonicalName(), searchConfig, null, Boolean.TRUE)
                                                                               .find(nativePersistenceService.getEntityManager())
                                                                               .stream()
                                                                               .map(ObjectArrays -> Arrays.asList(ObjectArrays))
                                                                               .collect(toList());
    }

    private String nameOrHeader(GenericFieldDetails x) {
		return Optional.ofNullable(x.getName()).orElse(x.getHeader());
	}

}
