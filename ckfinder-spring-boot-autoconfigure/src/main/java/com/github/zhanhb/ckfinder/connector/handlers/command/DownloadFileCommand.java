/*
 * CKFinder
 * ========
 * http://cksource.com/ckfinder
 * Copyright (C) 2007-2015, CKSource - Frederico Knabben. All rights reserved.
 *
 * The software, this file and its contents are subject to the CKFinder
 * License. Please read the license.txt file before using, installing, copying,
 * modifying or distribute this file or part of its contents. The contents of
 * this file is part of the Source Code of CKFinder.
 */
package com.github.zhanhb.ckfinder.connector.handlers.command;

import com.github.zhanhb.ckfinder.connector.configuration.Constants;
import com.github.zhanhb.ckfinder.connector.configuration.IConfiguration;
import com.github.zhanhb.ckfinder.connector.errors.ConnectorException;
import com.github.zhanhb.ckfinder.connector.handlers.arguments.DownloadFileArguments;
import com.github.zhanhb.ckfinder.connector.utils.AccessControl;
import com.github.zhanhb.ckfinder.connector.utils.FileUtils;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Class to handle <code>DownloadFile</code> command.
 */
public class DownloadFileCommand extends Command<DownloadFileArguments> {

  public DownloadFileCommand() {
    super(DownloadFileArguments::new);
  }

  /**
   * executes the download file command. Writes file to response.
   *
   * @throws ConnectorException when something went wrong during reading file.
   */
  @Override
  @SuppressWarnings("FinalMethod")
  final void execute(HttpServletResponse response) throws ConnectorException {
    if (!isTypeExists(getArguments().getType())) {
      getArguments().setType(null);
      throw new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_TYPE, false);
    }

    getArguments().setFile(Paths.get(getConfiguration().getTypes().get(getArguments().getType()).getPath()
            + getArguments().getCurrentFolder(), getArguments().getFileName()));

    if (!getConfiguration().getAccessControl().checkFolderACL(getArguments().getType(),
            getArguments().getCurrentFolder(), getArguments().getUserRole(),
            AccessControl.CKFINDER_CONNECTOR_ACL_FILE_VIEW)) {
      throw new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED);
    }

    if (!FileUtils.isFileNameInvalid(getArguments().getFileName())
            || FileUtils.checkFileExtension(getArguments().getFileName(),
                    this.getConfiguration().getTypes().get(getArguments().getType())) == 1) {
      throw new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST);
    }

    if (FileUtils.isDirectoryHidden(getArguments().getCurrentFolder(), getConfiguration())) {
      throw new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST);
    }
    try {
      if (!Files.exists(getArguments().getFile())
              || !Files.isRegularFile(getArguments().getFile())
              || FileUtils.isFileHidden(getArguments().getFileName(), this.getConfiguration())) {
        throw new ConnectorException(
                Constants.Errors.CKFINDER_CONNECTOR_ERROR_FILE_NOT_FOUND);
      }

      FileUtils.printFileContentToResponse(getArguments().getFile(), response.getOutputStream());
    } catch (IOException e) {
      throw new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED, e);
    }

  }

  /**
   * inits params for download file command.
   *
   * @param request request
   * @param configuration connector configuration
   * @throws ConnectorException when error occurs.
   */
  @Override
  protected void initParams(HttpServletRequest request, IConfiguration configuration)
          throws ConnectorException {

    super.initParams(request, configuration);
    // problem with showing filename when dialog window appear
    getArguments().setNewFileName(request.getParameter("FileName").replace("\"", "\\\""));
    getArguments().setFileName(request.getParameter("FileName"));
    try {
      if (request.getHeader("User-Agent").contains("MSIE")) {
        getArguments().setNewFileName(URLEncoder.encode(getArguments().getNewFileName(), "UTF-8"));
        getArguments().setNewFileName(getArguments().getNewFileName().replace("+", " ").replace("%2E", "."));
      } else {
        getArguments().setNewFileName(MimeUtility.encodeWord(getArguments().getNewFileName(), "utf-8", "Q"));
      }
    } catch (UnsupportedEncodingException ex) {
      throw new AssertionError(ex);
    }

  }

  /**
   * Sets response headers.
   *
   * @param request request
   * @param response response
   */
  @Override
  public void setResponseHeader(HttpServletRequest request, HttpServletResponse response) {
    String mimetype = request.getServletContext().getMimeType(getArguments().getFileName());
    response.setCharacterEncoding("utf-8");

    if (mimetype != null) {
      response.setContentType(mimetype);
    } else {
      response.setContentType("application/octet-stream");
    }
    if (getArguments().getFile() != null) {
      try {
        response.setContentLengthLong(Files.size(getArguments().getFile()));
      } catch (IOException ex) {
      }
    }

    response.setHeader("Content-Disposition", "attachment; filename=\""
            + getArguments().getNewFileName() + "\"");

    response.setHeader("Cache-Control", "cache, must-revalidate");
    response.setHeader("Pragma", "public");
    response.setHeader("Expires", "0");
  }

}
