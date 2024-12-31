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

package org.meveo.admin.job;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.meveo.admin.async.SynchronizedIterator;
import org.meveo.admin.util.DirectoriesConstants;
import org.meveo.commons.utils.ParamBean;
import org.meveo.model.billing.Invoice;
import org.meveo.model.billing.InvoiceStatusEnum;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.billing.impl.InvoiceService;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBException;

/**
 * Job implementation to generate invoice XML for all valid invoices that don't have it
 * 
 * @author Andrius Karpavicius
 */
@Stateless
public class XMLEInvoiceGenerationJobBean extends IteratorBasedJobBean<Long> {

    private static final long serialVersionUID = 7948947993905799076L;

    @Inject
    private InvoiceService invoiceService;

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) {
        super.execute(jobExecutionResult, jobInstance, this::initJobAndGetDataToProcess, this::generateXml, null, null, null);
    }

    /**
     * Initialize job settings and retrieve data to process
     * 
     * @param jobExecutionResult Job execution result
     * @return An iterator over a list of Invoices to generate PDF files
     */
    private Optional<Iterator<Long>> initJobAndGetDataToProcess(JobExecutionResultImpl jobExecutionResult) {

        JobInstance jobInstance = jobExecutionResult.getJobInstance();

        List<String> statusNamesList = (List<String>) this.getParamOrCFValue(jobInstance, "invoicesToProcess", asList("VALIDATED"));
        List<InvoiceStatusEnum> statusList = statusNamesList.stream().map(status -> InvoiceStatusEnum.valueOf(status)).collect(toList());
		
	    moveUBLFiles();
        List<Long> invoiceIds = this.fetchInvoiceIdsToProcess(statusList);
        return Optional.of(new SynchronizedIterator<>(invoiceIds));
    }

    /**
     * Generate E-INV XML file
     * 
     * @param invoiceId Invoice id to create XML for
     * @param jobExecutionResult Job execution result
     */
    private void generateXml(Long invoiceId, JobExecutionResultImpl jobExecutionResult) {

        Invoice invoice = invoiceService.findById(invoiceId);
	    try {
		    invoiceService.produceInvoiceUBLFormat(invoice);
	    } catch (JAXBException e) {
		    jobExecutionResult.addErrorReport("Error on invoice : " + invoice.getInvoiceNumber() + " while generate UBL format : " + e.getMessage());
			log.error("can not generate UBL format for invoice : " + invoice.getInvoiceNumber(), e);
	    }
    }
	
	/**
	 *
	 * @param statusList
	 * @return return list of invoice that have urbRefencre equal to false
	 */
    private List<Long> fetchInvoiceIdsToProcess(List<InvoiceStatusEnum> statusList) {
        log.debug(" fetchInvoiceIdsToProcess for InvoiceStatusEnums = {} and ublReference = false ", statusList);
        return invoiceService.listInvoicesWithoutXml(statusList);
    }
	public void moveUBLFiles() {
		// check if the directory ubl is present or not
		ParamBean paramBean = ParamBean.getInstance();
		final String ROOT_DIR = paramBean.getChrootDir("") + File.separator;
		final String NEW_UBL_DIRECTORY = DirectoriesConstants.INVOICES_ROOT_FOLDER + File.separator + "ubl";
		File oldUblDirectory = new File(ROOT_DIR  + "ubl");
		File ublDirectory = new File(ROOT_DIR + paramBean.getProperty("meveo.ubl.directory", NEW_UBL_DIRECTORY));
		if(!ublDirectory.exists()){
			ublDirectory.mkdirs();
		}
		if(!oldUblDirectory.exists() || !oldUblDirectory.isDirectory()){
			return;
		}
		File[] files = oldUblDirectory.listFiles();
		for (File file : files) {
			try{
				Path targetPath = Paths.get(ublDirectory.getAbsolutePath() + File.separator + file.getName());
				Files.move(file.toPath(), targetPath);
			}catch (IOException e) {
				log.error("Error while moving file : " + file.getName() + " to ubl directory", e);
			}
		}
	}
}