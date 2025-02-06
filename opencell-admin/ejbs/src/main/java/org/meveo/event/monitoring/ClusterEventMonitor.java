/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.meveo.event.monitoring;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.meveo.admin.job.IteratorBasedJobBean;
import org.meveo.commons.utils.EjbUtils;
import org.meveo.commons.utils.ReflectionUtils;
import org.meveo.event.monitoring.ClusterEventDto.ClusterEventActionEnum;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.customEntities.CustomEntityTemplate;
import org.meveo.model.jobs.JobExecutionResultStatusEnum;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.jobs.JobLauncherEnum;
import org.meveo.model.scripts.ScriptInstance;
import org.meveo.security.keycloak.CurrentUserProvider;
import org.meveo.service.base.NativePersistenceService;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.crm.impl.CustomFieldTemplateService;
import org.meveo.service.custom.CustomEntityTemplateService;
import org.meveo.service.job.Job;
import org.meveo.service.job.JobExecutionService;
import org.meveo.service.job.JobInstanceService;
import org.meveo.service.script.ScriptCompilerService;
import org.meveo.service.script.ScriptInstanceService;
import org.slf4j.Logger;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;

/**
 * A Message Driven Bean to handle data synchronization between cluster nodes. Messages are read from a topic "topic/CLUSTEREVENTTOPIC".
 * 
 * Currently the following event types are supported - job instance, compiled script instance, role mapping and CFT accumulation rule refresh
 * 
 * @author Andrius Karpavicius
 */
