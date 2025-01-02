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
package org.meveo.admin.action.admin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;

import org.jboss.seam.international.status.Messages;
import org.meveo.admin.action.BaseBean;
import org.meveo.commons.utils.FileUtils;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.admin.User;
import org.meveo.security.CurrentUser;
import org.meveo.security.MeveoUser;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Standard backing bean for {@link User} (extends {@link BaseBean} that provides almost all common methods to handle entities filtering/sorting in datatable, their create, edit, view, delete operations). It works with
 * Manaty custom JSF components.
 *
 * @author Abdellatif BARI
 * @lastModifiedVersion 8.0.0
 */
@Named
@RequestScoped
public class FileForDownloadBean implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Logger. */
    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    @CurrentUser
    protected MeveoUser currentUser;

    @Inject
    protected Messages messages;

    @Inject
    protected ParamBeanFactory paramBeanFactory;

    public StreamedContent getSelectedFile() {

        FacesContext context = FacesContext.getCurrentInstance();
        String selectedFileName = context.getExternalContext().getRequestParameterMap().get("selectedFileName");
        String selectedFolder = context.getExternalContext().getRequestParameterMap().get("selectedFolder");

        String filePath = paramBeanFactory.getInstance().getChrootDir(currentUser.getProviderCode());

        String folder = filePath + File.separator + (selectedFolder == null ? "" : selectedFolder);
        String file = folder + File.separator + selectedFileName;
        log.debug("Downloading file {}", file);
        try {
            FileInputStream is = new FileInputStream(new File(file));
            StreamedContent result = DefaultStreamedContent.builder().name(selectedFileName).stream(() -> is).build();
            return result;

        } catch (Exception e) {
            log.debug("Failed to zip a file {}", file, e);
            return null;
        }
    }

    public StreamedContent getDownloadZipFile() {

        FacesContext context = FacesContext.getCurrentInstance();
        String selectedFolder = context.getExternalContext().getRequestParameterMap().get("selectedFolder");

        String filePath = paramBeanFactory.getInstance().getChrootDir(currentUser.getProviderCode());

        String filename = selectedFolder == null ? "meveo-fileexplore" : selectedFolder.substring(selectedFolder.lastIndexOf(File.separator) + 1);
        String sourceFolder = filePath + File.separator + (selectedFolder == null ? "" : selectedFolder);

        log.debug("Downloading ziped folder {}", sourceFolder);
        try {
            byte[] filedata = FileUtils.createZipFile(sourceFolder);
            InputStream is = new ByteArrayInputStream(filedata);

            return DefaultStreamedContent.builder().name(filename + ".zip").stream(() -> is).contentType("application/octet-stream").build();

        } catch (Exception e) {
            log.debug("Failed to zip a file for folder {}", sourceFolder, e);
        }
        return null;
    }
}