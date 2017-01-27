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
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Base class for all command handlers.
 *
 * @param <T> arguments type
 */
@RequiredArgsConstructor
public abstract class Command<T extends Arguments> {

  /**
   * Name of the csrf token passed as request parameter.
   */
  protected static final String tokenParamName = "ckCsrfToken";

  @Getter
  @NonNull
  private final Supplier<? extends T> argumentsSupplier;

  /**
   * Runs command. Initialize, sets response and execute command.
   *
   * @param request request
   * @param response response
   * @param configuration connector configuration
   * @throws ConnectorException when error occurred.
   * @throws java.io.IOException
   */
  @SuppressWarnings("FinalMethod")
  public final void runCommand(HttpServletRequest request,
          HttpServletResponse response, IConfiguration configuration)
          throws ConnectorException, IOException {
    T arguments = argumentsSupplier.get();
    initParams(arguments, request, configuration);

    setResponseHeader(request, response, arguments);
    execute(arguments, response, configuration);
  }

  /**
   * initialize params for command handler.
   *
   * @param arguments
   * @param request request
   * @param configuration connector configuration
   * @throws ConnectorException to handle in error handler.
   */
  protected void initParams(T arguments, HttpServletRequest request,
          IConfiguration configuration) throws ConnectorException {
    HttpSession session = request.getSession(false);
    String userRole = session == null ? null : (String) session.getAttribute(configuration.getUserRoleName());
    arguments.setUserRole(userRole);

    arguments.setCurrentFolder(getCurrentFolderParam(request));

    String currentFolder = arguments.getCurrentFolder();
    checkConnectorEnabled(configuration);
    checkRequestPathValid(currentFolder);

    currentFolder = PathUtils.escape(currentFolder);
    arguments.setCurrentFolder(currentFolder);
    if (FileUtils.isDirectoryHidden(arguments.getCurrentFolder(), configuration)) {
      throw new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST,
              false);
    }

    if (currentFolder == null || currentFolder.isEmpty()
            || isCurrFolderExists(arguments, request, configuration)) {
      arguments.setType(request.getParameter("type"));
    }
  }

  /**
   * check if connector is enabled and checks authentication.
   *
   * @return true if connector is enabled and user is authenticated
   * @throws ConnectorException when connector is disabled
   */
  private void checkConnectorEnabled(IConfiguration configuration) throws ConnectorException {
    if (!configuration.isEnabled()) {
      throw new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_CONNECTOR_DISABLED, false);
    }
  }

  /**
   * Checks if current folder exists.
   *
   * @param arguments
   * @param request current request object
   * @param configuration connector configuration
   * @return {@code true} if current folder exists
   * @throws ConnectorException if current folder doesn't exist
   */
  @Deprecated
  protected boolean isCurrFolderExists(T arguments, HttpServletRequest request, IConfiguration configuration)
          throws ConnectorException {
    String tmpType = request.getParameter("type");
    if (tmpType != null) {
      try {
        checkTypeExists(tmpType, configuration);
      } catch (ConnectorException ex) {
        return false;
      }
      Path currDir = Paths.get(configuration.getTypes().get(tmpType).getPath(),
              arguments.getCurrentFolder());
      if (!Files.isDirectory(currDir)) {
        throw new ConnectorException(
                Constants.Errors.CKFINDER_CONNECTOR_ERROR_FOLDER_NOT_FOUND,
                false);
      } else {
        return true;
      }
    }
    return true;
  }

  /**
   * Checks if type of resource provided as parameter exists.
   *
   * @param type name of the resource type to check if it exists
   * @param configuration connector configuration
   * @throws com.github.zhanhb.ckfinder.connector.errors.ConnectorException
   */
  @Deprecated
  @SuppressWarnings("FinalMethod")
  protected final void checkTypeExists(String type, IConfiguration configuration) throws ConnectorException {
    ResourceType testType = configuration.getTypes().get(type);
    if (testType == null) {
      throw new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_TYPE, false);
    }
  }

  /**
   * executes command and writes to response.
   *
   * @param response
   * @throws ConnectorException when error occurs
   * @throws java.io.IOException
   */
  abstract void execute(T arguments, HttpServletResponse response, IConfiguration configuration) throws ConnectorException, IOException;

  /**
   * sets header in response.
   *
   * @param request servlet request
   * @param response servlet response
   * @param arguments
   */
  abstract void setResponseHeader(HttpServletRequest request, HttpServletResponse response, T arguments);

  /**
   * check request for security issue.
   *
   * @param reqParam request param
   * @param arguments
   * @return true if validation passed
   * @throws ConnectorException if validation error occurs.
   */
  @Deprecated
  @SuppressWarnings("FinalMethod")
  final void checkRequestPathValid(String reqParam) throws ConnectorException {
    if (reqParam != null && !reqParam.isEmpty()
            && Pattern.compile(Constants.INVALID_PATH_REGEX).matcher(reqParam).find()) {
      throw new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_NAME,
              false);
    }
  }

  /**
   * gets current folder request param or sets default value if it's not set.
   *
   * @param request request
   */
  @Deprecated
  String getCurrentFolderParam(HttpServletRequest request) {
    String currFolder = request.getParameter("currentFolder");
    if (currFolder != null && !currFolder.isEmpty()) {
      return PathUtils.addSlashToBeginning(PathUtils.addSlashToEnd(currFolder));
    } else {
      return "/";
    }
  }

  /**
   * Checks if the request contains a valid CSRF token that matches the value
   * sent in the cookie.<br>
   *
   * @see
   * <a href="https://www.owasp.org/index.php/Cross-Site_Request_Forgery_(CSRF)_Prevention_Cheat_Sheet#Double_Submit_Cookies">Cross-Site_Request_Forgery_(CSRF)_Prevention</a>
   *
   * @param request current request object
   * @return {@code true} if token is valid, {@code false} otherwise.
   */
  protected boolean checkCsrfToken(final HttpServletRequest request) {
    final String tokenCookieName = "ckCsrfToken", paramToken;
    final int minTokenLength = 32;

    String token = request.getParameter(tokenParamName);
    paramToken = token != null ? token.trim() : "";

    Cookie[] cookies = request.getCookies();
    String cookieToken = "";
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals(tokenCookieName)) {
          cookieToken = cookie.getValue();
          cookieToken = cookieToken != null ? cookieToken.trim() : "";
          break;
        }
      }
    }

    if (paramToken.length() >= minTokenLength && cookieToken.length() >= minTokenLength) {
      return paramToken.equals(cookieToken);
    }

    return false;
  }

}
