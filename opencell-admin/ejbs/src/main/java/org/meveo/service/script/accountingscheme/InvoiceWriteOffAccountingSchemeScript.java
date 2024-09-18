package org.meveo.service.script.accountingscheme;

import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.model.payments.AccountOperation;
import org.meveo.model.payments.OCCTemplate;
import org.meveo.model.payments.WriteOff;
import org.meveo.service.accountingscheme.JournalEntryService;
import org.meveo.service.payments.impl.OCCTemplateService;
import org.meveo.service.script.Script;

public class InvoiceWriteOffAccountingSchemeScript extends Script {
	
	private JournalEntryService journalEntryService = getServiceInterface(JournalEntryService.class.getSimpleName());
	private OCCTemplateService occTemplateService = getServiceInterface(OCCTemplateService.class.getSimpleName());
	@Override
	public void execute(Map<String, Object> context) throws BusinessException {
		log.info("InvoiceWriteOffAccountingSchemeScript EXECUTE context {}", context);
		
		AccountOperation ao = (AccountOperation) context.get(Script.CONTEXT_ENTITY);
		if (ao == null) {
			log.warn("No AccountOperation passed as CONTEXT_ENTITY");
			throw new BusinessException("No AccountOperation passed as CONTEXT_ENTITY");
		}
		
		if(!(ao instanceof WriteOff)) {
			log.warn("the account operation passed is not a type of WriteOff");
			throw new BusinessException("the account operation passed is not a type of WriteOff");
		}
		
		
		log.info("Process write off for account operation :  {}", ao.getId());
		
		OCCTemplate occT = occTemplateService.findByCode(ao.getCode());
		journalEntryService.validateOccTForWritOff(ao, occT);
		context.put(Script.RESULT_VALUE, journalEntryService.createFromInvoice(ao, occT));
	}
}
