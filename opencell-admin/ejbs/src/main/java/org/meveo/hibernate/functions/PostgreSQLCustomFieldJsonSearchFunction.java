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

package org.meveo.hibernate.functions;

import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.sql.internal.BasicValuedPathInterpretation;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.type.BasicType;
import org.meveo.commons.utils.StringUtils;

/**
 * A JPA/native function implementation to retrieve a custom field value or search for a custom field containing a value in a list of values<BR/>
 * PostgreSQL implementation
 * <p>
 * xxxFromJson('&lt;tablename&gt;.cf_values','&lt;custom field name&gt;','&lt;property name&gt;','&lt;value cast&gt;','&lt;position in case of versioned values&gt;') - for search in String/Picklist/Text area type custom
 * field
 * <p>
 * The first two parameters are required. The rest have default values.
 */
public class PostgreSQLCustomFieldJsonSearchFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

    private String propertyName;
    private String castType;
    private String cfValueProperty;
    private boolean isSearchForValue;

    /**
     * @param functionName Function name
     * @param returnType Function return type
     * @param propertyName Entity property name if used in JPA or db column if used in a native query
     * @param castType Additional cast requested for a returned value
     * @param cfValueProperty Additional property to retrieve of a given Custom field
     * @param isSearchForValue Is function used to search for a value inside a custom field value
     */
    public PostgreSQLCustomFieldJsonSearchFunction(String functionName, BasicType<?> returnType, String propertyName, String castType, String cfValueProperty, boolean isSearchForValue) {
        super(functionName, FunctionKind.NORMAL, StandardArgumentsValidators.min(isSearchForValue ? 3 : 2), StandardFunctionReturnTypeResolvers.invariant(returnType),
            StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE);
        this.propertyName = propertyName;
        this.castType = castType;
        this.cfValueProperty = cfValueProperty;
        this.isSearchForValue = isSearchForValue;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> sqlAstArguments, ReturnableType<?> returnType, SqlAstTranslator<?> walker) {

        if (sqlAstArguments.size() < 2) {
            throw new IllegalArgumentException("The function parseJson requires at least 2 arguments");
        }
        String customFieldPosition = "0";
        String entityColumnName = null;
        // Entity column name can be a DB fieldname with quotes of a JPA entity property without quotes
        if (sqlAstArguments.get(0) instanceof QueryLiteral) {
            entityColumnName = ((QueryLiteral<String>) sqlAstArguments.get(0)).getLiteralValue();
        } else {
            entityColumnName = ((BasicValuedPathInterpretation) sqlAstArguments.get(0)).getColumnReference().getExpressionText();
        }

        String customFieldName = ((QueryLiteral<String>) sqlAstArguments.get(1)).getLiteralValue();
        String customFieldValueProperty = propertyName;

        String fragment = null;

        if (isSearchForValue) {
            String value = null;
            if (sqlAstArguments.size() > 4) {
                customFieldValueProperty = ((QueryLiteral<String>) sqlAstArguments.get(2)).getLiteralValue();
                customFieldPosition = ((QueryLiteral<String>) sqlAstArguments.get(3)).getLiteralValue();
                value = ((QueryLiteral<String>) sqlAstArguments.get(4)).getLiteralValue();

            } else if (sqlAstArguments.size() > 2) {
                value = ((QueryLiteral<String>) sqlAstArguments.get(2)).getLiteralValue();
            }

            fragment = entityColumnName + "::jsonb ->'" + customFieldName + "'->" + customFieldPosition + "->'" + customFieldValueProperty + "'";

            fragment = "(" + fragment + " ^| array[" + value + "])";

        } else {

            if (sqlAstArguments.size() > 2) {
                customFieldValueProperty = ((QueryLiteral<String>) sqlAstArguments.get(2)).getLiteralValue();
            }
            if (sqlAstArguments.size() > 3) {
                castType = ((QueryLiteral<String>) sqlAstArguments.get(3)).getLiteralValue();
            }
            if (sqlAstArguments.size() > 4) {
                customFieldPosition = ((QueryLiteral<String>) sqlAstArguments.get(4)).getLiteralValue();
            }
            fragment = entityColumnName + "::json ->'" + customFieldName + "'->" + customFieldPosition + "->>'" + customFieldValueProperty + "'";
            if (cfValueProperty != null) {
                fragment = "((" + fragment + ")::json->>'" + cfValueProperty + "')";
            }

            if (StringUtils.isNotBlank(castType) && !"null".equalsIgnoreCase(castType)) {
                fragment = "(" + fragment + ")::" + castType;
            }
        }

        sqlAppender.append(fragment);

    }
}