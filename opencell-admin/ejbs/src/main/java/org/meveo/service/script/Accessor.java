package org.meveo.service.script;

import java.io.Serializable;
import java.lang.reflect.Method;

public class Accessor implements Serializable {

    public static final String GET = "get";

    public static final String IS = "is";

    public static final String SET = "set";

    private static final long serialVersionUID = -8787120921409437404L;

    /**
     * Description of the property
     */
    private String description;

    /**
     * Name of the getter or setter
     */
    private String methodName;

    /**
     * Name of the property
     */
    private String name;

    /**
     * Type of the property. Can be primitive, object or custom entity template.
     */
    private String type;

    public Accessor() {

    }

    public Accessor(Method m) {
        this.methodName = m.getName();

        String accessorFieldName = methodName.substring(methodName.startsWith(Accessor.GET) ? 3 : 2);
        this.name = Character.toLowerCase(accessorFieldName.charAt(0)) + accessorFieldName.substring(1);

        if (methodName.startsWith(GET)) {
            this.type = m.getReturnType().getSimpleName();
        } else {
            this.type = m.getParameters()[0].getType().getSimpleName();
        }
    }

    public String getDescription() {
        return description;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * Determine if a variable is multivalued - is a collection, map, set or array
     * 
     * @return True if a variable is multivalued
     */
    public boolean isMultivalued() {
        if (type.contains("List<") || type.contains("Set<") || type.contains("Collection<") || type.contains("Map<") || type.endsWith("[]")) {
            return true;
        }
        return false;
    }
}