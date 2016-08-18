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
import com.github.zhanhb.ckfinder.connector.handlers.arguments.ErrorArguments;
import com.github.zhanhb.ckfinder.connector.utils.PathUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Class to handle errors via HTTP headers (for non-XML commands).
 */
public class ErrorCommand extends Command<ErrorArguments> {

  private static final ErrorCommand INSTANCE = new ErrorCommand();

  public static ErrorCommand getInstance() {
    return INSTANCE;
  }

  private ErrorCommand() {
    super(ErrorArguments::new);
  }

  @Override
  @SuppressWarnings("FinalMethod")
  final void execute(ErrorArguments arguments, HttpServletResponse response) throws ConnectorException {
    try {
      response.setHeader("X-CKFinder-Error", String.valueOf(arguments.getConnectorException().getErrorCode()));
      switch (arguments.getConnectorException().getErrorCode()) {
        case Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST:
        case Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_NAME:
        case Constants.Errors.CKFINDER_CONNECTOR_ERROR_THUMBNAILS_DISABLED:
        case Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED:
          response.sendError(HttpServletResponse.SC_FORBIDDEN);
          break;
        case Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED:
          response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
          break;
        default:
          response.sendError(HttpServletResponse.SC_NOT_FOUND);
          break;
      }
    } catch (IOException ex) {
      throw new ConnectorException(ex);
    }
  }

  @Override
  void setResponseHeader(HttpServletRequest request, HttpServletResponse response, ErrorArguments arguments) {
    response.reset();
  }

  @Override
  protected void initParams(ErrorArguments arguments, HttpServletRequest request, IConfiguration configuration)
          throws ConnectorException {
    try {
      super.initParams(arguments, request, configuration);
    } catch (ConnectorException ex) {
      if (ex.getErrorCode() != Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_NAME) {
        arguments.setConnectorException(ex);
      }
    }
  }

  @Deprecated
  @Override
  protected boolean isCurrFolderExists(ErrorArguments arguments, HttpServletRequest request)
          throws ConnectorException {
    String tmpType = request.getParameter("type");
    try {
      checkTypeExists(tmpType);
    } catch (ConnectorException ex) {
      arguments.setConnectorException(new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_TYPE, false));
      return false;
    }
    Path currDir = Paths.get(getConfiguration().getTypes().get(tmpType).getPath(),
            arguments.getCurrentFolder());
    if (Files.isDirectory(currDir)) {
      return true;
    } else {
      arguments.setConnectorException(new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_FOLDER_NOT_FOUND));
      return false;
    }
  }

  @Deprecated
  @Override
  String getCurrentFolderParam(HttpServletRequest request) {
    String currFolder = request.getParameter("currentFolder");
    if (currFolder != null && !currFolder.isEmpty()) {
      return PathUtils.addSlashToBeginning(PathUtils.addSlashToEnd(currFolder));
    }
    return null;
  }

}
