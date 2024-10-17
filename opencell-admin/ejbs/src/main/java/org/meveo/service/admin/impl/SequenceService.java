package org.meveo.service.admin.impl;

import org.meveo.model.sequence.Sequence;
import org.meveo.service.base.BusinessService;

import javax.ejb.Stateless;

@Stateless
public class SequenceService extends BusinessService<Sequence> {

    /**
     * Generate nex sequence
     *
     * @param sequence
     * @return Sequence
     */
    public Sequence generateSequence(Sequence sequence) {
	    sequence.setCurrentNumber(sequence.getCurrentNumber() != null ? sequence.getCurrentNumber() + 1 : 0);
        return sequence;
    }
	

}