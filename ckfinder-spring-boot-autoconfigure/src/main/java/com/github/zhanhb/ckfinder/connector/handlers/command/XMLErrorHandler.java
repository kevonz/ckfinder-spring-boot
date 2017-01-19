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
import com.github.zhanhb.ckfinder.connector.handlers.response.Connector;
import com.github.zhanhb.ckfinder.connector.handlers.response.CurrentFolder;
import com.github.zhanhb.ckfinder.connector.handlers.response.Error;
import com.github.zhanhb.ckfinder.connector.utils.FileUtils;
import com.github.zhanhb.ckfinder.connector.utils.PathUtils;
import com.github.zhanhb.ckfinder.connector.utils.XMLCreator;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Class to handle errors from commands returning XML response.
 */
public enum XMLErrorHandler {

  INSTANCE;

  public void handleException(HttpServletRequest request,
          HttpServletResponse response, IConfiguration configuration,
          ConnectorException connectorException) throws IOException {
    HttpSession session = request.getSession(false);
    String userRole = session == null ? null : (String) session.getAttribute(configuration.getUserRoleName());

    Connector.Builder rootElement = Connector.builder();
    String currentFolder = request.getParameter("currentFolder");
    if (currentFolder != null && !currentFolder.isEmpty()) {
      currentFolder = PathUtils.addSlashToBeginning(PathUtils.addSlashToEnd(currentFolder));
    } else {
      currentFolder = null;
    }

    Map<String, ResourceType> types = configuration.getTypes();
    String type = null;
    try {
      if (!configuration.isEnabled()) {
        throw new ConnectorException(Constants.Errors.CKFINDER_CONNECTOR_ERROR_CONNECTOR_DISABLED);
      } else if (currentFolder != null && !currentFolder.isEmpty()
              && Pattern.compile(Constants.INVALID_PATH_REGEX).matcher(currentFolder).find()) {
        throw new ConnectorException(Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_NAME);
      } else {
        currentFolder = PathUtils.escape(currentFolder);
        if (FileUtils.isDirectoryHidden(currentFolder, configuration)) {
          throw new ConnectorException(Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST);
        } else if (currentFolder == null || currentFolder.isEmpty()
                || isCurrFolderExists(types, connectorException, request, currentFolder)) {
          type = request.getParameter("type");
        }
      }
    } catch (ConnectorException ex) {
      connectorException = ex;
    }
    if (connectorException.isAddCurrentFolder()) {
      String tmpType = request.getParameter("type");

      ResourceType testType = types.get(tmpType);
      if (testType == null) {
        connectorException = new ConnectorException(Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_TYPE);
      } else {
        type = tmpType;
      }
    }

    response.setContentType("text/xml;charset=UTF-8");
    response.setHeader("Cache-Control", "no-cache");
    int errorNum = connectorException.getErrorCode();

    if (type != null && !type.isEmpty()) {
      rootElement.resourceType(type);
    }
    if (connectorException.isAddCurrentFolder()) {
      rootElement.currentFolder(CurrentFolder.builder()
              .path(currentFolder)
              .url(types.get(type).getUrl() + currentFolder)
              .acl(configuration.getAccessControl().checkACLForRole(type, currentFolder, userRole))
              .build());
    }
    rootElement.error(Error.builder()
            .number(errorNum)
            .value(connectorException.getMessage()).build());
    try (PrintWriter out = response.getWriter()) {
      XMLCreator.INSTANCE.writeTo(rootElement.build(), out);
    }
  }

  private boolean isCurrFolderExists(Map<String, ResourceType> types, ConnectorException connectorException, HttpServletRequest request, String currentFolder) throws ConnectorException {
    String tmpType = request.getParameter("type");
    if (tmpType != null) {
      ResourceType testType = types.get(tmpType);
      if (testType == null) {
        return false;
      }
      Path currDir = Paths.get(types.get(tmpType).getPath(),
              currentFolder);
      if (!Files.isDirectory(currDir)) {
        throw new ConnectorException(
                Constants.Errors.CKFINDER_CONNECTOR_ERROR_FOLDER_NOT_FOUND);
      } else {
        return true;
      }
    }
    return true;
  }

}
