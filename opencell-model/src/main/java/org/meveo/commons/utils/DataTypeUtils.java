package org.meveo.commons.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utilities class dealing with data type conversion
 */
public class DataTypeUtils {

    /**
     * Convert a string value to a class type
     * 
     * @param targetClass Class type to convert to
     * @param value Value to convert
     * @return A class and converted value object
     */
    public static ClassAndValue convertFromString(@SuppressWarnings("rawtypes") Class targetClass, String value) {
        ClassAndValue classAndValue = new ClassAndValue();

        // Try to find boxed type
        if (int.class.isAssignableFrom(targetClass)) {
            classAndValue.setClass(int.class);
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(0);
            } else {
                classAndValue.setValue(Integer.parseInt(value));
            }

        } else if (Integer.class.isAssignableFrom(targetClass)) {
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(null);
            } else {
                classAndValue.setValue(Integer.parseInt(value));
            }

        } else if (double.class.isAssignableFrom(targetClass)) {
            classAndValue.setClass(double.class);
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(0.0);
            } else {
                classAndValue.setValue(Double.parseDouble(value));
            }

        } else if (Double.class.isAssignableFrom(targetClass)) {
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(null);
            } else {
                classAndValue.setValue(Double.parseDouble(value));
            }

        } else if (long.class.isAssignableFrom(targetClass)) {
            classAndValue.setClass(long.class);
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(0L);
            } else {
                classAndValue.setValue(Long.parseLong(value));
            }

        } else if (Long.class.isAssignableFrom(targetClass)) {
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(null);
            } else {
                classAndValue.setValue(Long.parseLong(value));
            }

        } else if (byte.class.isAssignableFrom(targetClass)) {
            classAndValue.setClass(byte.class);
            classAndValue.setValue(Byte.parseByte(value));

        } else if (Byte.class.isAssignableFrom(targetClass)) {
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(null);
            } else {
                classAndValue.setValue(Byte.parseByte(value));
            }

        } else if (short.class.isAssignableFrom(targetClass)) {
            classAndValue.setClass(short.class);
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(0);
            } else {
                classAndValue.setValue(Short.parseShort(value));
            }

        } else if (Short.class.isAssignableFrom(targetClass)) {
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(null);
            } else {
                classAndValue.setValue(Short.parseShort(value));
            }

        } else if (float.class.isAssignableFrom(targetClass)) {
            classAndValue.setClass(float.class);
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(0.0f);
            } else {
                classAndValue.setValue(Float.parseFloat(value));
            }
        } else if (Float.class.isAssignableFrom(targetClass)) {
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(null);
            } else {
                classAndValue.setValue(Float.parseFloat(value));
            }

        } else if (boolean.class.isAssignableFrom(targetClass)) {
            classAndValue.setClass(boolean.class);
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(false);
            } else {
                classAndValue.setValue(Boolean.parseBoolean(value));
            }

        } else if (Boolean.class.isAssignableFrom(targetClass)) {
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(null);
            } else {
                classAndValue.setValue(Boolean.parseBoolean(value));
            }

        } else {
            classAndValue.setValue(value);
            Logger log = LoggerFactory.getLogger(DataTypeUtils.class);
            log.warn("Type {} not handled for string parsing", targetClass.getName());
        }

        // Case where the class if boxed or we don't handle it yet
        if (classAndValue.clazz == null && classAndValue.value != null) {
            classAndValue.clazz = classAndValue.value.getClass();
        }

        return classAndValue;
    }

    /**
     * Convert a number value to a class type
     * 
     * @param targetClass Class type to convert to
     * @param value Value to convert
     * @return A class and converted value object
     */
    public static ClassAndValue convertFromNumber(@SuppressWarnings("rawtypes") Class targetClass, Number value) {
        ClassAndValue classAndValue = new ClassAndValue();

        // Try to find boxed type
        if (int.class.isAssignableFrom(targetClass)) {
            classAndValue.setClass(int.class);
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(0);
            } else {
                classAndValue.setValue(value.intValue());
            }

        } else if (Integer.class.isAssignableFrom(targetClass)) {
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(null);
            } else {
                classAndValue.setValue(value.intValue());
            }

        } else if (double.class.isAssignableFrom(targetClass)) {
            classAndValue.setClass(double.class);
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(0.0);
            } else {
                classAndValue.setValue(value.doubleValue());
            }

        } else if (Double.class.isAssignableFrom(targetClass)) {
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(null);
            } else {
                classAndValue.setValue(value.doubleValue());
            }

        } else if (long.class.isAssignableFrom(targetClass)) {
            classAndValue.setClass(long.class);
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(0L);
            } else {
                classAndValue.setValue(value.longValue());
            }

        } else if (Long.class.isAssignableFrom(targetClass)) {
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(null);
            } else {
                classAndValue.setValue(value.longValue());
            }

        } else if (byte.class.isAssignableFrom(targetClass)) {
            classAndValue.setClass(byte.class);
            classAndValue.setValue(value.byteValue());

        } else if (Byte.class.isAssignableFrom(targetClass)) {
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(null);
            } else {
                classAndValue.setValue(value.byteValue());
            }

        } else if (short.class.isAssignableFrom(targetClass)) {
            classAndValue.setClass(short.class);
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(0);
            } else {
                classAndValue.setValue(value.shortValue());
            }

        } else if (Short.class.isAssignableFrom(targetClass)) {
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(null);
            } else {
                classAndValue.setValue(value.shortValue());
            }

        } else if (float.class.isAssignableFrom(targetClass)) {
            classAndValue.setClass(float.class);
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(0.0f);
            } else {
                classAndValue.setValue(value.floatValue());
            }
        } else if (Float.class.isAssignableFrom(targetClass)) {
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(null);
            } else {
                classAndValue.setValue(value.floatValue());
            }

        } else if (boolean.class.isAssignableFrom(targetClass)) {
            classAndValue.setClass(boolean.class);
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(false);
            } else {
                classAndValue.setValue(value.intValue() == 1);
            }

        } else if (Boolean.class.isAssignableFrom(targetClass)) {
            if (StringUtils.isBlank(value)) {
                classAndValue.setValue(null);
            } else {
                classAndValue.setValue(value.intValue() == 1);
            }

        } else {
            classAndValue.setValue(value);
            Logger log = LoggerFactory.getLogger(DataTypeUtils.class);
            log.warn("Type {} not handled for number parsing", targetClass.getName());
        }

        // Case where the class if boxed or we don't handle it yet
        if (classAndValue.clazz == null && classAndValue.value != null) {
            classAndValue.clazz = classAndValue.value.getClass();
        }

        return classAndValue;
    }

    /**
     * Contains a value and its class type (in case value is a primitive type)
     */
    public static class ClassAndValue {
        private Object value;
        private Class<?> clazz;

        public ClassAndValue() {
        }

        public ClassAndValue(Object value) {
            this.value = value;
            this.clazz = value.getClass();
        }

        public Object getValue() {
            return value;
        }

        public Class<?> getTypeClass() {
            return clazz;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public void setClass(Class<?> setterClass) {
            this.clazz = setterClass;
        }

        @Override
        public String toString() {
            return clazz + " - " + value;
        }
    }
}
