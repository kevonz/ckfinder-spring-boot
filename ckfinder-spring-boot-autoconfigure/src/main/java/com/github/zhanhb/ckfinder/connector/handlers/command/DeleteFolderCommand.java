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
import com.github.zhanhb.ckfinder.connector.handlers.arguments.XMLArguments;
import com.github.zhanhb.ckfinder.connector.utils.AccessControl;
import com.github.zhanhb.ckfinder.connector.utils.FileUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Element;

/**
 * Class to handle <code>DeleteFolder</code> command.
 */
@Slf4j
public class DeleteFolderCommand extends XMLCommand<XMLArguments> implements IPostCommand {

  public DeleteFolderCommand() {
    super(XMLArguments::new);
  }

  @Override
  public void initParams(XMLArguments arguments, HttpServletRequest request, IConfiguration configuration)
          throws ConnectorException {
    super.initParams(arguments, request, configuration);
    if (getConfiguration().isEnableCsrfProtection() && !checkCsrfToken(request, null)) {
      throw new ConnectorException(Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST, "CSRF Attempt");
    }
  }

  @Override
  protected void createXMLChildNodes(int errorNum, Element rootElement, XMLArguments arguments) {
  }

  /**
   * @param arguments
   * @return error code or 0 if ok. Deletes folder and thumb folder.
   */
  @Override
  protected int getDataForXml(XMLArguments arguments) {
    try {
      checkTypeExists(arguments.getType());
    } catch (ConnectorException ex) {
      arguments.setType(null);
      return ex.getErrorCode();
    }

    if (!getConfiguration().getAccessControl().hasPermission(arguments.getType(),
            arguments.getCurrentFolder(),
            arguments.getUserRole(),
            AccessControl.CKFINDER_CONNECTOR_ACL_FOLDER_DELETE)) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED;
    }
    if (arguments.getCurrentFolder().equals("/")) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }

    if (FileUtils.isDirectoryHidden(arguments.getCurrentFolder(), getConfiguration())) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }

    Path dir = Paths.get(getConfiguration().getTypes().get(arguments.getType()).getPath(),
            arguments.getCurrentFolder());

    try {
      if (!Files.isDirectory(dir)) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_FOLDER_NOT_FOUND;
      }

      if (FileUtils.delete(dir)) {
        Path thumbDir = Paths.get(getConfiguration().getThumbsPath(),
                arguments.getType(), arguments.getCurrentFolder());
        FileUtils.delete(thumbDir);
      } else {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED;
      }
    } catch (SecurityException e) {
      log.error("", e);
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED;
    }

    return Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE;
  }

}
