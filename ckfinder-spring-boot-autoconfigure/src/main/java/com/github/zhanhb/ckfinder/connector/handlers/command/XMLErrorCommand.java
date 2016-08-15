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
import com.github.zhanhb.ckfinder.connector.handlers.arguments.XMLErrorArguments;
import com.github.zhanhb.ckfinder.connector.utils.FileUtils;
import com.github.zhanhb.ckfinder.connector.utils.PathUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
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
   * @throws ConnectorException it should never throw an exception
   */
  @Override
  protected boolean isRequestPathValid(String reqParam, XMLErrorArguments arguments)
          throws ConnectorException {
    return reqParam == null || reqParam.isEmpty()
            || !Pattern.compile(Constants.INVALID_PATH_REGEX).matcher(reqParam).find();
  }

  @Override
  protected boolean isConnectorEnabled(XMLErrorArguments arguments)
          throws ConnectorException {
    if (!getConfiguration().isEnabled()) {
      arguments.setConnectorException(new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_CONNECTOR_DISABLED));
      return false;
    }
    return true;
  }

  @Override
  protected boolean isHidden(XMLErrorArguments arguments) throws ConnectorException {
    if (FileUtils.isDirectoryHidden(arguments.getCurrentFolder(), getConfiguration())) {
      arguments.setConnectorException(new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_CONNECTOR_DISABLED));
      return true;
    }
    return false;
  }

  @Override
  protected boolean isCurrFolderExists(XMLErrorArguments arguments, HttpServletRequest request)
          throws ConnectorException {
    String tmpType = request.getParameter("type");
    if (isTypeExists(arguments, tmpType)) {
      Path currDir = Paths.get(getConfiguration().getTypes().get(tmpType).getPath()
              + arguments.getCurrentFolder());
      if (Files.exists(currDir) && Files.isDirectory(currDir)) {
        return true;
      } else {
        arguments.setConnectorException(new ConnectorException(
                Constants.Errors.CKFINDER_CONNECTOR_ERROR_FOLDER_NOT_FOUND));
        return false;
      }
    }
    return false;
  }

  @Override
  protected boolean isTypeExists(XMLErrorArguments arguments, String type) {
    ResourceType testType = getConfiguration().getTypes().get(type);
    if (testType == null) {
      arguments.setConnectorException(new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_TYPE, false));
      return false;
    }
    return true;
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
