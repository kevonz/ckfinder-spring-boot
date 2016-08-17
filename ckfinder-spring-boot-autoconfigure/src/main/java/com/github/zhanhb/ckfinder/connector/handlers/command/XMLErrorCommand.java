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
  protected void initParams(XMLErrorArguments arguments, HttpServletRequest request, IConfiguration configuration)
          throws ConnectorException {
    super.initParams(arguments, request, configuration);
    if (arguments.getConnectorException().isAddCurrentFolder()) {
      String tmpType = request.getParameter("type");
      if (isTypeExists(arguments, tmpType)) {
        arguments.setType(tmpType);
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

  @Override
  String getErrorMsg(XMLErrorArguments arguments) {
    return arguments.getConnectorException().getMessage();
  }

  /**
   * for error command there should be no exception throw because there is no
   * more exception handlers.
   *
   * @param reqParam request param
   * @param arguments
   * @return true if validation passed
   */
  @Override
  protected boolean isRequestPathValid(String reqParam, XMLErrorArguments arguments) {
    try {
      return super.isRequestPathValid(reqParam, arguments);
    } catch (ConnectorException ex) {
      return false;
    }
  }

  @Override
  protected boolean isConnectorEnabled(XMLErrorArguments arguments) {
    try {
      return super.isConnectorEnabled(arguments);
    } catch (ConnectorException ex) {
      arguments.setConnectorException(new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_CONNECTOR_DISABLED));
      return false;
    }
  }

  @Override
  protected boolean isHidden(XMLErrorArguments arguments) {
    try {
      return super.isHidden(arguments);
    } catch (ConnectorException ex) {
      arguments.setConnectorException(new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_CONNECTOR_DISABLED));
      return true;
    }
  }

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
  protected boolean isTypeExists(XMLErrorArguments arguments, String type) {
    boolean typeExists = super.isTypeExists(arguments, type);
    if (!typeExists) {
      arguments.setConnectorException(new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_TYPE, false));
    }
    return typeExists;
  }

  @Override
  protected boolean mustAddCurrentFolderNode(XMLErrorArguments arguments) {
    return arguments.getConnectorException().isAddCurrentFolder();
  }

  @Override
  protected void setCurrentFolderParam(HttpServletRequest request, XMLErrorArguments arguments) {
    String currFolder = request.getParameter("currentFolder");
    if (!(currFolder == null || currFolder.isEmpty())) {
      arguments.setCurrentFolder(PathUtils.addSlashToBeginning(PathUtils.addSlashToEnd(currFolder)));
    }
  }

}
