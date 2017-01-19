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
package com.github.zhanhb.ckfinder.connector.errors;

import com.github.zhanhb.ckfinder.connector.configuration.IConfiguration;
import com.github.zhanhb.ckfinder.connector.data.ResourceType;
import com.github.zhanhb.ckfinder.connector.handlers.response.Connector;
import com.github.zhanhb.ckfinder.connector.handlers.response.CurrentFolder;
import com.github.zhanhb.ckfinder.connector.handlers.response.Error;
import com.github.zhanhb.ckfinder.connector.utils.XMLCreator;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
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
    String currentFolder = connectorException.getCurrentFolder();
    String type = connectorException.getType();

    Map<String, ResourceType> types = configuration.getTypes();

    response.setContentType("text/xml;charset=UTF-8");
    response.setHeader("Cache-Control", "no-cache");
    int errorNum = connectorException.getErrorCode();

    if (type != null && !type.isEmpty()) {
      rootElement.resourceType(type);
    }
    if (currentFolder != null) {
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

}
