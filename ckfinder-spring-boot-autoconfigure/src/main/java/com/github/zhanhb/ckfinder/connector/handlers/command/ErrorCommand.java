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
import com.github.zhanhb.ckfinder.connector.handlers.arguments.ErrorArguments;
import com.github.zhanhb.ckfinder.connector.utils.PathUtils;
import java.io.IOException;
import java.util.regex.Pattern;
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
    super.initParams(arguments, request, configuration);
  }

  /**
   * for error command there should be no exception thrown because there are no
   * more exception handlers.
   *
   * @param reqParam request param
   * @param arguments
   * @return true if validation passed
   * @throws ConnectorException it should never throw an exception
   */
  @Override
  protected boolean isRequestPathValid(String reqParam, ErrorArguments arguments) throws ConnectorException {
    return reqParam == null || reqParam.isEmpty()
            || !Pattern.compile(Constants.INVALID_PATH_REGEX).matcher(reqParam).find();
  }

  @Override
  protected boolean isHidden(ErrorArguments arguments) {
    try {
      return super.isHidden(arguments);
    } catch (ConnectorException ex) {
      arguments.setConnectorException(ex);
      return true;
    }
  }

  @Override
  protected boolean isConnectorEnabled(ErrorArguments arguments) throws ConnectorException {
    try {
      return super.isConnectorEnabled(arguments);
    } catch (ConnectorException ex) {
      arguments.setConnectorException(ex);
      return false;
    }
  }

  @Override
  protected boolean isCurrFolderExists(ErrorArguments arguments, HttpServletRequest request)
          throws ConnectorException {
    try {
      return super.isCurrFolderExists(arguments, request);
    } catch (ConnectorException ex) {
      arguments.setConnectorException(ex);
      return false;
    }
  }

  @Override
  protected boolean isTypeExists(ErrorArguments arguments, String type) {
    ResourceType testType = getConfiguration().getTypes().get(type);
    if (testType == null) {
      arguments.setConnectorException(new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_TYPE, false));
      return false;
    }
    return true;
  }

  @Override
  protected void setCurrentFolderParam(HttpServletRequest request, ErrorArguments arguments) {
    String currFolder = request.getParameter("currentFolder");
    if (!(currFolder == null || currFolder.isEmpty())) {
      arguments.setCurrentFolder(PathUtils.addSlashToBeginning(PathUtils.addSlashToEnd(currFolder)));
    }
  }

}
