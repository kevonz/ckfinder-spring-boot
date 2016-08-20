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
import com.github.zhanhb.ckfinder.connector.handlers.arguments.XMLErrorArguments;
import com.github.zhanhb.ckfinder.connector.utils.PathUtils;
import javax.servlet.http.HttpServletRequest;
import org.w3c.dom.Element;

/**
 * Class to handle errors from commands returning XML response.
 */
public class XMLErrorCommand extends XMLCommand<XMLErrorArguments> {

  private static final XMLErrorCommand INSTANCE = new XMLErrorCommand();

  public static XMLErrorCommand getInstance() {
    return INSTANCE;
  }

  private XMLErrorCommand() {
    super(XMLErrorArguments::new);
  }

  @Override
  @SuppressWarnings("FinalMethod")
  protected final void initParams(XMLErrorArguments arguments, HttpServletRequest request, IConfiguration configuration)
          throws ConnectorException {
    try {
      super.initParams(arguments, request, configuration);
    } catch (ConnectorException ex) {
      arguments.setConnectorException(new ConnectorException(ex.getErrorCode()));
    }
    if (arguments.getConnectorException().isAddCurrentFolder()) {
      String tmpType = request.getParameter("type");
      try {
        checkTypeExists(tmpType);
        arguments.setType(tmpType);
      } catch (ConnectorException ex) {
        arguments.setConnectorException(new ConnectorException(ex.getErrorCode()));
      }
    }
  }

  @Override
  protected int getDataForXml(XMLErrorArguments arguments) {
    return arguments.getConnectorException().getErrorCode();
  }

  @Override
  protected void createXMLChildNodes(int errorNum, Element rootElement, XMLErrorArguments arguments) {
  }

  @Deprecated
  @Override
  String getErrorMsg(XMLErrorArguments arguments) {
    return arguments.getConnectorException().getMessage();
  }

  @Deprecated
  @Override
  protected boolean isCurrFolderExists(XMLErrorArguments arguments, HttpServletRequest request) {
    try {
      return super.isCurrFolderExists(arguments, request);
    } catch (ConnectorException ex) {
      arguments.setConnectorException(new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_FOLDER_NOT_FOUND));
      return false;
    }
  }

  @Override
  protected boolean shouldAddCurrentFolderNode(XMLErrorArguments arguments) {
    return arguments.getConnectorException().isAddCurrentFolder();
  }

  @Deprecated
  @Override
  String getCurrentFolderParam(HttpServletRequest request) {
    String currFolder = request.getParameter("currentFolder");
    if (currFolder != null && !currFolder.isEmpty()) {
      return PathUtils.addSlashToBeginning(PathUtils.addSlashToEnd(currFolder));
    }
    return null;
  }

}
