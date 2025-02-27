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

package org.meveo.api.admin;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.meveo.admin.storage.StorageFactory;
import org.meveo.admin.util.FlatFileValidator;
import org.meveo.admin.util.DirectoriesConstants;
import org.meveo.api.BaseApi;
import org.meveo.api.dto.admin.FileDto;
import org.meveo.api.dto.admin.FileRequestDto;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.InvalidParameterException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.commons.utils.FileUtils;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.bi.FlatFile;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipOutputStream;

import static java.lang.String.format;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

/**
 * @author Edward P. Legaspi
 * @author Wassim Drira
 * @author Youssef IZEM
 * @author Abdellatif BARI
 * @lastModifiedVersion 8.4.0
 */
@Stateless
public class FilesApi extends BaseApi {

    public static final String FILE_DOES_NOT_EXISTS = "File does not exists: ";
    
    public static final String FILE_INVALID_PATH = "Invalid path: ";

    public static final String SOURCE_FILE_OR_FOLDER_DOES_NOT_EXISTS = "Source file or folder does not exist: ";

    public static final String DESTINATION_FILE_OR_FOLDER_ALREADY_EXISTS = "Destination file or folder already exists: ";
    @Inject
    private FlatFileValidator flatFileValidator;

    public String getProviderRootDir() {
        return paramBeanFactory.getDefaultChrootDir();
    }
    
    @PostConstruct
    public void init() {
    	createMissingDirectories();
    }

