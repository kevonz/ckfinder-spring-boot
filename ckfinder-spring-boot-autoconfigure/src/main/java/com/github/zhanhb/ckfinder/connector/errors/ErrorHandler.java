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
package com.github.zhanhb.ckfinder.connector.errors;

import com.github.zhanhb.ckfinder.connector.configuration.Constants;
import com.github.zhanhb.ckfinder.connector.configuration.IConfiguration;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Class to handle errors via HTTP headers (for non-XML commands).
 */
public enum ErrorHandler {

  INSTANCE;

  public void handleException(HttpServletRequest request,
          HttpServletResponse response, IConfiguration configuration,
          ConnectorException connectorException) throws IOException {
    int errorCode = connectorException.getErrorCode();
    response.reset();
    response.setHeader("X-CKFinder-Error", String.valueOf(errorCode));
    switch (errorCode) {
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
  }

}
