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
import com.github.zhanhb.ckfinder.connector.handlers.arguments.Arguments;
import java.util.Objects;
import lombok.Getter;

/**
 * Connector Exception.
 */
@Getter
public class ConnectorException extends Exception {

  /**
   *
   */
  private static final long serialVersionUID = -8643752550259111562L;
  private final int errorCode;
  private final String currentFolder;
  private String type;

  /**
   * standard constructor.
   *
   * @param arguments the arguments
   * @param errorCode error code number
   */
  public ConnectorException(Arguments arguments, int errorCode) {
    this.errorCode = errorCode;
    this.currentFolder = Objects.requireNonNull(arguments.getCurrentFolder());
    this.type = Objects.requireNonNull(arguments.getType());
  }

  /**
   * standard constructor.
   *
   * @param errorCode error code number
   */
  public ConnectorException(int errorCode) {
    super(null, null);
    this.errorCode = errorCode;
    this.currentFolder = null;
  }

  /**
   * constructor with error code and error message parameters.
   *
   * @param errorCode error code number
   * @param errorMsg error text message
   */
  public ConnectorException(int errorCode, String errorMsg) {
    super(errorMsg, null);
    this.errorCode = errorCode;
    this.currentFolder = null;
  }

  /**
   * constructor with error code and error message parameters.
   *
   * @param errorCode error code number
   * @param e exception
   */
  public ConnectorException(int errorCode, Exception e) {
    super(e.getMessage(), e);
    if (e instanceof ConnectorException) {
      throw new IllegalArgumentException();
    }
    this.errorCode = errorCode;
    this.currentFolder = null;
  }

  /**
   * constructor with exception param.
   *
   * @param cause Exception
   */
  public ConnectorException(Exception cause) {
    super(cause.getMessage(), cause);
    if (cause instanceof ConnectorException) {
      throw new IllegalArgumentException();
    }
    this.currentFolder = null;
    this.errorCode = Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNKNOWN;
  }

}
