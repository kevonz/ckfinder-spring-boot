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
import com.github.zhanhb.ckfinder.connector.handlers.arguments.RenameFileArguments;
import com.github.zhanhb.ckfinder.connector.utils.AccessControl;
import com.github.zhanhb.ckfinder.connector.utils.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Element;

/**
 * Class to handle <code>RenameFile</code> command.
 */
@Slf4j
public class RenameFileCommand extends XMLCommand<RenameFileArguments> implements IPostCommand {

  public RenameFileCommand() {
    super(RenameFileArguments::new);
  }

  @Override
  protected void createXMLChildNodes(int errorNum, Element rootElement, RenameFileArguments arguments) {
    if (arguments.isAddRenameNode()) {
      createRenamedFileNode(rootElement, arguments);
    }
  }

  /**
   * create rename file XML node.
   *
   * @param rootElement XML root node
   */
  private void createRenamedFileNode(Element rootElement, RenameFileArguments arguments) {
    Element element = arguments.getDocument().createElement("RenamedFile");
    element.setAttribute("name", arguments.getFileName());
    if (arguments.isRenamed()) {
      element.setAttribute("newName", arguments.getNewFileName());
    }
    rootElement.appendChild(element);
  }

  /**
   * gets data for XML and checks all validation.
   *
   * @param arguments
   * @return error code or 0 if it's correct.
   */
  @Override
  protected int getDataForXml(RenameFileArguments arguments) {
    log.trace("getDataForXml");

    try {
      checkTypeExists(arguments.getType());
    } catch (ConnectorException ex) {
      log.info("isTypeExists({}): false", arguments.getType());
      arguments.setType(null);
      return ex.getErrorCode();
    }

    if (!getConfiguration().getAccessControl().hasPermission(arguments.getType(),
            arguments.getCurrentFolder(), arguments.getUserRole(),
            AccessControl.CKFINDER_CONNECTOR_ACL_FILE_RENAME)) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED;
    }

    if (getConfiguration().isForceAscii()) {
      arguments.setNewFileName(FileUtils.convertToASCII(arguments.getNewFileName()));
    }

    if (arguments.getFileName() != null && !arguments.getFileName().isEmpty()
            && arguments.getNewFileName() != null && !arguments.getNewFileName().isEmpty()) {
      arguments.setAddRenameNode(true);
    }

    int checkFileExt = FileUtils.checkFileExtension(arguments.getNewFileName(),
            this.getConfiguration().getTypes().get(arguments.getType()));
    if (checkFileExt == 1) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_EXTENSION;
    }
    if (getConfiguration().isCheckDoubleFileExtensions()) {
      arguments.setNewFileName(FileUtils.renameFileWithBadExt(this.getConfiguration().getTypes().get(arguments.getType()),
              arguments.getNewFileName()));
    }

    if (!FileUtils.isFileNameInvalid(arguments.getFileName())
            || FileUtils.isFileHidden(arguments.getFileName(), getConfiguration())) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }

    if (!FileUtils.isFileNameInvalid(arguments.getNewFileName(), getConfiguration())
            || FileUtils.isFileHidden(arguments.getNewFileName(), getConfiguration())) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_NAME;
    }

    if (FileUtils.checkFileExtension(arguments.getFileName(),
            this.getConfiguration().getTypes().get(arguments.getType())) == 1) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }

    String dirPath = getConfiguration().getTypes().get(arguments.getType()).getPath();
    Path file = Paths.get(dirPath, arguments.getCurrentFolder(), arguments.getFileName());
    Path newFile = Paths.get(dirPath, arguments.getCurrentFolder(), arguments.getNewFileName());

    try {
      if (!Files.exists(file)) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_FILE_NOT_FOUND;
      }

      if (Files.exists(newFile)) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ALREADY_EXIST;
      }

      try {
        Files.move(file, newFile);
        arguments.setRenamed(true);
        renameThumb(arguments);
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE;
      } catch (IOException ex) {
        arguments.setRenamed(false);
        log.error("IOException", ex);
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED;
      }
    } catch (SecurityException e) {
      log.error("", e);
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED;
    }

  }

  /**
   * rename thumb file.
   */
  private void renameThumb(RenameFileArguments arguments) throws IOException {
    Path thumbFile = Paths.get(getConfiguration().getThumbsPath(),
            arguments.getType(), arguments.getCurrentFolder(),
            arguments.getFileName());
    Path newThumbFile = Paths.get(getConfiguration().getThumbsPath(),
            arguments.getType(), arguments.getCurrentFolder(),
            arguments.getNewFileName());

    try {
      Files.move(thumbFile, newThumbFile);
    } catch (IOException ex) {
    }
  }

  @Override
  protected void initParams(RenameFileArguments arguments, HttpServletRequest request, IConfiguration configuration)
          throws ConnectorException {
    super.initParams(arguments, request, configuration);
    if (getConfiguration().isEnableCsrfProtection() && !checkCsrfToken(request, null)) {
      throw new ConnectorException(Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST, "CSRF Attempt");
    }
    arguments.setFileName(request.getParameter("fileName"));
    arguments.setNewFileName(request.getParameter("newFileName"));
  }

}
