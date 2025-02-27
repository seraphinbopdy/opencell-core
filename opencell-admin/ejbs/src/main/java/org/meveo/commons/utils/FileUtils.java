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
package org.meveo.commons.utils;

import jakarta.xml.bind.Marshaller;
import net.sf.jasperreports.engine.data.JRXmlDataSource;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.meveo.admin.job.SortingFilesEnum;
import org.meveo.admin.storage.StorageFactory;
import org.meveo.model.shared.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.CopyOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * File utilities class.
 *
 * @author Donatas Remeika
 * @author Edward P. Legaspi
 * @author Abdellatif BARI
 * @lastModifiedVersion 14.3.17
 */
public final class FileUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    /**
     * The Constant DATETIME_FORMAT for file names
     */
    private static final String DATETIME_FORMAT = "dd_MM_yyyy-HHmmss";

    /**
     * No need to create instance.
     */
    private FileUtils() {

    }

    /**
     * Add extension to existing file by renamig it.
     *
     * @param file      File to be renamed.
     * @param extension Extension.
     * @return Renamed File object.
     */
    public static synchronized File addExtension(File file, String extension) {
        if (FileUtils.existsFile(file)) {
            String name = file.getName();
            File dest = new File(file.getParentFile(), name + extension);
            if (StorageFactory.renameTo(file, dest)) {
                return dest;
            }
        }
        return null;
    }

    /**
     * Replaces file extension with new one.
     *
     * @param file      Old file.
     * @param extension New extension.
     * @return New File.
     */
    public static File replaceFileExtension(File file, String extension) {

        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }
        String newFileName = file.getName() + extension;
        int indexOfExtension = file.getName().lastIndexOf(".");
        if (indexOfExtension >= 1) {
            newFileName = file.getName().substring(0, indexOfExtension) + extension;
        }
        return renameFile(file, newFileName);
    }

    /**
     * rename a file in FS, or object in S3
     *
     * @param srcFile instance of File needs to rename
     * @param newName new file's name
     * @return file
     */
    public static File renameFile(File srcFile, String newName) {
        if (FileUtils.existsFile(srcFile)) {
            File destFile = new File(srcFile.getParentFile(), newName);
            if (StorageFactory.renameTo(srcFile, destFile)) {
                return destFile;
            }
        }
        return null;
    }

    /**
     * rename a file in FS, or object in S3
     *
     * @param srcFile  file whose name/extension needs to be changed/modified.
     * @param destFile new file after modification.
     * @return true if name is successfully renamed, false otherwise
     */
    public static boolean renameFile(File srcFile, File destFile) {
        return StorageFactory.renameTo(srcFile, destFile);
    }

    /**
     * Move file. In case a file with the same name exists, create a name with a timestamp
     *
     * @param dest the destination
     * @param file the file to move
     * @param name the new file name to give
     * @return the new file name
     */
    public static String moveFileDontOverwrite(String dest, File file, String name) {
        String destName = name;
        if (existsFile(new File(dest + File.separator + name))) {
            destName += "_COPY_" + DateUtils.formatDateWithPattern(new Date(), DATETIME_FORMAT);
        }
        moveFile(dest, file, destName);
        return destName;
    }

    /**
     * Move file to destination directory.
     *
     * @param destination Absolute path to destination directory.
     * @param file        File object to move.
     * @param newFilename New filename for moved file.
     * @return true if operation was successful, false otherwise.
     */
    public static boolean moveFile(String destination, File file, String newFilename) {
        File destinationDir = new File(destination);

        if (!existsDirectory(destinationDir)) {
            StorageFactory.mkdirs(destinationDir);
        }

        if (StorageFactory.isDirectory(destinationDir)) {
            return StorageFactory.renameTo(file, new File(destination, newFilename != null ? newFilename : file.getName()));
        }

        return false;
    }

    /**
     * Copy file. If destination file name is directory, then create copy of file with same name in that directory. I destination is file, then copy data to file with this name.
     *
     * @param fromFileName File name that we are copying.
     * @param toFileName   File(dir) name where to copy.
     * @throws IOException IO exeption.
     */
    public static void copy(String fromFileName, String toFileName) throws IOException {
        File fromFile = new File(fromFileName);
        File toFile = new File(toFileName);

        if (!existsFile(fromFile)) {
            throw new IOException("FileCopy: no such source file: " + fromFileName);
        }
        if (!isFile(fromFile)) {
            throw new IOException("FileCopy: can't copy directory: " + fromFileName);
        }
        if (!canRead(fromFile)) {
            throw new IOException("FileCopy: source file is unreadable: " + fromFileName);
        }

        boolean isDirectory = isDirectory(toFile);
        if (isDirectory) {
            toFile = new File(toFile, fromFile.getName());
        }

        if (existsFile(toFile)) {
            if (!canWrite(toFile)) {
                throw new IOException("FileCopy: destination file is unwriteable: " + toFileName);
            }
        } else {
            String parent = toFile.getParent();
            if (parent == null) {
                parent = System.getProperty("user.dir");
            }
            File dir = new File(parent);
            if (!existsDirectory(dir)) {
                throw new IOException("FileCopy: destination directory doesn't exist: " + parent);
            }
            if (isDirectory(dir)) {
                throw new IOException("FileCopy: destination is not a directory: " + parent);
            }
            if (!canWrite(dir)) {
                throw new IOException("FileCopy: destination directory is unwriteable: " + parent);
            }
        }

        InputStream from = null;
        OutputStream to = null;
        try {
            from = getInputStream(fromFile);
            to = getOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = from.read(buffer)) != -1) {
                to.write(buffer, 0, bytesRead);
            }
        } finally {
            if (from != null) {
                try {
                    from.close();
                } catch (IOException e) {
                    logger.warn("Failed to close file resource!", e);
                }
            }
            if (to != null) {
                try {
                    to.close();
                } catch (IOException e) {
                    logger.warn("Failed to close file resource!", e);
                }
            }
        }
    }

    /**
     * Replaces filename extension with new one.
     *
     * @param filename  Old filename.
     * @param extension New extension.
     * @return New Filename.
     */
    public static String replaceFilenameExtension(String filename, String extension) {

        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }
        int indexOfExtension = filename.lastIndexOf(".");
        if (indexOfExtension < 1) {
            return filename + extension;
        } else {
            filename = filename.substring(0, indexOfExtension) + extension;
            return filename;
        }
    }

    /**
     * Get file format by file name extension.
     *
     * @param filename File name.
     * @return FileFormat enum.
     */
    public static FileFormat getFileFormatByExtension(String filename) {
        int indexOfExtension = filename.lastIndexOf(".");
        if (indexOfExtension < 1 || indexOfExtension >= filename.length()) {
            return FileFormat.OTHER;
        } else {
            String extension = filename.substring(indexOfExtension + 1);
            return FileFormat.parseFromExtension(extension);
        }

    }

    /**
     * Get the first file from a given directory matching extensions
     *
     * @param sourceDirectory Directory to search inside.
     * @param extensions      list of extensions to match
     * @return First found file
     */
    public static File getFirstFile(String sourceDirectory, final List<String> extensions) {

        File[] files = listFiles(sourceDirectory, extensions);

        if (files == null || files.length == 0) {
            return null;
        }

        for (File file : files) {
            if (isFile(file)) {
                return file;
            }
        }

        return null;
    }

    /**
     * List files matching extensions in a given directory
     *
     * @param directory  Directory to inspect
     * @param extensions List of extensions to filter by
     * @return Array of matched files
     */
    public static File[] listFiles(File directory, final List<String> extensions) {
        return StorageFactory.listFiles(directory, extensions, "*", null);
    }

    /**
     * List files matching extensions in a given directory
     *
     * @param directory Directory to inspect
     * @return Array of matched files
     */
    public static File[] listFiles(File directory) {
        return StorageFactory.listFiles(directory, null, null, null);
    }

    /**
     * List files matching extensions in a given directory
     *
     * @param directory  Directory to inspect
     * @param extensions List of extensions to filter by
     * @param recursive  indicates if the search will be recursive or not
     * @return List of matched files
     */
    public static List<File> listFiles(File directory, String[] extensions, boolean recursive) {
        return StorageFactory.listFiles(directory, extensions, recursive);
    }

    /**
     * List files matching extensions in a given directory
     *
     * @param directoryPath Directory path to inspect
     * @param extensions    List of extensions to filter by
     * @return Array of matched files
     */
    public static File[] listFiles(String directoryPath, final List<String> extensions) {
        File directory = new File(directoryPath);
        return StorageFactory.listFiles(directory, extensions, "*", null);
    }

    /**
     * List files matching extensions and prefix in a given directory
     *
     * @param directoryPath Directory path to inspect
     * @param extensions    List of extensions to filter by
     * @param prefix        File prefix to match
     * @return Array of matched files
     */
    public static File[] listFiles(String directoryPath, final List<String> extensions, final String prefix) {
        File directory = new File(directoryPath);
        return StorageFactory.listFiles(directory, extensions, prefix, null);
    }

    /**
     * List files matching extensions and prefix in a given directory
     *
     * @param directoryPath Directory path to inspect
     * @param extensions    List of extensions to filter by
     * @param prefix        File prefix to match
     * @param sortingOption the sorting option
     * @return Array of matched files
     */
    public static File[] listFiles(String directoryPath, final List<String> extensions, final String prefix, final String sortingOption) {
        File directory = new File(directoryPath);
        return StorageFactory.listFiles(directory, extensions, prefix, sortingOption);
    }

    /**
     * List files, only in FileSystem, matching extension and prefix in a given directory
     *
     * @param dir           Directory to inspect
     * @param extension     File extension to match
     * @param prefix        File prefix to match
     * @param sortingOption the sorting option
     * @return Array of matched files
     */
    public static List<File> listFileSystemFiles(File dir, String extension, String prefix, String sortingOption) {
        List<File> files = new ArrayList<File>();
        ImportFileFiltre filtre = new ImportFileFiltre(prefix, extension);
        File[] listFile = dir.listFiles(filtre);

        if (listFile == null) {
            return files;
        }

        for (File file : listFile) {
            if (file.isFile()) {
                files.add(file);
            }
        }

        return Arrays.asList(sortFiles(files.toArray(new File[]{}), sortingOption));
    }

    /**
     * List files matching extension and prefix in a given directory
     *
     * @param directory     Directory to inspect
     * @param extension     File extension to match
     * @param prefix        File prefix to match
     * @param sortingOption the sorting option
     * @return Array of matched files
     */
    public static List<File> listFiles(File directory, String extension, String prefix, String sortingOption) {
        File[] files = StorageFactory.listFiles(directory, Arrays.asList(extension), prefix, sortingOption);
        if (files == null) {
            return new ArrayList<File>();
        }
        return Arrays.asList(files);
    }

    /**
     * List files matching extension and prefix in a given directory
     *
     * @param directory Directory to inspect
     * @param extension File extension to match
     * @param prefix    File prefix to match
     * @return Array of matched files
     */
    public static List<File> listFiles(File directory, String extension, String prefix) {
        return listFiles(directory, extension, prefix, null);
    }

    /**
     * Creates directory by name if it does not exist.
     *
     * @param pathname – A pathname directory string
     * @return the file.
     */
    public static File createDirectory(String pathname) {
        File directory = new File(pathname);
        return createDirectory(directory);
    }

    /**
     * Creates directory by name if it does not exist.
     *
     * @param directory – A directory to create.
     * @return the file.
     */
    public static File createDirectory(File directory) {
        if (!existsDirectory(directory)) {
            mkdirs(directory);
        }
        return directory;
    }

    /**
     * @param zipFilename zipe file name
     * @param filesToAdd  list of files to add
     */
    public static void createZipArchive(String zipFilename, String... filesToAdd) {
        final int BUFFER = 2048;
        try (OutputStream dest = getOutputStream(zipFilename); ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest))) {
            byte[] data = new byte[BUFFER];
            for (int i = 0; i < filesToAdd.length; i++) {
                try (InputStream fi = getInputStream(filesToAdd[i]); BufferedInputStream origin = new BufferedInputStream(fi, BUFFER)) {
                    ZipEntry entry = new ZipEntry(new File(filesToAdd[i]).getName());
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, BUFFER)) != -1) {
                        out.write(data, 0, count);
                    }
                    FileUtils.closeStream(origin);
                } catch (Exception ex) {
                    logger.error("Error while working with zip archive", ex);
                }
            }
            FileUtils.closeStream(out);
        } catch (Exception e) {
            logger.error("Error while creating zip archive", e);
        }
    }

    /**
     * @param c closable
     * @return true/false
     */
    public static boolean closeStream(Closeable c) {
        try {
            if (c != null) {
                c.close();
                return true;
            } else {
                logger.warn("Stream provided for closing was null");
                return false;
            }
        } catch (Exception e) {
            logger.error("Error while closing output stream", e);
            return false;
        }
    }

    /**
     * @param filename file name
     * @return content of file as string
     * @throws IOException IO exception
     */
    public static String getFileAsString(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        StringBuffer fileData = new StringBuffer();
        char[] buf = new char[1024];
        int numRead = 0;
        try {
            while ((numRead = reader.read(buf)) != -1) {
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(readData);
                buf = new char[1024];
            }
        } finally {
            reader.close();
        }
        return fileData.toString();

    }

    /**
     * unzip files into folder.
     *
     * @param folder folder name
     * @param in     input stream
     * @throws Exception exception
     */
    public static void unzipFile(String folder, InputStream in) throws Exception {
        ZipInputStream zis = null;
        BufferedInputStream bis = null;
        CheckedInputStream cis = null;
        try {
            cis = new CheckedInputStream(in, new CRC32());
            zis = new ZipInputStream(cis);
            bis = new BufferedInputStream(zis);
            ZipEntry entry = null;
            File fileout = null;
            while ((entry = zis.getNextEntry()) != null) {
                fileout = new File(folder + File.separator + entry.getName());
                if (!fileout.toString().startsWith(folder)) {
                    throw new IOException("Entry is outside of the target directory");
                }
                if (entry.isDirectory()) {
                    if (!existsDirectory(fileout)) {
                        mkdirs(fileout);
                    }
                    continue;
                }
                if (!existsFile(fileout)) {
                    mkdirs(new File(fileout.getParent()));
                }
                try (OutputStream fos = getOutputStream(fileout)) {
                    assert fos != null;
                    try (BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                        int b = -1;
                        while ((b = bis.read()) != -1) {
                            bos.write(b);
                        }
                        bos.flush();
                        fos.flush();
                    }
                }
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        } finally {
            IOUtils.closeQuietly(bis);
            IOUtils.closeQuietly(zis);
            IOUtils.closeQuietly(cis);
        }
    }

    /**
     * unzip files into folder in file system
     *
     * @param folder folder name
     * @param in     input stream
     * @throws Exception exception
     */
    public static void unzipFileInFileSystem(String folder, InputStream in) throws Exception {
        folder = folder.replace("/", File.separator);
        ZipInputStream zis = null;
        BufferedInputStream bis = null;
        CheckedInputStream cis = null;
        try {
            cis = new CheckedInputStream(in, new CRC32());
            zis = new ZipInputStream(cis);
            bis = new BufferedInputStream(zis);
            ZipEntry entry = null;
            File fileout = null;
            while ((entry = zis.getNextEntry()) != null) {
                fileout = new File(folder + File.separator + entry.getName());
                if (!fileout.toString().startsWith(folder)) {
                    throw new IOException("Entry is outside of the target directory");
                }
                if (entry.isDirectory()) {
                    createDirectory(fileout);
                    continue;
                }
                if (!FileUtils.existsFile(fileout)) {
                    FileUtils.createDirectory(new File(fileout.getParent()));
                }
                try (OutputStream fos = getOutputStream(fileout)) {
                    try (BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                        int b = -1;
                        while ((b = bis.read()) != -1) {
                            bos.write(b);
                        }
                        bos.flush();
                        fos.flush();
                    }
                }
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        } finally {
            IOUtils.closeQuietly(bis);
            IOUtils.closeQuietly(zis);
            IOUtils.closeQuietly(cis);
        }
    }

    /**
     * Compress a folder with sub folders and its files into byte array.
     *
     * @param sourceFolder source folder
     * @return zip file as byte array
     * @throws Exception exception.
     */
    public static byte[] createZipFile(String sourceFolder) throws Exception {

        Logger log = LoggerFactory.getLogger(FileUtils.class);
        log.info("Creating zip file for {}", sourceFolder);

        ZipOutputStream zos = null;
        ByteArrayOutputStream baos = null;
        CheckedOutputStream cos = null;
        try {
            baos = new ByteArrayOutputStream();
            cos = new CheckedOutputStream(baos, new CRC32());
            zos = new ZipOutputStream(new BufferedOutputStream(cos));
            File sourceFile = new File(sourceFolder);
            for (File file : listFiles(sourceFile)) {
                addToZipFile(file, zos, null);
            }
            zos.flush();
            zos.close();
            return baos.toByteArray();

        } finally {
            IOUtils.closeQuietly(zos);
            IOUtils.closeQuietly(cos);
            IOUtils.closeQuietly(baos);
        }
    }

    public static void addToZipFile(File source, ZipOutputStream zos, String basedir) throws Exception {
        boolean isDirectory = FileUtils.isDirectory(source);
        if (!isDirectory && !FileUtils.isFile(source)) {
            return;
        }

        if (isDirectory) {
            addDirectoryToZip(source, zos, basedir);
        } else {
            addFileToZip(source, zos, basedir);
        }
    }

    public static void addFileToZip(File source, ZipOutputStream zos, String basedir) throws Exception {
        if (!FileUtils.existsFile(source)) {
            return;
        }

        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(getInputStream(source));
            ZipEntry entry = new ZipEntry(((basedir != null ? (basedir + File.separator) : "") + source.getName()).replaceAll("\\" + File.separator, "/"));
            entry.setTime(source.lastModified());
            zos.putNextEntry(entry);
            int count;
            byte data[] = new byte[1024];
            while ((count = bis.read(data, 0, 1024)) != -1) {
                zos.write(data, 0, count);
            }
            zos.flush();
        } finally {
            if (bis != null) {
                bis.close();
            }

        }
    }

    public static void addZipEntry(ZipOutputStream zipOut, InputStream fis, ZipEntry zipEntry) throws IOException {
        zipOut.putNextEntry(zipEntry);
        final byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        zipOut.closeEntry();
    }

    public static void addDirectoryToZip(File source, ZipOutputStream zos, String basedir) throws Exception {
        if (!FileUtils.existsDirectory(source)) {
            return;
        }

        File[] files = FileUtils.listFiles(source);
        if (files != null && files.length != 0) {
            for (File file : files) {
                addToZipFile(file, zos, (basedir != null ? (basedir + File.separator) : "") + source.getName());
            }
        } else {
            ZipEntry entry = new ZipEntry(((basedir != null ? (basedir + File.separator) : "") + source.getName() + File.separator).replaceAll("\\" + File.separator, "/"));
            entry.setTime(source.lastModified());
            zos.putNextEntry(entry);
        }
    }

    /**
     * list all files inside of a directory
     *
     * @param sourceDirectory a source directory.
     * @return a file arrays inside of the source directory
     */
    public static String[] list(File sourceDirectory) {
        return StorageFactory.list(sourceDirectory);
    }

    /**
     * @param relativeRoot relative root path
     * @param dir2zip      directory to be zipped
     * @param zos          zip output stream
     * @throws IOException inpu/ouput exception.
     */
    public static void addDirToArchive(String relativeRoot, String dir2zip, ZipOutputStream zos) throws IOException {
        File zipDir = new File(dir2zip);
        String[] dirList = list(zipDir);
        byte[] readBuffer = new byte[2156];
        int bytesIn = 0;

        for (int i = 0; i < Objects.requireNonNull(dirList).length; i++) {
            File f = new File(zipDir, dirList[i]);
            if (isDirectory(f)) {
                String filePath = f.getPath();
                addDirToArchive(relativeRoot, filePath, zos);
                continue;
            }
            try (InputStream fis = getInputStream(f)) {
                String relativePath = Paths.get(relativeRoot).relativize(f.toPath()).toString();
                ZipEntry anEntry = new ZipEntry(relativePath);
                zos.putNextEntry(anEntry);

                while ((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
            } catch (IOException ex) {
                throw ex;
            }
        }
    }

    /**
     * @param file file to be archived
     * @throws IOException input/ouput exception
     */
    public static void archiveFile(File file) throws IOException {
        byte[] buffer = new byte[1024];
        try (OutputStream fos = getOutputStream(file.getParent() + File.separator + FilenameUtils.removeExtension(file.getName()) + ".zip");
             ZipOutputStream zos = new ZipOutputStream(fos);
             InputStream in = getInputStream(file)) {
            ZipEntry ze = new ZipEntry(file.getName());
            zos.putNextEntry(ze);
            int len;
            while ((len = in.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
        } catch (IOException ex) {
            throw ex;
        }
    }

    /**
     * Change the extension of a file to the given a new file extension.
     *
     * @param filename     Name of the file
     * @param newExtension New extension
     * @return Filename with renamed extension
     */
    public static String changeExtension(String filename, String newExtension) {
        String name = filename.substring(0, filename.lastIndexOf('.'));
        return name + newExtension;
    }

    /**
     * Encode a file to byte64 string.
     *
     * @param file File
     * @return byte string representation of the file
     * @throws IOException IO exeption.
     */
    public static String encodeFileToBase64Binary(File file) throws IOException {
        String encodedFile = null;
        try (InputStream fileInputStreamReader = getInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            fileInputStreamReader.read(bytes);
            encodedFile = org.apache.commons.codec.binary.Base64.encodeBase64String(bytes);
        }

        return encodedFile;
    }

    /**
     * Gets a list of files
     *
     * @param directory the source directory
     * @param filter    the file name filter
     * @return the files for parsing
     */
    public static File[] listFiles(File directory, FilenameFilter filter) {
        return StorageFactory.listFiles(directory, filter);
    }

    /**
     * Gets a list of files
     *
     * @param sourceDirectory the source directory
     * @param extensions      the extensions
     * @param fileNameFilter  the file name key
     * @param sortingOption   the sorting option
     * @return the files for parsing
     */
    public static File[] listFilesByNameFilter(String sourceDirectory, ArrayList<String> extensions, String fileNameFilter, String sortingOption) {

        File sourceDir = new File(sourceDirectory);
        if (!isDirectory(sourceDir)) {
            logger.info(String.format("Wrong source directory: %s", sourceDir.getAbsolutePath()));
            return null;
        }

        String fileNameFilterUpper = fileNameFilter != null ? fileNameFilter.toUpperCase() : null;

        File[] files = listFiles(sourceDir, new FilenameFilter() {

            public boolean accept(File dir, String name) {

                boolean emptyExtensions = true;
                if (extensions != null) {
                    for (String extension : extensions) {
                        if (extension != null) {
                            emptyExtensions = false;
                        }
                    }
                }

                if (emptyExtensions && fileNameFilterUpper == null) {
                    return true;
                }

                String nameUpper = name.toUpperCase();
                if (emptyExtensions && nameUpper.contains(fileNameFilterUpper)) {
                    return true;
                }

                for (String extension : extensions) {
                    if (extension != null && (name.toUpperCase().endsWith(extension.toUpperCase()) || "*".equals(extension)) && (fileNameFilterUpper == null || nameUpper.contains(fileNameFilterUpper))) {
                        return true;
                    }
                }

                return false;
            }

        });

        return sortFiles(files, sortingOption);

    }

    /**
     * Checks if the file param is valid zip
     *
     * @param file
     * @return isValidZip
     */
    public static boolean isValidZip(final File file) {
        try (ZipFile zipfile = new ZipFile(file);) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Count lines of file '\n'
     *
     * @param file
     * @return A number of lines in a file
     * @throws IOException Unable to access a file
     */
    public static int countLines(File file) throws IOException {

        try (InputStream is = new BufferedInputStream(Objects.requireNonNull(getInputStream(file)));) {
            byte[] c = new byte[1024];

            int readChars = is.read(c);
            if (readChars == -1) {
                // bail out if nothing to read
                return 0;
            }

            // make it easy for the optimizer to tune this loop
            int count = 0;
            while (readChars == 1024) {
                for (int i = 0; i < 1024; ) {
                    if (c[i++] == '\n') {
                        ++count;
                    }
                }
                readChars = is.read(c);
            }

            // count remaining characters
            while (readChars != -1) {
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n') {
                        ++count;
                    }
                }
                readChars = is.read(c);
            }

            return count == 0 ? 1 : count;

        } catch (IOException e) {
            logger.error("Failed to count number of lines in a file {}", file.getName(), e);
            throw e;
        }
    }

    /**
     * Get list of files in a folder
     *
     * @param pFolder              Folder
     * @param pReturnListFilesPath A list of files path
     * @param pExtension           Extension
     */
    public static void listAllFiles(File pFolder, List<String> pReturnListFilesPath, String pExtension) {
        try {
            if (isDirectory(pFolder)) {
                File[] fileNames = listFiles(pFolder);
                for (File file : fileNames) {
                    if (file.isDirectory()) {
                        listAllFiles(file, pReturnListFilesPath, pExtension);
                    } else {
                        if (file.getCanonicalPath().endsWith(pExtension)) {
                            pReturnListFilesPath.add(file.getCanonicalPath());
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to get file name in a folder {}", e);
        }
    }

    /**
     * check existence of a file on File System or S3.
     *
     * @param pathname the path name file
     * @return true if file exists, false otherwise
     */
    public static boolean existsFile(String pathname) {
        return StorageFactory.existsFile(pathname);
    }

    /**
     * check existence of a file on File System or S3.
     *
     * @param file the file
     * @return true if file exists, false otherwise
     */
    public static boolean existsFile(File file) {
        return StorageFactory.existsFile(file);
    }

    /**
     * check existence of a directory on File System or S3.
     *
     * @param pathname a pathname of directory.
     * @return true if file exists, false otherwise
     */
    public static boolean existsDirectory(String pathname) {
        return StorageFactory.existsDirectory(pathname);
    }

    /**
     * check existence of a directory on File System or S3.
     *
     * @param directory the directory
     * @return true if directory exists, false otherwise
     */
    public static boolean existsDirectory(File directory) {
        return StorageFactory.existsDirectory(directory);
    }

    /**
     * delete a directory on File System or S3.
     *
     * @param directory the directory to delete
     * @throws IOException If something fails at I/O level.
     */
    public static void deleteDirectory(File directory) throws IOException {
        if (existsDirectory(directory)) {
            StorageFactory.deleteDirectory(directory);
        }
    }

    /**
     * delete a file on File System or an object on S3
     *
     * @param pathname a file path name to delete.
     * @throws IOException If something fails at I/O level.
     */
    public static void delete(String pathname) throws IOException {
        File file = new File(pathname);
        if (existsFile(file)) {
            StorageFactory.delete(file);
        }
    }

    /**
     * delete a file on File System or an object on S3
     *
     * @param file a file to delete.
     * @throws IOException If something fails at I/O level.
     */
    public static void delete(File file) throws IOException {
        if (existsFile(file)) {
            StorageFactory.delete(file);
        }
    }

    /**
     * Copy an object from a directory to an other one
     *
     * @param srcFile  source file.
     * @param destFile destination file.
     */
    public static void copyFileOrObject(File srcFile, File destFile) {
        StorageFactory.copyFileOrObject(srcFile, destFile);
    }

    /**
     * create a new empty file on File System or S3.
     *
     * @param pathname the path name of file to create.
     * @return the new file
     */
    public static File create(String pathname) {
        File file = new File(pathname);
        return create(file);
    }

    /**
     * create a new empty file on File System or S3.
     *
     * @param file the file to create.
     * @return the new file
     */
    public static File create(File file) {
        if (!existsFile(file)) {
            StorageFactory.createNewFile(file);
        }
        return file;
    }

    /**
     * create a new directory on File System or S3.
     *
     * @param directory the directory
     */
    public static void mkdirs(File directory) {
        StorageFactory.mkdirs(directory);
    }

    /**
     * create a new directory on File System or S3.
     *
     * @param pathname the directory path name
     */
    public static void mkdirs(String pathname) {
        File directory = new File(pathname);
        StorageFactory.mkdirs(directory);
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
        StorageFactory.moveFile(source, target, options);
    }

    /**
     * get PrintWriter of a file on File System or S3.
     *
     * @param file the file
     * @return PrintWriter object
     */
    public static PrintWriter getPrintWriter(File file) {
        return StorageFactory.getPrintWriter(file);
    }

    /**
     * get InputStream of a file.
     *
     * @param file the file
     * @return InputStream object
     */
    public static InputStream getInputStream(File file) {
        return StorageFactory.getInputStream(file);
    }

    /**
     * get inputStream based on a filename S3
     *
     * @param fileName String
     * @return InputStream
     * @throws FileNotFoundException If file not found.
     */
    public static InputStream getInputStream(String fileName) throws FileNotFoundException {
        return StorageFactory.getInputStream(fileName);
    }

    /**
     * get buffer reader to read data from a file
     *
     * @param file a file
     * @return BufferReader
     */
    public static Reader getBufferedReader(File file) {
        return StorageFactory.getBufferedReader(file);
    }

    /**
     * get OutputStream of a file.
     *
     * @param fileName the filename
     * @return OutputStream object
     */
    public static OutputStream getOutputStream(String fileName) {
        return StorageFactory.getOutputStream(fileName, false);
    }

    /**
     * get OutputStream of a file.
     *
     * @param fileName the filename
     * @param append   – if true, then bytes will be written to the end of the file rather than the beginning
     * @return OutputStream object
     */
    public static OutputStream getOutputStream(String fileName, boolean append) {
        return StorageFactory.getOutputStream(fileName, append);
    }

    /**
     * Marshal to XML File
     *
     * @param marshaller The Marshaller object.
     * @param obj        the object to be marshalled
     * @param file       the file to it the object will be marshalled
     */
    public static void marshal(Marshaller marshaller, Object obj, File file) {
        StorageFactory.marshal(marshaller, obj, file);
    }

    /**
     * get writer based on file, used to write character-oriented data to a file.
     *
     * @param file String filename of the file
     * @return Writer
     */
    public static Writer getWriter(String file) {
        return StorageFactory.getWriter(file);
    }

    /**
     * rename a file in FS, or object in S3
     *
     * @param srcFile  file whose name/extension needs to be changed/modified.
     * @param destFile new file after modification.
     * @return true if name is successfully renamed, false otherwise
     */
    public static boolean renameTo(File srcFile, File destFile) {
        return StorageFactory.renameTo(srcFile, destFile);
    }

    /**
     * creates a data source of type JRXmlDataSource from a file
     *
     * @param file a file.
     * @return JRXmlDataSource JRXmlDataSource object
     */
    public static JRXmlDataSource getJRXmlDataSource(File file) {
        return StorageFactory.getJRXmlDataSource(file);
    }

    /**
     * get length of a file on File System or length of an object on S3
     *
     * @param file a file.
     * @return a file arrays inside of the source directory
     */
    public static long length(File file) {
        return StorageFactory.length(file);
    }

    /**
     * Parse the content of the given file as an XML document
     * and return a new DOM {@link Document} object.
     *
     * @param file The file containing the XML to parse.
     * @return Document object
     */
    public static Document parse(DocumentBuilder db, File file) {
        return StorageFactory.parse(db, file);
    }

    /**
     * create a new empty file on File System or S3.
     *
     * @param file the file
     */
    public static void createNewFile(File file) {
        StorageFactory.createNewFile(file);
    }

    /**
     * get writer based on file, used to write character-oriented data to a file.
     *
     * @param file the file
     * @return Writer
     */
    public static Writer getWriter(File file) {
        return StorageFactory.getWriter(file);
    }

    /**
     * get reader based on file, to read data from a file
     *
     * @param file String filename of the file
     * @return Reader
     */
    public static Reader getReader(String file) {
        return StorageFactory.getReader(file);
    }

    /**
     * check if storage type is S3
     *
     * @return true if S3 is used, false otherwise
     */
    public static boolean isS3Activated() {
        return StorageFactory.isS3Activated();
    }

    /**
     * list sub-files and sub-folders inside of a directory
     *
     * @param sourceDirectory a source directory.
     * @return a file arrays inside of the source directory
     */
    public static Map<String, Date> listSubFoldersAndFiles(File sourceDirectory) {
        return StorageFactory.listSubFoldersAndFiles(sourceDirectory);
    }

    /**
     * get OutputStream of a file.
     *
     * @param file the file
     * @return OutputStream object
     */
    public static OutputStream getOutputStream(File file) {
        return StorageFactory.getOutputStream(file, false);
    }

    /**
     * get OutputStream of a file.
     *
     * @param file   the file
     * @param append – if true, then bytes will be written to the end of the file rather than the beginning
     * @return OutputStream object
     */
    public static OutputStream getOutputStream(File file, boolean append) {
        return StorageFactory.getOutputStream(file, append);
    }

    /**
     * Tests if the provided file is a normal directory
     *
     * @param directory the directory
     * @return true if file is directory, false otherwise
     */
    public static boolean isDirectory(File directory) {
        return StorageFactory.isDirectory(directory);
    }

    /**
     * Writes bytes to a file.
     *
     * @param path    the path to the file
     * @param bytes   the byte array with the bytes to write
     * @param options options specifying how the file is opened
     */
    public static void write(Path path, byte[] bytes, OpenOption... options) {
        StorageFactory.write(path, bytes, options);
    }

    /**
     * Tests whether the application can read the file
     *
     * @param file the file for which doing the read access check
     * @return Returns : true if and only if the file specified by this abstract pathname exists
     * and can be read by the application; false otherwise
     */
    public static boolean canRead(File file) {
        return StorageFactory.canRead(file);
    }

    /**
     * Tests whether the application can write the file
     *
     * @param file the file for which doing the write access check
     * @return Returns : true if and only if the file specified by this abstract pathname exists
     * and can be write by the application; false otherwise
     */
    public static boolean canWrite(File file) {
        return StorageFactory.canWrite(file);
    }

    /**
     * Tests if the provided file  is a normal file
     *
     * @param file the file
     * @return true if file is a normal file, false otherwise
     */
    public static boolean isFile(File file) {
        return StorageFactory.isFile(file);
    }

    /**
     * Copy the source directory in to destination one.
     *
     * @param sourceDirectory      the source directory
     * @param destinationDirectory the destination directory
     */
    public static void copyDirectory(File sourceDirectory, File destinationDirectory) {
        StorageFactory.copyDirectory(sourceDirectory, destinationDirectory);
    }

    /**
     * Copy input stream to file
     *
     * @param source      the input stream
     * @param destination the destination file
     */
    public static void copyInputStreamToFile(InputStream source, File destination) {
        StorageFactory.copyInputStreamToFile(source, destination);
    }

    /**
     * Sort the list of files
     *
     * @param files         the files tob sorted
     * @param sortingOption the sorting option
     * @return the sorted list of files
     */
    public static File[] sortFiles(File[] files, String sortingOption) {
        if (files != null && files.length > 0 && !StringUtils.isBlank(sortingOption)) {
            if (SortingFilesEnum.ALPHA.name().equals(sortingOption)) {
                Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            } else if (SortingFilesEnum.CREATION_DATE.name().equals(sortingOption)) {
                Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
            }
        }
        return files;
    }

    /**
     * Return the last-modified time of the file or directory.
     *
     * @param filePath a file path.
     */
    public static long getLastModified(String filePath) {
        return StorageFactory.getLastModified(filePath);
    }

    /**
     * Return the last-modified time of the file or directory named by this abstract pathname.
     *
     * @param file the file.
     */
    public static long getLastModified(File file) {
        return StorageFactory.getLastModified(file);
    }

    /**
     * Return the last-modified time of the file or directory.
     *
     * @param filePath    a file path.
     * @param isDirectory true if it's the directory.
     */
    public static long getLastModified(String filePath, boolean isDirectory) {
        return StorageFactory.getLastModified(filePath, isDirectory);
    }

    /**
     * Return the last-modified time of the file or directory named by this abstract pathname.
     *
     * @param file        the file.
     * @param isDirectory true if it's the directory.
     */
    public static long getLastModified(File file, boolean isDirectory) {
        return StorageFactory.getLastModified(file, isDirectory);
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
        return StorageFactory.createTempFile(prefix, suffix);
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
        StorageFactory.copy(file, output, start, length);
    }
}
