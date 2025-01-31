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
package org.meveo.service.script;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.SoftReferenceObjectPool;
import org.infinispan.Cache;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.ElementNotFoundException;
import org.meveo.admin.exception.InvalidPermissionException;
import org.meveo.admin.exception.InvalidScriptException;
import org.meveo.admin.util.ResourceBundle;
import org.meveo.cache.CacheKeyStr;
import org.meveo.commons.utils.DataTypeUtils;
import org.meveo.commons.utils.DataTypeUtils.ClassAndValue;
import org.meveo.commons.utils.EjbUtils;
import org.meveo.commons.utils.ReflectionUtils;
import org.meveo.event.monitoring.ClusterEventDto.ClusterEventActionEnum;
import org.meveo.event.monitoring.ClusterEventPublisher;
import org.meveo.jpa.JpaAmpNewTx;
import org.meveo.model.persistence.JacksonUtil;
import org.meveo.model.scripts.ScriptInstance;
import org.meveo.model.scripts.ScriptInstanceError;
import org.meveo.model.scripts.ScriptParameter;
import org.meveo.model.scripts.ScriptPool;
import org.meveo.model.scripts.ScriptSourceTypeEnum;
import org.meveo.service.base.BusinessService;

import com.fasterxml.jackson.databind.type.TypeFactory;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

/**
 * Script service implementation.
 * 
 * @author Andrius Karpavicius
 * @lastModifiedVersion 7.2.0
 *
 */
@Stateless
public class ScriptInstanceService extends BusinessService<ScriptInstance> implements Serializable {

    private static final long serialVersionUID = -560761831622106789L;

    @Inject
    private ResourceBundle resourceMessages;

    @Inject
    private ClusterEventPublisher clusterEventPublisher;

    @Inject
    private ScriptCompilerService scriptCompilerService;

    /**
     * Stores compiled scripts. Key format: &lt;cluster node code&gt;_&lt;scriptInstance code&gt;. Value is a compiled script class and class instance
     */
    @Resource(lookup = "java:jboss/infinispan/cache/opencell/opencell-script-cache")
    private Cache<CacheKeyStr, Class<ScriptInterface>> compiledScripts;

    /**
     * Stores a pooled instances of scripts. Key is Script code. Value is a pooled instance of a script instances.
     */
    private static Map<String, ObjectPool<ScriptInterface>> poolOfScriptInstances = new ConcurrentHashMap<>();

    /**
     * Get all ScriptInstances with error.
     *
     * @return list of custom script.
     */
    public List<ScriptInstance> getScriptInstancesWithError() {
        return ((List<ScriptInstance>) getEntityManager().createNamedQuery("CustomScript.getScriptInstanceOnError", ScriptInstance.class).setParameter("isError", Boolean.TRUE).getResultList());
    }

    /**
     * Count scriptInstances with error.
     * 
     * @return number of script instances with error.
     */
    public long countScriptInstancesWithError() {
        return ((Long) getEntityManager().createNamedQuery("CustomScript.countScriptInstanceOnError", Long.class).setParameter("isError", Boolean.TRUE).getSingleResult());
    }

    /**
     * Only users having a role in executionRoles can execute the script, not having the role should throw an InvalidPermission exception that extends businessException. A script with no executionRoles can be executed by
     * any user.
     *
     * @param scriptInstance instance of script
     * @throws InvalidPermissionException invalid permission exception.
     */
    public void isUserHasExecutionRole(ScriptInstance scriptInstance) throws InvalidPermissionException {
        if (scriptInstance != null && scriptInstance.getExecutionRoles() != null && !scriptInstance.getExecutionRoles().isEmpty()) {
            Set<String> execRoles = scriptInstance.getExecutionRoles();
            for (String role : execRoles) {
                if (currentUser.hasRole(role)) {
                    return;
                }
            }
            throw new InvalidPermissionException();
        }
    }

