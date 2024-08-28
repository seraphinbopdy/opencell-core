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

package org.meveo.event.monitoring;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSDestinationDefinition;
import javax.jms.JMSDestinationDefinitions;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Topic;

import org.meveo.commons.utils.EjbUtils;
import org.meveo.commons.utils.ReflectionUtils;
import org.meveo.event.monitoring.ClusterEventDto.CrudActionEnum;
import org.meveo.model.BusinessEntity;
import org.meveo.model.IEntity;
import org.meveo.security.CurrentUser;
import org.meveo.security.MeveoUser;
import org.slf4j.Logger;

/**
 * Handle data synchronization between cluster nodes. Inform about CRUD actions to certain entities. Messages are written to topic "topic/CLUSTEREVENTTOPIC".
 * 
 * Used in cases when each cluster node caches locally some information that needs to be updated when data changes.
 * 
 * @author Andrius Karpavicius
 */
@JMSDestinationDefinitions(value = { @JMSDestinationDefinition(name = "java:/topic/CLUSTEREVENTTOPIC", interfaceName = "javax.jms.Topic", destinationName = "ClusterEventTopic"),
        @JMSDestinationDefinition(name = "java:/queue/CLUSTEREVENTREPLY", interfaceName = "javax.jms.Queue", destinationName = "ClusterEventReply") })
@Stateless
public class ClusterEventPublisher implements Serializable {

    private static final long serialVersionUID = 4434372450314613654L;

    /**
     * Time to wait for a reply message to a MQ message send
     */
    private static final long MQ_RESPONSE_WAIT = 10000L;

    @Inject
    private Logger log;

    @Inject
    private JMSContext context;

    @Inject
    @CurrentUser
    protected MeveoUser currentUser;

    @Resource(lookup = "java:/topic/CLUSTEREVENTTOPIC")
    private Topic topic;

    @Resource(lookup = "java:/queue/CLUSTEREVENTREPLY")
    private Queue replyQueue;

    /**
     * Publish event about some action on a given entity
     * 
     * @param entity Entity that triggered event
     * @param action Action performed
     */
    public void publishEvent(IEntity entity, CrudActionEnum action) {
        publishEvent(entity, action, null, false);
    }

    /**
     * Publish event about some action on a given entity.</br>
     * </br>
     * Transaction is not enabled, so that message is published right away to MQ instead of waiting for a TX to commit. <br/>
     * That way a message is received and processed by another node whule this node waits if expectedResponse=true
     * 
     * @param entity Entity that triggered event
     * @param action Action performed
     * @param additionalInformation Additional information about the action
     * @param expectResponse Is expected to receive a response to a message send
     * @return A received response from a message send. Applicable only when expectedResponse = true
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Object publishEvent(IEntity entity, CrudActionEnum action, Map<String, Object> additionalInformation, boolean expectResponse) {

        if (!EjbUtils.isRunningInClusterMode()) {
            return null;
        }

        try {
            String code = entity instanceof BusinessEntity ? ((BusinessEntity) entity).getCode() : null;
            ClusterEventDto eventDto = new ClusterEventDto(ReflectionUtils.getCleanClassName(entity.getClass().getSimpleName()), (Long) entity.getId(), code, action, EjbUtils.getCurrentClusterNode(),
                currentUser.getProviderCode(), currentUser.getUserName(), additionalInformation);
            log.debug("Publishing data synchronization between cluster nodes event {}", eventDto);

            JMSProducer jmsProducer = context.createProducer();
            Message message = context.createObjectMessage(eventDto);

            // Specify Message id and reply queue if response is expected
            if (expectResponse) {
                message.setJMSReplyTo(replyQueue);
                String messageId = entity.getId() + "_" + action + "_" + EjbUtils.getCurrentClusterNode() + "_" + (new Date()).getTime();
                message.setJMSCorrelationID(messageId);
            }

            // For create and update CRUD actions, send message with a delivery delay of two seconds, so data is saved to DB already before another node process the message
            if (action == CrudActionEnum.create || action == CrudActionEnum.update) {
                jmsProducer.setDeliveryDelay(2000L).send(topic, message);
            } else {
                jmsProducer.send(topic, message);
            }

            // Wait for response
            if (expectResponse) {
                String selector = "JMSCorrelationID = '" + message.getJMSCorrelationID() + "'";

                // Use try-with-resources to ensure JMSConsumer is closed
                try (JMSConsumer consumer = context.createConsumer(replyQueue, selector)) {
                    ObjectMessage responseMessage = (ObjectMessage) consumer.receive(MQ_RESPONSE_WAIT);
                    if (responseMessage != null) {
                        Object responseObj = responseMessage.getObject();

                        log.debug("Received a reply to data synchronization message {}", responseObj);

                        return responseObj;
                    } else {
                        log.warn("Failed to receive a reply message to a data synchronization message sent within a time out of {}", MQ_RESPONSE_WAIT);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to publish data synchronization between cluster nodes event", e);
        }
        return null;
    }

    /**
     * Publish event about some action on a given entity
     * 
     * @param entity Entity that triggered event
     * @param action Action performed
     * @param additionalInformation Additional information about the action
     */
    @Asynchronous
    public void publishEventAsync(IEntity entity, CrudActionEnum action, Map<String, Object> additionalInformation, String providerCode, String username) {

        if (!EjbUtils.isRunningInClusterMode()) {
            return;
        }

        try {
            String code = entity instanceof BusinessEntity ? ((BusinessEntity) entity).getCode() : null;
            ClusterEventDto eventDto = new ClusterEventDto(ReflectionUtils.getCleanClassName(entity.getClass().getSimpleName()), (Long) entity.getId(), code, action, EjbUtils.getCurrentClusterNode(), providerCode,
                username, additionalInformation);
            log.debug("Publishing data synchronization between cluster nodes event {}", eventDto);

            context.createProducer().send(topic, eventDto);

        } catch (Exception e) {
            log.error("Failed to publish data synchronization between cluster nodes event", e);
        }
    }

    /**
     * Publish multiple items to a queue or a topic
     * 
     * @param <T>
     * @param destination Queue or a topic to publish to
     * @param iterator Iterator of a data to publish
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public <T> void publish(Destination destination, Iterator<T> iterator) {

        JMSProducer jmsProducer = context.createProducer();
        T itemToProcess = iterator.next();
        while (itemToProcess != null) {
            jmsProducer.send(destination, (Serializable) itemToProcess);
            itemToProcess = iterator.next();
        }
    }
}