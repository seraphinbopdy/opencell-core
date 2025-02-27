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
package org.meveo.admin.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.meveo.commons.utils.FileUtils;
import org.meveo.commons.utils.ParamBean;
import org.primefaces.model.CroppedImage;

/**
 * a help class for meveo pictures like provider/media/module/pictures
 * 
 * @author Tyshan(tyshan@manaty.net)
 * @author Wassim Drira
 * @lastModifiedVersion 5.0
 * 
 */

public class ModuleUtil {

    public static String getRootPicturePath(String providerCode) {
        // To be checked carefully
        String path = ParamBean.getInstanceByProvider(providerCode).getChrootDir(providerCode) + File.separator + "media";
        return getPath(path);
    }

    public static String getPicturePath(String providerCode, String group) {
        return getPicturePath(providerCode, group, true);
    }

    public static String getPicturePath(String providerCode, String group, boolean createDir) {
        String path = getRootPicturePath(providerCode) + File.separator + group + File.separator + "pictures";
        return getPath(path, createDir);
    }

    public static String getModulePicturePath(String providerCode) {
        return getPicturePath(providerCode, "module");
    }

    public static String getTmpRootPath(String providerCode) throws IOException {
        String tmpFolder = System.getProperty("java.io.tmpdir");
        if (StringUtils.isBlank(tmpFolder)) {
            tmpFolder = "/tmp";
        }
        return getPath(tmpFolder + File.separator + providerCode);
    }

    private static String getPath(String path) {
        return getPath(path, true);
    }

    private static String getPath(String path, boolean createDir) {
        if (createDir && !FileUtils.existsDirectory(path)) {
            FileUtils.createDirectory(path);
        }
        return path;
    }

    public static byte[] readPicture(String filename) throws IOException {
        File file = new File(filename);
        if (!FileUtils.existsFile(file)) {
            return new byte[] { };
        }
        BufferedImage img = ImageIO.read(file);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, filename.substring(filename.indexOf('.') + 1), out);
        return out.toByteArray();
    }

    public static void writePicture(String filename, byte[] fileData) throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(fileData);
        BufferedImage img = ImageIO.read(in);
        in.close();
        ImageIO.write(img, filename.substring(filename.indexOf(".") + 1), new File(filename));
    }

    /**
     * read a module picture and save into byte[].
     * 
     * @param providerCode provider code
     * @param filename file name
     * @return module picture as bytes
     * @throws IOException IO exception
     */
    public static byte[] readModulePicture(String providerCode, String filename) throws IOException {
        String picturePath = getModulePicturePath(providerCode);
        String file = picturePath + File.separator + filename;
        return readPicture(file);

    }

    /**
     * save a byte[] data of module picture into file.
     * 
     * @param providerCode provider code.
     * @param filename file name
     * @param fileData file data
     * @throws Exception exception
     */
    public static void writeModulePicture(String providerCode, String filename, byte[] fileData) throws Exception {
        String picturePath = getModulePicturePath(providerCode);
        String file = picturePath + File.separator + filename;
        writePicture(file, fileData);
    }

    /**
     * Remove picture
     *
     * @param filename file name of picture
     * @throws IOException exception when something happens
     */
    public static void removePicture(String filename) throws IOException {
        FileUtils.delete(filename);
    }

    public static void removeModulePicture(String providerCode, String filename) throws Exception {
        String picturePath = getModulePicturePath(providerCode);
        filename = picturePath + File.separator + filename;
        removePicture(filename);
    }

    public static void cropPicture(String filename, CroppedImage croppedImage) throws Exception {
        try (FileImageOutputStream imageOutput = new FileImageOutputStream(new File(filename))) {
            imageOutput.write(croppedImage.getBytes(), 0, croppedImage.getBytes().length);
            imageOutput.flush();
        } catch (Exception ex) {
            throw ex;
        }
    }
}
