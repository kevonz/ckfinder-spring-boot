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
import com.github.zhanhb.ckfinder.connector.handlers.arguments.CreateFolderArguments;
import com.github.zhanhb.ckfinder.connector.handlers.response.Connector;
import com.github.zhanhb.ckfinder.connector.handlers.response.NewFolder;
import com.github.zhanhb.ckfinder.connector.utils.AccessControl;
import com.github.zhanhb.ckfinder.connector.utils.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Class to handle <code>CreateFolder</code> command. Create subfolder.
 */
@Slf4j
public class CreateFolderCommand extends XMLCommand<CreateFolderArguments> implements IPostCommand {

  public CreateFolderCommand() {
    super(CreateFolderArguments::new);
  }

  @Override
  protected void createXMLChildNodes(int errorNum, Connector.Builder rootElement, CreateFolderArguments arguments, IConfiguration configuration) {
    if (errorNum == Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE) {
      createNewFolderElement(rootElement, arguments);
    }
  }

  /**
   * creates current folder XML node.
   *
   * @param rootElement XML root element.
   */
  private void createNewFolderElement(Connector.Builder rootElement, CreateFolderArguments arguments) {
    rootElement.newFolder(NewFolder.builder()
            .name(arguments.getNewFolderName())
            .build());
  }

  /**
   * gets data for xml. Not used in this handler.
   *
   * @param arguments
   * @param configuration connector configuration
   * @return returns 0 if success
   */
  @Override
  protected int getDataForXml(CreateFolderArguments arguments, IConfiguration configuration) {
    try {
      checkRequestPathValid(arguments.getNewFolderName());
    } catch (ConnectorException e) {
      return e.getErrorCode();
    }

    try {
      checkTypeExists(arguments.getType(), configuration);
    } catch (ConnectorException ex) {
      arguments.setType(null);
      return ex.getErrorCode();
    }

    if (!configuration.getAccessControl().hasPermission(arguments.getType(),
            arguments.getCurrentFolder(), arguments.getUserRole(),
            AccessControl.CKFINDER_CONNECTOR_ACL_FOLDER_CREATE)) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED;
    }

    if (configuration.isForceAscii()) {
      arguments.setNewFolderName(FileUtils.convertToASCII(arguments.getNewFolderName()));
    }

    if (!FileUtils.isFolderNameInvalid(arguments.getNewFolderName(), configuration)) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_NAME;
    }
    if (FileUtils.isDirectoryHidden(arguments.getCurrentFolder(), configuration)) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }
    if (FileUtils.isDirectoryHidden(arguments.getNewFolderName(), configuration)) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_NAME;
    }

    try {
      if (createFolder(arguments, configuration)) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE;
      } else {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED;
      }

    } catch (SecurityException e) {
      log.error("", e);
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED;
    } catch (ConnectorException e) {
      return e.getErrorCode();
    }
  }

  /**
   * creates folder. throws Exception when security problem occurs or folder
   * already exists
   *
   * @return true if folder is created correctly
   * @throws ConnectorException when error occurs or dir exists
   */
  private boolean createFolder(CreateFolderArguments arguments, IConfiguration configuration) throws ConnectorException {
    Path dir = Paths.get(configuration.getTypes().get(arguments.getType()).getPath(),
            arguments.getCurrentFolder(), arguments.getNewFolderName());
    if (Files.exists(dir)) {
      arguments.throwException(Constants.Errors.CKFINDER_CONNECTOR_ERROR_ALREADY_EXIST);
    }
    try {
      Files.createDirectories(dir);
      return true;
    } catch (IOException ex) {
      return false;
    }
  }

  @Override
  protected void initParams(CreateFolderArguments arguments, HttpServletRequest request, IConfiguration configuration)
          throws ConnectorException {
    super.initParams(arguments, request, configuration);
    if (configuration.isEnableCsrfProtection() && !checkCsrfToken(request)) {
      throw new ConnectorException(Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST, "CSRF Attempt");
    }
    arguments.setNewFolderName(request.getParameter("NewFolderName"));
  }

}
