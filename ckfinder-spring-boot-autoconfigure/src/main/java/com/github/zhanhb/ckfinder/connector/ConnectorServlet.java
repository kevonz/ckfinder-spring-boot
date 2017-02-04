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

import com.github.zhanhb.ckfinder.connector.configuration.CommandFactory;
import com.github.zhanhb.ckfinder.connector.configuration.Constants;
import com.github.zhanhb.ckfinder.connector.configuration.Events;
import com.github.zhanhb.ckfinder.connector.configuration.IConfiguration;
import com.github.zhanhb.ckfinder.connector.data.BeforeExecuteCommandEventArgs;
import com.github.zhanhb.ckfinder.connector.errors.ConnectorException;
import com.github.zhanhb.ckfinder.connector.errors.ErrorHandler;
import com.github.zhanhb.ckfinder.connector.errors.XMLErrorHandler;
import com.github.zhanhb.ckfinder.connector.handlers.command.Command;
import com.github.zhanhb.ckfinder.connector.handlers.command.FileUploadCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.IPostCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.XMLCommand;
import java.io.IOException;
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
  private final CommandFactory commandFactory = new CommandFactory();

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
          throws ServletException, IOException {
    request.setCharacterEncoding("UTF-8");
    String commandName = request.getParameter("command");
    Command<?> command = null;

    try {
      if (commandName == null || commandName.isEmpty()) {
        throw new ConnectorException(
                Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_COMMAND);
      }

      BeforeExecuteCommandEventArgs args = new BeforeExecuteCommandEventArgs(commandName, request, response);

      Events events = configuration.getEvents();
      if (events == null || events.runBeforeExecuteCommand(args, configuration)) {
        command = commandFactory.getCommand(commandName);
        log.debug("{} {}", command, events);
        if (command != null) {
          // checks if command should go via POST request or it's a post request
          // and it's not upload command
          Class<?> commandClass = command.getClass();
          if ((IPostCommand.class.isAssignableFrom(commandClass) || post)
                  && !FileUploadCommand.class.isAssignableFrom(commandClass)) {
            checkPostRequest(request);
          }
          command.runCommand(request, response, configuration);
        } else {
          throw new ConnectorException(
                  Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_COMMAND);
        }
      }
    } catch (RuntimeException | Error e) {
      log.error("", e);
      handleError(new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_COMMAND),
              configuration, request, response, command);
    } catch (ConnectorException e) {
      log.error("", e);
      handleError(e, configuration, request, response, command);
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
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST);
    }
  }

  /**
   * handles error from execute command.
   *
   * @param e exception
   * @param request request
   * @param response response
   * @param configuration connector configuration
   * @throws ServletException when error handling fails.
   * @param command current command
   */
  private void handleError(ConnectorException e, IConfiguration configuration,
          HttpServletRequest request, HttpServletResponse response,
          Command<?> command) throws IOException {
    if (command == null || command instanceof XMLCommand) {
      XMLErrorHandler.INSTANCE.handleException(request, response, configuration, e);
    } else {
      ErrorHandler.INSTANCE.handleException(request, response, configuration, e);
    }
  }

}
