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
import com.github.zhanhb.ckfinder.connector.handlers.command.IErrorCommand;
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
import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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

  /**
   */
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
    getResponse(request, response, false);
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
    getResponse(request, response, true);
  }

  /**
   * Creating response for every command in request parameter.
   *
   * @param request request
   * @param response response
   * @param post if it's post command.
   * @throws ServletException when error occurs.
   */
  private void getResponse(HttpServletRequest request,
          HttpServletResponse response, boolean post)
          throws ServletException {
    String command = request.getParameter("command");
    try {
      if (command == null || command.isEmpty()) {
        throw new ConnectorException(
                Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_COMMAND, false);
      }

      boolean isNativeCommand;
      if (CommandHandlerEnum.contains(command.toUpperCase())) {
        isNativeCommand = true;
        CommandHandlerEnum cmd = CommandHandlerEnum.valueOf(command.toUpperCase());
        // checks if command should go via POST request or it's a post request
        // and it's not upload command
        if ((cmd.getCommand() instanceof IPostCommand || post)
                && !CommandHandlerEnum.FILEUPLOAD.equals(cmd)
                && !CommandHandlerEnum.QUICKUPLOAD.equals(cmd)) {
          checkPostRequest(request);
        }
      } else {
        isNativeCommand = false;
        command = null;
      }

      Events events = configuration.getEvents();
      if (events != null) {
        BeforeExecuteCommandEventArgs args = new BeforeExecuteCommandEventArgs(command, request, response);
        if (events.runBeforeExecuteCommand(args, configuration)) {
          executeNativeCommand(command, request, response, configuration, isNativeCommand);
        }
      } else {
        executeNativeCommand(command, request, response, configuration, isNativeCommand);
      }
    } catch (IllegalArgumentException e) {
      log.error("", e);
      handleError(
              new ConnectorException(
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
   * @param command string representing command name
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
  private void executeNativeCommand(String command, HttpServletRequest request,
          HttpServletResponse response, IConfiguration configuration,
          boolean isNativeCommand) throws IllegalArgumentException, ConnectorException {
    if (isNativeCommand) {
      CommandHandlerEnum cmd = CommandHandlerEnum.valueOf(command.toUpperCase());
      cmd.execute(request, response, configuration, null);
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
  private void handleError(ConnectorException e,
          IConfiguration configuration,
          HttpServletRequest request, HttpServletResponse response,
          String currentCommand) throws ServletException {
    log.debug(currentCommand);
    try {
      if (currentCommand != null && !currentCommand.isEmpty()) {
        Command command = CommandHandlerEnum.valueOf(currentCommand.toUpperCase()).getCommand();
        if (command instanceof XMLCommand) {
          CommandHandlerEnum.XMLERROR.execute(request, response, configuration, e);
        } else {
          CommandHandlerEnum.ERROR.execute(request, response, configuration, e);
        }
      } else {
        CommandHandlerEnum.XMLERROR.execute(request, response, configuration, e);
      }
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
  }

  /**
   * Enum with all command handles by servlet.
   *
   */
  private static enum CommandHandlerEnum {

    /**
     * init command.
     */
    INIT(InitCommand::new),
    /**
     * get subfolders for selected location command.
     */
    GETFOLDERS(GetFoldersCommand::new),
    /**
     * get files from current folder command.
     */
    GETFILES(GetFilesCommand::new),
    /**
     * get thumbnail for file command.
     */
    THUMBNAIL(ThumbnailCommand::new),
    /**
     * download file command.
     */
    DOWNLOADFILE(DownloadFileCommand::new),
    /**
     * create subfolder.
     */
    CREATEFOLDER(CreateFolderCommand::new),
    /**
     * rename file.
     */
    RENAMEFILE(RenameFileCommand::new),
    /**
     * rename folder.
     */
    RENAMEFOLDER(RenameFolderCommand::new),
    /**
     * delete folder.
     */
    DELETEFOLDER(DeleteFolderCommand::new),
    /**
     * copy files.
     */
    COPYFILES(CopyFilesCommand::new),
    /**
     * move files.
     */
    MOVEFILES(MoveFilesCommand::new),
    /**
     * delete files.
     */
    DELETEFILES(DeleteFilesCommand::new),
    /**
     * file upload.
     */
    FILEUPLOAD(FileUploadCommand::new),
    /**
     * quick file upload.
     */
    QUICKUPLOAD(QuickUploadCommand::new),
    /**
     * XML error command.
     */
    XMLERROR(XMLErrorCommand::new),
    /**
     * error command.
     */
    ERROR(ErrorCommand::new);
    /**
     * command class for enum field.
     */
    private final Supplier<? extends Command> supplier;
    /**
     * {@code Set} holding enumeration values,
     */
    private static final Set<String> enumValues = Arrays.asList(CommandHandlerEnum.values())
            .stream().map(CommandHandlerEnum::name).collect(Collectors.toSet());

    /**
     * Enum constructor to set command.
     *
     * @param command1 command name
     */
    private CommandHandlerEnum(Supplier<? extends Command> supplier) {
      this.supplier = supplier;
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

    /**
     * Executes command.
     *
     * @param request request
     * @param response response
     * @param configuration connector configuration
     * @param sc servletContext
     * @param params params for command.
     * @throws ConnectorException when error occurs
     */
    private void execute(HttpServletRequest request,
            HttpServletResponse response,
            IConfiguration configuration,
            ConnectorException e) throws ConnectorException {
      Command com = supplier.get();
      if (com instanceof IErrorCommand) {
        ((IErrorCommand) com).setConnectorException(e);
      }
      com.runCommand(request, response, configuration);
    }

    /**
     * gets command.
     *
     * @return command
     */
    public Command getCommand() {
      return supplier.get();
    }

  }

}
