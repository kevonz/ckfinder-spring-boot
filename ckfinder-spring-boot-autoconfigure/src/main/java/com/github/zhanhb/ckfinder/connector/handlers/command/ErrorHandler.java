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
import com.github.zhanhb.ckfinder.connector.data.ResourceType;
import com.github.zhanhb.ckfinder.connector.errors.ConnectorException;
import com.github.zhanhb.ckfinder.connector.utils.FileUtils;
import com.github.zhanhb.ckfinder.connector.utils.PathUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Class to handle errors via HTTP headers (for non-XML commands).
 */
public enum ErrorHandler {

  INSTANCE;

  public void handleException(HttpServletRequest request,
          HttpServletResponse response, IConfiguration configuration,
          ConnectorException connectorException) throws IOException {
    String currentFolder = request.getParameter("currentFolder");
    if (currentFolder != null && !currentFolder.isEmpty()) {
      currentFolder = PathUtils.addSlashToBeginning(PathUtils.addSlashToEnd(currentFolder));
    } else {
      currentFolder = null;
    }

    int errorCode = connectorException.getErrorCode();

    if (!configuration.isEnabled()) {
      errorCode = Constants.Errors.CKFINDER_CONNECTOR_ERROR_CONNECTOR_DISABLED;
    } else if (currentFolder != null && !currentFolder.isEmpty()
            && Pattern.compile(Constants.INVALID_PATH_REGEX).matcher(currentFolder).find()) {
//        connectorException = new ConnectorException(
//                Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_NAME,
//                false);
    } else {
      currentFolder = PathUtils.escape(currentFolder);
      if (FileUtils.isDirectoryHidden(currentFolder, configuration)) {
        errorCode = Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
      } else if (currentFolder != null && !currentFolder.isEmpty()) {
        String tmpType = request.getParameter("type");

        ResourceType testType = configuration.getTypes().get(tmpType);
        if (testType == null) {
          errorCode = Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_TYPE;
        }
        Path currDir = Paths.get(configuration.getTypes().get(tmpType).getPath(), currentFolder);
        if (!Files.isDirectory(currDir)) {
          errorCode = Constants.Errors.CKFINDER_CONNECTOR_ERROR_FOLDER_NOT_FOUND;
        }
      }
    }

    response.reset();
    response.setHeader("X-CKFinder-Error", String.valueOf(errorCode));
    switch (errorCode) {
      case Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST:
      case Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_NAME:
      case Constants.Errors.CKFINDER_CONNECTOR_ERROR_THUMBNAILS_DISABLED:
      case Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED:
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        break;
      case Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED:
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        break;
      default:
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        break;
    }
  }

}
