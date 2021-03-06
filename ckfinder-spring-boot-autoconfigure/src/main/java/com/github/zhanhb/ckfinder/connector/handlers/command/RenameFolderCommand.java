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
import com.github.zhanhb.ckfinder.connector.handlers.arguments.RenameFolderArguments;
import com.github.zhanhb.ckfinder.connector.handlers.response.Connector;
import com.github.zhanhb.ckfinder.connector.handlers.response.RenamedFolder;
import com.github.zhanhb.ckfinder.connector.utils.AccessControl;
import com.github.zhanhb.ckfinder.connector.utils.FileUtils;
import com.github.zhanhb.ckfinder.connector.utils.PathUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Class to handle <code>RenameFolder</code> command.
 */
@Slf4j
public class RenameFolderCommand extends XMLCommand<RenameFolderArguments> implements IPostCommand {

  public RenameFolderCommand() {
    super(RenameFolderArguments::new);
  }

  @Override
  protected void createXMLChildNodes(int errorNum, Connector.Builder rootElement, RenameFolderArguments arguments, IConfiguration configuration) {
    if (errorNum == Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE) {
      createRenamedFolderNode(rootElement, arguments, configuration);
    }

  }

  /**
   * creates XML node for renamed folder.
   *
   * @param rootElement XML root element.
   */
  private void createRenamedFolderNode(Connector.Builder rootElement, RenameFolderArguments arguments, IConfiguration configuration) {
    rootElement.renamedFolder(RenamedFolder.builder()
            .newName(arguments.getNewFolderName())
            .newPath(arguments.getNewFolderPath())
            .newUrl(configuration.getTypes().get(arguments.getType()).getUrl() + arguments.getNewFolderPath())
            .build());
  }

  @Override
  protected int getDataForXml(RenameFolderArguments arguments, IConfiguration configuration) {

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
            arguments.getCurrentFolder(),
            arguments.getUserRole(),
            AccessControl.CKFINDER_CONNECTOR_ACL_FOLDER_RENAME)) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED;
    }

    if (configuration.isForceAscii()) {
      arguments.setNewFolderName(FileUtils.convertToASCII(arguments.getNewFolderName()));
    }

    if (FileUtils.isDirectoryHidden(arguments.getNewFolderName(), configuration)
            || !FileUtils.isFolderNameInvalid(arguments.getNewFolderName(), configuration)) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_NAME;
    }

    if (arguments.getCurrentFolder().equals("/")) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }

    Path dir = Paths.get(configuration.getTypes().get(arguments.getType()).getPath(),
            arguments.getCurrentFolder());
    try {
      if (!Files.isDirectory(dir)) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
      }
      setNewFolder(arguments);
      Path newDir = Paths.get(configuration.getTypes().get(arguments.getType()).getPath(),
              arguments.getNewFolderPath());
      if (Files.exists(newDir)) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ALREADY_EXIST;
      }
      try {
        Files.move(dir, newDir);
        renameThumb(arguments, configuration);
      } catch (IOException ex) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED;
      }
    } catch (SecurityException e) {
      log.error("", e);
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED;
    }

    return Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE;
  }

  /**
   * renames thumb folder.
   */
  private void renameThumb(RenameFolderArguments arguments, IConfiguration configuration) throws IOException {
    Path thumbDir = Paths.get(configuration.getThumbsPath(),
            arguments.getType(), arguments.getCurrentFolder());
    Path newThumbDir = Paths.get(configuration.getThumbsPath(),
            arguments.getType(), arguments.getNewFolderPath());
    try {
      Files.move(thumbDir, newThumbDir);
    } catch (IOException ex) {
    }
  }

  /**
   * sets new folder name.
   */
  private void setNewFolder(RenameFolderArguments arguments) {
    String tmp1 = arguments.getCurrentFolder().substring(0,
            arguments.getCurrentFolder().lastIndexOf('/'));
    arguments.setNewFolderPath(tmp1.substring(0,
            tmp1.lastIndexOf('/') + 1).concat(arguments.getNewFolderName()));
    arguments.setNewFolderPath(PathUtils.addSlashToEnd(arguments.getNewFolderPath()));
  }

  /**
   * @param arguments
   * @param request request
   * @param configuration connector conf
   * @throws ConnectorException when error occurs.
   */
  @Override
  protected void initParams(RenameFolderArguments arguments, HttpServletRequest request, IConfiguration configuration) throws ConnectorException {
    super.initParams(arguments, request, configuration);
    if (configuration.isEnableCsrfProtection() && !checkCsrfToken(request)) {
      throw new ConnectorException(Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST, "CSRF Attempt");
    }
    arguments.setNewFolderName(request.getParameter("NewFolderName"));
  }

}
