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
import com.github.zhanhb.ckfinder.connector.data.FilePostParam;
import com.github.zhanhb.ckfinder.connector.errors.ConnectorException;
import com.github.zhanhb.ckfinder.connector.handlers.arguments.DeleteFilesArguments;
import com.github.zhanhb.ckfinder.connector.utils.AccessControl;
import com.github.zhanhb.ckfinder.connector.utils.FileUtils;
import com.github.zhanhb.ckfinder.connector.utils.XMLCreator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Element;

/**
 * Class used to handle <code>DeleteFiles</code> command.
 */
@Slf4j
public class DeleteFilesCommand extends XMLCommand<DeleteFilesArguments> implements IPostCommand {

  public DeleteFilesCommand() {
    super(DeleteFilesArguments::new);
  }

  @Override
  protected void createXMLChildNodes(int errorNum, Element rootElement, DeleteFilesArguments arguments) {
    if (XMLCreator.INSTANCE.hasErrors(arguments)) {
      Element errorsNode = arguments.getDocument().createElement("Errors");
      XMLCreator.INSTANCE.addErrors(arguments, errorsNode);
      rootElement.appendChild(errorsNode);
    }

    if (arguments.isAddDeleteNode()) {
      createDeleteFielsNode(rootElement, arguments);
    }
  }

  /**
   * Adds file deletion node in XML response.
   *
   * @param rootElement root element in XML response
   */
  private void createDeleteFielsNode(Element rootElement, DeleteFilesArguments arguments) {
    Element element = arguments.getDocument().createElement("DeleteFiles");
    element.setAttribute("deleted", String.valueOf(arguments.getFilesDeleted()));
    rootElement.appendChild(element);
  }

  /**
   * Prepares data for XML response.
   *
   * @param arguments
   * @return error code or 0 if action ended with success.
   */
  @Override
  protected int getDataForXml(DeleteFilesArguments arguments) {

    arguments.setFilesDeleted(0);

    arguments.setAddDeleteNode(false);

    try {
      checkTypeExists(arguments.getType());
    } catch (ConnectorException ex) {
      arguments.setType(null);
      return ex.getErrorCode();
    }

    for (FilePostParam fileItem : arguments.getFiles()) {
      if (!FileUtils.isFileNameInvalid(fileItem.getName())) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
      }

      if (getConfiguration().getTypes().get(fileItem.getType()) == null) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
      }

      if (fileItem.getFolder() == null || fileItem.getFolder().isEmpty()
              || Pattern.compile(Constants.INVALID_PATH_REGEX).matcher(
              fileItem.getFolder()).find()) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
      }

      if (FileUtils.isDirectoryHidden(fileItem.getFolder(), this.getConfiguration())) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
      }

      if (FileUtils.isFileHidden(fileItem.getName(), this.getConfiguration())) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
      }

      if (FileUtils.checkFileExtension(fileItem.getName(), this.getConfiguration().getTypes().get(fileItem.getType())) == 1) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;

      }

      if (!getConfiguration().getAccessControl().hasPermission(fileItem.getType(), fileItem.getFolder(), arguments.getUserRole(),
              AccessControl.CKFINDER_CONNECTOR_ACL_FILE_DELETE)) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED;
      }

      Path file = Paths.get(getConfiguration().getTypes().get(fileItem.getType()).getPath(), fileItem.getFolder(), fileItem.getName());

      try {
        arguments.setAddDeleteNode(true);
        if (!Files.exists(file)) {
          XMLCreator.INSTANCE.appendErrorNodeChild(arguments, Constants.Errors.CKFINDER_CONNECTOR_ERROR_FILE_NOT_FOUND,
                  fileItem.getName(), fileItem.getFolder(), fileItem.getType());
          continue;
        }

        log.debug("prepare delete file '{}'", file);
        if (FileUtils.delete(file)) {
          Path thumbFile = Paths.get(getConfiguration().getThumbsPath(),
                  fileItem.getType(), arguments.getCurrentFolder(), fileItem.getName());
          arguments.filesDeletedPlus();

          try {
            log.debug("prepare delete thumb file '{}'", thumbFile);
            FileUtils.delete(thumbFile);
          } catch (Exception ignore) {
            log.debug("delete thumb file '{}' failed", thumbFile);
            // No errors if we are not able to delete the thumb.
          }
        } else { //If access is denied, report error and try to delete rest of files.
          XMLCreator.INSTANCE.appendErrorNodeChild(arguments, Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED,
                  fileItem.getName(), fileItem.getFolder(), fileItem.getType());
        }
      } catch (SecurityException e) {
        log.error("", e);
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED;

      }
    }
    if (XMLCreator.INSTANCE.hasErrors(arguments)) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_DELETE_FAILED;
    } else {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE;
    }
  }

  /**
   * Initializes parameters for command handler.
   *
   * @param arguments
   * @param request current response object
   * @param configuration connector configuration object
   * @throws ConnectorException when initialization parameters can't be loaded
   * for command handler.
   */
  @Override
  @SuppressWarnings("CollectionWithoutInitialCapacity")
  protected void initParams(DeleteFilesArguments arguments, HttpServletRequest request, IConfiguration configuration) throws ConnectorException {
    super.initParams(arguments, request, configuration);
    arguments.setFiles(new ArrayList<>());
    RequestFileHelper.addFilesListFromRequest(request, arguments.getFiles());
  }

}
