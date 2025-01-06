package org.meveo.admin.web.servlet;

import jakarta.inject.Inject;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.security.CurrentUser;
import org.meveo.security.MeveoUser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@WebServlet("/download")
public class FileDownloadServlet extends HttpServlet {
	
	@Inject
	protected ParamBeanFactory paramBeanFactory;
	@Inject
	@CurrentUser
	protected MeveoUser currentUser;
	
	String providerFilePath;
	
	@Override
	public void init() throws ServletException {
		super.init();
		providerFilePath = paramBeanFactory.getInstance().getChrootDir(currentUser.getProviderCode());
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		var fileName = request.getParameter("fileName");
		var folder = request.getParameter("folder");
		String folderPath = providerFilePath + File.separator + (folder == null ? "" : folder);
		String filePath = folderPath + File.separator + fileName;
		
		File file = new File(filePath);
		if (!file.exists()) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		response.setContentType("application/octet-stream");
		response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
		
		try (FileInputStream in = new FileInputStream(file);
		     OutputStream out = response.getOutputStream()) {
			
			byte[] buffer = new byte[8192];
			int bytesRead;
			while ((bytesRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
			}
		}
	}
}
