package org.meveo.admin.storage;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRXmlDataSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.carlspring.cloud.storage.s3fs.S3FileSystem;
import org.carlspring.cloud.storage.s3fs.S3FileSystemProvider;
import org.meveo.commons.keystore.KeystoreManager;
import org.meveo.commons.utils.ImportFileFiltre;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.commons.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import javax.xml.parsers.DocumentBuilder;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * StorageFactory
 *
 * @author Thang Nguyen
 * @author Wassim Drira
 * @author Andrius Karpavicius
 * @author Abdellatif BARI
 * @lastModifiedVersion 14.3.17
 */

public class StorageFactory {

    public static final int DEFAULT_BUFFER_SIZE = 10240; // ..bytes = 10KB.
    private static String storageType;
    private static String bucketName;

    private static S3FileSystem s3FileSystem;

    private static final String NFS = "FileSystem";
    private static final String S3 = "S3";


    /**
     * Logger.
     */
    protected static Logger log = LoggerFactory.getLogger(StorageFactory.class);

    /**
     * init StorageFactory.
     * <p>
     * required configuration parameters : endpointUrl, region, accessKeyId, secretAccessKey, bucketName
     */
    public void init() {
        ParamBean tmpParamBean = ParamBeanFactory.getAppScopeInstance();
        storageType = tmpParamBean.getProperty("storage.type", NFS);

        if (S3.equalsIgnoreCase(storageType)) {
            String endpointUrl = tmpParamBean.getProperty("S3.endpointUrl", "endPointUrl");
            String region = tmpParamBean.getProperty("S3.region", "region");
            bucketName = tmpParamBean.getProperty("S3.bucketName", "bucketName");

            String accessKeyId;
            String secretAccessKey;
            boolean credInKeystore = tmpParamBean.getPropertyAsBoolean("S3.credential.in.keystore", false);
            if (credInKeystore) {
                // get accessKeyId and secretAccessKey from the Keystore
                accessKeyId = KeystoreManager.retrieveCredential("S3.accessKeyId");
                secretAccessKey = KeystoreManager.retrieveCredential("S3.secretAccessKey");
            } else {
                // get accessKeyId and secretAccessKey from System Settings
                accessKeyId = tmpParamBean.getProperty("S3.accessKeyId", "accessKeyId");
                secretAccessKey = tmpParamBean.getProperty("S3.secretAccessKey", "secretAccessKey");
            }

            S3Configuration serviceConfiguration = S3Configuration.builder()
                    .checksumValidationEnabled(false)
                    .chunkedEncodingEnabled(false)
                    .build();

            S3Client client =
                    S3Client.builder().forcePathStyle(true).region(Region.of(region))
                            .endpointOverride(URI.create(endpointUrl))
                            .credentialsProvider(
                                    StaticCredentialsProvider.create(
                                            AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                            .serviceConfiguration(serviceConfiguration)
                            .build();

            boolean validParameters = false;

            try {
                log.debug("check configuration parameters of S3 bucket");

                client.headBucket(HeadBucketRequest.builder()
                        .bucket(bucketName)
                        .build());

                validParameters = true;
            } catch (NoSuchBucketException e) {
                log.error("NoSuchBucketException exception message : {}", e.getMessage());
            } catch (S3Exception e) {
                log.error("S3Exception exception message : {}", e.getMessage());
                log.error("Failed to connect to S3 repository. Check all your S3 configuration parameters : region {}, " +
                        " endpointUrl {}, accessKeyId, and secretAccessKey", region, endpointUrl);
            }

            if (validParameters) {
                log.debug("S3 parameters are correctly configured");
            } else {
                log.error("S3 parameters are not correctly configured");
                throw S3Exception.builder().build();
            }

            s3FileSystem = new S3FileSystem(new S3FileSystemProvider(), accessKeyId, client, endpointUrl);
        }
    }

    /**
     * get S3Client instance
     *
     * @return S3Client
     */
    public static S3Client getS3Client() {
        return s3FileSystem.getClient();
    }

    /**
     * get bucket name
     *
     * @return bucketName bucket name
     */
    public static String getBucketName() {
        return bucketName;
    }

    /**
     * get path of object in S3
     *
     * @param objectPath String
     * @return Path object
     */
    public static Path getObjectPath(String objectPath) {
        return s3FileSystem.getPath("/" + objectPath);
    }

    /**
     * get inputStream based on a filename S3
     *
     * @param fileName String
     * @return InputStream
     * @throws FileNotFoundException If file not found.
     */
    public static InputStream getInputStream(String fileName) throws FileNotFoundException {
        if (NFS.equals(storageType)) {
            try {
                return new FileInputStream(fileName);
            } catch (FileNotFoundException e) {
                log.error("file not found : {} ", e.getMessage());
            }
        } else if (S3.equalsIgnoreCase(storageType)) {
            InputStream inStream;

            Path objectPath = getObjectPath(fileName);

            try {
                inStream = s3FileSystem.provider().newInputStream(objectPath);

                return inStream;
            } catch (IOException e) {
                log.error("IOException message in getInputStream(String) : {}", e.getMessage());
                throw new FileNotFoundException(e.getMessage());
            }
        }

        return null;
    }

    /**
     * get inputStream based on array of bytes
     *
     * @param bytes array of bytes
     * @return InputStream
     */
    public static InputStream getInputStream(byte[] bytes) {
        if (NFS.equals(storageType)) {
            return new ByteArrayInputStream(bytes);
        } else if (S3.equalsIgnoreCase(storageType)) {
            Path objectPath = getObjectPath("");

            try (InputStream inStream = s3FileSystem.provider().newInputStream(objectPath)) {
                inStream.read(bytes);

                return inStream;
            } catch (IOException e) {
                log.error("IOException message in getInputStream(byte) : {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * get reader based on file, to read data from a file
     *
     * @param file a file
     * @return Reader
     */
    public static Reader getReader(File file) {
        if (NFS.equals(storageType)) {
            try {
                return new FileReader(file);
            } catch (FileNotFoundException e) {
                log.error("File not found exception in getReader: {}", e.getMessage());
            }
        } else if (S3.equalsIgnoreCase(storageType)) {
            InputStream inStream;
            InputStreamReader inputStreamReader;
            String fileName = formatObjectKey(bucketName + File.separator + file.getPath());

            Path objectPath = getObjectPath(fileName);

            try {
                inStream = s3FileSystem.provider().newInputStream(objectPath);

                inputStreamReader = new InputStreamReader(inStream, StandardCharsets.US_ASCII);

                return inputStreamReader;
            } catch (IOException e) {
                log.error("IOException message in getReader : {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * get buffer reader to read data from a file
     *
     * @param file a file
     * @return BufferReader
     */
    public static Reader getBufferedReader(File file) {
        if (NFS.equals(storageType)) {
            try {
                return new BufferedReader(new FileReader(file));
            } catch (FileNotFoundException e) {
                log.error("File not found exception in getReader: {}", e.getMessage());
            }
        } else if (S3.equalsIgnoreCase(storageType)) {
            InputStream inStream;
            String fileName = formatObjectKey(bucketName + File.separator + file.getPath());

            Path objectPath = getObjectPath(fileName);

            try {
                inStream = s3FileSystem.provider().newInputStream(objectPath);

                return new BufferedReader(new InputStreamReader(inStream));
            } catch (IOException e) {
                log.error("IOException message in getReader : {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * get reader based on file, to read data from a file
     *
     * @param file String filename of the file
     * @return Reader
     */
    public static Reader getReader(String file) {
        if (NFS.equals(storageType)) {
            try {
                return new FileReader(file);
            } catch (FileNotFoundException e) {
                log.error("File not found exception : {}", e.getMessage());
            }
        } else if (S3.equalsIgnoreCase(storageType)) {
            InputStream inStream;
            InputStreamReader inputStreamReader;
            String fileName = bucketName + file.substring(1).replace("\\", "/");

            Path objectPath = getObjectPath(fileName);

            try {
                inStream = s3FileSystem.provider().newInputStream(objectPath);

                inputStreamReader = new InputStreamReader(inStream, StandardCharsets.US_ASCII);

                return inputStreamReader;
            } catch (IOException e) {
                log.error("IOException message in getReader : {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * get writer based on file, used to write character-oriented data to a file.
     *
     * @param file String filename of the file
     * @return Writer
     */
    public static Writer getWriter(String file) {
        if (NFS.equals(storageType)) {
            try {
                return new FileWriter(file);
            } catch (IOException e) {
                log.error("IO exception : {}", e.getMessage());
            }
        } else if (S3.equalsIgnoreCase(storageType)) {
            OutputStream outStream;
            OutputStreamWriter outputStreamWriter;
            String fileName = bucketName + file.substring(1).replace("\\", "/");

            Path objectPath = getObjectPath(fileName);

            try {
                outStream = s3FileSystem.provider().newOutputStream(objectPath);

                outputStreamWriter = new OutputStreamWriter(outStream, StandardCharsets.US_ASCII);

                return outputStreamWriter;
            } catch (IOException e) {
                log.error("IOException message in getWriter : {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * get writer based on file, used to write character-oriented data to a file.
     *
     * @param file the file
     * @return Writer
     */
    public static Writer getWriter(File file) {
        if (NFS.equals(storageType)) {
            try {
                return new FileWriter(file);
            } catch (IOException e) {
                log.error("File not found exception : {}", e.getMessage());
            }
        } else if (S3.equalsIgnoreCase(storageType)) {
            OutputStream outStream;
            OutputStreamWriter outputStreamWriter;
            String fileName = bucketName + file.getPath().substring(1).replace("\\", "/");

            Path objectPath = getObjectPath(fileName);

            try {
                outStream = s3FileSystem.provider().newOutputStream(objectPath);

                outputStreamWriter = new OutputStreamWriter(outStream, StandardCharsets.US_ASCII);

                return outputStreamWriter;
            } catch (IOException e) {
                log.error("IOException message in getWriter : {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * check existence of a file or directory on File System or S3.
     *
     * @param pathname    a pathname of file or directory.
     * @param isDirectory true if it's the directory.
     * @return true if file exists, false otherwise
     */
    private static boolean exists(String pathname, boolean isDirectory) {
        if (NFS.equals(storageType)) {
            return new File(pathname).exists();
        } else if (S3.equalsIgnoreCase(storageType)) {
            String objectKey = formatObjectKey(pathname, isDirectory);
            log.debug("check existence of an object based in fileName on S3 at key {}", objectKey);
            if (!StringUtils.isBlank(objectKey)) {
                try {
                    s3FileSystem.getClient()
                            .headObject(HeadObjectRequest.builder().bucket(bucketName).key(objectKey).build());
                    return true;
                } catch (NoSuchKeyException e) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * check existence of a file or directory on File System or S3.
     *
     * @param file        the file
     * @param isDirectory true if it's the directory.
     * @return true if file exists, false otherwise
     */
    private static boolean exists(File file, boolean isDirectory) {
        if (NFS.equals(storageType)) {
            return file.exists();
        } else if (S3.equalsIgnoreCase(storageType)) {
            String objectKey = formatObjectKey(file.getPath(), isDirectory);
            log.debug("check existence of an object based on file on S3 at key {}", objectKey);
            if (!StringUtils.isBlank(objectKey)) {
                try {
                    s3FileSystem.getClient()
                            .headObject(HeadObjectRequest.builder().bucket(bucketName).key(objectKey).build());
                    return true;
                } catch (NoSuchKeyException e) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * check existence of a directory on File System or S3.
     *
     * @param pathname a pathname of directory.
     * @return true if directory exists, false otherwise
     */
    public static boolean existsDirectory(String pathname) {
        return exists(pathname, true);
    }

    /**
     * check existence of a directory on File System or S3.
     *
     * @param directory the directory
     * @return true if directory exists, false otherwise
     */
    public static boolean existsDirectory(File directory) {
        return exists(directory.getPath(), true);
    }

    /**
     * check existence of a file on File System or S3.
     *
     * @param pathname a pathname of directory.
     * @return true if file exists, false otherwise
     */
    public static boolean existsFile(String pathname) {
        return exists(pathname, false);
    }

    /**
     * check existence of a file on File System or S3.
     *
     * @param file the file
     * @return true if file exists, false otherwise
     */
    public static boolean existsFile(File file) {
        return exists(file.getPath(), false);
    }

    /**
     * create a new directory on File System or S3.
     *
     * @param directory the directory
     */
    public static void mkdirs(File directory) {
        if (NFS.equals(storageType)) {
            directory.mkdirs();
        } else if (S3.equalsIgnoreCase(storageType)) {
            String objectKey = formatObjectKey(directory.getPath(), true);
            log.debug("create a directory in S3 at key {}", objectKey);
            while (!StringUtils.isBlank(objectKey)) {
                if (!existsDirectory(new File(objectKey))) {
                    putObject(objectKey, RequestBody.empty());
                } else {
                    break;
                }
                String parentPath = new File(objectKey).getParent();
                if (parentPath != null) {
                    objectKey = formatObjectKey(parentPath, true);
                } else {
                    objectKey = null;
                }
            }
        }
    }

    /**
     * create a directory.
     *
     * @param file the file
     */
    public static void createDirectory(File file) {
        if (NFS.equals(storageType)) {
            file.mkdirs();
        } else if (S3.equalsIgnoreCase(storageType)) {
            String fullObjectKey = formatFullObjectKey(file.toString(), true);
            Path objectPath = getObjectPath(fullObjectKey);
            try {
                s3FileSystem.provider().createDirectory(objectPath);
            } catch (IOException e) {
                log.error("IOException message : {}", e.getMessage());
            }
        }

    }

    /**
     * delete a directory on File System or S3.
     *
     * @param srcDir the directory
     * @throws IOException If something fails at I/O level.
     */
    public static void deleteDirectory(File srcDir) throws IOException {
        if (NFS.equals(storageType)) {
            try {
                FileUtils.deleteDirectory(srcDir);
            } catch (IOException e) {
                log.error("IOException message {}", e.getMessage());
            }
        } else if (S3.equalsIgnoreCase(storageType)) {
            Set<String> setKeys = listAllSubFoldersAndFiles(srcDir);

            // remove all sub-folders and files objects
            for (String key : setKeys) {
                log.debug("object on S3 to remove at the key {}", key);
                deleteObject(key, true);
            }
        }
    }

    /**
     * create a new empty file on File System or S3.
     *
     * @param file the file
     */
    public static void createNewFile(File file) {
        if (NFS.equals(storageType)) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                log.error("IO Exception : {}", e.getMessage());
            }
        } else if (S3.equalsIgnoreCase(storageType)) {
            OutputStream outStream;
            String fileName = bucketName + file.getPath().substring(1).replace("\\", "/");

            Path bucketPath = getObjectPath(fileName);

            try {
                outStream = s3FileSystem.provider().newOutputStream(bucketPath);

                outStream.close();
            } catch (IOException e) {
                log.error("IOException message in createNewFile : {}", e.getMessage());
            }
        }
    }

    /**
     * get PrintWriter of a file on File System or S3.
     *
     * @param file the file
     * @return PrintWriter object
     */
    public static PrintWriter getPrintWriter(File file) {
        if (NFS.equals(storageType)) {
            try {
                return new PrintWriter(file);
            } catch (FileNotFoundException e) {
                log.error("file not found in getPrintWriter : {}", e.getMessage());
            }
        } else if (S3.equalsIgnoreCase(storageType)) {
            PrintWriter printWriter;
            String fileName = bucketName + file.getPath().substring(1).replace("\\", "/");

            Path objectPath = getObjectPath(fileName);

            try {
                printWriter = new PrintWriter(
                        new OutputStreamWriter(s3FileSystem.provider().newOutputStream(objectPath), StandardCharsets.US_ASCII));

                return printWriter;
            } catch (IOException e) {
                log.error("IOException message in getPrintWriter : {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * get InputStream of a file.
     *
     * @param file the file
     * @return InputStream object
     */
    public static InputStream getInputStream(File file) {
        if (NFS.equals(storageType)) {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                log.error("file not found in getInputStream : {}", e.getMessage());
            }
        } else if (S3.equalsIgnoreCase(storageType)) {
            String objectKey = formatObjectKey(file.getPath());
            log.info("objectKey in getInputStream {}", objectKey);
            if (!StringUtils.isBlank(objectKey)) {
                GetObjectRequest request = GetObjectRequest.builder().bucket(bucketName).key(objectKey).build();
                return s3FileSystem.getClient().getObject(request);
            }
        }
        return null;
    }

    /**
     * get OutputStream of a file.
     *
     * @param file   the file
     * @param append – if true, then bytes will be written to the end of the file rather than the beginning
     * @return OutputStream object
     */
    public static OutputStream getOutputStream(File file, boolean append) {
        if (NFS.equals(storageType)) {
            try {
                return new FileOutputStream(file, append);
            } catch (FileNotFoundException e) {
                log.error("file not found : {}", e.getMessage());
            }
        } else if (S3.equalsIgnoreCase(storageType)) {
            OutputStream outStream;
            String fullObjectKey = formatFullObjectKey(file.getPath());

            Path objectPath = getObjectPath(fullObjectKey);

            try {
                if (append) {
                    outStream = s3FileSystem.provider().newOutputStream(objectPath, StandardOpenOption.APPEND);
                } else {
                    outStream = s3FileSystem.provider().newOutputStream(objectPath);
                }
                return outStream;
            } catch (IOException e) {
                log.error("IOException message in getOutputStream : {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * get OutputStream of a file.
     *
     * @param fileName the filename
     * @param append   – if true, then bytes will be written to the end of the file rather than the beginning
     * @return OutputStream object
     */
    public static OutputStream getOutputStream(String fileName, boolean append) {
        if (NFS.equals(storageType)) {
            try {
                return new FileOutputStream(fileName, append);
            } catch (FileNotFoundException e) {
                log.error("file not found : {}", e.getMessage());
            }
        } else if (S3.equalsIgnoreCase(storageType)) {
            OutputStream outStream;
            String fullObjectKey = formatFullObjectKey(fileName);

            Path bucketPath = getObjectPath(fullObjectKey);

            try {
                if (append) {
                    outStream = s3FileSystem.provider().newOutputStream(bucketPath, StandardOpenOption.APPEND);
                } else {
                    outStream = s3FileSystem.provider().newOutputStream(bucketPath);
                }
                return outStream;
            } catch (IOException e) {
                log.error("IOException message in getOutputStream : {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * Writes bytes to a file.
     *
     * @param path    the path to the file
     * @param bytes   the byte array with the bytes to write
     * @param options options specifying how the file is opened
     */
    public static void write(Path path, byte[] bytes, OpenOption... options) {
        if (NFS.equals(storageType)) {
            try {
                Files.write(path, bytes, options);
            } catch (IOException e) {
                log.error("IOException exception : {}", e.getMessage());
            }
        } else if (S3.equalsIgnoreCase(storageType)) {
            OutputStream outStream;
            String fullObjectKey = formatFullObjectKey(path.toString());

            Path bucketPath = getObjectPath(fullObjectKey);

            try {
                outStream = Files.newOutputStream(bucketPath);

                outStream.write(bytes);

                outStream.close();
            } catch (IOException e) {
                log.error("IOException message in write : {}", e.getMessage());
            }

        }
    }

    /**
     * Parse the content of the given file as an XML document
     * and return a new DOM {@link Document} object.
     *
     * @param file The file containing the XML to parse.
     * @return Document object
     */
    public static Document parse(DocumentBuilder db, File file) {
        if (NFS.equals(storageType)) {
            try {
                return db.parse(file);
            } catch (SAXException | IOException e) {
                log.error("IOException or SAXException message in parse : {}", e.getMessage());
            }
        } else if (S3.equalsIgnoreCase(storageType)) {
            InputStream inStream;
            String fullObjectKey = formatFullObjectKey(file.getPath());

            Path objectPath = getObjectPath(fullObjectKey);

            try {
                inStream = s3FileSystem.provider().newInputStream(objectPath);

                return db.parse(inStream);
            } catch (IOException | SAXException e) {
                log.error("IOException or SAXException message in parse : {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * Marshal to XML File
     *
     * @param marshaller The Marshaller object.
     * @param obj        the object to be marshalled
     * @param file       the file to it the object will be marshalled
     */
    public static void marshal(Marshaller marshaller, Object obj, File file) {
        if (NFS.equals(storageType)) {
            try {
                marshaller.marshal(obj, file);
            } catch (JAXBException e) {
                log.error("marshaller exception : {}", e.getMessage());
            }
        } else if (S3.equalsIgnoreCase(storageType)) {
            OutputStream outStream;
            String fullObjectKey = formatFullObjectKey(file.getPath());

            Path bucketPath = getObjectPath(fullObjectKey);

            try {
                outStream = s3FileSystem.provider().newOutputStream(bucketPath);

                marshaller.marshal(obj, outStream);

                outStream.close();
            } catch (JAXBException | IOException e) {
                log.error("IO Exception or JAXBException in marshal method : {}", e.getMessage());
            }
        }
    }

    /**
     * format object key with bucketName
     * and return a string of object key.
     *
     * @param filePath The file containing the XML to parse.
     * @return String
     */
    public static String formatFullObjectKey(String filePath) {
        return formatFullObjectKey(filePath, false);
    }

    /**
     * format object key with bucketName
     * and return a string of object key.
     *
     * @param filePath    The file containing the XML to parse.
     * @param isDirectory true if it's the directory.
     * @return String
     */
    public static String formatFullObjectKey(String filePath, boolean isDirectory) {
        String fullObjectKey = bucketName;
        if (!StringUtils.isBlank(filePath)) {
            if (filePath.charAt(0) == '.') {
                fullObjectKey += filePath.substring(1).replace("\\", "/");
            } else {
                if (filePath.charAt(1) == '\\') {
                    fullObjectKey += filePath.replace("\\", "/");
                } else {
                    fullObjectKey += '/' + filePath.replace("\\", "/");
                }
            }
            fullObjectKey = fullObjectKey.replace("//", "/");
            if (isDirectory && !fullObjectKey.endsWith("/")) {
                fullObjectKey = fullObjectKey + "/";
            } else if (!isDirectory && fullObjectKey.endsWith("/")) {
                fullObjectKey = fullObjectKey.substring(0, fullObjectKey.length() - 1);
            }
        }
        return fullObjectKey;
    }

    /**
     * format object key without bucketName
     * and return a string of object key.
     *
     * @param filePath The file containing the XML to parse.
     * @return String
     */
    public static String formatObjectKey(String filePath) {
        return formatObjectKey(filePath, false);
    }

    /**
     * format object key without bucketName
     * and return a string of object key.
     *
     * @param filePath    The file containing the XML to parse.
     * @param isDirectory true if it's the directory.
     * @return String
     */
    public static String formatObjectKey(String filePath, boolean isDirectory) {
        String objectKey = "";
        if (!StringUtils.isBlank(filePath)) {
            if (filePath.charAt(0) == '.' && filePath.charAt(1) == '/') {
                objectKey += filePath.substring(2).replace("\\", "/");
            } else if (filePath.charAt(0) == '.' && filePath.charAt(1) == '\\') {
                objectKey += filePath.substring(2).replace("\\", "/");
            } else {
                objectKey += filePath.replace("\\", "/");
            }
            objectKey = objectKey.replace("//", "/");
            if (isDirectory && !objectKey.endsWith("/")) {
                objectKey = objectKey + "/";
            } else if (!isDirectory && objectKey.endsWith("/")) {
                objectKey = objectKey.substring(0, objectKey.length() - 1);
            }
        }
        return objectKey;
    }

    /**
     * export report to a pdf file
     *
     * @param jasperPrint the JasperPrint object.
     * @param fileName    String
     */
    public static void exportReportToPdfFile(JasperPrint jasperPrint, String fileName) {
        if (NFS.equals(storageType)) {
            try {
                JasperExportManager.exportReportToPdfFile(jasperPrint, fileName);
            } catch (JRException e) {
                log.error("failed to generate PDF file : {}", e.getMessage());
            }
        } else if (S3.equalsIgnoreCase(storageType)) {
            OutputStream outStream;
            Path bucketPath = getObjectPath(fileName);

            try {
                outStream = s3FileSystem.provider().newOutputStream(bucketPath);
                JasperExportManager.exportReportToPdfStream(jasperPrint, outStream);
            } catch (IOException | JRException e) {
                log.error("error message : {}", e.getMessage());
            }
        }
    }

    /**
     * creates a data source of type JRXmlDataSource from a file
     *
     * @param file a file.
     * @return JRXmlDataSource JRXmlDataSource object
     */
    public static JRXmlDataSource getJRXmlDataSource(File file) {
        if (NFS.equals(storageType)) {
            try {
                return new JRXmlDataSource(file);
            } catch (JRException e) {
                log.error("JRException : {}", e.getMessage());
            }
        } else if (S3.equalsIgnoreCase(storageType)) {
            try {
                return new JRXmlDataSource(getInputStream(file));
            } catch (JRException e) {
                log.error("JRException in getJRXmlDataSource : {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * list all files inside of a directory with extensions
     *
     * @param directory     a string of source directory.
     * @param extensions    list of file extensions.
     * @param prefix        Filename prefix to filter by
     * @param sortingOption the sorting option
     * @return a file arrays inside of the source directory
     */
    public static File[] listFiles(File directory, final List<String> extensions, final String prefix, final String sortingOption) {
        File[] files = null;
        if (NFS.equals(storageType)) {
            files = listFileSystemFiles(directory, extensions, prefix);
        } else if (S3.equalsIgnoreCase(storageType)) {
            files = listS3Files(directory, extensions, prefix, false);
        }
        return org.meveo.commons.utils.FileUtils.sortFiles(files, sortingOption);
    }

    /**
     * list all files inside of a directory with extensions
     *
     * @param directory  a string of source directory.
     * @param extensions List of extensions to filter by
     * @param recursive  indicates if the search will be recursive or not
     * @return a file list inside of the source directory
     */
    public static List<File> listFiles(File directory, final String[] extensions, final boolean recursive) {
        List<File> files = null;
        if (NFS.equals(storageType)) {
            files = listFileSystemFiles(directory, extensions, recursive);
        } else if (S3.equalsIgnoreCase(storageType)) {
            files = Arrays.asList(listS3Files(directory, Arrays.asList(extensions), null, recursive));
        }
        return files;
    }

    /**
     * Move or rename a file to a target file.
     *
     * @param source  the path to the file to move
     * @param target  the path to the target file (may be associated with a different
     *                provider to the source path)
     * @param options options specifying how the move should be done (REPLACE_EXISTING, COPY_ATTRIBUTES or ATOMIC_MOVE.)
     */
    public static void moveFile(String source, String target, CopyOption... options) {
        if (NFS.equals(storageType)) {
            try {
                Files.move(Paths.get(source), Paths.get(target), options);
            } catch (IOException e) {
                log.error("IOException while moving file : {}", e.getMessage());
            }
        } else if (S3.equalsIgnoreCase(storageType)) {
            log.debug("move object from source key {} to destination key {}", source, target);
            // copy object from source to target
            CopyObjectRequest copyObjRequest = CopyObjectRequest.builder()
                    .sourceBucket(bucketName).sourceKey(source)
                    .destinationBucket(bucketName).destinationKey(target)
                    .build();

            try {
                s3FileSystem.getClient().copyObject(copyObjRequest);
            } catch (NoSuchKeyException e) {
                log.error("NoSuchKeyException while copying object in addExtension method : {}", e.getMessage());
            }

            log.debug("delete old object at source key {}", source);
            // delete old object
            DeleteObjectRequest deleteObjRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName).key(source)
                    .build();

            try {
                s3FileSystem.getClient().deleteObject(deleteObjRequest);
            } catch (NoSuchKeyException e) {
                log.error("NoSuchKeyException while deleting object in addExtension method : {}", e.getMessage());
            }
        }
    }

    /**
     * rename a file in FS, or object in S3
     *
     * @param srcFile  file whose name/extension needs to be changed/modified.
     * @param destFile new file after modification.
     * @return true if name is successfully renamed, false otherwise
     */
    public static boolean renameTo(File srcFile, File destFile) {
        if (existsFile(srcFile) && destFile != null) {
            if (NFS.equals(storageType)) {
                return srcFile.renameTo(destFile);
            } else if (S3.equalsIgnoreCase(storageType)) {
                String srcKey = formatObjectKey(srcFile.getPath());
                String destKey = formatObjectKey(destFile.getPath());
                log.debug("rename key object in S3 bucket from source key {} to destination key {}", srcKey, destKey);

                moveFile(srcKey, destKey, StandardCopyOption.ATOMIC_MOVE);

                return true;
            }
        }
        return false;
    }

    /**
     * Tests if the provided file is a normal directory
     *
     * @param directory the directory
     * @return true if file is directory, false otherwise
     */
    public static boolean isDirectory(File directory) {
        if (NFS.equals(storageType)) {
            return directory.isDirectory();
        } else if (S3.equalsIgnoreCase(storageType)) {
            String objectKey = formatObjectKey(directory.getPath(), true);
            log.debug("check if object is a directory in S3 bucket at key {}", objectKey);
            if (!StringUtils.isBlank(objectKey) && exists(objectKey, true)) {
                HeadObjectRequest request = HeadObjectRequest.builder().bucket(bucketName).key(objectKey).build();
                HeadObjectResponse response = s3FileSystem.getClient().headObject(request);
                return response.contentLength() >= 0;
            }
        }
        return false;
    }

    /**
     * Tests if the file is a valid zip file.
     *
     * @param file the file
     * @return true if file is a valid zip file, false otherwise
     */
    public static boolean isValidZip(File file) {
        if (NFS.equals(storageType)) {
            return org.meveo.commons.utils.FileUtils.isValidZip(file);
        } else if (S3.equalsIgnoreCase(storageType)) {
            if (!FilenameUtils.getExtension(file.getName()).equals("zip"))
                return false;

            return existsFile(file);
        }

        return false;
    }

    /**
     * list all files inside of a directory
     *
     * @param sourceDirectory a source directory.
     * @param filter          FilenameFilter.
     * @return a file arrays inside of the source directory
     */
    public static File[] listFiles(File sourceDirectory, FilenameFilter filter) {
        if (NFS.equals(storageType)) {
            return listFileSystemFiles(sourceDirectory, filter);
        } else if (S3.equalsIgnoreCase(storageType)) {
            return listS3Files(sourceDirectory, filter);
        }
        return null;
    }

    /**
     * list all files inside of a directory
     *
     * @param sourceDirectory a source directory.
     * @return a file arrays inside of the source directory
     */
    public static String[] list(File sourceDirectory) {
        if (NFS.equals(storageType)) {
            return sourceDirectory.list();
        } else if (S3.equalsIgnoreCase(storageType)) {
            String objectKey = formatObjectKey(sourceDirectory.getPath(), true);
            log.debug("list files in S3 bucket at objectKey {}", objectKey);
            if (!StringUtils.isBlank(objectKey)) {
                final ListObjectsV2Request objectRequest =
                        ListObjectsV2Request.builder()
                                .bucket(bucketName)
                                .prefix(objectKey)
                                .build();

                ListObjectsV2Response listObjects = s3FileSystem.getClient().listObjectsV2(objectRequest);
                Set<String> files = new HashSet<>();
                String patternStr = "^" + objectKey + "([^/\n]+/?)";
                Pattern pattern = Pattern.compile(patternStr);
                List<S3Object> s3Objects = listObjects.contents();
                for (S3Object obj : s3Objects) {
                    Matcher matcher = pattern.matcher(obj.key());
                    if (matcher.find() && matcher.group(1) != null) {
                        files.add(matcher.group(1));
                    }
                }
                return files.toArray(new String[0]);
            }
        }
        return null;
    }

    /**
     * delete a file on File System or an object on S3
     *
     * @param file a file to delete.
     * @throws IOException If something fails at I/O level.
     */
    public static void delete(File file) throws IOException {
        if (NFS.equals(storageType)) {
            file.delete();
        } else if (S3.equalsIgnoreCase(storageType)) {
            deleteObject(file.getPath(), false);
        }
    }

    /**
     * put an object on S3
     *
     * @param objectKey an objectKey.
     * @param body      RequestBody.
     */
    public static void putObject(String objectKey, RequestBody body) {
        PutObjectRequest request = PutObjectRequest.builder().bucket(bucketName)
                .key(objectKey).build();

        s3FileSystem.getClient().putObject(request, body);
    }

    /**
     * delete an object on S3
     *
     * @param filePath    a file path.
     * @param isDirectory true if it's the directory.
     * @throws IOException If something fails at I/O level.
     */
    public static void deleteObject(String filePath, boolean isDirectory) throws IOException {
        String objectKey = formatObjectKey(filePath, isDirectory);
        log.debug("delete object with key {} in S3 bucket", objectKey);
        if (!StringUtils.isBlank(objectKey)) {
            DeleteObjectRequest request = DeleteObjectRequest.builder().bucket(bucketName)
                    .key(objectKey).build();
            try {
                s3FileSystem.getClient().deleteObject(request);
            } catch (AwsServiceException | SdkClientException e) {
                throw new IOException(e);
            }
        }
    }

    /**
     * check if storage type is S3
     *
     * @return true if S3 is used, false otherwise
     */
    public static boolean isS3Activated() {
        return S3.equalsIgnoreCase(storageType);
    }

    /**
     * list sub-files and sub-folders inside of a directory
     *
     * @param sourceDirectory a source directory.
     * @return a file arrays inside of the source directory
     */
    public static Map<String, Date> listSubFoldersAndFiles(File sourceDirectory) {
        String sourceDir = formatObjectKey(sourceDirectory.getPath(), true);
        log.debug("list files and directories in S3 bucket at directory {} ", sourceDir);
        Map<String, Date> result = new HashMap<>();
        if (!StringUtils.isBlank(sourceDir)) {
            final ListObjectsV2Request objectRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(sourceDir)
                    .build();

            ListObjectsV2Response listObjects = s3FileSystem.getClient().listObjectsV2(objectRequest);
            String patternStr = "^" + sourceDir + "([^/\n]+/?).*";
            Pattern pattern = Pattern.compile(patternStr);
            List<S3Object> s3Objects = listObjects.contents();
            for (S3Object obj : s3Objects) {
                Matcher matcher = pattern.matcher(obj.key());
                if (matcher.find()) {
                    result.put(matcher.group(1), Date.from(obj.lastModified()));
                }
            }
        }
        return result;
    }

    /**
     * list all nested sub-files and sub-folders inside of a directory
     *
     * @param sourceDirectory a source directory.
     * @return a file arrays inside of the source directory
     */
    public static Set<String> listAllSubFoldersAndFiles(File sourceDirectory) {
        String sourceDir = formatObjectKey(sourceDirectory.getPath(), true);
        log.debug("list all nested files and directories in S3 bucket at source directory {} ", sourceDir);
        Set<String> setKeys = new HashSet<>();
        if (!StringUtils.isBlank(sourceDir)) {
            final ListObjectsV2Request objectRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(sourceDir)
                    .build();

            ListObjectsV2Response listObjects = s3FileSystem.getClient().listObjectsV2(objectRequest);
            String patternStr = "^" + sourceDir + "([^/\n]*/?)*";
            Pattern pattern = Pattern.compile(patternStr);
            List<S3Object> s3Objects = listObjects.contents();

            for (S3Object obj : s3Objects) {
                Matcher matcher = pattern.matcher(obj.key());
                if (matcher.matches()) {
                    setKeys.add(obj.key());
                }
            }
        }
        return setKeys;
    }

    /**
     * get length of a file on File System or length of an object on S3
     *
     * @param file a file.
     * @return a file arrays inside of the source directory
     */
    public static long length(File file) {
        if (NFS.equals(storageType)) {
            return file.length();
        } else if (S3.equalsIgnoreCase(storageType)) {
            String objectKey = formatObjectKey(file.getPath());
            log.debug("get length of object on S3 at key {}", objectKey);
            if (!StringUtils.isBlank(objectKey)) {
                HeadObjectRequest request = HeadObjectRequest.builder().bucket(bucketName).key(objectKey).build();
                HeadObjectResponse response = s3FileSystem.getClient().headObject(request);
                return response.contentLength();
            }
        }
        return 0L;
    }

    /**
     * List files matching extensions and prefix in a given directory
     *
     * @param directory  Directory to inspect
     * @param extensions List of extensions to filter by
     * @param prefix     Filename prefix to filter by
     * @return Array of matched files
     */
    private static File[] listFileSystemFiles(File directory, final List<String> extensions, final String prefix) {
        if (!directory.exists() || !directory.isDirectory()) {
            log.error(String.format("Wrong source directory: %s", directory.getAbsolutePath()));
            return null;
        }
        File[] files = null;
        if (StringUtils.isBlank(prefix) && StringUtils.isBlank(extensions)) {
            files = directory.listFiles();
        } else {
            ImportFileFiltre filter = new ImportFileFiltre(prefix, extensions);
            files = directory.listFiles(filter);
        }
        return files;
    }

    /**
     * List files matching extensions and prefix in a given directory
     *
     * @param directory  Directory to inspect
     * @param extensions List of extensions to filter by
     * @param recursive  indicates if the search will be recursive or not
     * @return List of matched files
     */
    private static List<File> listFileSystemFiles(File directory, final String[] extensions, final boolean recursive) {
        if (!directory.exists() || !directory.isDirectory()) {
            log.error(String.format("Wrong source directory: %s", directory.getAbsolutePath()));
            return null;
        }
        return new ArrayList(FileUtils.listFiles(directory, extensions, recursive));
    }

    /**
     * List files matching filter in a given directory
     *
     * @param directory Directory to inspect
     * @param filter    used to filter filenames
     * @return Array of matched files
     */
    private static File[] listFileSystemFiles(File directory, FilenameFilter filter) {
        if (!directory.exists() || !directory.isDirectory()) {
            log.error(String.format("Wrong source directory: %s", directory.getAbsolutePath()));
            return null;
        }
        return directory.listFiles(filter);
    }

    /**
     * List files matching extensions and prefix in a given directory
     *
     * @param directory  Directory to inspect
     * @param extensions List of extensions to filter by
     * @param prefix     Filename prefix to filter by
     * @param recursive  if true then grab all subdirectories and its files.
     * @return Array of matched files
     */
    private static File[] listS3Files(File directory, final List<String> extensions, final String prefix, final boolean recursive) {
        String objectKey = formatObjectKey(directory.getPath(), true);
        log.debug("list files in S3 bucket at directory {} with extension, prefix and or not recursive", objectKey);
        List<File> listFiles = new ArrayList<>();
        if (!StringUtils.isBlank(objectKey)) {
            if (recursive) {
                final ListObjectsV2Request objectRequest =
                        ListObjectsV2Request.builder()
                                .bucket(bucketName)
                                .prefix(objectKey)
                                .build();
                ListObjectsV2Response listObjects = s3FileSystem.getClient().listObjectsV2(objectRequest);
                for (S3Object object : listObjects.contents()) {
                    log.debug("object.key() = <" + object.key() + ">");
                    listFiles.add(new File(object.key()));
                }
            } else { // Gets only first level
                log.debug("Gets only first level at key {}", objectKey);
                return listS3Files(objectKey);
            }
        }
        return listFiles.toArray(new File[0]);
    }

    /**
     * List files and directories in a directory with the given key.
     *
     * @param objectKey the S3 object key
     * @return Array of matched files
     */
    private static File[] listS3Files(String objectKey) {
        log.debug("list files in S3 bucket at directory {} without recursive", objectKey);
        List<File> listFiles = new ArrayList<>();
        if (!StringUtils.isBlank(objectKey)) {
            ListObjectsV2Request objectRequest =
                    ListObjectsV2Request.builder()
                            .bucket(bucketName)
                            .prefix(objectKey)
                            .delimiter("/")
                            .build();
            ListObjectsV2Response listObjects = s3FileSystem.getClient().listObjectsV2(objectRequest);

            // Gets the folders
            log.debug("Gets folders first level at key {}", objectKey);
            for (CommonPrefix commonPrefix : listObjects.commonPrefixes()) {
                String file = commonPrefix.prefix();
                if (!StringUtils.isBlank(file)) {
                    log.debug("file = <" + file + ">");
                    listFiles.add(new File(file));
                }
            }

            // Gets the files
            log.debug("Gets files first level at key {}", objectKey);
            for (S3Object object : listObjects.contents()) {
                // Exclude the root directory
                if (!object.key().equalsIgnoreCase(objectKey)) {
                    log.debug("object.key() = <" + object.key() + ">");
                    listFiles.add(new File(object.key()));
                }
            }
        }
        return listFiles.toArray(new File[0]);
    }

    /**
     * List files matching filter in a given directory
     *
     * @param directory Directory to inspect
     * @param filter    used to filter filenames
     * @return Array of matched files
     */
    private static File[] listS3Files(File directory, FilenameFilter filter) {
        String objectKey = formatObjectKey(directory.getPath(), true);
        log.debug("list files in S3 bucket at directory {} with filter", objectKey);
        //FIXME : add the filter in the search.
        return listS3Files(objectKey);
    }

    /**
     * Copy an object from a directory to an other one
     *
     * @param srcFile  source file.
     * @param destFile destination file.
     */
    public static void copyFileOrObject(File srcFile, File destFile) {
        if (NFS.equals(storageType)) {
            try {
                com.google.common.io.Files.copy(srcFile, destFile);
            } catch (IOException e) {
                log.error("IOException while copying file : {}", e.getMessage());
            }
        } else if (S3.equalsIgnoreCase(storageType)) {
            String srcKey = formatObjectKey(srcFile.getPath());
            String destKey = formatObjectKey(destFile.getPath());
            log.debug("copy object from source key {} to destination key {}", srcKey, destKey);

            if (!StringUtils.isBlank(srcKey) && !StringUtils.isBlank(destKey)) {
                // copy object from srckey to destKey
                CopyObjectRequest copyObjRequest = CopyObjectRequest.builder()
                        .sourceBucket(bucketName).sourceKey(srcKey)
                        .destinationBucket(bucketName).destinationKey(destKey)
                        .build();

                try {
                    s3FileSystem.getClient().copyObject(copyObjRequest);
                } catch (NoSuchKeyException e) {
                    log.error("NoSuchKeyException while copying object in addExtension method : {}", e.getMessage());
                }

                log.debug("delete old object at source key {}", srcKey);
                // delete old object
                DeleteObjectRequest deleteObjRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName).key(srcKey)
                        .build();

                try {
                    s3FileSystem.getClient().deleteObject(deleteObjRequest);
                } catch (NoSuchKeyException e) {
                    log.error("NoSuchKeyException while deleting object in addExtension method : {}", e.getMessage());
                }

            }
        }
    }

    /**
     * Tests if the provided file  is a normal file
     *
     * @param file the file
     * @return true if file is a normal file, false otherwise
     */
    public static boolean isFile(File file) {
        if (NFS.equals(storageType)) {
            return file.isFile();
        } else if (S3.equalsIgnoreCase(storageType)) {
            String objectKey = formatObjectKey(file.getPath());
            log.debug("check if object is a file in S3 bucket at key {}", objectKey);
            if (!StringUtils.isBlank(objectKey) && existsFile(objectKey)) {
                HeadObjectRequest request = HeadObjectRequest.builder().bucket(bucketName).key(objectKey).build();
                HeadObjectResponse response = s3FileSystem.getClient().headObject(request);
                return response.contentLength() >= 0;
            }
        }
        return false;
    }

    /**
     * Copy the source directory in to destination one.
     *
     * @param sourceDirectory      the source directory
     * @param destinationDirectory the destination directory
     */
    public static void copyDirectory(File sourceDirectory, File destinationDirectory) {
        if (NFS.equals(storageType)) {
            org.meveo.commons.utils.FileUtils.copyDirectory(sourceDirectory, destinationDirectory);
        } else if (S3.equalsIgnoreCase(storageType)) {
            String sourceDir = formatObjectKey(sourceDirectory.getPath(), true);
            String destDir = formatObjectKey(destinationDirectory.getPath(), true);
            if (!StringUtils.isBlank(sourceDir) && !StringUtils.isBlank(destDir)) {
                CopyObjectRequest copyObjRequest = CopyObjectRequest.builder()
                        .sourceBucket(bucketName).sourceKey(sourceDir)
                        .destinationBucket(bucketName).destinationKey(destDir)
                        .build();
                try {
                    s3FileSystem.getClient().copyObject(copyObjRequest);
                } catch (NoSuchKeyException e) {
                    log.error("NoSuchKeyException while copying object in addExtension method : {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Copy input stream to file
     *
     * @param source      the input stream
     * @param destination the destination file
     */
    public static void copyInputStreamToFile(InputStream source, File destination) {
        if (NFS.equals(storageType)) {
            try {
                FileUtils.copyInputStreamToFile(source, destination);
            } catch (IOException e) {
                log.error("IOException while copying input stream to file : {}", e.getMessage());
            }
        } else if (S3.equalsIgnoreCase(storageType)) {
            String objectKey = formatObjectKey(destination.getPath());
            log.debug("check input stream to file in S3 bucket at key {}", objectKey);
            if (!StringUtils.isBlank(objectKey) && existsFile(objectKey)) {
                PutObjectRequest request = PutObjectRequest.builder().bucket(bucketName)
                        .key(objectKey).build();
                try {
                    s3FileSystem.getClient().putObject(request, RequestBody.fromInputStream(source, source.available()));
                } catch (IOException e) {
                    log.error("IOException while copying input stream to file : {}", e.getMessage());
                }
            }
        }
    }


    /**
     * Tests whether the application can read the file
     *
     * @param file the file for which doing the read access check
     * @return Returns : true if and only if the file specified by this abstract pathname exists
     * and can be read by the application; false otherwise
     */
    public static boolean canRead(File file) {
        if (NFS.equals(storageType)) {
            return file.canRead();
        } else if (S3.equalsIgnoreCase(storageType)) {
            return true;
        }
        return false;
    }

    /**
     * Tests whether the application can write the file
     *
     * @param file the file for which doing the write access check
     * @return Returns : true if and only if the file specified by this abstract pathname exists
     * and can be write by the application; false otherwise
     */
    public static boolean canWrite(File file) {
        if (NFS.equals(storageType)) {
            return file.canWrite();
        } else if (S3.equalsIgnoreCase(storageType)) {
            return true;
        }
        return false;
    }

    /**
     * Return the last-modified time of the file or directory.
     *
     * @param filePath a file path.
     */
    public static long getLastModified(String filePath) {
        File file = new File(filePath);
        return getLastModified(file);
    }

    /**
     * Return the last-modified time of the file or directory named by this abstract pathname.
     *
     * @param file the file.
     */
    public static long getLastModified(File file) {
        if (NFS.equals(storageType)) {
            return file.lastModified();
        } else if (S3.equalsIgnoreCase(storageType)) {
            String objectKey = formatObjectKey(file.getPath(), isDirectory(file));
            log.debug("check if object is a file in S3 bucket at key {}", objectKey);
            if (!StringUtils.isBlank(objectKey)) {
                HeadObjectRequest request = HeadObjectRequest.builder().bucket(bucketName).key(objectKey).build();
                HeadObjectResponse response = s3FileSystem.getClient().headObject(request);
                return Date.from(response.lastModified()).getTime();
            }
        }
        return 0;
    }

    /**
     * Return the last-modified time of the file or directory.
     *
     * @param filePath    a file path.
     * @param isDirectory true if it's the directory.
     */
    public static long getLastModified(String filePath, boolean isDirectory) {
        File file = new File(filePath);
        return getLastModified(file, isDirectory);
    }

    /**
     * Return the last-modified time of the file or directory named by this abstract pathname.
     *
     * @param file        the file.
     * @param isDirectory true if it's the directory.
     */
    public static long getLastModified(File file, boolean isDirectory) {
        if (NFS.equals(storageType) || (S3.equalsIgnoreCase(storageType) && isDirectory)) {
            return file.lastModified();
        } else if (S3.equalsIgnoreCase(storageType)) {
            String objectKey = formatObjectKey(file.getPath());
            log.debug("check if object is a file in S3 bucket at key {}", objectKey);
            if (!StringUtils.isBlank(objectKey) && existsFile(objectKey)) {
                HeadObjectRequest request = HeadObjectRequest.builder().bucket(bucketName).key(objectKey).build();
                HeadObjectResponse response = s3FileSystem.getClient().headObject(request);
                return Date.from(response.lastModified()).getTime();
            }
        }
        return 0;
    }

    /**
     * Create the temp file
     *
     * @param prefix – The prefix string to be used in generating the file's name;
     *               must be at least three characters long
     * @param suffix – The suffix string to be used in generating the file's name; may be null,
     *               in which case the suffix ".tmp" will be used
     * @return the temp file.
     */
    public static File createTempFile(String prefix, String suffix) {
        if (NFS.equals(storageType)) {
            try {
                return File.createTempFile(prefix, suffix);
            } catch (IOException e) {
                log.error("IO exception : {}", e.getMessage());
            }
        } else if (S3.equalsIgnoreCase(storageType)) {
            if (StringUtils.isBlank(prefix) || prefix.length() < 3) {
                throw new IllegalArgumentException("Prefix string \"" + prefix +
                        "\" too short: length must be at least 3");
            }
            if (StringUtils.isBlank(suffix))
                suffix = ".tmp";

            File tmpdir = new File(System.getProperty("java.io.tmpdir"));
            String tmpFileName = prefix + suffix;
            String tmpFilePath = tmpdir.getAbsolutePath() + File.separator + tmpFileName;
            putObject(tmpFilePath, RequestBody.empty());
            return new File(tmpFilePath);
        }
        return null;
    }


    /**
     * Copy the given byte range of the given input to the given output.
     *
     * @param file   The file input to copy the given range to the given output for.
     * @param output The output to copy the given range from the given input for.
     * @param start  Start of the byte range.
     * @param length Length of the byte range.
     * @throws IOException If something fails at I/O level.
     */
    private static void fileSystemCopy(File file, OutputStream output, long start, long length) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;

        try (RandomAccessFile input = new RandomAccessFile(file, "r")) {
            if (input.length() == length) {
                // Write full range.
                while ((read = input.read(buffer)) > 0) {
                    output.write(buffer, 0, read);
                }
            } else {
                // Write partial range.
                input.seek(start);
                long toRead = length;

                while ((read = input.read(buffer)) > 0) {
                    if ((toRead -= read) > 0) {
                        output.write(buffer, 0, read);
                    } else {
                        output.write(buffer, 0, (int) toRead + read);
                        break;
                    }
                }
            }
        } finally {
            // Gently close streams.
            close(output);
        }
    }

    /**
     * Copy the given byte range of the given input to the given output.
     *
     * @param file   The file input to copy the given range to the given output for.
     * @param output The output to copy the given range from the given input for.
     * @param start  Start of the byte range.
     * @param length Length of the byte range.
     * @throws IOException If something fails at I/O level.
     */
    public static void s3Copy(File file, OutputStream output, long start, long length) throws IOException {
        String objectKey = formatObjectKey(file.getPath());
        log.info("Fetching object from S3 - Key: {}, Start: {}, Length: {}", objectKey, start, length);

        // Construct the S3 request
        GetObjectRequest.Builder requestBuilder = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey);

        if (start != 0 || length > 0) {
            String range = "bytes=" + start + "-" + ((length > 0) ? (start + length - 1) : "");
            requestBuilder.range(range);
        }

        GetObjectRequest request = requestBuilder.build();

        try (InputStream inputStream = s3FileSystem.getClient().getObject(request)) {
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int bytesRead;
            long remaining = (length > 0) ? length : Long.MAX_VALUE; // Handle unlimited length

            while ((bytesRead = inputStream.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                output.write(buffer, 0, bytesRead);
                remaining -= bytesRead;
                if (remaining <= 0) break;
            }
        } finally {
            // Gently close streams.
            close(output);
        }
    }

    /**
     * Close the given resource.
     *
     * @param resource The resource to be closed.
     */
    private static void close(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException ignore) {
                // Ignore IOException. If you want to handle this anyway, it might be useful to know
                // that this will generally only be thrown when the client aborted the request.
            }
        }
    }

    /**
     * Copy the given byte range of the given input to the given output.
     *
     * @param file   The file input to copy the given range to the given output for.
     * @param output The output to copy the given range from the given input for.
     * @param start  Start of the byte range.
     * @param length Length of the byte range.
     * @throws IOException If something fails at I/O level.
     */
    public static void copy(File file, OutputStream output, long start, long length) throws IOException {
        if (NFS.equals(storageType)) {
            fileSystemCopy(file, output, start, length);
        } else if (S3.equalsIgnoreCase(storageType)) {
            s3Copy(file, output, start, length);
        }
    }
}
