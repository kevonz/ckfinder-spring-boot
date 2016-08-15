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
  protected void createXMLChildNodes(int errorNum, Element rootElement) {
    if (getArguments().isAddRenameNode()) {
      createRenamedFileNode(rootElement);
    }
  }

  /**
   * create rename file XML node.
   *
   * @param rootElement XML root node
   */
  private void createRenamedFileNode(Element rootElement) {
    Element element = getArguments().getDocument().createElement("RenamedFile");
    element.setAttribute("name", getArguments().getFileName());
    if (getArguments().isRenamed()) {
      element.setAttribute("newName", getArguments().getNewFileName());
    }
    rootElement.appendChild(element);
  }

  /**
   * gets data for XML and checks all validation.
   *
   * @return error code or 0 if it's correct.
   * @throws java.io.IOException
   */
  @Override
  protected int getDataForXml() throws IOException {
    log.trace("getDataForXml");
    if (!isTypeExists(getArguments().getType())) {
      log.info("isTypeExists({}): false", getArguments().getType());
      getArguments().setType(null);
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_TYPE;
    }

    if (!getConfiguration().getAccessControl().checkFolderACL(getArguments().getType(),
            getArguments().getCurrentFolder(), getArguments().getUserRole(),
            AccessControl.CKFINDER_CONNECTOR_ACL_FILE_RENAME)) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED;
    }

    if (getConfiguration().isForceAscii()) {
      getArguments().setNewFileName(FileUtils.convertToASCII(getArguments().getNewFileName()));
    }

    if (getArguments().getFileName() != null && !getArguments().getFileName().isEmpty()
            && getArguments().getNewFileName() != null && !getArguments().getNewFileName().isEmpty()) {
      getArguments().setAddRenameNode(true);
    }

    int checkFileExt = FileUtils.checkFileExtension(getArguments().getNewFileName(),
            this.getConfiguration().getTypes().get(getArguments().getType()));
    if (checkFileExt == 1) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_EXTENSION;
    }
    if (getConfiguration().isCheckDoubleFileExtensions()) {
      getArguments().setNewFileName(FileUtils.renameFileWithBadExt(this.getConfiguration().getTypes().get(getArguments().getType()), getArguments().getNewFileName()));
    }

    if (!FileUtils.isFileNameInvalid(getArguments().getFileName())
            || FileUtils.isFileHidden(getArguments().getFileName(), getConfiguration())) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }

    if (!FileUtils.isFileNameInvalid(getArguments().getNewFileName(), getConfiguration())
            || FileUtils.isFileHidden(getArguments().getNewFileName(), getConfiguration())) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_NAME;
    }

    if (FileUtils.checkFileExtension(getArguments().getFileName(),
            this.getConfiguration().getTypes().get(getArguments().getType())) == 1) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }

    String dirPath = getConfiguration().getTypes().get(getArguments().getType()).getPath()
            + getArguments().getCurrentFolder();

    Path file = Paths.get(dirPath, getArguments().getFileName());
    Path newFile = Paths.get(dirPath, getArguments().getNewFileName());
    Path dir = Paths.get(dirPath);

    try {
      if (!Files.exists(file)) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_FILE_NOT_FOUND;
      }

      if (Files.exists(newFile)) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ALREADY_EXIST;
      }

      if (!Files.isWritable(dir) || !Files.isWritable(file)) {
        log.info("Not writable");
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED;
      }
      try {
        Files.move(file, newFile);
        getArguments().setRenamed(true);
        renameThumb();
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE;
      } catch (IOException ex) {
        getArguments().setRenamed(false);
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
  private void renameThumb() throws IOException {
    Path thumbFile = Paths.get(getConfiguration().getThumbsPath(),
            getArguments().getType() + getArguments().getCurrentFolder(),
            getArguments().getFileName());
    Path newThumbFile = Paths.get(getConfiguration().getThumbsPath(),
            getArguments().getType() + getArguments().getCurrentFolder(),
            getArguments().getNewFileName());

    try {
      Files.move(thumbFile, newThumbFile);
    } catch (IOException ex) {
    }
  }

  @Override
  protected void initParams(HttpServletRequest request, IConfiguration configuration)
          throws ConnectorException {
    super.initParams(request, configuration);
    getArguments().setFileName(request.getParameter("fileName"));
    getArguments().setNewFileName(request.getParameter("newFileName"));
  }

}
