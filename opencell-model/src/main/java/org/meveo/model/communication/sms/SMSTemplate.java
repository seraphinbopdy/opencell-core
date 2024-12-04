package org.meveo.model.communication.sms;

import org.meveo.model.communication.MessageTemplate;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("SMS")
public class SMSTemplate extends MessageTemplate {

}
