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
package com.github.zhanhb.ckfinder.connector;

import com.github.zhanhb.ckfinder.connector.configuration.Constants;
import com.github.zhanhb.ckfinder.connector.configuration.Events;
import com.github.zhanhb.ckfinder.connector.configuration.IConfiguration;
import com.github.zhanhb.ckfinder.connector.data.BeforeExecuteCommandEventArgs;
import com.github.zhanhb.ckfinder.connector.errors.ConnectorException;
import com.github.zhanhb.ckfinder.connector.handlers.command.Command;
import com.github.zhanhb.ckfinder.connector.handlers.command.CopyFilesCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.CreateFolderCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.DeleteFilesCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.DeleteFolderCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.DownloadFileCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.ErrorCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.FileUploadCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.GetFilesCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.GetFoldersCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.IPostCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.InitCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.MoveFilesCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.QuickUploadCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.RenameFileCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.RenameFolderCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.ThumbnailCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.XMLCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.XMLErrorCommand;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Main connector servlet for handling CKFinder requests.
 */
@RequiredArgsConstructor
@Slf4j
public class ConnectorServlet extends HttpServlet {

  private static final long serialVersionUID = 2960665641425153638L;

  private final IConfiguration configuration;

  /**
   * Handling get requests.
   *
   * @param request request
   * @param response response
   * @throws IOException
   * @throws ServletException
   */
  @Override
  protected void doGet(HttpServletRequest request,
          HttpServletResponse response) throws ServletException,
          IOException {
    request.setCharacterEncoding("UTF-8");
    response.setCharacterEncoding("UTF-8");
    processRequest(request, response, false);
  }

  /**
   * Handling post requests.
   *
   * @param request request
   * @param response response
   * @throws IOException .
   * @throws ServletException .
   */
  @Override
  protected void doPost(HttpServletRequest request,
          HttpServletResponse response) throws ServletException,
          IOException {
    request.setCharacterEncoding("UTF-8");
    response.setCharacterEncoding("UTF-8");
    processRequest(request, response, true);
  }

