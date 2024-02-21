package org.meveo.service.payments.impl;

import static java.io.File.separator;
import static java.lang.Math.abs;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.apache.commons.lang3.StringUtils.split;
import static org.meveo.apiv2.payments.RejectionCodeImportMode.UPDATE;
import static org.meveo.commons.utils.StringUtils.EMPTY;
import static org.meveo.service.payments.impl.RejectionCodeImportResult.EMPTY_IMPORT_RESULT;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.api.exception.BusinessApiException;
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
    private static final String SEPARATOR = ";\"";
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
                .map(descriptionI18n::get)
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
        return join(";", data);
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

    public RejectionCodeImportResult importRejectionCodes(ImportRejectionCodeConfig config) {
        byte[] importStream = Base64.getDecoder().decode(config.getBase64Csv());
        String[] lines = new String(importStream).split("\\n");
        List<String> errors = new ArrayList<>();
        RejectionCodeImportResult rejectionCodeImportResult = EMPTY_IMPORT_RESULT;
        if (lines.length > 0) {
            String[] header = split(lines[0].trim(), SEPARATOR);
            List<Object[]> languagesDetails = getAvailableTradingLanguage();
            validateHeader(header, languagesDetails);
            List<PaymentRejectionCode> rejectionCodes =
                    createCodesFromLines(lines, header, languagesDetails, errors, config);
            rejectionCodeImportResult = processDataAndSave(rejectionCodes, errors, config, lines.length - 1);
        }
        return rejectionCodeImportResult;
    }

    private void validateHeader(String[] header, List<Object[]> languagesDetails) {
        if(!header[0].equalsIgnoreCase("Payment gateway")
                || !header[1].equalsIgnoreCase("Rejection code")) {
            throw new BusinessApiException("Incorrect header, please export existing settings to get correct headers");
        }
        for (int index = 2; index < header.length; index++) {
            String language = header[index];
            if(language.split(" ").length > 1) {
                String languageCode = language.split(" ")[1];
                boolean match = languagesDetails.stream()
                        .anyMatch(lang -> ((String) lang[2]).equalsIgnoreCase(languageCode));
                if(!match) {
                    throw new BusinessApiException("Language " + languageCode + " is not a defined trading language");
                }
            }
        }
    }

    private  List<PaymentRejectionCode> createCodesFromLines(String[] lines, String[] header,
                                                                     List<Object[]> languagesDetails,
                                                                     List<String> errors, ImportRejectionCodeConfig config) {
        List<PaymentRejectionCode> rejectionCodes = new ArrayList<>();
        for (int index = 1; index < lines.length; index++) {
            String[] line = split(lines[index].trim(), SEPARATOR);
            PaymentRejectionCode paymentRejectionCode =  createFromImportData(line, header);
            if(!config.isIgnoreLanguageErrors()) {
                String languagesValidation =
                        checkDescriptionI18(paymentRejectionCode.getDescriptionI18n(), languagesDetails);
                if(!languagesValidation.isBlank()) {
                    errors.add(format("Error occurred during importing line %d, error : %s", index, languagesValidation));
                } else {
                    rejectionCodes.add(paymentRejectionCode);
                }
            } else {
                rejectionCodes.add(paymentRejectionCode);
            }
        }
        return rejectionCodes;
    }

    private PaymentRejectionCode createFromImportData(String[] line, String[] header) {
        PaymentRejectionCode rejectionCode = new PaymentRejectionCode();
        Map<String, String> inputLanguages = new HashMap<>();
        for (int i = 0; i < line.length; i++) {
            if ("payment gateway".equalsIgnoreCase(header[i])) {
                rejectionCode.setPaymentGateway(paymentGatewayService.findByCode(line[i].trim()));
            }
            if ("rejection code".equalsIgnoreCase(header[i])) {
                rejectionCode.setCode(line[i].trim());
            }
            if ("description".equalsIgnoreCase(header[i])) {
                rejectionCode.setDescription(line[i]);
            }
            if (header[i] != null && header[i].matches(DESCRIPTION_I18N_REGEX)) {
                inputLanguages.put(header[i].split(" ")[1], line[i].trim());
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

    private RejectionCodeImportResult processDataAndSave(List<PaymentRejectionCode> rejectionCodes,
                                                         List<String> errors, ImportRejectionCodeConfig config, int lineCount) {
        if (rejectionCodes != null && !rejectionCodes.isEmpty()) {
            if (UPDATE.equals(config.getMode())) {
                for (PaymentRejectionCode paymentRejectionCode : rejectionCodes) {
                    Optional<PaymentRejectionCode> rejectionCode = findByCodeAndPaymentGateway(paymentRejectionCode.getCode(),
                            paymentRejectionCode.getPaymentGateway().getId());
                    if (rejectionCode.isPresent()) {
                        PaymentRejectionCode rejectionCodeToUpdate = rejectionCode.get();
                        rejectionCodeToUpdate.setDescription(paymentRejectionCode.getDescription());
                        rejectionCodeToUpdate.setDescriptionI18n(paymentRejectionCode.getDescriptionI18n());
                        rejectionCodeToUpdate.setPaymentGateway(paymentRejectionCode.getPaymentGateway());
                        update(rejectionCodeToUpdate);
                    } else {
                        create(paymentRejectionCode);
                    }
                    getEntityManager().flush();
                }
            } else {
                Map<PaymentGateway, List<PaymentRejectionCode>> groupedCodes = rejectionCodes.stream()
                        .collect(groupingBy(PaymentRejectionCode::getPaymentGateway));
                for (Map.Entry<PaymentGateway, List<PaymentRejectionCode>> entry : groupedCodes.entrySet()) {
                    clearAll(entry.getKey());
                    entry.getValue().forEach(rejectionCodesCode -> {
                        if(findByCodeAndPaymentGateway(rejectionCodesCode.getCode(), entry.getKey().getId()).isPresent()) {
                            errors.add(format("Error occurred during importing rejection code, %s already exists in paymentGateway %s",
                                    rejectionCodesCode.getCode(), entry.getKey().getCode()));
                        } else {
                            create(rejectionCodesCode);
                            getEntityManager().flush();
                        }
                    });
                }
            }
        }
        return new RejectionCodeImportResult(lineCount, abs(lineCount - errors.size()), errors.size(), errors);
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
        rejectionCodes.forEach(this::remove);
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
