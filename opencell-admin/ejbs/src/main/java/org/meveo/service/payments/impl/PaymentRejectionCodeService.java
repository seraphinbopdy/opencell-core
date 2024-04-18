package org.meveo.service.payments.impl;

import static java.io.File.separator;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.meveo.apiv2.payments.RejectionCodeImportMode.UPDATE;
import static org.meveo.commons.utils.StringUtils.EMPTY;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.model.payments.PaymentGateway;
import org.meveo.model.payments.PaymentRejectionCode;
import org.meveo.service.base.BusinessService;
import org.meveo.service.billing.impl.TradingLanguageService;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Stateless
public class PaymentRejectionCodeService extends BusinessService<PaymentRejectionCode> {

    public static final String FILE_PATH_RESULT_LABEL = "FILE_PATH";
    public static final String EXPORT_SIZE_RESULT_LABEL = "EXPORT_SIZE";
    public static final String ENCODED_FILE_RESULT_LABEL = "ENCODED_FILE";

    private static final String EXPORT_DATE_FORMAT_PATTERN = "yyyyMMdd_HHmmss";
    private static final String DESCRIPTION_I18N_REGEX = "Description ([a-zA-Z]*$)";
    private static final String DELIMITER = ";";
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat(EXPORT_DATE_FORMAT_PATTERN);

    @Inject
    private TradingLanguageService tradingLanguageService;

    @Inject
    private PaymentGatewayService paymentGatewayService;

    /**
     * Create payment rejection code
     *
     * @param rejectionCode payment rejection code
     */
    @Override
    public void create(PaymentRejectionCode rejectionCode) {
        if (findByCodeAndPaymentGateway(rejectionCode.getCode(),
                rejectionCode.getPaymentGateway().getId()).isPresent()) {
            throw new BusinessException(format("Rejection code with code %s already exists in gateway %s",
                    rejectionCode.getCode(), rejectionCode.getPaymentGateway().getCode()));
        }
        super.create(rejectionCode);

    }

    /**
     * Find a payment rejection code using code and payment gateway id
     *
     * @param code             payment rejection code
     * @param paymentGatewayId payment gateway id
     * @return Optional of PaymentRejectionCode
     */
    public Optional<PaymentRejectionCode> findByCodeAndPaymentGateway(String code, Long paymentGatewayId) {
        try {
            return of((PaymentRejectionCode) getEntityManager()
                    .createNamedQuery("PaymentRejectionCode.findByCodeAndPaymentGateway")
                    .setParameter("code", code)
                    .setParameter("paymentGatewayId", paymentGatewayId)
                    .getSingleResult());
        } catch (NoResultException exception) {
            return empty();
        }
    }

    /**
     * Clear rejectionsCodes by gateway
     *
     * @param paymentGateway payment gateway
     */
    public int clearAll(PaymentGateway paymentGateway) {
        String namedQuery = paymentGateway != null
                ? "PaymentRejectionCode.clearAllByPaymentGateway" : "PaymentRejectionCode.clearAll";
        Query clearQuery = getEntityManager().createNamedQuery(namedQuery);
        if (paymentGateway != null) {
            clearQuery.setParameter("paymentGatewayId", paymentGateway.getId());
        }
        try {
            return clearQuery.executeUpdate();
        } catch (Exception exception) {
            throw new BusinessException("Error occurred during rejection codes clearing " + exception.getMessage());
        }
    }

    /**
     * Export rejectionsCodes by gateway
     *
     * @param paymentGateway payment gateway
     */
    public Map<String, Object> export(PaymentGateway paymentGateway) {
        Map<String, Object> filters = null;
        if (paymentGateway != null) {
            filters = new HashMap<>();
            filters.put("paymentGateway", paymentGateway);
        }
        String exportFileName = "PaymentRejectionCodes_"
                + (paymentGateway != null ? paymentGateway.getCode() : "AllGateways") + "_" + dateFormatter.format(new Date());
        List<Object[]> languagesDetails = getAvailableTradingLanguage();
        List<String> dataToExport = prepareLines(list(new PaginationConfiguration(filters)), languagesDetails);
        try {
            String exportFile = buildExportFilePath(exportFileName, "exports");
            String header = "Payment gateway;Rejection code;Description;" + getAvailableTradingLanguages(languagesDetails);
            try (PrintWriter writer = new PrintWriter(exportFile)) {
                writer.println(header);
                dataToExport.forEach(writer::println);
            }
            Map<String, Object> exportResult = new HashMap<>();
            exportResult.put(FILE_PATH_RESULT_LABEL, exportFile);
            exportResult.put(EXPORT_SIZE_RESULT_LABEL, dataToExport.size());
            exportResult.put(ENCODED_FILE_RESULT_LABEL, Base64.getEncoder().encodeToString(readFileToByteArray(new File(exportFile))));
            log.debug("Rejection codes export file name : " + exportFileName);
            log.debug("Rejection codes export size : " + dataToExport.size());
            return exportResult;
        } catch (IOException exception) {
            throw new BusinessException(exception.getMessage());
        }
    }