  /**
   * Creating response for every command in request parameter.
   *
   * @param request request
   * @param response response
   * @param post if it's post command.
   * @throws ServletException when error occurs.
   */
  private void processRequest(HttpServletRequest request,
          HttpServletResponse response, boolean post)
          throws ServletException {
    String commandName = request.getParameter("command");
    Command<?> command = null;

    try {
      if (commandName == null || commandName.isEmpty()) {
        throw new ConnectorException(
                Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_COMMAND, false);
      }

      BeforeExecuteCommandEventArgs args = new BeforeExecuteCommandEventArgs(commandName, request, response);

      final String commandUpperCase = commandName.toUpperCase();
      if (CommandHolder.contains(commandUpperCase)) {
        command = CommandHolder.getCommand(commandUpperCase);
        // checks if command should go via POST request or it's a post request
        // and it's not upload command
        if ((command instanceof IPostCommand || post)
                && !"FILEUPLOAD".equals(commandUpperCase)
                && !"QUICKUPLOAD".equals(commandUpperCase)) {
          checkPostRequest(request);
        }
      } else {
        commandName = null;
        command = null;
      }

      Events events = configuration.getEvents();
      log.debug("{} {}", commandName, events);
      if (events != null) {
        if (events.runBeforeExecuteCommand(args, configuration)) {
          log.debug("events.runBeforeExecuteCommand(...):true");
          executeCommand(command, request, response, configuration);
        }
      } else {
        executeCommand(command, request, response, configuration);
      }
    } catch (IllegalArgumentException e) {
      log.error("", e);
      handleError(new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_COMMAND, false),
              configuration, request, response, command);
    } catch (ConnectorException e) {
      log.error("", e);
      handleError(e, configuration, request, response, command);
    }
  }

  /**
   * Executes one of connector's predefined commands specified as parameter.
   *
   * @param commandName string representing command name
   * @param request current request object
   * @param respose current response object
   * @param configuration CKFinder connector configuration
   * @param isNativeCommand flag indicating whether command is available in
   * enumeration object
   *
   * @throws ConnectorException when command isn't native
   * @throws IllegalArgumentException when provided command is not found in
   * enumeration object
   */
  private void executeCommand(Command<?> command, HttpServletRequest request, HttpServletResponse response, IConfiguration configuration) throws IllegalArgumentException, ConnectorException {
    if (command != null) {
      try {
        command.runCommand(request, response, configuration);
      } finally {
        command.clearArguments();
      }
    } else {
      throw new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_COMMAND, false);
    }
  }

  /**
   * checks post request if it's ckfinder command.
   *
   * @param request request
   * @throws ConnectorException when param isn't set or has wrong value.
   */
  private void checkPostRequest(HttpServletRequest request)
          throws ConnectorException {
    if (!"true".equals(request.getParameter("CKFinderCommand"))) {
      throw new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST, true);
    }
  }

  /**
   * handles error from execute command.
   *
   * @param e exception
   * @param request request
   * @param response response
   * @param configuration connector configuration
   * @param currentCommand current command
   * @throws ServletException when error handling fails.
   */
  @SuppressWarnings({"BroadCatchBlock", "TooBroadCatch"})
  private void handleError(ConnectorException e, IConfiguration configuration,
          HttpServletRequest request, HttpServletResponse response,
          Command<?> command) throws ServletException {
    try {
      if (command == null || command instanceof XMLCommand) {
        XMLErrorCommand.getInstance().withArgument(e).runCommand(request, response, configuration);
      } else {
        ErrorCommand.getInstance().withArgument(e).runCommand(request, response, configuration);
      }
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
  }

  @SuppressWarnings("UtilityClassWithoutPrivateConstructor")
  private static class CommandHolder {

    private static final Map<String, Command<?>> MAP;
    /**
     * {@code Set} holding enumeration values,
     */
    private static final Set<String> enumValues;

    static {
      Map<String, Command<?>> map = new HashMap<>(15);

      /**
       * init command.
       */
      map.put("INIT", new InitCommand());
      /**
       * get subfolders for selected location command.
       */
      map.put("GETFOLDERS", new GetFoldersCommand());
      /**
       * get files from current folder command.
       */
      map.put("GETFILES", new GetFilesCommand());
      /**
       * get thumbnail for file command.
       */
      map.put("THUMBNAIL", new ThumbnailCommand());
      /**
       * download file command.
       */
      map.put("DOWNLOADFILE", new DownloadFileCommand());
      /**
       * create subfolder.
       */
      map.put("CREATEFOLDER", new CreateFolderCommand());
      /**
       * rename file.
       */
      map.put("RENAMEFILE", new RenameFileCommand());
      /**
       * rename folder.
       */
      map.put("RENAMEFOLDER", new RenameFolderCommand());
      /**
       * delete folder.
       */
      map.put("DELETEFOLDER", new DeleteFolderCommand());
      /**
       * copy files.
       */
      map.put("COPYFILES", new CopyFilesCommand());
      /**
       * move files.
       */
      map.put("MOVEFILES", new MoveFilesCommand());
      /**
       * delete files.
       */
      map.put("DELETEFILES", new DeleteFilesCommand());
      /**
       * file upload.
       */
      map.put("FILEUPLOAD", new FileUploadCommand());
      /**
       * quick file upload.
       */
      map.put("QUICKUPLOAD", new QuickUploadCommand());
      MAP = map;
      enumValues = map.keySet();
    }

    static Command<?> getCommand(String commandUpperCase) {
      return MAP.get(commandUpperCase);
    }

    /**
     * Checks whether enumeration object contains command name specified as
     * parameter.
     *
     * @param enumValue string representing command name to check
     *
     * @return {@code true} is command exists, {@code false} otherwise
     */
    public static boolean contains(String enumValue) {
      return enumValues.contains(enumValue);
    }

  }

}
