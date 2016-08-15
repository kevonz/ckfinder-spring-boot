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
  protected void createXMLChildNodes(int errorNum, Element rootElement) {
    if (hasErrors()) {
      Element errorsNode = getArguments().getDocument().createElement("Errors");
      addErrors(errorsNode);
      rootElement.appendChild(errorsNode);
    }

    if (getArguments().isAddDeleteNode()) {
      createDeleteFielsNode(rootElement);
    }
  }

  /**
   * Adds file deletion node in XML response.
   *
   * @param rootElement root element in XML response
   */
  private void createDeleteFielsNode(Element rootElement) {
    Element element = getArguments().getDocument().createElement("DeleteFiles");
    element.setAttribute("deleted", String.valueOf(getArguments().getFilesDeleted()));
    rootElement.appendChild(element);
  }

  /**
   * Prepares data for XML response.
   *
   * @return error code or 0 if action ended with success.
   */
  @Override
  protected int getDataForXml() {

    getArguments().setFilesDeleted(0);

    getArguments().setAddDeleteNode(false);

    if (!isTypeExists(getArguments().getType())) {
      getArguments().setType(null);
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_TYPE;
    }

    for (FilePostParam fileItem : getArguments().getFiles()) {
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

      if (!getConfiguration().getAccessControl().checkFolderACL(fileItem.getType(), fileItem.getFolder(), getArguments().getUserRole(),
              AccessControl.CKFINDER_CONNECTOR_ACL_FILE_DELETE)) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED;
      }

      Path file = Paths.get(getConfiguration().getTypes().get(fileItem.getType()).getPath() + fileItem.getFolder(), fileItem.getName());

      try {
        getArguments().setAddDeleteNode(true);
        if (!Files.exists(file)) {
          appendErrorNodeChild(
                  Constants.Errors.CKFINDER_CONNECTOR_ERROR_FILE_NOT_FOUND,
                  fileItem.getName(), fileItem.getFolder(), fileItem.getType());
          continue;
        }

        if (FileUtils.delete(file)) {
          Path thumbFile = Paths.get(getConfiguration().getThumbsPath(),
                  fileItem.getType() + getArguments().getCurrentFolder(), fileItem.getName());
          getArguments().filesDeletedPlus();

          try {
            FileUtils.delete(thumbFile);
          } catch (Exception exp) {
            // No errors if we are not able to delete the thumb.
          }
        } else { //If access is denied, report error and try to delete rest of files.
          appendErrorNodeChild(
                  Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED,
                  fileItem.getName(), fileItem.getFolder(), fileItem.getType());
        }
      } catch (SecurityException e) {
        log.error("", e);
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED;

      }
    }
    if (hasErrors()) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_DELETE_FAILED;
    } else {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE;
    }
  }

  /**
   * Initializes parameters for command handler.
   *
   * @param request current response object
   * @param configuration connector configuration object
   * @throws ConnectorException when initialization parameters can't be loaded
   * for command handler.
   */
  @Override
  @SuppressWarnings("CollectionWithoutInitialCapacity")
  protected void initParams(HttpServletRequest request, IConfiguration configuration) throws ConnectorException {
    super.initParams(request, configuration);
    getArguments().setFiles(new ArrayList<>());
    getFilesListFromRequest(request);
  }

  /**
   * Gets list of files from request.
   *
   * @param request current request object
   */
  @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
  private void getFilesListFromRequest(HttpServletRequest request) {
    int i = 0;
    String paramName = "files[" + i + "][name]";
    while (request.getParameter(paramName) != null) {
      String name = request.getParameter(paramName);
      String folder = request.getParameter("files[" + i + "][folder]");
      String options = request.getParameter("files[" + i + "][options]");
      String type = request.getParameter("files[" + i + "][type]");
      getArguments().getFiles().add(FilePostParam.builder().name(name).folder(folder).options(options).type(type).build());
      paramName = "files[" + (++i) + "][name]";
    }
  }

}
