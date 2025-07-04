package org.meveo.api.restful.swagger;

import io.swagger.v3.jaxrs2.integration.JaxrsApplicationAndAnnotationScanner;
import jakarta.ws.rs.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpencellJaxrsScanner extends JaxrsApplicationAndAnnotationScanner {

    private static final Logger log = LoggerFactory.getLogger(OpencellJaxrsScanner.class);
    
    private Set<String> resourcePackages = new HashSet<>();
    private ClassLoader[] classLoaders;

    public OpencellJaxrsScanner() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public OpencellJaxrsScanner(ClassLoader... classLoaders) {
        this.classLoaders = classLoaders;
    }

    public void setResourcePackages(Set<String> resourcePackages) {
        if (resourcePackages != null) {
            this.resourcePackages = new HashSet<>(resourcePackages);
        }
    }

    public void addResourcePackage(String packageName) {
        if (packageName != null && !packageName.isEmpty()) {
            this.resourcePackages.add(packageName);
        }
    }

    @Override
    public Set<Class<?>> classes() {
        if (resourcePackages.isEmpty()) {
            log.warn("No resource packages configured for scanning");
            return Collections.emptySet();
        }

        log.info("Scanning JAX-RS resources in packages: {}", resourcePackages);
        
        try {
            Reflections reflections = createReflectionsScanner();
            
            Set<Class<?>> resources = new HashSet<>();
            resources.addAll(findAnnotatedClasses(reflections));
            resources.addAll(findInterfaceImplementations(reflections));
            
            return filterValidClasses(resources);
        } catch (Exception e) {
            log.error("Failed to scan JAX-RS resources", e);
            return Collections.emptySet();
        }
    }

    private Reflections createReflectionsScanner() {
        ConfigurationBuilder config = new ConfigurationBuilder()
            .addScanners(Scanners.TypesAnnotated, Scanners.SubTypes)
            .addClassLoaders(classLoaders);
        
        resourcePackages.forEach(pkg -> {
            config.forPackages(pkg);
            log.debug("Configured scanning for package: {}", pkg);
        });
        
        return new Reflections(config);
    }

    private Set<Class<?>> findAnnotatedClasses(Reflections reflections) {
        return reflections.getTypesAnnotatedWith(Path.class).stream()
            .filter(this::isValidClass)
            .collect(Collectors.toSet());
    }

    private Set<Class<?>> findInterfaceImplementations(Reflections reflections) {
        Set<Class<?>> implementations = new HashSet<>();
        
        reflections.getTypesAnnotatedWith(Path.class).stream()
            .filter(Class::isInterface)
            .forEach(iface -> {
                reflections.getSubTypesOf(iface).stream()
                    .filter(this::isValidClass)
                    .forEach(implementations::add);
            });
        
        return implementations;
    }

    private Set<Class<?>> filterValidClasses(Set<Class<?>> classes) {
        return classes.stream()
            .filter(this::isValidClass)
            .collect(Collectors.toSet());
    }

    private boolean isValidClass(Class<?> cls) {
        if (cls == null || cls.isInterface() || 
            cls.isAnonymousClass() || cls.isLocalClass() || cls.isMemberClass()) {
            return false;
        }
        
        String className = cls.getName();
        boolean inPackage = resourcePackages.stream()
            .anyMatch(pkg -> className.startsWith(pkg));
        
        if (!inPackage) {
            log.trace("Excluding class {} - not in scanned packages", className);
            return false;
        }
        
        log.debug("Including valid JAX-RS resource class: {}", className);
        return true;
    }
}