    /**
     * @param scriptInstance instance of script
     * @return true if user have the souring role.
     */
    public boolean isUserHasSourcingRole(ScriptInstance scriptInstance) {
        if (scriptInstance != null && scriptInstance.getSourcingRoles() != null && !scriptInstance.getSourcingRoles().isEmpty()) {
            Set<String> sourcingRoles = scriptInstance.getSourcingRoles();
            for (String role : sourcingRoles) {
                if (currentUser.hasRole(role)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * This is used to invoke a method in a new transaction from a script.<br>
     * This will prevent DB errors in the script from affecting notification history creation.
     *
     * @param workerName The name of the API or service that will be invoked.
     * @param methodName The name of the method that will be invoked.
     * @param parameters The array of parameters accepted by the method. They must be specified in exactly the same order as the target method.
     * @throws BusinessException business exception.
     */
    @JpaAmpNewTx
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void callWithNewTransaction(String workerName, String methodName, Object... parameters) throws BusinessException {
        try {
            Object worker = EjbUtils.getServiceInterface(workerName);
            String workerClassName = ReflectionUtils.getCleanClassName(worker.getClass().getName());
            Class<?> workerClass = Class.forName(workerClassName);
            Method method = null;
            if (parameters.length < 1) {
                method = workerClass.getDeclaredMethod(methodName);
            } else {
                String className = null;
                Object parameter = null;
                Class<?>[] parameterTypes = new Class<?>[parameters.length];
                for (int i = 0; i < parameters.length; i++) {
                    parameter = parameters[i];
                    className = ReflectionUtils.getCleanClassName(parameter.getClass().getName());
                    parameterTypes[i] = Class.forName(className);
                }
                method = workerClass.getDeclaredMethod(methodName, parameterTypes);
            }
            method.setAccessible(true);
            method.invoke(worker, parameters);
        } catch (Exception e) {
            if (e.getCause() != null) {
                throw new BusinessException(e.getCause());
            } else {
                throw new BusinessException(e);
            }
        }
    }

    /**
     * This is used to invoke a method in a new transaction from a script.<br>
     * This will prevent DB errors in the script from affecting notification history creation.
     *
     * @param runnable a runnable that will be run inside a separate transaction
     * @throws BusinessException business exception.
     */
    @JpaAmpNewTx
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void runRunnableWithNewTransaction(Runnable runnable) throws BusinessException {
        try {
            runnable.run();
        } catch (Exception e) {
            if (e.getCause() != null) {
                throw new BusinessException(e.getCause());
            } else {
                throw new BusinessException(e);
            }
        }
    }

    @Override
    public void create(ScriptInstance script) throws BusinessException {

        if (script.getSourceTypeEnum() == ScriptSourceTypeEnum.JAVA_CLASS) {

            if (!ScriptUtils.isOverwritesJavaClass(script.getCode())) {
                throw new BusinessException(resourceMessages.getString("message.scriptInstance.classDoesNotExist", script.getCode()));
            } else if (!ScriptUtils.isScriptInterfaceClass(script.getCode())) {
                throw new BusinessException(resourceMessages.getString("message.scriptInstance.classNotScriptInstance", script.getCode()));
            }
            super.create(script);

        } else {

            String className = ScriptUtils.getClassName(script.getScript());
            if (className == null) {
                throw new BusinessException(resourceMessages.getString("message.scriptInstance.sourceInvalid"));
            }
            String fullClassName = ScriptUtils.getFullClassname(script.getScript());

            if (ScriptUtils.isOverwritesJavaClass(fullClassName)) {
                throw new BusinessException(resourceMessages.getString("message.scriptInstance.classInvalid", fullClassName));
            }
            script.setCode(fullClassName);

            super.create(script);
            compileScript(script, false);

            clusterEventPublisher.publishEvent(script, ClusterEventActionEnum.create);
        }
    }

    @Override
    public ScriptInstance update(ScriptInstance script) throws BusinessException {

        if (script.getSourceTypeEnum() == ScriptSourceTypeEnum.JAVA_CLASS) {

            if (!ScriptUtils.isOverwritesJavaClass(script.getCode())) {
                throw new BusinessException(resourceMessages.getString("message.scriptInstance.classDoesNotExist", script.getCode()));
            } else if (!ScriptUtils.isScriptInterfaceClass(script.getCode())) {
                throw new BusinessException(resourceMessages.getString("message.scriptInstance.classNotScriptInstance", script.getCode()));
            }
            script = super.update(script);

        } else {

            String className = ScriptUtils.getClassName(script.getScript());
            if (className == null) {
                throw new BusinessException(resourceMessages.getString("message.scriptInstance.sourceInvalid"));
            }

            String fullClassName = ScriptUtils.getFullClassname(script.getScript());
            if (ScriptUtils.isOverwritesJavaClass(fullClassName)) {
                throw new BusinessException(resourceMessages.getString("message.scriptInstance.classInvalid", fullClassName));
            }

            script.setCode(fullClassName);

            script = super.update(script);

            compileScript(script, false);

            clusterEventPublisher.publishEvent(script, ClusterEventActionEnum.update);

        }
        return script;

    }

    @Override
    public void remove(ScriptInstance script) throws BusinessException {
        super.remove(script);
        clearCompiledScriptFromCacheAndPool(script.getCode());
        clusterEventPublisher.publishEvent(script, ClusterEventActionEnum.remove);
    }

    @Override
    public ScriptInstance enable(ScriptInstance script) throws BusinessException {
        script = super.enable(script);
        compileScript(script, false);
        clusterEventPublisher.publishEvent(script, ClusterEventActionEnum.enable);
        return script;
    }

    @Override
    public ScriptInstance disable(ScriptInstance script) throws BusinessException {
        script = super.disable(script);
        clearCompiledScriptFromCacheAndPool(script.getCode());
        clusterEventPublisher.publishEvent(script, ClusterEventActionEnum.disable);
        return script;
    }

    /**
     * Execute the script identified by a script code. No init nor finalize methods are called.
     *
     * @param scriptCode ScriptInstanceCode
     * @param context Context parameters (optional)
     * @return Context parameters. Will not be null even if "context" parameter is null.
     * @throws InvalidPermissionException Insufficient access to run the script
     * @throws ElementNotFoundException Script not found
     * @throws BusinessException General execution exception
     */
    public Map<String, Object> execute(String scriptCode, Map<String, Object> context) throws InvalidPermissionException, ElementNotFoundException, BusinessException {

        ScriptInstance scriptInstance = findByCode(scriptCode, true);
        if (scriptInstance == null) {
            throw new BusinessException("Script instance not found code : " + scriptCode);
        }
        // Check access to the script
        isUserHasExecutionRole(scriptInstance);

        return execute(scriptCode, context, false, true, false);
    }

    /**
     * Execute action on an entity/event. Does not call init() nor finalize() methods of the script.
     * 
     * @param entityOrEvent Entity or event to execute action on
     * @param scriptCode Script to execute, identified by a code
     * @param encodedParameters Additional parameters encoded in URL like style param=value&amp;param=value
     * @return Context parameters. Will not be null even if "context" parameter is null.
     * @throws InvalidPermissionException Insufficient access to run the script
     * @throws ElementNotFoundException Script not found
     * @throws BusinessException Any execution exception
     */
    public Map<String, Object> execute(Object entityOrEvent, String scriptCode, String encodedParameters) throws BusinessException {
        return execute(entityOrEvent, scriptCode, ScriptUtils.parseParameters(encodedParameters));
    }

    /**
     * Execute action on an entity/event. Does not call init() nor finalize() methods of the script.
     * 
     * @param entityOrEvent Entity or event to execute action on. Will be added to context under Script.CONTEXT_ENTITY key.
     * @param scriptCode Script to execute, identified by a code. Will be added to context under Script.CONTEXT_ACTION key.
     * @param context Additional parameters
     * @return Context parameters. Will not be null even if "context" parameter is null.
     * @throws InvalidScriptException Were not able to instantiate or compile a script
     * @throws ElementNotFoundException Script not found
     * @throws InvalidPermissionException Insufficient access to run the script
     * @throws BusinessException Any execution exception
     */
    public Map<String, Object> execute(Object entityOrEvent, String scriptCode, Map<String, Object> context) throws BusinessException {

        if (context == null) {
            context = new HashMap<>();
        }
        context.put(Script.CONTEXT_ENTITY, entityOrEvent);
        Map<String, Object> result = execute(scriptCode, context);
        return result;
    }

    /**
     * Execute action on an entity/event. Reuse an existing, earlier initialized script interface from a pool of script instances. Does not call init() nor finalize() methods of the script.
     * 
     * @param entityOrEvent Entity or event to execute action on. Will be added to context under Script.CONTEXT_ENTITY key.
     * @param scriptCode Script to execute, identified by a code. Will be added to context under Script.CONTEXT_ACTION key.
     * @param context Additional parameters
     * @return Context parameters. Will not be null even if "context" parameter is null.
     * @throws InvalidScriptException Were not able to instantiate or compile a script
     * @throws ElementNotFoundException Script not found
     * @throws InvalidPermissionException Insufficient access to run the script
     * @throws BusinessException Any execution exception
     */
    public Map<String, Object> executeFromPool(Object entityOrEvent, String scriptCode, Map<String, Object> context) throws BusinessException {

        if (context == null) {
            context = new HashMap<String, Object>();
        }
        context.put(Script.CONTEXT_ENTITY, entityOrEvent);

        return executePooled(scriptCode, context);
    }

    /**
     * Execute action on an entity/event. Reuse an existing, earlier initialized script interface from a pool of script instances. Does not call init() nor finalize() methods of the script.
     * 
     * @param scriptCode Script to execute, identified by a code. Will be added to context under Script.CONTEXT_ACTION key.
     * @param context Additional parameters
     * @return Context parameters. Will not be null even if "context" parameter is null.
     * @throws InvalidScriptException Were not able to instantiate or compile a script
     * @throws ElementNotFoundException Script not found
     * @throws InvalidPermissionException Insufficient access to run the script
     * @throws BusinessException Any execution exception
     */
    public Map<String, Object> executePooled(String scriptCode, Map<String, Object> context) throws BusinessException {

        log.trace("Script (pooled) {} to be executed", scriptCode); // INTRD-24801 with parameters {}", scriptCode, context);

        if (context == null) {
            context = new HashMap<String, Object>();
        }
        if (context.get(Script.CONTEXT_ACTION) == null) {
            context.put(Script.CONTEXT_ACTION, scriptCode);
        }
        context.put(Script.CONTEXT_CURRENT_USER, currentUser);
        context.put(Script.CONTEXT_APP_PROVIDER, appProvider);

        ScriptInterface classInstance = getPooledScriptInstance(scriptCode);

        // Inject default values and validate script Params
        ScriptInstance scriptInstance = findByCode(scriptCode, true);
        if (scriptInstance.getScriptParameters() != null && !scriptInstance.getScriptParameters().isEmpty()) {
            validateScriptParams(scriptInstance, context);
        }

        applyParametersToScriptInstance(classInstance, context);

        classInstance.execute(context);

        // Append getter values after script has completed
        Map<String, Object> getterValues = ReflectionUtils.getGetterValues(classInstance);
        if (getterValues != null) {
            context.put(Script.CONTEXT_RESULT_GETTER_VALUES, getterValues);
        }

        returnScriptInstanceToPool(scriptCode, classInstance);

        log.trace("Script (pooled) {} executed", scriptCode); // INTRD-24801 with parameters {}", scriptCode, context);
        return context;
    }

    /**
     * Execute action on an entity/event. DOES call init() and finalize() methods of the script.
     * 
     * @param entityOrEvent Entity or event to execute action on. Will be added to context under Script.CONTEXT_ENTITY key.
     * @param scriptCode Script to execute, identified by a code. Will be added to context under Script.CONTEXT_ACTION key.
     * @param context Additional parameters
     * @return Context parameters. Will not be null even if "context" parameter is null.
     * @throws InvalidScriptException Were not able to instantiate or compile a script
     * @throws ElementNotFoundException Script not found
     * @throws InvalidPermissionException Insufficient access to run the script
     * @throws BusinessException Any execution exception
     */
    public Map<String, Object> executeWInitAndFinalize(Object entityOrEvent, String scriptCode, Map<String, Object> context) throws BusinessException {

        if (context == null) {
            context = new HashMap<>();
        }
        context.put(Script.CONTEXT_ENTITY, entityOrEvent);
        Map<String, Object> result = executeWInitAndFinalize(scriptCode, context);
        return result;
    }

    /**
     * Execute action on an entity/event.
     * 
     * @param entityOrEvent Entity or event to execute action on. Will be added to context under Script.CONTEXT_ENTITY key.
     * @param scriptCode Script to execute, identified by a code. Will be added to context under Script.CONTEXT_ACTION key.
     * @param context Additional parameters
     * @return Context parameters. Will not be null even if "context" parameter is null.
     * @throws InvalidScriptException Were not able to instantiate or compile a script
     * @throws ElementNotFoundException Script not found
     * @throws InvalidPermissionException Insufficient access to run the script
     * @throws BusinessException Any execution exception
     */
    public Map<String, Object> execute(Object entityOrEvent, String scriptCode, Map<String, Object> context, boolean isToInit, boolean isToExecute, boolean isToTerminate) throws BusinessException {

        if (context == null) {
            context = new HashMap<>();
        }
        context.put(Script.CONTEXT_ENTITY, entityOrEvent);
        Map<String, Object> result = execute(scriptCode, context, isToInit, isToExecute, isToTerminate);
        return result;
    }

    /**
     * Execute script. DOES call init() or finalize() methods of the script.
     * 
     * @param scriptCode Script to execute, identified by a code. Will be added to context under Script.CONTEXT_ACTION key.
     * @param context Method context
     * @return Context parameters. Will not be null even if "context" parameter is null.
     * @throws InvalidScriptException Were not able to instantiate or compile a script
     * @throws ElementNotFoundException Script not found
     * @throws InvalidPermissionException Insufficient access to run the script
     * @throws BusinessException Any execution exception
     */
    public Map<String, Object> executeWInitAndFinalize(String scriptCode, Map<String, Object> context) throws BusinessException {

        return execute(scriptCode, context, true, true, true);
    }

    /**
     * Execute script.
     * 
     * @param scriptCode Script to execute, identified by a code. Will be added to context under Script.CONTEXT_ACTION key.
     * @param context Method context
     * @param isToInit Shall init() method be called in a script
     * @param isToExecute Shall execute() method be called in a script
     * @param isToTerminate Shall terminate() method be called in a script
     * @return Context parameters. Will not be null even if "context" parameter is null.
     * @throws InvalidScriptException Were not able to instantiate or compile a script
     * @throws ElementNotFoundException Script not found
     * @throws InvalidPermissionException Insufficient access to run the script
     * @throws BusinessException Any execution exception
     */
    public Map<String, Object> execute(String scriptCode, Map<String, Object> context, boolean isToInit, boolean isToExecute, boolean isToTerminate) throws BusinessException {

        // log.trace("Script {} to be executed with parameters {}", scriptCode, context);

        if (context == null) {
            context = new HashMap<String, Object>();
        }
        if (context.get(Script.CONTEXT_ACTION) == null) {
            context.put(Script.CONTEXT_ACTION, scriptCode);
        }
        context.put(Script.CONTEXT_CURRENT_USER, currentUser);
        context.put(Script.CONTEXT_APP_PROVIDER, appProvider);

        // Inject default values and validate script Params
        ScriptInstance scriptInstance = findByCode(scriptCode, true);
        if (scriptInstance.getScriptParameters() != null && !scriptInstance.getScriptParameters().isEmpty()) {
            validateScriptParams(scriptInstance, context);
        }

        ScriptInterface classInstance = getScriptInstance(scriptCode);

        return execute(classInstance, context, isToInit, isToExecute, isToTerminate);
    }

    /**
     * Execute script
     * 
     * @param classInstance Compiled script class
     * @param context Method context
     * @param isToInit Shall init() method be called in a script
     * @param isToExecute Shall execute() method be called in a script
     * @param isToTerminate Shall terminate() method be called in a script
     * 
     * @return Context parameters. Will not be null even if "context" parameter is null.
     * @throws BusinessException Any execution exception
     */
    protected Map<String, Object> execute(ScriptInterface classInstance, Map<String, Object> context, boolean isToInit, boolean isToExecute, boolean isToTerminate) throws BusinessException {

        if (context == null) {
            context = new HashMap<String, Object>();
        }
        context.put(Script.CONTEXT_CURRENT_USER, currentUser);
        context.put(Script.CONTEXT_APP_PROVIDER, appProvider);

        log.trace("Script {} to be executed", classInstance.getClass()); // INTRD-24801 with parameters {}", classInstance.getClass(), context);

        applyParametersToScriptInstance(classInstance, context);

        if (isToInit) {
            classInstance.init(context);
        }
        if (isToExecute) {
            classInstance.execute(context);
        }
        if (isToTerminate) {
            classInstance.terminate(context);
        }

        // Append getter values after script has completed
        Map<String, Object> getterValues = ReflectionUtils.getGetterValues(classInstance);
        if (getterValues != null && !getterValues.isEmpty()) {
            context.put(Script.CONTEXT_RESULT_GETTER_VALUES, getterValues);
        }
        log.trace("Script {} executed", classInstance.getClass()); // INTRD-24801 with parameters {}", classInstance.getClass(), context);
        return context;
    }

    /**
     * Wrap the logger and execute script.
     *
     * @param scriptInstance Script to test
     * @param context context used in execution of script.
     * @return Log messages
     */
    public String test(ScriptInstance scriptInstance, Map<String, Object> context) {
        try {

            isUserHasExecutionRole(scriptInstance);
            String javaSrc = scriptInstance.getScript();
            javaSrc = javaSrc.replaceAll("\\blog.", "logTest.");
            Class<ScriptInterface> compiledScript = scriptCompilerService.compileJavaSource(javaSrc);
            ScriptInterface scriptClassInstance = compiledScript.getDeclaredConstructor().newInstance();

            execute(scriptClassInstance, context, true, true, true);

            String logMessages = scriptClassInstance.getLogMessages();
            return logMessages;

        } catch (CharSequenceCompilerException e) {
            log.error("Failed to compile script {}. Compilation errors:", scriptInstance.getCode());

            List<ScriptInstanceError> scriptErrors = new ArrayList<>();

            List<Diagnostic<? extends JavaFileObject>> diagnosticList = e.getDiagnostics().getDiagnostics();
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticList) {
                if ("ERROR".equals(diagnostic.getKind().name())) {
                    ScriptInstanceError scriptInstanceError = new ScriptInstanceError();
                    scriptInstanceError.setMessage(diagnostic.getMessage(Locale.getDefault()));
                    scriptInstanceError.setLineNumber(diagnostic.getLineNumber());
                    scriptInstanceError.setColumnNumber(diagnostic.getColumnNumber());
                    scriptInstanceError.setSourceFile(diagnostic.getSource().toString());
                    // scriptInstanceError.setScript(scriptInstance);
                    scriptErrors.add(scriptInstanceError);
                    // scriptInstanceErrorService.create(scriptInstanceError, scriptInstance.getAuditable().getCreator());
                    log.warn("{} script {} location {}:{}: {}", diagnostic.getKind().name(), scriptInstance.getCode(), diagnostic.getLineNumber(), diagnostic.getColumnNumber(),
                        diagnostic.getMessage(Locale.getDefault()));
                }
            }
            scriptInstance.setError(scriptErrors != null && !scriptErrors.isEmpty());
            scriptInstance.setScriptErrors(scriptErrors);

            return "Compilation errors";

        } catch (Exception e) {
            log.error("Script test failed", e);
            return ExceptionUtils.getStackTrace(e);
        }
    }

    /**
     * Compile and initialize all scriptInstances.
     */
    public void compileAndInitializeAll() {

        scriptCompilerService.clearCompiledScripts();

        try {

            // Initialize reusable scripts that are based on compiled and included JAVA class in the project
            List<ScriptInstance> scriptInstances = scriptCompilerService.findByType(ScriptSourceTypeEnum.JAVA_CLASS);

            // Initialize scripts that are defined as java classes and are pooled
            // AKK unsure if useful at all - java class type scripts are not pooled - they are used directly??
            for (ScriptInstance scriptInstance : scriptInstances) {
                if (!scriptInstance.isUsePool()) {
                    continue;
                }
                try {
                    // Obtain a deployed script
                    ScriptInterface script = (ScriptInterface) EjbUtils
                        .getServiceInterface(scriptInstance.getCode().lastIndexOf('.') > 0 ? scriptInstance.getCode().substring(scriptInstance.getCode().lastIndexOf('.') + 1) : scriptInstance.getCode());
                    if (script == null) {
                        log.error("Script " + scriptInstance.getCode() + " was not found as a deployed script");
                    } else {
                        log.info("Initializing script " + scriptInstance.getCode());
                        script.init(new HashMap<String, Object>());
                    }
                } catch (Exception e) {
                    log.error("Failed to initialize a script " + scriptInstance.getCode(), e);
                }
            }

            // Compile JAVA type classes
            scriptInstances = scriptCompilerService.findByType(ScriptSourceTypeEnum.JAVA);

            for (ScriptInstance script : scriptInstances) {
                compileScript(script, false);
            }

        } catch (Exception e) {
            log.error("Failed to compile and initialize scripts", e);
        }
    }

    /**
     * Compile script, a and update script entity status with compilation errors. Successfully compiled script is added to a compiled script cache if active and not in test compilation mode. Script.init() method will be
     * called during script instantiation (for cache) if script is marked as poolable.
     * 
     * @param script Script entity to compile
     * @param testCompile Is it a compilation for testing purpose. Won't clear nor overwrite existing compiled script cache.
     */
    public void compileScript(ScriptInstance script, boolean testCompile) {

        clearScriptInstancePool(script.getCode());
        try {
            List<ScriptInstanceError> scriptErrors = scriptCompilerService.compileScript(script.getCode(), script.getSourceTypeEnum(), script.getScript(), script.isActive(), script.isUsePool(), testCompile);

            script.setError(scriptErrors != null && !scriptErrors.isEmpty());
            script.setScriptErrors(scriptErrors);

        } catch (Exception e) {
            script.setError(true);
            script.setScriptErrors(Collections.singletonList(new ScriptInstanceError("Failed to compile script: " + e.getMessage())));
            log.error("Failed while compiling script {}", script.getCode(), e);
        }
    }

    /**
     * Find the script class for a given script code
     * 
     * @param scriptCode Script code
     * @return Script interface Class
     * @throws InvalidScriptException Were not able to instantiate or compile a script
     * @throws ElementNotFoundException Script not found
     */
    public Class<ScriptInterface> getScriptInterface(String scriptCode) throws ElementNotFoundException, InvalidScriptException {
        CacheKeyStr cacheKey = new CacheKeyStr(currentUser.getProviderCode(), EjbUtils.getCurrentClusterNode() + "_" + scriptCode);

        Class<ScriptInterface> compiledScript = compiledScripts.get(cacheKey);
        if (compiledScript == null) {
            compiledScript = scriptCompilerService.getOrCompileScript(scriptCode);
        }

        return compiledScript;
    }

    /**
     * Get a compiled script class
     * 
     * @param scriptCode Script code
     * @return A compiled script class
     * @throws InvalidScriptException Were not able to instantiate or compile a script
     * @throws ElementNotFoundException Script not found
     */
    @SuppressWarnings("unchecked")
    public ScriptInterface getScriptInstance(String scriptCode) throws ElementNotFoundException, InvalidScriptException {

        // First check if it is a deployed script
        ScriptInterface script = (ScriptInterface) EjbUtils.getServiceInterface(scriptCode.lastIndexOf('.') > 0 ? scriptCode.substring(scriptCode.lastIndexOf('.') + 1) : scriptCode);
        if (script != null) {
            return script;
        }

        try {
            // Then check if its a deployed class
            @SuppressWarnings("rawtypes")
            Class clazz = Class.forName(scriptCode);
            if (clazz != null) {
                script = (ScriptInterface) clazz.getDeclaredConstructor().newInstance();
                return script;
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            log.error("Failed to instantiate script {}", scriptCode, e);
            throw new InvalidScriptException(scriptCode, getEntityClass().getName());

        } catch (ClassNotFoundException e) {
            // Ignore error - its not deployed class
        }

        // Otherwise get it from the compiled source code

        try {
            Class<ScriptInterface> scriptClass = getScriptInterface(scriptCode);

            script = scriptClass.getDeclaredConstructor().newInstance();
            return script;

        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            log.error("Failed to instantiate script {}", scriptCode, e);
            throw new InvalidScriptException(scriptCode, getEntityClass().getName());
        }

    }

    /**
     * Get a the same/single/cached instance of compiled script class. A subsequent call to this method will return the same instance of script.
     * 
     * @param scriptCode Script code
     * @return A compiled script class
     * @throws InvalidScriptException Failed to instantiate a script instance
     */
    private ScriptInterface getPooledScriptInstance(String scriptCode) throws InvalidScriptException {

        ObjectPool<ScriptInterface> pool = poolOfScriptInstances.get(scriptCode);

        if (pool == null) {
            synchronized (poolOfScriptInstances) {

                // Double check in case of serialized calls to this method
                if (poolOfScriptInstances.get(scriptCode) == null) {
                    pool = buildPool(scriptCode);
                    poolOfScriptInstances.put(scriptCode, pool);
                }
            }
        }

        try {
            return pool.borrowObject();
        } catch (Exception e) {
            throw new InvalidScriptException("Failed to get a script instance from a pool", scriptCode, e);
        }
    }

    /**
     * Return a pooled script instance back to the pool
     * 
     * @param scriptCode Script code
     * @param script Script instance
     */
    public void returnScriptInstanceToPool(String scriptCode, ScriptInterface script) {
        ObjectPool<ScriptInterface> pool = poolOfScriptInstances.get(scriptCode);
        if (pool != null) {
            try {
                pool.returnObject(script);
            } catch (Exception e) {
                log.error("Failed to return script {} to pool", scriptCode, e);
            }
        }
    }

    /**
     * Clear all compiled scripts and their instances from the cache and pool
     * 
     * @param scriptCode Script code
     */
    public void clearCompiledScriptFromCacheAndPool(String scriptCode) {
        scriptCompilerService.clearCompiledScript(scriptCode);
        clearScriptInstancePool(scriptCode);
    }

    /**
     * Remove a script from a script instance pool
     * 
     * @param scriptCode Script to remove
     */
    private void clearScriptInstancePool(String scriptCode) {
        ObjectPool<ScriptInterface> pool = poolOfScriptInstances.get(scriptCode);
        if (pool != null) {
            try {
                pool.clear();
                poolOfScriptInstances.remove(scriptCode);
            } catch (Exception e) {
                log.error("Failed to clear script pool {}", scriptCode, e);
            }
        }
    }

    /**
     * Build a pool of script instances
     * 
     * @param scriptCode Script code
     * @return A pool of script instances
     * @throws InvalidScriptException Failed to instantiate a script instance
     */
    private ObjectPool<ScriptInterface> buildPool(String scriptCode) throws InvalidScriptException {

        // Script is not necessarily is defined as a Script Instance. It can be a simple bean deployed as a regular class in a .jar file
        ScriptPool poolConfig = null;
        ScriptInstance scriptInstance = findByCode(scriptCode, true);
        if (scriptInstance != null) {
            poolConfig = scriptInstance.getPool();
        } else {
            poolConfig = new ScriptPool();
        }

        ScriptInstancePoolFactory factory = new ScriptInstancePoolFactory(() -> getScriptInstance(scriptCode));

        ObjectPool<ScriptInterface> pool = null;

        if (poolConfig.getMaxIdleTime() == null && poolConfig.getMax() == null) {
            pool = new SoftReferenceObjectPool<>(factory);

        } else {
            pool = new GenericObjectPool<ScriptInterface>(factory);
            if (poolConfig.getMaxIdleTime() != null) {
                ((GenericObjectPool<ScriptInterface>) pool).setMinEvictableIdle(Duration.of(poolConfig.getMaxIdleTime(), ChronoUnit.SECONDS));
                // ((GenericObjectPool<ScriptInterface>) pool).setSoftMinEvictableIdle(Duration.of(poolConfig.getMaxIdleTime(), ChronoUnit.SECONDS));
                // ((GenericObjectPool<ScriptInterface>) pool).setTimeBetweenEvictionRuns(Duration.of(poolConfig.getMaxIdleTime(), ChronoUnit.SECONDS));
            }

            if (poolConfig.getMax() != null) {
                ((GenericObjectPool<ScriptInterface>) pool).setMaxTotal(poolConfig.getMax());
            }

            if (poolConfig.getMin() != null) {
                ((GenericObjectPool<ScriptInterface>) pool).setMinIdle(poolConfig.getMin());
            }
        }

        if (poolConfig.getMin() != null) {
            try {
                pool.addObjects(poolConfig.getMin());
            } catch (Exception e) {
                throw new InvalidScriptException(scriptCode, scriptCode, e);
            }
        }

        return pool;
    }

    /**
     * Validate script Params : inject default values, check mantadory params and allowed values
     * 
     * @param scriptInstance
     * @param context
     */
    private void validateScriptParams(ScriptInstance scriptInstance, Map<String, Object> context) {
        // Inject default values on context for missing params
        injectDefaultValues(scriptInstance, context);

        // Check mantadory params and allowed values
        scriptInstance.getScriptParameters().stream().forEach(sp -> {
            if (sp.isMandatory() && !context.containsKey(sp.getCode())) {
                throw new BusinessException(resourceMessages.getString("message.scriptInstance.paramMandatory", sp.getCode(), sp.getClassName(), scriptInstance.getCode()));
            }
            if (StringUtils.isNotEmpty(sp.getAllowedValues()) && context.containsKey(sp.getCode()) && Arrays.stream(sp.getAllowedValues().split(sp.getValuesSeparator())).noneMatch(context.get(sp.getCode())::equals)) {
                throw new BusinessException(resourceMessages.getString("message.scriptInstance.allowedValues", sp.getCode(), sp.getAllowedValues()));
            }
            if (context.containsKey(sp.getCode())) {
                context.put(sp.getCode(), (sp.isCollection()) ? parseListFromString(String.valueOf(context.get(sp.getCode())), sp.getClassName(), sp.getValuesSeparator())
                        : parseObjectFromString(String.valueOf(context.get(sp.getCode())), sp.getClassName()));
            }
        });

    }

    /**
     * Inject default values on context for missing params
     * 
     * @param scriptInstance
     * @param context
     */
    private void injectDefaultValues(ScriptInstance scriptInstance, Map<String, Object> context) {
        List<ScriptParameter> paramsWithDefaultValue = scriptInstance.getScriptParameters().stream().filter(sp -> StringUtils.isNotEmpty(sp.getDefaultValue())).collect(Collectors.toList());
        paramsWithDefaultValue.stream().filter(sp -> !context.containsKey(sp.getCode())).forEach(sp -> context.put(sp.getCode(), sp.getDefaultValue()));
    }

    /**
     * Parse a list of object from String
     * 
     * @param value
     * @param clazzName
     * @return the object or the entity parsed
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> parseListFromString(String value, String clazzName, String separator) {
        try {
            if (StringUtils.isBlank(value))
                return null;
            else
                return (List<T>) Arrays.stream(value.split(separator)).map(val -> parseObjectFromString(val, clazzName)).collect(Collectors.toList());
        } catch (Exception e) {
            throw new BusinessException(String.format("Failed to parse %s as list of %s", value, clazzName));
        }
    }

    /**
     * Parse an object from String
     * 
     * @param value
     * @param clazzName
     * @return the object or the entity parsed
     */
    public <T> T parseObjectFromString(String value, String clazzName) {
        try {
            if (StringUtils.isBlank(value)) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Class<T> clazz = (Class<T>) Class.forName(clazzName);
            if (clazzName.startsWith("org.meveo.model")) {
                if (value.matches("\\d+")) {
                    return (T) getEntityManager().find(clazz, Long.parseLong(value));
                }
                return (T) getEntityManager().createQuery("from " + clazzName + " where code = :code", clazz).setParameter("code", value).getSingleResult();
            }

            return (T) clazz.getConstructor(new Class[] { String.class }).newInstance(value);
        } catch (Exception e) {
            throw new BusinessException(String.format("Failed to parse %s as %s", value, clazzName));
        }
    }

    /**
     * Apply parameters to script instance by calling corresponding setter methods on the script instance.
     * 
     * @param scriptInstance Script instance
     * @param parameters Parameters to apply
     */
    public void applyParametersToScriptInstance(ScriptInterface scriptInstance, Map<String, Object> parameters) {

        for (Method method : scriptInstance.getClass().getDeclaredMethods()) {
            if (method.getName().startsWith("set") && method.getParameterTypes().length == 1) {
                String parameterName = method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4);
                Object setterValue = parameters.get(parameterName);
                if (setterValue == null) {
                    continue;
                }
                Class<?> paramType = method.getParameterTypes()[0];
                DataTypeUtils.ClassAndValue classAndValue = new DataTypeUtils.ClassAndValue(setterValue);
                if (setterValue instanceof String && !String.class.isAssignableFrom(paramType)) {
                    classAndValue = DataTypeUtils.convertFromString(paramType, (String) setterValue);
                    setterValue = classAndValue.getValue();
                } else if (setterValue instanceof Number && !paramType.isAssignableFrom(setterValue.getClass())) {
                    classAndValue = DataTypeUtils.convertFromNumber(paramType, (Number) setterValue);
                    setterValue = classAndValue.getValue();
                } else if (setterValue instanceof Boolean && paramType.isPrimitive() && boolean.class.isAssignableFrom(paramType)) {
                    classAndValue = new ClassAndValue(((Boolean) setterValue).booleanValue());
                    classAndValue.setClass(boolean.class);
                }

                // log.trace("Converting input value " + setterValue + " of type " + setterValue.getClass() + " to " + paramType + " type for setter " + method.getName() + " converted value is " +
                // classAndValue);

                try {
                    if (!classAndValue.getTypeClass().isAssignableFrom(paramType)) {

                        // If value is a map or a custom entity instance, convert into target class
                        if (setterValue instanceof Map || setterValue instanceof Collection) {
                            setterValue = JacksonUtil.convert(setterValue, paramType);

                            // } else if (setterValue instanceof CustomEntityInstance) { // TODO
                            // CustomEntityInstance cei = (CustomEntityInstance) setterValue;
                            // setterValue = CEIUtils.ceiToPojo(cei, paramType);

                        } else if (Collection.class.isAssignableFrom(paramType)) {
                            // If value which is supposed to be a collection comes with a single value, automatically deserialize it to a collection
                            var type = method.getParameters()[0].getParameterizedType();
                            var jacksonType = TypeFactory.defaultInstance().constructType(type);
                            setterValue = (Collection<?>) JacksonUtil.convert(setterValue, jacksonType);

                        } else {
                            log.error("Failed to convert input value {} of type {} to {} type for setter {}", setterValue, setterValue.getClass(), paramType, method.getName());
                            throw new IllegalArgumentException("Failed to convert input value " + setterValue + " of type " + setterValue.getClass() + " to " + paramType + " type for setter " + method.getName());
                        }
                    }
                    method.invoke(scriptInstance, setterValue);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        }
    }

}