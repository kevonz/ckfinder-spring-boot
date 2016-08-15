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
import com.github.zhanhb.ckfinder.connector.utils.AccessControl;
import com.github.zhanhb.ckfinder.connector.utils.FileUtils;
import com.github.zhanhb.ckfinder.connector.utils.PathUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Element;

/**
 * Class to handle <code>RenameFolder</code> command.
 */
@Slf4j
public class RenameFolderCommand extends XMLCommand<RenameFolderArguments> implements IPostCommand {

  public RenameFolderCommand() {
    super(RenameFolderArguments::new);
  }

  @Override
  protected void createXMLChildNodes(int errorNum, Element rootElement, RenameFolderArguments arguments) {
    if (errorNum == Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE) {
      createRenamedFolderNode(rootElement, arguments);
    }

  }

  /**
   * creates XML node for renamed folder.
   *
   * @param rootElement XML root element.
   */
  private void createRenamedFolderNode(Element rootElement, RenameFolderArguments arguments) {
    Element element = arguments.getDocument().createElement("RenamedFolder");
    element.setAttribute("newName", arguments.getNewFolderName());
    element.setAttribute("newPath", arguments.getNewFolderPath());
    element.setAttribute("newUrl", getConfiguration().getTypes().get(arguments.getType()).getUrl() + arguments.getNewFolderPath());
    rootElement.appendChild(element);

  }

  @Override
  protected int getDataForXml(RenameFolderArguments arguments) throws IOException {

    try {
      isRequestPathValid(arguments.getNewFolderName(), arguments);
    } catch (ConnectorException e) {
      return e.getErrorCode();
    }

    if (!isTypeExists(arguments, arguments.getType())) {
      arguments.setType(null);
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_TYPE;
    }

    if (!getConfiguration().getAccessControl().checkFolderACL(arguments.getType(),
            arguments.getCurrentFolder(),
            arguments.getUserRole(),
            AccessControl.CKFINDER_CONNECTOR_ACL_FOLDER_RENAME)) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED;
    }

    if (getConfiguration().isForceAscii()) {
      arguments.setNewFolderName(FileUtils.convertToASCII(arguments.getNewFolderName()));
    }

    if (FileUtils.isDirectoryHidden(arguments.getNewFolderName(), getConfiguration())
            || !FileUtils.isFolderNameInvalid(arguments.getNewFolderName(), getConfiguration())) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_NAME;
    }

    if (arguments.getCurrentFolder().equals("/")) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }

    Path dir = Paths.get(getConfiguration().getTypes().get(arguments.getType()).getPath()
            + arguments.getCurrentFolder());
    try {
      if (!Files.isDirectory(dir)) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
      }
      setNewFolder(arguments);
      Path newDir = Paths.get(getConfiguration().getTypes().get(arguments.getType()).getPath()
              + arguments.getNewFolderPath());
      if (Files.exists(newDir)) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ALREADY_EXIST;
      }
      try {
        Files.move(dir, newDir);
        renameThumb(arguments);
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
  private void renameThumb(RenameFolderArguments arguments) throws IOException {
    Path thumbDir = Paths.get(getConfiguration().getThumbsPath(),
            arguments.getType()
            + arguments.getCurrentFolder());
    Path newThumbDir = Paths.get(getConfiguration().getThumbsPath(),
            arguments.getType()
            + arguments.getNewFolderPath());
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
    arguments.setNewFolderName(request.getParameter("NewFolderName"));
  }

}
