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

/**
 * 
 */
package org.meveo.service.script;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.ValidationException;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.scripts.ScriptInstance;
import org.meveo.model.scripts.ScriptSourceTypeEnum;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.javadoc.JavadocBlockTag;

/**
 * @author melyoussoufi
 * @lastModifiedVersion 7.2.0
 *
 */
public abstract class ScriptUtils {

    /**
     * Check that the class implements ScriptInterface interface
     * 
     * @param fullClassName Full class name
     * @return True if class implements ScriptInterface interface
     */
    @SuppressWarnings("rawtypes")
    public static boolean isScriptInterfaceClass(String fullClassName) {
        try {
            Class classDefinition = Class.forName(fullClassName);

            return ScriptInterface.class.isAssignableFrom(classDefinition);

        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Check full class name is existed class path or not.
     * 
     * @param fullClassName Full class name
     * @return True if class is overridden
     */
    public static boolean isOverwritesJavaClass(String fullClassName) {
        try {
            Class.forName(fullClassName);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    /**
     * Find the package name in a source java text.
     * 
     * @param src Java source code
     * @return Package name
     */
    public static String getPackageName(String src) {
        return StringUtils.patternMacher("package (.*?);", src);
    }

    /**
     * Find the class name in a source java text
     * 
     * @param src Java source code
     * @return Class name
     */
    public static String getClassName(String src) {
        String className = StringUtils.patternMacher("public class (.*) extends", src);
        if (className == null) {
            className = StringUtils.patternMacher("public class (.*) implements", src);
        }
        return className != null ? className.trim() : null;
    }

    /**
     * Gets a full classname of a script by combining a package (if applicable) and a classname
     * 
     * @param script Java source code
     * @return Full classname
     */
    public static String getFullClassname(String script) {
        String packageName = getPackageName(script);
        String className = getClassName(script);
        return (packageName != null ? packageName.trim() + "." : "") + className;
    }

    /**
     * Parse parameters encoded in URL like style param=value&amp;param=value.
     * 
     * @param encodedParameters Parameters encoded in URL like style param=value&amp;param=value
     * @return A map of parameter keys and values
     */
    public static Map<String, Object> parseParameters(String encodedParameters) {
        Map<String, Object> parameters = new HashMap<String, Object>();

        if (!StringUtils.isBlank(encodedParameters)) {
            StringTokenizer tokenizer = new StringTokenizer(encodedParameters, "&");
            while (tokenizer.hasMoreElements()) {
                String paramValue = tokenizer.nextToken();
                String[] paramValueSplit = paramValue.split("=");
                if (paramValueSplit.length == 2) {
                    parameters.put(paramValueSplit[0], paramValueSplit[1]);
                } else {
                    parameters.put(paramValueSplit[0], null);
                }
            }

        }
        return parameters;
    }

    /**
     * Convert Comparison operator to an sql or java operator
     * 
     * @param comparison operator
     * @param toSql or toJava
     * @return sql Comparison operator
     */
    public static String buildOperator(String operator, boolean toSql) {
        String operatorExpression;
        switch (operator) {
        case "<":
            operatorExpression = "<";
            break;
        case "≤":
            operatorExpression = "<=";
            break;
        case "=":
            operatorExpression = toSql ? "=" : "==";
            break;
        case "≠":
            operatorExpression = toSql ? "<>" : "!=";
            break;
        case "≥":
            operatorExpression = ">=";
            break;
        default:
            operatorExpression = ">";
            break;
        }
        return operatorExpression;
    }

    /**
     * Get a list of getters from a script instance
     * 
     * @param methods A list of class methods
     * @return List of getter property information
     */
    public static List<Accessor> getGetters(final List<MethodDeclaration> methods) {
        return methods.stream().filter(e -> e.getNameAsString().startsWith(Accessor.GET) || e.getNameAsString().startsWith(Accessor.IS)).filter(e -> e.getAnnotationByClass(JsonIgnore.class).isEmpty())
            .filter(e -> e.getModifiers().stream().anyMatch(modifier -> modifier.getKeyword().equals(Modifier.Keyword.PUBLIC))).filter(e -> e.getParameters().isEmpty()).map(methodDeclaration -> {
                Accessor getter = new Accessor();
                String accessorFieldName;
                if (methodDeclaration.getNameAsString().startsWith(Accessor.GET)) {
                    accessorFieldName = methodDeclaration.getNameAsString().substring(3);
                } else {
                    accessorFieldName = methodDeclaration.getNameAsString().substring(2);
                }
                getter.setName(Character.toLowerCase(accessorFieldName.charAt(0)) + accessorFieldName.substring(1));
                getter.setMethodName(methodDeclaration.getNameAsString());
                getter.setType(methodDeclaration.getTypeAsString());
                methodDeclaration.getComment().ifPresent(comment -> comment.ifJavadocComment(javadocComment -> {
                    javadocComment.parse().getBlockTags().stream().filter(e -> e.getType() == JavadocBlockTag.Type.RETURN).findFirst()
                        .ifPresent(javadocBlockTag -> getter.setDescription(javadocBlockTag.getContent().toText()));
                }));
                return getter;
            }).collect(Collectors.toList());
    }

    /**
     * Get a list of setters from a script instance
     * 
     * @param methods A list of class methods
     * @return List of setter property information
     */
    private static List<Accessor> getSetters(final List<MethodDeclaration> methods) {
        return methods.stream().filter(e -> e.getNameAsString().startsWith(Accessor.SET)).filter(e -> e.getAnnotationByClass(JsonIgnore.class).isEmpty())
            .filter(e -> e.getModifiers().stream().anyMatch(modifier -> modifier.getKeyword().equals(Modifier.Keyword.PUBLIC))).filter(e -> e.getParameters().size() == 1).map(methodDeclaration -> {
                Accessor setter = new Accessor();
                String accessorFieldName = methodDeclaration.getNameAsString().substring(3);
                setter.setName(Character.toLowerCase(accessorFieldName.charAt(0)) + accessorFieldName.substring(1));
                setter.setType(methodDeclaration.getParameter(0).getTypeAsString());
                setter.setMethodName(methodDeclaration.getNameAsString());
                methodDeclaration.getComment().ifPresent(comment -> comment.ifJavadocComment(javadocComment -> {
                    javadocComment.parse().getBlockTags().stream().filter(e -> e.getType() == JavadocBlockTag.Type.PARAM).findFirst()
                        .ifPresent(javadocBlockTag -> setter.setDescription(javadocBlockTag.getContent().toText()));
                }));
                return setter;
            }).collect(Collectors.toList());
    }

    /**
     * Get a list of getters from a scrip instance ina form of property names
     * 
     * @param scriptInstance ScriptInstance
     * @return List of property names
     */
    public static List<String> getGetterPropertyNames(ScriptInstance scriptInstance) {

        List<Accessor> gettersList = getGetters(scriptInstance);
        List<String> getters = gettersList.stream().map(e -> e.getName()).collect(Collectors.toList());

        return getters;
    }

    /**
     * Get a list of getters from a script instance
     * 
     * @param scriptInstance ScriptInstance
     * @return List of getter property information
     */
    public static List<Accessor> getGetters(ScriptInstance scriptInstance) {

        CompilationUnit compilationUnit;
        try {
            compilationUnit = new JavaParser().parse(scriptInstance.getScript()).getResult().get();
            final ClassOrInterfaceDeclaration classOrInterfaceDeclaration = compilationUnit.getChildNodes().stream().filter(e -> e instanceof ClassOrInterfaceDeclaration).map(e -> (ClassOrInterfaceDeclaration) e)
                .findFirst().get();

            final List<MethodDeclaration> methods = classOrInterfaceDeclaration.getMembers().stream().filter(e -> e instanceof MethodDeclaration).map(e -> (MethodDeclaration) e).collect(Collectors.toList());

            final List<Accessor> gettersList = getGetters(methods);
            return gettersList;

        } catch (Exception e) {
            throw new BusinessException("Failed to get a list of getter property names for a script " + scriptInstance.getCode(), e);
        }
    }

    /**
     * Get a list of setters from a script instance in a form of property names
     * 
     * @param scriptInstance
     * @return List of property names
     */
    public static List<String> getSetterPropertyNames(ScriptInstance scriptInstance) {

        List<Accessor> settersList = getGetters(scriptInstance);
        List<String> setters = settersList.stream().map(e -> e.getName()).collect(Collectors.toList());

        return setters;
    }

    /**
     * Get a list of setters from a script instance
     * 
     * @param scriptInstance ScriptInstance
     * @return List of setter property information
     */
    public static List<Accessor> getSetters(ScriptInstance scriptInstance) {
        CompilationUnit compilationUnit;
        if (scriptInstance.getSourceTypeEnum() == ScriptSourceTypeEnum.JAVA_CLASS) {
            throw new ValidationException("Currently only scripts with JAVA code are supported as custom API scripts");
        }
        try {
            compilationUnit = new JavaParser().parse(scriptInstance.getScript()).getResult().get();
            final ClassOrInterfaceDeclaration classOrInterfaceDeclaration = compilationUnit.getChildNodes().stream().filter(e -> e instanceof ClassOrInterfaceDeclaration).map(e -> (ClassOrInterfaceDeclaration) e)
                .findFirst().get();

            final List<MethodDeclaration> methods = classOrInterfaceDeclaration.getMembers().stream().filter(e -> e instanceof MethodDeclaration).map(e -> (MethodDeclaration) e).collect(Collectors.toList());

            final List<Accessor> settersList = getSetters(methods);
            return settersList;

        } catch (Exception e) {
            throw new BusinessException("Failed to get a list of setter property names for a script " + scriptInstance.getCode(), e);
        }
    }

    /**
     * Determine a type of a variable in a script instance
     * 
     * @param scriptInstance Script instance
     * @param variableName Variable name - property name of a corresponding getter method
     * @return Classname of a variable
     */
    public static String findScriptVariableType(ScriptInstance scriptInstance, String variableName) {

        String result = "Object";
        CompilationUnit compilationUnit;

        try {
            compilationUnit = new JavaParser().parse(scriptInstance.getScript()).getResult().get();
            final ClassOrInterfaceDeclaration classOrInterfaceDeclaration = compilationUnit.getChildNodes().stream().filter(e -> e instanceof ClassOrInterfaceDeclaration).map(e -> (ClassOrInterfaceDeclaration) e)
                .findFirst().get();

            final List<MethodDeclaration> methods = classOrInterfaceDeclaration.getMembers().stream().filter(e -> e instanceof MethodDeclaration).map(e -> (MethodDeclaration) e).collect(Collectors.toList());

            final List<Accessor> getters = getGetters(methods);

            Optional<Accessor> returnMethod = getters.stream().filter(e -> e.getName().equals(variableName)).findAny();

            if (returnMethod.isPresent()) {
                result = returnMethod.get().getType();
            }

        } catch (Exception e) {
        }

        return result;
    }

    /**
     * Determine if a variable is multivalued - is a collection, map, set or array
     * 
     * @param scriptInstance Script instance
     * @param variableName Variable name - property name of a corresponding getter method
     * @return True if a variable is multivalued
     */
    public static boolean isScriptVariableMultivalued(ScriptInstance scriptInstance, String variableName) {
        String type = findScriptVariableType(scriptInstance, variableName);
        if (type.contains("List<") || type.contains("Set<") || type.contains("Collection<") || type.contains("Map<") || type.endsWith("[]")) {
            return true;
        }
        return false;
    }
}