    private void createMissingDirectories() {
        log.info("createMissingDirectories() * ");

        String importDir = getProviderRootDir() + File.separator + DirectoriesConstants.IMPORTS_ROOT_FOLDER + File.separator + "customers" + File.separator;
        String customerDirIN = importDir + DirectoriesConstants.INPUT_SUBFOLDER;
        String customerDirOUT = importDir + DirectoriesConstants.OUTPUT_SUBFOLDER;
        String customerDirERR = importDir + DirectoriesConstants.ERRORS_SUBFOLDER;
        String customerDirWARN = importDir + DirectoriesConstants.WARNINGS_SUBFOLDER;
        String customerDirKO = importDir + DirectoriesConstants.REJECT_SUBFOLDER;
        importDir = getProviderRootDir() + File.separator + DirectoriesConstants.IMPORTS_ROOT_FOLDER + File.separator + "accounts" + File.separator;
        String accountDirIN = importDir + DirectoriesConstants.INPUT_SUBFOLDER;
        String accountDirOUT = importDir + DirectoriesConstants.OUTPUT_SUBFOLDER;
        String accountDirERR = importDir + DirectoriesConstants.ERRORS_SUBFOLDER;
        String accountDirWARN = importDir + DirectoriesConstants.WARNINGS_SUBFOLDER;
        String accountDirKO = importDir + DirectoriesConstants.REJECT_SUBFOLDER;
        importDir = getProviderRootDir() + File.separator + DirectoriesConstants.IMPORTS_ROOT_FOLDER + File.separator + "subscriptions" + File.separator;
        String subDirIN = importDir + DirectoriesConstants.INPUT_SUBFOLDER;
        String subDirOUT = importDir + DirectoriesConstants.OUTPUT_SUBFOLDER;
        String subDirERR = importDir + DirectoriesConstants.ERRORS_SUBFOLDER;
        String subDirWARN = importDir + DirectoriesConstants.WARNINGS_SUBFOLDER;
        String subDirKO = importDir + DirectoriesConstants.REJECT_SUBFOLDER;
        importDir = getProviderRootDir() + File.separator + DirectoriesConstants.IMPORTS_ROOT_FOLDER + File.separator + "catalog" + File.separator;
        String catDirIN = importDir + DirectoriesConstants.INPUT_SUBFOLDER;
        String catDirOUT = importDir + DirectoriesConstants.OUTPUT_SUBFOLDER;
        String catDirKO = importDir + DirectoriesConstants.REJECT_SUBFOLDER;
        importDir = getProviderRootDir() + File.separator + DirectoriesConstants.IMPORTS_ROOT_FOLDER + File.separator + "metering" + File.separator;
        String meterDirIN = importDir + DirectoriesConstants.INPUT_SUBFOLDER;
        String meterDirOUT = importDir + DirectoriesConstants.OUTPUT_SUBFOLDER;
        String meterDirKO = importDir + DirectoriesConstants.REJECT_SUBFOLDER;
        String invoicePdfDir = getProviderRootDir() + File.separator + DirectoriesConstants.INVOICES_ROOT_FOLDER + File.separator + "pdf";
        String invoiceXmlDir = getProviderRootDir() + File.separator + DirectoriesConstants.INVOICES_ROOT_FOLDER + File.separator + "xml";
        String jasperDir = getProviderRootDir() + File.separator + DirectoriesConstants.JASPER_ROOT_FOLDER;
        String priceplanVersionsDir = getProviderRootDir() + File.separator + DirectoriesConstants.IMPORTS_ROOT_FOLDER + File.separator + "priceplan_versions";
        importDir = getProviderRootDir() + File.separator + DirectoriesConstants.IMPORTS_ROOT_FOLDER + File.separator + "cdr" + File.separator + "flatFile" + File.separator;
        String cdrFlatFileDirIn = importDir + DirectoriesConstants.INPUT_SUBFOLDER;
        String cdrFlatFileDirOut = importDir + DirectoriesConstants.OUTPUT_SUBFOLDER;
	    importDir = getProviderRootDir() + File.separator + DirectoriesConstants.IMPORTS_ROOT_FOLDER + File.separator + "writeOff" + File.separator;
	    String writeOffDirIN = importDir + DirectoriesConstants.INPUT_SUBFOLDER;
	    String writeOffDirOUT = importDir + DirectoriesConstants.OUTPUT_SUBFOLDER;
	    String writeOffDirKO = importDir + DirectoriesConstants.REJECT_SUBFOLDER;
	    String writeOffDirArchiv = importDir + DirectoriesConstants.INVOICE_WRITEOF_ARCHIVE_ROOT_FOLDER;
	    
	    importDir = getProviderRootDir() + File.separator + DirectoriesConstants.IMPORTS_ROOT_FOLDER + File.separator + "discount" + File.separator;
	    String discountDirIN = importDir + DirectoriesConstants.INPUT_SUBFOLDER;
	    String discountDirOUT = importDir + DirectoriesConstants.OUTPUT_SUBFOLDER;
	    String discountDirKO = importDir + DirectoriesConstants.REJECT_SUBFOLDER;
	    String discountDirArchiv = importDir + DirectoriesConstants.INVOICE_WRITEOF_ARCHIVE_ROOT_FOLDER;
        
        List<String> filePaths = Arrays.asList("", customerDirIN, customerDirOUT, customerDirERR, customerDirWARN, customerDirKO, accountDirIN, accountDirOUT, accountDirERR, accountDirWARN, accountDirKO, subDirIN,
            subDirOUT, subDirERR, subDirWARN, catDirIN, catDirOUT, catDirKO, subDirKO, meterDirIN, meterDirOUT, meterDirKO, invoicePdfDir, invoiceXmlDir, jasperDir, priceplanVersionsDir, cdrFlatFileDirIn, cdrFlatFileDirOut
        , writeOffDirIN, writeOffDirOUT, writeOffDirKO, writeOffDirArchiv, discountDirIN, discountDirOUT, discountDirKO, discountDirArchiv);
        for (String custDirs : filePaths) {
            FileUtils.createDirectory(custDirs);
        }
    }

