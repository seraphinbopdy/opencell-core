package org.meveo.api.swagger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.model.customEntities.CustomEntityTemplate;
import org.meveo.service.crm.impl.CustomFieldTemplateService;
import org.meveo.service.custom.CustomEntityTemplateService;

import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import jakarta.inject.Inject;

/**
 * Utility class for Swagger documentation.
 * 
 * @author Edward P. Legaspi | czetsuya@gmail.com
 * @since 6.8.0
 * @version 6.9.0
 */
public class SwaggerHelperService {

    @Inject
    private CustomEntityTemplateService customEntityTemplateService;

    @Inject
    private CustomFieldTemplateService customFieldTemplateService;

    /**
     * Build a OpenAPI v3 schema definition for a Custom field template type variable
     * 
     * @param cft Custom field template
     * @return OpenAPI v3 schema definition
     */
    @SuppressWarnings("rawtypes")
    private Schema convertCftToSchema(CustomFieldTemplate cft) {

        Schema schema = new Schema();

        switch (cft.getFieldType()) {

        case STRING:
            schema = new StringSchema();
            break;

        case DATE:
            schema = new DateSchema();
            break;

        case LONG:
            schema = new IntegerSchema();
            break;

        case DOUBLE:
            schema = new NumberSchema();
            break;

        case LIST:
            schema.setName(CustomFieldTypeEnum.LIST.name());
            break;

        case ENTITY:
            CustomEntityTemplate cet = customEntityTemplateService.findByCode(cft.getEntityClazzCetCode());
            if (cet != null) {
                schema = buildSchemaForCetTypeValue(cet);
            }
            schema.setName(CustomFieldTypeEnum.ENTITY.name());
            break;

        case TEXT_AREA:
            schema.setName(CustomFieldTypeEnum.TEXT_AREA.name());
            break;

        case CHILD_ENTITY:
            schema.setName(CustomFieldTypeEnum.CHILD_ENTITY.name());
            break;

        case MULTI_VALUE:
            schema.setName(CustomFieldTypeEnum.MULTI_VALUE.name());
            break;

        case BOOLEAN:
            schema = new BooleanSchema();
            break;

        case CUSTOM_TABLE_WRAPPER:
            cet = customEntityTemplateService.findByCode(cft.getEntityClazzCetCode());
            if (cet != null) {
                schema = buildSchemaForCetTypeValue(cet);
            }
            schema.setName(CustomFieldTypeEnum.CUSTOM_TABLE_WRAPPER.name());
            break;
        default:
            schema.setType(cft.getFieldType().toString().toLowerCase());
            break;
        }

        schema.setName(cft.getCode());
        schema.setTitle(cft.getDescription());
        if (cft.getDefaultValue() != null) {
            schema.setDefault(schema);
        }

        return schema;
    }

    /**
     * Retrieves a list of required {@link CustomFieldTemplate} names
     * 
     * @param cfts list of custom fields
     * @return A list of required custom field names
     * @see CustomFieldTemplate
     */
    private List<String> getRequiredFieldNames(Map<String, CustomFieldTemplate> cfts) {

        List<String> result = new ArrayList<>();
        if (!cfts.isEmpty()) {
            result = cfts.entrySet().stream().filter(e -> e.getValue().isValueRequired()).map(e -> e.getValue().getCode()).collect(Collectors.toList());
        }

        return result;
    }

    /**
     * Build a OpenAPI v3 schema definition for a Custom entity template type variable
     * 
     * @param cet Custom entity template
     * @return OpenAPI v3 schema definition
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Schema buildSchemaForCetTypeValue(CustomEntityTemplate cet) {

        Map<String, CustomFieldTemplate> cfts = customFieldTemplateService.findByAppliesTo(cet.getAppliesTo());

        Schema schema = new Schema();
        schema.setType("object");
        schema.setDescription(cet.getDescription());
        schema.setFormat("cet-" + cet.getCode());

        Map<String, Schema> properties = new HashMap<>();

        if (!cfts.isEmpty()) {
            for (CustomFieldTemplate cft : cfts.values()) {
                Schema cftSchema = convertCftToSchema(cft);
                properties.put(cft.getCode(), cftSchema);
            }
        }

        schema.setProperties(properties);
        schema.setRequired(getRequiredFieldNames(cfts));

        return schema;
    }

    /**
     * Build a OpenAPI v3 schema definition for a primitive type variable
     * 
     * @param variableName Variable name
     * @param variableType Variable type
     * @param isRequired Is value required
     * @param defaultValue A default value
     * @return OpenAPI v3 schema definition
     */
    @SuppressWarnings("rawtypes")
    public Schema buildSchemaForPrimitiveTypeValue(String variableName, String variableType, boolean isRequired, String defaultValue) {

        variableType = variableType.toLowerCase();

        String format = null;
        if (variableType.equals("long")) {
            variableType = "integer";
            format = "int64";

        } else if (variableType.equals("double")) {
            variableType = "number";
            format = "double";
        }

        Schema schema = new Schema();

        schema.setDescription(variableName);
        schema.setTitle(variableName);
        schema.setNullable(!isRequired);
        schema.setDefault(defaultValue);
        schema.setFormat(format);
        schema.setType(variableType);
        return schema;
    }

    /**
     * Build a OpenAPI v3 schema definition for an Object type variable
     * 
     * @param variableName Variable name
     * @return OpenAPI v3 schema definition
     */
    public ObjectSchema buildSchemaForObjectTypeValue(String variableName) {

        ObjectSchema schema = new ObjectSchema();

        schema.setDescription(variableName);
        schema.setTitle(variableName);

        return schema;
    }
}
