package org.meveo.apiv2.generic.security.config;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

/**
 * Qualifier to mark {@link SecuredBusinessEntityJsonConfigFactory}
 *
 * @author Mounir Boukayoua
 * @since 10.X
 */
@Qualifier
@Target({PARAMETER, FIELD, METHOD, TYPE})
@Retention(RUNTIME)
public @interface JsonConfigFactory {
}