    public List<FileDto> listFiles(String dir) throws BusinessApiException {
        if (!StringUtils.isBlank(dir)) {
            dir = getProviderRootDir() + File.separator + normalizePath(dir);
        } else {
            dir = getProviderRootDir();
        }

        File folder = new File(dir);

        if (FileUtils.isFile(folder)) {
            throw new BusinessApiException("Path " + dir + " is a file.");
        }

        List<FileDto> result = new ArrayList<FileDto>();

        if (!FileUtils.isS3Activated()) {
            File[] files = FileUtils.listFiles(folder);
            if ( files != null && Objects.requireNonNull(files).length > 0) {
                files = Objects.requireNonNull(files);
                for (File file : files) {
                    result.add(new FileDto(file));
                }
            }
        }
        else {
            if (Objects.requireNonNull(FileUtils.listSubFoldersAndFiles(folder)).size() > 0) {
                Map<String, Date> map = Objects.requireNonNull(FileUtils.listSubFoldersAndFiles(folder));
                for (Map.Entry<String, Date> entry : map.entrySet()) {
                    result.add(new FileDto(entry.getKey(), entry.getValue()));
                }
            }
        }

        return result;
    }

    /**
     * Remove any directory above the provider directory root
     * @param dir
     * @return
     */
    private String normalizePath(String dir) {
        if (dir == null) {
            throw new BusinessApiException("Invalid parameter, file or directory is null");
        }

        String providerRootDir = getProviderRootDir();

        File dirFile = new File(providerRootDir + File.separator + dir);
        Path path = dirFile.toPath();
        path = path.normalize();
        String prefix = new File(providerRootDir.replace("./", "")).toPath().normalize().toString();
        if (!path.toString().contains(prefix)) {

            log.error("File requested {} and resolved to {} is not within provider's root directory {}", dir, path, prefix);
            throw new EntityDoesNotExistsException(FILE_INVALID_PATH + dir);
        }
        return dir;
    }

    public void createDir(String dir) throws BusinessApiException {
        FileUtils.createDirectory(getProviderRootDir() + File.separator + normalizePath(dir));
    }

    public void zipFile(String filePath) throws BusinessApiException {
        File file = new File(getProviderRootDir() + File.separator + normalizePath(filePath));
        if (!FileUtils.existsFile(file)) {
            throw new BusinessApiException(FILE_DOES_NOT_EXISTS + file.getPath());
        }

        try {
            FileUtils.archiveFile(file);
        } catch (IOException e) {
            throw new BusinessApiException("Error zipping file: " + file.getName() + ". " + e.getMessage());
        }
    }

    /**
     * @param dir directory to be zipped.
     * @throws BusinessApiException business exception.
     */
    public void zipDir(String dir) throws BusinessApiException {
        String normalizedDir = (isLocalDir(dir) ? "" : normalizePath(dir));
        File file = new File(getProviderRootDir() + File.separator + normalizedDir);
        if (!FileUtils.existsDirectory(file)) {
            throw new BusinessApiException("Directory does not exists: " + file.getPath());
        }
        File zipFile = new File(FilenameUtils.removeExtension(file.getParent() + File.separator + file.getName()) + ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(Objects.requireNonNull(FileUtils.getOutputStream(zipFile)))) {
            FileUtils.addDirToArchive(getProviderRootDir(), file.getPath(), zos);
            zos.flush();
            if (isLocalDir(dir))
                Files.move(zipFile.toPath(), Paths.get(getProviderRootDir() + File.separator + zipFile.getName()), ATOMIC_MOVE);
        } catch (IOException e) {
            throw new BusinessApiException("Error zipping directory: " + file.getName() + ". " + e.getMessage());
        }
    }

    private boolean isLocalDir(String dir) {
        return dir.equals("./") || dir.equals("/.") || dir.equals(".");
    }

    /**
     * @param data       array of bytes as data uploaded
     * @param filename   file name
     * @param fileFormat file format
     * @return The created flat file record
     * @throws BusinessApiException business api exeption.
     */
    public FlatFile uploadFile(byte[] data, String filename, String fileFormat) throws BusinessApiException {
        File file = new File(getProviderRootDir() + File.separator + normalizePath(filename));
        try (OutputStream fop = FileUtils.getOutputStream(file)){
            // FileUtils.create(file);

            assert fop != null;
            fop.write(data);
            fop.close();
            
            if (FilenameUtils.getExtension(file.getName()).equals("zip")) {
                // unzip
                // get parent dir
                String parentDir = file.getParent();
                FileUtils.unzipFile(parentDir, FileUtils.getInputStream(file));
            }

            if (!StringUtils.isBlank(fileFormat)) {
                return flatFileValidator.validateProcessFile(file, filename, fileFormat);
            }
            return null;

        } catch (Exception e) {
            throw new BusinessApiException("Error uploading file: " + filename + ". " + e.getMessage());
        }
    }