    private List<Object[]> getAvailableTradingLanguage() {
        return (List<Object[]>) tradingLanguageService.getEntityManager()
                .createNamedQuery("TradingLanguage.findLanguageDetails")
                .getResultList();
    }

    private List<String> prepareLines(List<PaymentRejectionCode> rejectionCodes, List<Object[]> languagesDetails) {
        return rejectionCodes.stream()
                .map(rejectionCode -> convertToCSV(rejectionCode.getPaymentGateway().getCode(),
                        rejectionCode.getCode(),
                        rejectionCode.getDescription(),
                        buildI18nDescription(rejectionCode.getDescriptionI18n(), languagesDetails)))
                .collect(toList());
    }

    private String buildI18nDescription(Map<String, String> descriptionI18n, List<Object[]> languagesDetails) {
        if(languagesDetails == null) {
            return EMPTY;
        }
        return languagesDetails.stream()
                .map(language -> language[2])
                .map(languageCode -> {
                    if(descriptionI18n == null) {
                        return EMPTY;
                    } else {
                        return ofNullable(descriptionI18n.get(languageCode)).orElse(EMPTY);
                    }
                })
                .collect(joining(";"));
    }

    private String buildExportFilePath(String fileName, String directoryName) {
        String exportDirectoryPath = paramBeanFactory.getChrootDir() + separator + directoryName + separator;
        File exportDirectory = new File(exportDirectoryPath);
        if (!exportDirectory.exists()) {
            exportDirectory.mkdirs();
        }
        return exportDirectory.getPath() + separator + fileName + ".csv";
    }

    public String convertToCSV(String... data) {
        return join(DELIMITER, data);
    }

    private String getAvailableTradingLanguages(List<Object[]> languagesDetails) {
        String tradingLanguages = "";
        if (languagesDetails != null) {
            tradingLanguages = languagesDetails.stream()
                    .map(language -> "Description " + language[2])
                    .collect(joining(";"));
        }
        return tradingLanguages;
    }

    public int importRejectionCodes(ImportRejectionCodeConfig config) {
        byte[] importStream = Base64.getDecoder().decode(config.getBase64Csv());
        String[] lines = new String(importStream).split("\\n");
        int resultCount = 0;
        if (lines.length > 0) {
            String[] header = lines[0].trim().split(DELIMITER);
            List<Object[]> languagesDetails = getAvailableTradingLanguage();
            validateHeader(header, languagesDetails);
            List<PaymentRejectionCode> rejectionCodes = createCodesFromLines(lines, header, languagesDetails, config);
            processDataAndSave(rejectionCodes, config);
            resultCount = rejectionCodes.size();
        }
        return resultCount;
    }

    private void validateHeader(String[] header, List<Object[]> languagesDetails) {
        if(!header[0].equalsIgnoreCase("Payment gateway")
                || !header[1].equalsIgnoreCase("Rejection code")) {
            throw new BusinessException("Incorrect header, please export existing settings to get correct headers");
        }
        for (int index = 2; index < header.length; index++) {
            String language = header[index];
            if(language.split(" ").length > 1) {
                String languageCode = language.split(" ")[1];
                boolean match = languagesDetails.stream()
                        .anyMatch(lang -> ((String) lang[2]).equalsIgnoreCase(languageCode));
                if(!match) {
                    throw new BusinessException(format("Language %s is not a defined trading language", languageCode));
                }
            }
        }
    }

    private  List<PaymentRejectionCode> createCodesFromLines(String[] lines, String[] header,
                                                             List<Object[]> languagesDetails, ImportRejectionCodeConfig config) {
        List<PaymentRejectionCode> rejectionCodes = new ArrayList<>();
        for (int index = 1; index < lines.length; index++) {
            String[] line = lines[index].trim().split(DELIMITER);
            PaymentRejectionCode paymentRejectionCode = createFromImportData(line, header, index);
            if(!config.isIgnoreLanguageErrors()) {
                String languagesValidation =
                        checkDescriptionI18(paymentRejectionCode.getDescriptionI18n(), languagesDetails);
                if(!languagesValidation.isBlank()) {
                    throw new BusinessException(format("Error occurred during importing line %d, %s",
                            index, languagesValidation));
                }
            }
            rejectionCodes.add(paymentRejectionCode);
        }
        return rejectionCodes;
    }