@MessageDriven(name = "ClusterEventMonitor", activationConfig = { @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "topic/CLUSTEREVENTTOPIC"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Topic"), @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class ClusterEventMonitor implements MessageListener {

    @Inject
    private Logger log;

    @Inject
    private JMSContext context;

    @Inject
    private JobInstanceService jobInstanceService;

    @Inject
    private ScriptCompilerService scriptCompilerService;

    @Inject
    private ScriptInstanceService scriptInstanceService;

    @Inject
    private CurrentUserProvider currentUserProvider;

//    @Inject
//    private CfValueAccumulator cfValueAccumulator;

    @Inject
    private CustomFieldTemplateService customFieldTemplateService;

    @Inject
    private CustomEntityTemplateService customEntityTemplateService;

    @Inject
    private JobExecutionService jobExecutionService;

    @Inject
    @Named
    private NativePersistenceService nativePersistenceService;

    /**
     * @see MessageListener#onMessage(Message)
     */
    public void onMessage(Message rcvMessage) {
        try {
            if (rcvMessage instanceof ObjectMessage) {
                ClusterEventDto eventDto = (ClusterEventDto) ((ObjectMessage) rcvMessage).getObject();
                // Ignore message from same node unless it is Endpoint execution result lookup or job completion - JMS message is send in all cases when operating in cluster mode
                if (EjbUtils.getCurrentClusterNode().equals(eventDto.getSourceNode()) && eventDto.getAction() != ClusterEventActionEnum.getEndpointExecutionResult
                        && eventDto.getAction() != ClusterEventActionEnum.jobExecutionCompleted) {
                    return;
                }
                log.info("{} Received cluster synchronization event message {}", EjbUtils.getCurrentClusterNode(), eventDto);

                Object responseValue = processClusterEvent(eventDto);

                // If reply was requested, send a response message with event processing result value
                if (rcvMessage.getJMSReplyTo() != null && responseValue != null) {

                    JMSProducer jmsProducer = context.createProducer();
                    Message responseMessage = context.createObjectMessage((Serializable) responseValue);
                    responseMessage.setJMSCorrelationID(rcvMessage.getJMSCorrelationID());

                    log.debug("Responding to a cluster synchronization event message with response {}", responseValue);

                    jmsProducer.send(rcvMessage.getJMSReplyTo(), responseMessage);
                }

            } else {
                log.warn("Unhandled cluster synchronization event message type: " + rcvMessage.getClass().getName());
            }
        } catch (Exception e) {
            log.error("Failed to process JMS message", e);
        }
    }

    /**
     * Process incoming data synchronization between cluster nodes event
     * 
     * @param eventDto Data synchronization between cluster nodes event.
     * @return A response value of processing an event
     * @throws Exception Any exception during message processing
     */
    private Object processClusterEvent(ClusterEventDto eventDto) throws Exception {

        currentUserProvider.forceAuthentication(eventDto.getUserName(), eventDto.getProviderCode());

        if (eventDto.getClazz().equals(ScriptInstance.class.getSimpleName())) {
            scriptInstanceService.clearCompiledScriptFromCacheAndPool(eventDto.getCode());

        } else if (eventDto.getClazz().equals(JobInstance.class.getSimpleName())) {

            if (eventDto.getAction() == ClusterEventActionEnum.execute) {
                JobLauncherEnum jobLauncher = eventDto.getAdditionalInfo() != null && eventDto.getAdditionalInfo().get(Job.JOB_PARAM_LAUNCHER) != null
                        ? (JobLauncherEnum) eventDto.getAdditionalInfo().get(Job.JOB_PARAM_LAUNCHER)
                        : null;

                return jobExecutionService.executeJob(jobInstanceService.findById(eventDto.getId()), null, jobLauncher, false);

            } else if (eventDto.getAction() == ClusterEventActionEnum.executeWorker) {
                JobLauncherEnum jobLauncher = JobLauncherEnum.WORKER;

                return jobExecutionService.executeJob(jobInstanceService.findById(eventDto.getId()), eventDto.getAdditionalInfo(), jobLauncher, false);

            } else if (eventDto.getAction() == ClusterEventActionEnum.stop) {
                jobExecutionService.stopJob(jobInstanceService.findById(eventDto.getId()), false);

            } else if (eventDto.getAction() == ClusterEventActionEnum.stopByForce) {
                jobExecutionService.stopJobByForce(jobInstanceService.findById(eventDto.getId()), false);

            } else if (eventDto.getAction() == ClusterEventActionEnum.lastJobDataMessageReceived) {
                IteratorBasedJobBean.releaseJobDataProcessingThreads(eventDto.getId());

            } else if (eventDto.getAction() == ClusterEventActionEnum.jobExecutionCompleted) {
                JobExecutionService.releaseJobCompletionWaits(eventDto.getId(),
                    eventDto.getAdditionalInfo() != null ? (JobExecutionResultStatusEnum) eventDto.getAdditionalInfo().get(JobExecutionResultStatusEnum.class.getSimpleName()) : null);

                // Any modify/update
            } else {
                jobInstanceService.scheduleUnscheduleJob(eventDto.getId());
            }

        } else if (eventDto.getClazz().equals(CustomFieldTemplate.class.getSimpleName())) {
            CustomFieldTemplate cft = customFieldTemplateService.findById(eventDto.getId());
//            cfValueAccumulator.refreshCfAccumulationRules(cft);
            // Refresh native table field to data type mapping
            if (cft.getAppliesTo().startsWith(CustomEntityTemplate.CFT_PREFIX) && (eventDto.getAction() == ClusterEventActionEnum.create || eventDto.getAction() == ClusterEventActionEnum.update)) {
                nativePersistenceService.refreshTableFieldMapping(CustomEntityTemplate.getCodeFromAppliesTo(cft.getAppliesTo()));
            }

            // Refresh custom entity template cache
        } else if (eventDto.getClazz().equals(CustomEntityTemplate.class.getSimpleName())) {

            if (eventDto.getAction() == ClusterEventActionEnum.create || eventDto.getAction() == ClusterEventActionEnum.enable) {
                CustomEntityTemplate cet = customEntityTemplateService.findByCode(eventDto.getCode()); // Find by code instead of ID, so it would be added to a cache

            } else if (eventDto.getAction() == ClusterEventActionEnum.update) {
                CustomEntityTemplate cet = customEntityTemplateService.findByCode(eventDto.getCode()); // Find by code instead of ID, so it would be added to a cache
            }

            // Get or wait for endpoint execution result
        } else if (eventDto.getAction() == ClusterEventActionEnum.getEndpointExecutionResult) {

            String asyncId = (String) eventDto.getAdditionalInfo().get("asyncId");
            boolean isCancel = (boolean) eventDto.getAdditionalInfo().get("isCancel");
            boolean isKeep = (boolean) eventDto.getAdditionalInfo().get("isKeep");
            boolean isWait = (boolean) eventDto.getAdditionalInfo().get("isWait");
            Long delayMax = (Long) eventDto.getAdditionalInfo().get("delayMax");
            TimeUnit delayUnit = (TimeUnit) eventDto.getAdditionalInfo().get("delayUnit");

            @SuppressWarnings("rawtypes")
            PersistenceService endpointServiceBean = (PersistenceService) EjbUtils.getServiceInterface("EndpointService");
            Optional<Object> executionResult = ReflectionUtils.getMethodValue(endpointServiceBean, "getOrWaitForEndpointExecutionResult", String.class, asyncId, boolean.class, isCancel, boolean.class, isKeep,
                boolean.class, isWait, Long.class, delayMax, TimeUnit.class, delayUnit);

            return executionResult.orElse(null);

            // Any CRUD action on Endpoint shall refresh the cache of endpoints by code
        } else if (eventDto.getClazz().equals("Endpoint")) {

            Object endpointCacheContainerProviderBean = EjbUtils.getCdiBean(Class.forName("org.meveo.service.endpoint.EndpointCacheContainerProvider"));
            ReflectionUtils.getMethodValue(endpointCacheContainerProviderBean, "refreshCache", String.class, "opencell-endpoints");

        }
        return null;
    }
}