    /**
     * Allows to upload a base64 file
     *
     * @param postData contains filename and the base64 data to upload
     * @throws MeveoApiException
     */
    public void uploadFileBase64(FileRequestDto postData) throws MeveoApiException {
        if (postData == null) {
            throw new InvalidParameterException("Body request is empty");
        }
        if (StringUtils.isBlank(postData.getFilepath())) {
            missingParameters.add("filepath");
        }
        if (StringUtils.isBlank(postData.getContent())) {
            missingParameters.add("content");
        }

        handleMissingParametersAndValidate(postData);


        String filepath = getProviderRootDir() + File.separator + normalizePath(postData.getFilepath());
        File file = new File(filepath);
        OutputStream fop = null;
        try {

            File parent = file.getParentFile();
            if (parent == null) {
                throw new BusinessApiException("Invalid path : " + filepath);
            }
            FileUtils.mkdirs(parent);
            FileUtils.createNewFile(file);
            fop = FileUtils.getOutputStream(file);
            assert fop != null;
            fop.write(Base64.decodeBase64(postData.getContent()));
            fop.flush();

        } catch (Exception e) {
            throw new BusinessApiException("Error uploading file: " + postData.getFilepath() + ". " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(fop);
        }
    }

    /**
     * Allows to unzip a file
     *
     * @param filePath
     * @param deleteOnError
     * @throws MeveoApiException
     */
    public void unzipFile(String filePath, boolean deleteOnError) throws MeveoApiException {
        if (filePath == null || StringUtils.isBlank(filePath)) {
            throw new BusinessApiException("filePath is required ! ");
        }

        File file = new File(getProviderRootDir() + File.separator + normalizePath(filePath));
        if (!FileUtils.isValidZip(file)) {
            suppressFile(filePath);
            throw new BusinessApiException("The zipped file is invalid ! ");
        }

        try(InputStream inputStream = FileUtils.getInputStream(file)) {
            String parentDir = file.getParent();
            FileUtils.unzipFile(parentDir, inputStream);
        } catch (Exception e) {
            if (deleteOnError) {
                suppressFile(filePath);
            }
            throw new BusinessApiException("Error unziping file: " + filePath + ". " + e.getMessage());
        }
    }

    public void suppressFile(String filePath) throws BusinessApiException {
        String filename = getProviderRootDir() + File.separator + normalizePath(filePath);
        File file = new File(filename);

        if (FileUtils.existsFile(file)) {
            try {
                FileUtils.delete(file);
            } catch (Exception e) {
                throw new BusinessApiException("Error suppressing file: " + filename + ". " + e.getMessage());
            }
        } else {
            throw new BusinessApiException(FILE_DOES_NOT_EXISTS + filename);
        }
    }

    public void suppressDir(String dir) throws BusinessApiException {
        String filename = getProviderRootDir() + File.separator + normalizePath(dir);
        File file = new File(filename);

        if (FileUtils.existsDirectory(file)) {
            try {
                FileUtils.deleteDirectory(file);
            } catch (Exception e) {
                throw new BusinessApiException("Error suppressing file: " + filename + ". " + e.getMessage());
            }
        } else {
            throw new BusinessApiException("Directory does not exists: " + filename);
        }
    }

    /**
     * @param filePath file's path
     * @param response http servlet response.
     * @throws BusinessApiException business exception.
     */
    public void downloadFile(String filePath, HttpServletResponse response) throws BusinessApiException {
        
        if(StringUtils.isBlank(filePath)) {
            missingParameters.add("file");
        }
        
        handleMissingParameters();
        
        File file = checkAndGetExistingFile(filePath);

        try (InputStream fis = FileUtils.getInputStream(file)) {
            response.setContentType(Files.probeContentType(file.toPath()));
            response.setContentLength((int) FileUtils.length(file));
            response.addHeader("Content-disposition", "attachment;filename=\"" + file.getName() + "\"");
            IOUtils.copy(fis, response.getOutputStream());
            response.flushBuffer();
        } catch (IOException e) {
            throw new BusinessApiException("Error zipping file: " + file.getName() + ". " + e.getMessage());
        }
    }

    /**
     * Check existing file with two format (Java format and also SQL format) depends on the type of ReportExtract
     *
     * @param filePath File Path
     * @return The existing File {@link File}
     */
    public File checkAndGetExistingFile(String filePath) {

        String providerRootDir = getProviderRootDir();

        File javaXMlFormatFile = (filePath.contains(providerRootDir.replace("\\", "/"))) ? new File(filePath) : new File(providerRootDir + File.separator + normalizePath(filePath));

        if (FileUtils.existsFile(javaXMlFormatFile)) {
            return javaXMlFormatFile;
        } else {
            String[] fileNameParts = filePath.split("\\.");

            if (fileNameParts.length > 2) {
                File sqlXMlFormatFile = new File((".").concat(filePath.split("\\.")[1] + "_" + format("%04d", 0) + "." + filePath.split("\\.")[2]));
                if (FileUtils.existsFile(sqlXMlFormatFile)) {
                    return sqlXMlFormatFile;

                } else {
                    log.error("File requested {} and resolved to sql xml format file {} does not exist", filePath, sqlXMlFormatFile.getPath());
                    throw new BusinessApiException(FILE_DOES_NOT_EXISTS + javaXMlFormatFile.getPath());
                }
            } else {
                log.error("File requested {} and resolved to {} does not exist", filePath, javaXMlFormatFile.getPath());
                throw new BusinessApiException(FILE_DOES_NOT_EXISTS + javaXMlFormatFile.getPath());
            }
        }
    }

    public void moveFileOrDirectory(String srcPath, String destPath) throws BusinessApiException {
        String srcName = getProviderRootDir() + File.separator + normalizePath(srcPath);
        File source = new File(srcName);

        String[] arrFileName = srcName.split("/");
        String nameFileOrDir = arrFileName[arrFileName.length - 1];
        String destName = getProviderRootDir() + File.separator + normalizePath(destPath) + File.separator + nameFileOrDir;
        File dest = new File(destName);

        if (FileUtils.existsFile(source) || FileUtils.existsDirectory(source)) {
            if (!FileUtils.isDirectory(source)) {
                if (!FileUtils.existsFile(dest)) {
                    if (!FileUtils.existsDirectory(dest.getParentFile())) {
                        FileUtils.mkdirs(dest.getParentFile());
                    }

                    try {
                        FileUtils.moveFile(srcName, destName, StandardCopyOption.ATOMIC_MOVE);
                    } catch (Exception e) {
                        throw new BusinessApiException("Error while moving file with source path: " + srcName
                                + " and destination path: " + destName + e.getMessage());
                    }
                }
                else {
                    throw new BusinessApiException(DESTINATION_FILE_OR_FOLDER_ALREADY_EXISTS + destName);
                }
            } else {
                if (!FileUtils.existsDirectory(dest)) {
                    FileUtils.createDirectory(dest.getParentFile());
                    try {
                        FileUtils.moveFile(srcName, destName, StandardCopyOption.ATOMIC_MOVE);
                    } catch (Exception e) {
                        throw new BusinessApiException("Error while moving directory with source path: " + srcName
                                + " and destination path: " + destName + e.getMessage());
                    }
                }
                else {
                    throw new BusinessApiException(DESTINATION_FILE_OR_FOLDER_ALREADY_EXISTS + destName);
                }
            }
        }
        else {
            throw new BusinessApiException(SOURCE_FILE_OR_FOLDER_DOES_NOT_EXISTS + srcName);
        }
    }
}
