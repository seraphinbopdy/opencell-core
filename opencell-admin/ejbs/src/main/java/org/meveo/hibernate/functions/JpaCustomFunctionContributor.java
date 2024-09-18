package org.meveo.hibernate.functions;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;

/**
 * A custom JPA/Hibernate function registration based on dialect used. <br/>
 * Enable org.hibernate.HQL_FUNCTIONS debug logs to see all custom functions registered. *
 * <p>
 * Use:
 * <ul>
 * <li>varcharFromJson(&lt;entity&gt;.cfValuesAsJson,'&lt;custom field name&gt;') - for search in String/Picklist/Text area type custom field</li>
 * <li>varcharFromJson('&lt;tablename&gt;.cf_values','&lt;custom field name&gt;') - for search in String/Picklist/Text area type custom field</li>
 * <li>varcharFromJson('&lt;tablename&gt;.cf_values','&lt;custom field name&gt;','&lt;property name&gt;','&lt;value cast&gt;','&lt;position in case of versioned values&gt;') - for search in String/Picklist/Text area type
 * custom field</li>
 * 
 * <li>numericFromJson(&lt;entity&gt;.cfValuesAsJson,'&lt;custom field name&gt;') - for search in Double type custom field</li>
 * <li>bigIntFromJson(&lt;entity&gt;.cfValuesAsJson,'&lt;custom field name&gt;') - for search in Long type custom field</li>
 * <li>timestampFromJson(&lt;entity&gt;.cfValuesAsJson,'&lt;custom field name&gt;') - for search in Date type custom field</li>
 * <li>booleanFromJson(&lt;entity&gt;.cfValuesAsJson,'&lt;custom field name&gt;') - for search in Boolean type custom field</li>
 * <li>entityFromJson(&lt;entity&gt;.cfValuesAsJson,'&lt;custom field name&gt;') - for search in Entity type custom field. Returns EntityReferenceWrapper.code field value.</li>
 * <li>listFromJson(&lt;entity&gt;.cfValuesAsJson,'&lt;custom field name&gt;',&lt;value to search for&gt;) - for search in String type custom field of List storage type</li>
 * <li>listFromJson(&lt;entity&gt;.cfValuesAsJson,'&lt;custom field name&gt;','&lt;property name&gt;','&lt;position&gt;','&lt;value to search for&gt;') - for search in String type custom field of List storage type</li>
 * </ul>
 */
public class JpaCustomFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {

        SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
        BasicTypeRegistry basicTypeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();

        // Register different functions based on a dialect used
        if (functionContributions.getDialect().getClass().getName().contains("Postgre")) {

            // functionRegistry.namedDescriptorBuilder("string_agg").setMinArgumentCount(1).setArgumentTypeResolver(StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE).register();
            functionRegistry.register("string_agg_long", new PostgreSQLStringAggLongFunction(basicTypeRegistry.resolve(StandardBasicTypes.STRING)));
            functionRegistry.register("numericFromJson", new PostgreSQLCustomFieldJsonSearchFunction("numericFromJson", basicTypeRegistry.resolve(StandardBasicTypes.DOUBLE), "double", "numeric(23,12)", null, false));
            functionRegistry.register("varcharFromJson", new PostgreSQLCustomFieldJsonSearchFunction("varcharFromJson", basicTypeRegistry.resolve(StandardBasicTypes.STRING), "string", null, null, false));
            functionRegistry.register("bigIntFromJson", new PostgreSQLCustomFieldJsonSearchFunction("bigIntFromJson", basicTypeRegistry.resolve(StandardBasicTypes.BIG_INTEGER), "long", "bigInt", null, false));
            functionRegistry.register("timestampFromJson", new PostgreSQLCustomFieldJsonSearchFunction("timestampFromJson", basicTypeRegistry.resolve(StandardBasicTypes.DATE), "date", "timestamp", null, false));
            functionRegistry.register("booleanFromJson", new PostgreSQLCustomFieldJsonSearchFunction("booleanFromJson", basicTypeRegistry.resolve(StandardBasicTypes.BOOLEAN), "boolean", "boolean", null, false));
            functionRegistry.register("entityFromJson", new PostgreSQLCustomFieldJsonSearchFunction("entityFromJson", basicTypeRegistry.resolve(StandardBasicTypes.STRING), "entity", null, "code", false));
            functionRegistry.register("listFromJson", new PostgreSQLCustomFieldJsonSearchFunction("listFromJson", basicTypeRegistry.resolve(StandardBasicTypes.BOOLEAN), "listString", null, null, true));

        } else if (functionContributions.getDialect().getClass().getName().contains("Oracle")) {

            functionRegistry.patternDescriptorBuilder("string_agg", "LISTAGG").setExactArgumentCount(2).setParameterTypes(FunctionParameterType.STRING)
                .setInvariantType(basicTypeRegistry.resolve(StandardBasicTypes.STRING)).setArgumentListSignature("(VARCHAR base)").register();
            functionRegistry.patternDescriptorBuilder("string_agg_long", "LISTAGG_CLOB").setExactArgumentCount(2).setParameterTypes(FunctionParameterType.STRING_OR_CLOB)
                .setInvariantType(basicTypeRegistry.resolve(StandardBasicTypes.STRING)).setArgumentListSignature("(CLOB base)").register();

            functionRegistry.register("numericFromJson", new OracleCustomFieldJsonSearchFunction("numericFromJson", basicTypeRegistry.resolve(StandardBasicTypes.DOUBLE), "double", "numeric(23,12)", null, false));
            functionRegistry.register("varcharFromJson", new OracleCustomFieldJsonSearchFunction("varcharFromJson", basicTypeRegistry.resolve(StandardBasicTypes.STRING), "string", null, null, false));
            functionRegistry.register("bigIntFromJson", new OracleCustomFieldJsonSearchFunction("bigIntFromJson", basicTypeRegistry.resolve(StandardBasicTypes.BIG_INTEGER), "long", "number(38,0", null, false));
            functionRegistry.register("timestampFromJson", new OracleCustomFieldJsonSearchFunction("timestampFromJson", basicTypeRegistry.resolve(StandardBasicTypes.DATE), "date", "varchar(50)", null, false));
            functionRegistry.register("booleanFromJson", new OracleCustomFieldJsonSearchFunction("booleanFromJson", basicTypeRegistry.resolve(StandardBasicTypes.BOOLEAN), "boolean", "boolean", null, false));
            functionRegistry.register("entityFromJson", new OracleCustomFieldJsonSearchFunction("entityFromJson", basicTypeRegistry.resolve(StandardBasicTypes.STRING), "entity", null, "code", false));
            functionRegistry.register("listFromJson", new OracleCustomFieldJsonSearchFunction("listFromJson", basicTypeRegistry.resolve(StandardBasicTypes.BOOLEAN), "listString", null, null, true));

        }
    }
}