    private PaymentRejectionCode createFromImportData(String[] line, String[] header, int lineNumber) {
        PaymentRejectionCode rejectionCode = new PaymentRejectionCode();
        Map<String, String> inputLanguages = new HashMap<>();
        for (int index = 0; index < line.length; index++) {
            if ("payment gateway".equalsIgnoreCase(header[index])) {
                String gatewayCode = line[index].trim();
                if(isBlank(gatewayCode)) {
                    throw new BusinessException(format("Error importing line %d, payment gateway is mandatory", lineNumber));
                }
                PaymentGateway paymentGateway = paymentGatewayService.findByCode(line[index].trim());
                if(paymentGateway == null) {
                    throw new BusinessException(format("Error importing line %d, payment Gateway %s does not exist",
                            lineNumber, gatewayCode));
                }
                rejectionCode.setPaymentGateway(paymentGateway);
            }
            if ("rejection code".equalsIgnoreCase(header[index])) {
                if(isBlank(line[index].trim())) {
                    throw new BusinessException(format("Error importing line %d, rejection code is mandatory", lineNumber));
                }
                rejectionCode.setCode(line[index].trim());
            }
            if ("description".equalsIgnoreCase(header[index])) {
                rejectionCode.setDescription(line[index]);
            }
            if (header[index] != null && header[index].matches(DESCRIPTION_I18N_REGEX)) {
                inputLanguages.put(header[index].split(" ")[1], line[index].trim());
            }
        }
        rejectionCode.setDescriptionI18n(inputLanguages);
        return rejectionCode;
    }

    private String checkDescriptionI18(Map<String, String> inputLanguages, List<Object[]> languagesDetails) {
        if (languagesDetails == null || languagesDetails.isEmpty()) {
            return EMPTY;
        }
        return languagesDetails.stream()
                .filter(language -> !inputLanguages.containsKey(language[2]) && inputLanguages.get(language[2]) == null)
                .map(language -> "Trading language " + language[2] + " not provided")
                .collect(joining("\\n"));
    }

    private void processDataAndSave(List<PaymentRejectionCode> rejectionCodes, ImportRejectionCodeConfig config) {
        Map<String, PaymentRejectionCode> rejectionCodeMemoryStore = new HashMap<>();
        if (rejectionCodes != null && !rejectionCodes.isEmpty()) {
            if (UPDATE.equals(config.getMode())) {
                for (PaymentRejectionCode paymentRejectionCode : rejectionCodes) {
                    createOrUpdateCode(paymentRejectionCode, paymentRejectionCode.getPaymentGateway(), rejectionCodeMemoryStore);
                }
            } else {
                Map<PaymentGateway, List<PaymentRejectionCode>> groupedCodes = rejectionCodes.stream()
                        .collect(groupingBy(PaymentRejectionCode::getPaymentGateway));
                for (Map.Entry<PaymentGateway, List<PaymentRejectionCode>> entry : groupedCodes.entrySet()) {
                    clearAll(entry.getKey());
                    entry.getValue().forEach(rejectionCodesCode -> {
                        createOrUpdateCode(rejectionCodesCode, entry.getKey(), rejectionCodeMemoryStore);
                    });
                }
            }
        }
    }

    private void createOrUpdateCode(PaymentRejectionCode rejectionCode, PaymentGateway paymentGateway,
                                    Map<String, PaymentRejectionCode> rejectionCodeMemoryStore) {
        Optional<PaymentRejectionCode> rejectionCodeToSave =
                findByCodeAndPaymentGateway(rejectionCode.getCode(), paymentGateway.getId());
        String key = rejectionCode.getCode() + rejectionCode.getPaymentGateway().getCode();
        if(rejectionCodeToSave.isEmpty() && rejectionCodeMemoryStore.containsKey(key)) {
            rejectionCodeToSave = of(rejectionCodeMemoryStore.get(key));
        }
        if (rejectionCodeToSave.isPresent()) {
            PaymentRejectionCode rejectionCodeToUpdate = rejectionCodeToSave.get();
            rejectionCodeToUpdate.setDescription(rejectionCode.getDescription());
            rejectionCodeToUpdate.setDescriptionI18n(rejectionCode.getDescriptionI18n());
            rejectionCodeToUpdate.setPaymentGateway(rejectionCode.getPaymentGateway());
            rejectionCodeMemoryStore.put(key, rejectionCodeToUpdate);
            update(rejectionCodeToUpdate);
        } else {
            rejectionCodeMemoryStore.put(key, rejectionCode);
            super.create(rejectionCode);
        }
    }

    /**
     * Remove rejection codes
     *
     * @param rejectionCodes List of rejection codes
     */
    public void remove(List<PaymentRejectionCode> rejectionCodes) {
        if (rejectionCodes == null || rejectionCodes.isEmpty()) {
            return;
        }
        rejectionCodes.forEach(rejectionCode ->  {
            if(rejectionCode.getPaymentRejectionCodesGroup() == null) {
                remove(rejectionCode);
            } else {
                throw new BusinessException("Rejection code " + rejectionCode.getCode()
                        + " is used in a rejection codes group.");
            }
        } );
        getEntityManager().flush();
    }

    /**
     * Find payment rejection codes by payment gateway id
     *
     * @param paymentGatewayId payment gateway id
     * @return List of PaymentRejectionCode
     */
    public List<PaymentRejectionCode> findBYPaymentGateway(Long paymentGatewayId) {
        if (paymentGatewayId == null) {
            return emptyList();
        }
        return (List<PaymentRejectionCode>) getEntityManager()
                .createNamedQuery("PaymentRejectionCode.findByPaymentGateway")
                .setParameter("paymentGatewayId", paymentGatewayId)
                .getResultList();
    }
}
