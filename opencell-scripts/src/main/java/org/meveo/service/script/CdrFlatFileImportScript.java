package org.meveo.service.script;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.rating.CDR;
import org.meveo.service.medina.impl.CDRService;
import org.primefaces.shaded.commons.io.FilenameUtils;

public class CdrFlatFileImportScript extends Script {

    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX";
    private final transient ParamBeanFactory paramBeanFactory = (ParamBeanFactory) getServiceInterface(ParamBeanFactory.class.getSimpleName());
    private final transient CDRService cdrService = (CDRService) getServiceInterface(CDRService.class.getSimpleName());

    public static boolean elementExisted(Map<String, String> context, String[] header, String[] body, String element) {
        return ArrayUtils.indexOf(header, context.get(element), 0) >= 0 && ArrayUtils.indexOf(header, context.get(element), 0) < body.length;
    }

    @Override
    public void execute(Map<String, Object> contextMethod) throws BusinessException {
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        JobExecutionResultImpl jobExecutionResult = (JobExecutionResultImpl) contextMethod.get("JobExecutionResult");
        Map<String, String> context = (Map<String, String>) jobExecutionResult.getJobInstance().getCfValues().getValues().get("mapping");
        String pathFile = (String) jobExecutionResult.getJobInstance().getCfValues().getValues().get("pathFile");

        String rootPathFile = getProviderRootDir() + File.separator + pathFile;
        File dir = new File(rootPathFile);

        File[] fileList = dir.listFiles();
        if (fileList == null) {
            return;
        }

        for (File fileInput : fileList) {
            if (!"csv".equals(FilenameUtils.getExtension(fileInput.getName()))) {
                continue;
            }

            File rejectFile = new File(fileInput.getAbsolutePath().replace("input", "reject") + ".rejected");
            if (!rejectFile.getParentFile().exists()) {
                rejectFile.getParentFile().mkdirs();
            }

            try (FileWriter fw = new FileWriter(rejectFile, true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 FileReader fread = new FileReader(fileInput);
                 BufferedReader br = new BufferedReader(fread)) {

                String line;
                String splitBy = ";";
                String[] header = br.readLine().split(splitBy);

                if (header.length == 1 && header[0].contains(",")) {
                    splitBy = ",";
                    header = header[0].split(splitBy);
                }

                while ((line = br.readLine()) != null) {
                    try {
                        CDR cdr = new CDR();
                        String[] body = line.split(splitBy);
                        boolean reject = processLine(context, header, body, dateFormat, cdr, bw, line);
                        if (!reject) {
                            if (cdr.getEventDate() != null && cdr.getQuantity() != null && cdr.getAccessCode() != null && cdr.getParameter1() != null) {
                                cdrService.create(cdr);
                            } else {
                                validateCdr(line, cdr, context, bw);
                            }
                        }
                    } catch (Exception e) {
                        bw.write(line + " => " + e.getMessage() + " \n");
                    }
                }
                
                moveOrDeleteFile(fileInput, rejectFile);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private boolean processLine(Map<String, String> context, String[] header, String[] body, DateFormat dateFormat, CDR cdr, BufferedWriter bw, String line) {
        boolean reject = false;
        try {
            cdr.setEventDate(parseDate(context, header, body, "eventDate", dateFormat, bw, line));
            cdr.setAccessCode(getField(context, header, body, "accessCode"));
            cdr.setQuantity(parseBigDecimal(context, header, body, "quantity"));
            cdr.setParameter1(getField(context, header, body, "parameter1"));
            cdr.setParameter2(getField(context, header, body, "parameter2"));
            cdr.setParameter3(getField(context, header, body, "parameter3"));
            cdr.setParameter4(getField(context, header, body, "parameter4"));
            cdr.setParameter5(getField(context, header, body, "parameter5"));
            cdr.setParameter6(getField(context, header, body, "parameter6"));
            cdr.setParameter7(getField(context, header, body, "parameter7"));
            cdr.setParameter8(getField(context, header, body, "parameter8"));
            cdr.setParameter9(getField(context, header, body, "parameter9"));
            cdr.setDateParam1(parseDate(context, header, body, "dateParam1", dateFormat, bw, line));
            cdr.setDateParam2(parseDate(context, header, body, "dateParam2", dateFormat, bw, line));
            cdr.setDateParam3(parseDate(context, header, body, "dateParam3", dateFormat, bw, line));
            cdr.setDateParam4(parseDate(context, header, body, "dateParam4", dateFormat, bw, line));
            cdr.setDateParam5(parseDate(context, header, body, "dateParam5", dateFormat, bw, line));
            cdr.setDecimalParam1(parseBigDecimal(context, header, body, "decimalParam1"));
            cdr.setDecimalParam2(parseBigDecimal(context, header, body, "decimalParam2"));
            cdr.setDecimalParam3(parseBigDecimal(context, header, body, "decimalParam3"));
            cdr.setDecimalParam4(parseBigDecimal(context, header, body, "decimalParam4"));
            cdr.setDecimalParam5(parseBigDecimal(context, header, body, "decimalParam5"));
            cdr.setExtraParameter(getField(context, header, body, "extraParam"));
        } catch (Exception e) {
            try {
                bw.write(line + " => " + e.getMessage() + " \n");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            reject = true;
        }
        return reject;
    }

    private void moveOrDeleteFile(File fileInput, File rejectFile) throws IOException {
        String toPath = getProviderRootDir() + File.separator + "imports/cdr/flatFile/archive" + File.separator + fileInput.getName();
        Files.createDirectories(Paths.get(toPath));
        Files.move(Paths.get(fileInput.getPath()), Paths.get(toPath), StandardCopyOption.REPLACE_EXISTING);
        if (rejectFile.length() == 0) {
            Files.delete(rejectFile.toPath());
        }
    }

    private Date parseDate(Map<String, String> context, String[] header, String[] body, String key, DateFormat dateFormat, BufferedWriter bw, String line) throws IOException {
        Date date = null;
        if (elementExisted(context, header, body, key)) {
            try {
                String dateStr = body[ArrayUtils.indexOf(header, context.get(key))];
                if (!StringUtils.isEmpty(dateStr)) {
                    date = dateFormat.parse(dateStr);
                }
            } catch (ParseException e) {
                bw.write(line + " => Incorrect format date for cdr " + context.get(key) + " \n");
            }
        }
        return date;
    }

    private String getField(Map<String, String> context, String[] header, String[] body, String key) {
        return elementExisted(context, header, body, key) && !body[ArrayUtils.indexOf(header, context.get(key))].isEmpty()
                ? body[ArrayUtils.indexOf(header, context.get(key))]
                : null;
    }

    private BigDecimal parseBigDecimal(Map<String, String> context, String[] header, String[] body, String key) {
        return elementExisted(context, header, body, key) && !body[ArrayUtils.indexOf(header, context.get(key))].isEmpty()
                ? new BigDecimal(body[ArrayUtils.indexOf(header, context.get(key))])
                : null;
    }

    private void validateCdr(String line, CDR cdr, Map<String, String> context, BufferedWriter file) throws IOException {
        if (cdr.getEventDate() == null)
            file.write(line + " => " + context.get("eventDate") + " is required\n");
        else if (cdr.getQuantity() == null)
            file.write(line + " => " + context.get("quantity") + " is required\n");
        else if (cdr.getAccessCode() == null)
            file.write(line + " => " + context.get("accessCode") + " is required\n");
        else if (cdr.getParameter1() == null)
            file.write(line + " => " + context.get("parameter1") + " is required\n");

    }

    public String getProviderRootDir() {
        return paramBeanFactory.getDefaultChrootDir();
    }

}