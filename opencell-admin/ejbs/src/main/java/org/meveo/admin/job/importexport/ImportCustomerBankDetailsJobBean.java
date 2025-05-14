package org.meveo.admin.job.importexport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.meveo.admin.async.SubListCreator;

import org.meveo.admin.async.CustomerBankDetailsAsync;
import org.meveo.admin.job.BaseJobBean;
import org.meveo.admin.job.logging.JobLoggingInterceptor;
import org.meveo.commons.utils.FileUtils;
import org.meveo.commons.utils.ImportFileFiltre;
import org.meveo.commons.utils.JAXBUtils;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.interceptor.PerformanceInterceptor;
import org.meveo.model.admin.CustomerBankDetailsImportHisto;
import org.meveo.model.crm.Provider;
import org.meveo.model.jaxb.customer.bankdetails.Document;
import org.meveo.model.jaxb.customer.bankdetails.Modification;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.payments.DDPaymentMethod;
import org.meveo.model.payments.MandateChangeAction;
import org.meveo.model.payments.PaymentMethod;
import org.meveo.service.admin.impl.CustomerBankDetailsImportHistoService;
import org.meveo.service.job.JobExecutionService;
import org.meveo.service.payments.impl.PaymentMethodService;
import org.meveo.util.ApplicationProvider;
import org.slf4j.Logger;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.xml.bind.JAXBException;

@Stateless
public class ImportCustomerBankDetailsJobBean extends BaseJobBean {

    @Inject
    private Logger log;

    @Inject
    private CustomerBankDetailsImportHistoService customerBankDetailsImportHistoService;
    
    @Inject
    private CustomerBankDetailsAsync customerBankDetailsAsync;
    
    private CustomerBankDetailsImportHisto customerBankDetailsImport;
    
    @Inject
    @ApplicationProvider
    protected Provider appProvider;
    
    private int nbModifications;
    private int nbModificationsError;
    private int nbModificationsIgnored;
    private int nbModificationsCreated;
    private String msgModifications="";

    @Inject
    private ParamBeanFactory paramBeanFactory;
    
    private Long nbRuns = 1L;
    private Long waitingMillis = 0L;

    @Interceptors({ JobLoggingInterceptor.class, PerformanceInterceptor.class })
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void execute(JobExecutionResultImpl result, JobInstance jobInstance) {
    	

		try {
			nbRuns = (Long) this.getParamOrCFValue(jobInstance, "ImportCustomerBankDetailsJob_nbRuns");
			waitingMillis = (Long) this.getParamOrCFValue(jobInstance, "ImportCustomerBankDetailsJob_waitingMillis");
			if (nbRuns == -1) {
				nbRuns = (long) Runtime.getRuntime().availableProcessors();
			}

		} catch (Exception e) {
			nbRuns = 1L;
			waitingMillis = 0L;
			log.warn("Cant get nbRuns and waitingMillis customFields for " + jobInstance.getCode(), e.getMessage());
		}
        ParamBean paramBean = paramBeanFactory.getInstance();
        String importDir = paramBeanFactory.getChrootDir() + File.separator + "imports" + File.separator + "bank_Mobility" + File.separator;        
              
        String dirOK = importDir + "output";
        String dirKO = importDir + "reject";        
        List<File> files = getFilesFromInput(paramBean, importDir);
        traitementFiles(result, dirOK, dirKO, files);

        result.setNbItemsToProcess(nbModifications);
        result.setNbItemsCorrectlyProcessed(nbModificationsCreated);
        result.setNbItemsProcessedWithError(nbModificationsError);
        result.setNbItemsProcessedWithWarning(nbModificationsIgnored);
        result.setReport(msgModifications);
    }

    private List<File> getFilesFromInput(ParamBean paramBean, String importDir) {
        String dirIN = importDir + "input";
        String prefix = paramBean.getProperty("importCustomerBankDetails.prefix", "acmt");
        String ext = paramBean.getProperty("importCustomerBankDetails.extension", "");
        File dir = new File(dirIN);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        List<File> files = getFilesToProcess(dir, prefix, ext);
        int numberOfFiles = files.size();
        log.info("InputFiles job to import={}", numberOfFiles);
        return files;
    }

