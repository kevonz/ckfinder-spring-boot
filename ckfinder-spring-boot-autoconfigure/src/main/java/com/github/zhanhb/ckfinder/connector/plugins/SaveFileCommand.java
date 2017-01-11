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
package com.github.zhanhb.ckfinder.connector.plugins;

import com.github.zhanhb.ckfinder.connector.configuration.Constants;
import com.github.zhanhb.ckfinder.connector.configuration.IConfiguration;
import com.github.zhanhb.ckfinder.connector.data.BeforeExecuteCommandEventArgs;
import com.github.zhanhb.ckfinder.connector.data.BeforeExecuteCommandEventHandler;
import com.github.zhanhb.ckfinder.connector.errors.ConnectorException;
import com.github.zhanhb.ckfinder.connector.handlers.arguments.SaveFileArguments;
import com.github.zhanhb.ckfinder.connector.handlers.command.XMLCommand;
import com.github.zhanhb.ckfinder.connector.utils.AccessControl;
import com.github.zhanhb.ckfinder.connector.utils.FileUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Element;

@Slf4j
public class SaveFileCommand extends XMLCommand<SaveFileArguments> implements BeforeExecuteCommandEventHandler {

  public SaveFileCommand() {
    super(SaveFileArguments::new);
  }

  @Override
  protected void createXMLChildNodes(int errorNum, Element rootElement, SaveFileArguments arguments) {
  }

  @Override
  protected int getDataForXml(SaveFileArguments arguments) {
    if (getConfiguration().isEnableCsrfProtection() && !checkCsrfToken(arguments.getRequest(), null)) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }

    try {
      checkTypeExists(arguments.getType());
    } catch (ConnectorException ex) {
      arguments.setType(null);
      return ex.getErrorCode();
    }

    if (!getConfiguration().getAccessControl().hasPermission(arguments.getType(),
            arguments.getCurrentFolder(), arguments.getUserRole(),
            AccessControl.CKFINDER_CONNECTOR_ACL_FILE_DELETE)) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED;
    }

    if (arguments.getFileName() == null || arguments.getFileName().isEmpty()) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_NAME;
    }

    if (arguments.getFileContent() == null || arguments.getFileContent().isEmpty()) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }

    if (FileUtils.checkFileExtension(arguments.getFileName(), getConfiguration().getTypes().get(arguments.getType())) == 1) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_EXTENSION;
    }

    if (!FileUtils.isFileNameInvalid(arguments.getFileName())) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }

    Path sourceFile = Paths.get(getConfiguration().getTypes().get(arguments.getType()).getPath(),
            arguments.getCurrentFolder(), arguments.getFileName());

    try {
      if (!Files.isRegularFile(sourceFile)) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_FILE_NOT_FOUND;
      }
      Files.write(sourceFile, arguments.getFileContent().getBytes("UTF-8"));
    } catch (FileNotFoundException e) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_FILE_NOT_FOUND;
    } catch (SecurityException | IOException e) {
      log.error("", e);
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED;
    }

    return Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE;
  }

  @Override
  public boolean runEventHandler(BeforeExecuteCommandEventArgs args, IConfiguration configuration)
          throws ConnectorException, IOException {
    if ("SaveFile".equals(args.getCommand())) {
      this.runCommand(args.getRequest(), args.getResponse(), configuration);
      return false;
    }
    return true;
  }

  @Override
  protected void initParams(SaveFileArguments arguments, HttpServletRequest request, IConfiguration configuration)
          throws ConnectorException {
    super.initParams(arguments, request, configuration);
    arguments.setCurrentFolder(request.getParameter("currentFolder"));
    arguments.setType(request.getParameter("type"));
    arguments.setFileContent(request.getParameter("content"));
    arguments.setFileName(request.getParameter("fileName"));
    arguments.setRequest(request);
  }

}
