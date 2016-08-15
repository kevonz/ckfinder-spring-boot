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
import com.github.zhanhb.ckfinder.connector.handlers.arguments.Arguments;
import com.github.zhanhb.ckfinder.connector.utils.FileUtils;
import com.github.zhanhb.ckfinder.connector.utils.PathUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for all command handlers.
 *
 * @param <T> arguments type
 */
@Slf4j
public abstract class Command<T extends Arguments> {

  /**
   * Connector configuration.
   */
  @Getter(AccessLevel.PROTECTED)
  private IConfiguration configuration;
  private final ThreadLocal<T> arguments;

  protected Command(Supplier<? extends T> argumentsSupplier) {
    this.arguments = ThreadLocal.withInitial(argumentsSupplier);
  }

  public T getArguments() {
    return arguments.get();
  }

  /**
   * Runs command. Initialize, sets response and execute command.
   *
   * @param request request
   * @param response response
   * @param configuration connector configuration
   * @throws ConnectorException when error occurred.
   */
  public void runCommand(HttpServletRequest request, HttpServletResponse response,
          IConfiguration configuration) throws ConnectorException {
    this.initParams(request, configuration);
    try {
      setResponseHeader(request, response);
      execute(response);
    } catch (IOException e) {
      throw new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED, e);
    }
  }

  /**
   * initialize params for command handler.
   *
   * @param request request
   * @param configuration connector configuration
   * @throws ConnectorException to handle in error handler.
   */
  protected void initParams(HttpServletRequest request, IConfiguration configuration)
          throws ConnectorException {
    if (configuration != null) {
      this.configuration = configuration;
      HttpSession session = request.getSession(false);
      String userRole = session == null ? null : (String) session.getAttribute(configuration.getUserRoleName());
      getArguments().setUserRole(userRole);

      getCurrentFolderParam(request);

      if (isConnectorEnabled() && isRequestPathValid(getArguments().getCurrentFolder())) {
        getArguments().setCurrentFolder(PathUtils.escape(getArguments().getCurrentFolder()));
        if (!isHidden()) {
          if ((getArguments().getCurrentFolder() == null || getArguments().getCurrentFolder().isEmpty())
                  || isCurrFolderExists(request)) {
            getArguments().setType(request.getParameter("type"));
          }
        }
      }
    }
  }

  /**
   * check if connector is enabled and checks authentication.
   *
   * @return true if connector is enabled and user is authenticated
   * @throws ConnectorException when connector is disabled
   */
  protected boolean isConnectorEnabled() throws ConnectorException {
    if (!getConfiguration().isEnabled()) {
      throw new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_CONNECTOR_DISABLED, false);
    }
    return true;
  }

  /**
   * Checks if current folder exists.
   *
   * @param request current request object
   * @return {@code true} if current folder exists
   * @throws ConnectorException if current folder doesn't exist
   */
  protected boolean isCurrFolderExists(HttpServletRequest request)
          throws ConnectorException {
    String tmpType = request.getParameter("type");
    if (tmpType != null) {
      if (isTypeExists(tmpType)) {
        Path currDir = Paths.get(getConfiguration().getTypes().get(tmpType).getPath() + getArguments().getCurrentFolder());
        if (!Files.exists(currDir) || !Files.isDirectory(currDir)) {
          throw new ConnectorException(
                  Constants.Errors.CKFINDER_CONNECTOR_ERROR_FOLDER_NOT_FOUND,
                  false);
        } else {
          return true;
        }
      }
      return false;
    }
    return true;
  }

  /**
   * Checks if type of resource provided as parameter exists.
   *
   * @param type name of the resource type to check if it exists
   * @return {@code true} if provided type exists, {@code false} otherwise.
   */
  protected boolean isTypeExists(String type) {
    ResourceType testType = getConfiguration().getTypes().get(type);
    return testType != null;
  }

  /**
   * checks if current folder is hidden.
   *
   * @return false if isn't.
   * @throws ConnectorException when is hidden
   */
  protected boolean isHidden() throws ConnectorException {
    if (FileUtils.isDirectoryHidden(getArguments().getCurrentFolder(), getConfiguration())) {
      throw new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST,
              false);
    }
    return false;
  }

  /**
   * executes command and writes to response.
   *
   * @param response
   * @throws ConnectorException when error occurs
   * @throws java.io.IOException
   */
  abstract void execute(HttpServletResponse response) throws ConnectorException, IOException;

  /**
   * sets header in response.
   *
   * @param request servlet request
   * @param response servlet response
   */
  public abstract void setResponseHeader(HttpServletRequest request, HttpServletResponse response);

  /**
   * check request for security issue.
   *
   * @param reqParam request param
   * @return true if validation passed
   * @throws ConnectorException if validation error occurs.
   */
  protected boolean isRequestPathValid(String reqParam) throws ConnectorException {
    if (reqParam == null || reqParam.isEmpty()) {
      return true;
    }
    if (Pattern.compile(Constants.INVALID_PATH_REGEX).matcher(reqParam).find()) {
      throw new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_NAME,
              false);
    }

    return true;
  }

  /**
   * gets current folder request param or sets default value if it's not set.
   *
   * @param request request
   */
  protected void getCurrentFolderParam(HttpServletRequest request) {
    String currFolder = request.getParameter("currentFolder");
    if (currFolder == null || currFolder.isEmpty()) {
      getArguments().setCurrentFolder("/");
    } else {
      getArguments().setCurrentFolder(PathUtils.addSlashToBeginning(PathUtils.addSlashToEnd(currFolder)));
    }
  }

  /**
   * If string provided as parameter is null, this method converts it to empty
   * string.
   *
   * @param s string to check and convert if it is null
   * @return empty string if parameter was {@code null} or unchanged string if
   * parameter was nonempty string.
   */
  protected String nullToString(String s) {
    return s == null ? "" : s;
  }

  public Command clearArguments() {
    if (log.isTraceEnabled()) {
      log.trace("prepare clear arguments '{}'", arguments.get());
    }
    arguments.remove();
    return this;
  }

}