    private void traitementFiles(JobExecutionResultImpl result, String dirOK, String dirKO, List<File> files) {
        for (File file : files) {
            File currentFile = null;
            try {
                log.info("InputFiles job {} in progress...", file.getName());
                currentFile = FileUtils.addExtension(file, ".processing");

                importFile(currentFile, file.getName(), result);
                FileUtils.moveFile(dirOK, currentFile, file.getName());
                log.info("InputFiles job {} done.", file.getName());
            } catch (Exception e) {
                log.error("failed to import Customer Bank Details", e);
                FileUtils.moveFile(dirKO, currentFile, file.getName());
            } finally {
                if (currentFile != null)
                {
                    currentFile.delete();
                }
            }
        }
    }

    

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    private void importFile(File file, String fileName, JobExecutionResultImpl result) throws JAXBException, CloneNotSupportedException,Exception {
        createCustomerBankDetailsImport(fileName);

        if (file.length() < 100) {
            createHistory();
            return;
        }

        Document customerBankDetails = (Document) JAXBUtils.unmarshaller(Document.class, file);        
        log.debug("parsing file ok");

        nbModifications = customerBankDetails.getMessageBanqueEmetteur().getModification().size();
        log.debug("nbModifications: {}", nbModifications);
        if (nbModifications == 0) {
            return;
        }

        paymentMethodeDepartArrivee(customerBankDetails, result);    
        createHistory();
        log.info("end import file ");
    }

    
    private void paymentMethodeDepartArrivee(Document customerBankDetails,JobExecutionResultImpl result) throws Exception {
    	List<Future<Map<String,Object>>> futures = new ArrayList<>();
    	
		SubListCreator<Modification> subListCreator = new SubListCreator(customerBankDetails.getMessageBanqueEmetteur().getModification(), nbRuns.intValue());
    	
		while (subListCreator.isHasNext()) {
			futures.add(customerBankDetailsAsync.launchAndForgetImportDeatails( subListCreator.getNextWorkSet())); 
			try {
				Thread.sleep(waitingMillis);
			} catch (InterruptedException e) {
				log.error("", e);
			}
		}
		// Wait for all async methods to finish
		for (Future<Map<String, Object>> future : futures) {
			try {
				Map<String, Object> futureResult = future.get();
				nbModifications += (Integer) futureResult.get("nbModifications");
				nbModificationsError += (Integer) futureResult.get("nbModificationsError");
				nbModificationsIgnored += (Integer) futureResult.get("nbModificationsIgnored");
				nbModificationsCreated += (Integer) futureResult.get("nbModificationsCreated");
				
				msgModifications += (String) futureResult.get("msgModifications");

			} catch (InterruptedException e) {
				// It was cancelled from outside - no interest

			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				result.registerError(cause.getMessage());
				result.addReport(cause.getMessage());
				log.error("Failed to execute async method", cause);
			}
		}
	    	
    }
    

    private void createCustomerBankDetailsImport(String fileName) {
        customerBankDetailsImport = new CustomerBankDetailsImportHisto();
        customerBankDetailsImport.setExecutionDate(new Date());
        customerBankDetailsImport.setFileName(fileName);
    }

    /**
     * @throws Exception exception
     */
    private void createHistory() {
        customerBankDetailsImport.setLinesRead(nbModifications);
        customerBankDetailsImport.setLinesInserted(nbModificationsCreated);
        customerBankDetailsImport.setLinesRejected(nbModificationsIgnored);
        customerBankDetailsImport.setNbCustomerAccountsIgnored(nbModificationsIgnored);
        customerBankDetailsImport.setNbCustomerAccountsError(nbModificationsError);
        customerBankDetailsImport.setNbCustomerAccountsCreated(nbModificationsCreated);
        customerBankDetailsImport.setNbCustomerAccounts(nbModifications);
        customerBankDetailsImportHistoService.create(customerBankDetailsImport);
    }

    /**
     * @param dir folder
     * @param prefix prefix file
     * @param ext extension file
     * @return list of file to proceed
     */
    private List<File> getFilesToProcess(File dir, String prefix, String ext) {
        List<File> files = new ArrayList<File>();
        ImportFileFiltre filtre = new ImportFileFiltre(prefix, ext);
        File[] listFile = dir.listFiles(filtre);

        if (listFile == null) {
            return files;
        }

        for (File file : listFile) {
            if (file.isFile()) {
                files.add(file);
                // we just process one file
                return files;
            }
        }
        return files;
    }
    
}