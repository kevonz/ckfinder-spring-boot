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
public class SaveFileCommand extends XMLCommand implements BeforeExecuteCommandEventHandler {

  private String fileName;
  private String fileContent;

  @Override
  protected void createXMLChildNodes(int arg0, Element arg1) {
  }

  @Override
  protected int getDataForXml() {

    if (!isTypeExists(getType())) {
      this.setType(null);
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_TYPE;
    }

    if (!getConfiguration().getAccessControl().checkFolderACL(getType(), getCurrentFolder(), getUserRole(),
            AccessControl.CKFINDER_CONNECTOR_ACL_FILE_DELETE)) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED;
    }

    if (this.fileName == null || this.fileName.isEmpty()) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_NAME;
    }

    if (this.fileContent == null || this.fileContent.isEmpty()) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }

    if (FileUtils.checkFileExtension(fileName, getConfiguration().getTypes().get(getType())) == 1) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_EXTENSION;
    }

    if (!FileUtils.isFileNameInvalid(fileName)) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }

    Path sourceFile = Paths.get(getConfiguration().getTypes().get(this.getType()).getPath()
            + this.getCurrentFolder(), this.fileName);

    try {
      if (!(Files.exists(sourceFile) && Files.isRegularFile(sourceFile))) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_FILE_NOT_FOUND;
      }
      Files.write(sourceFile, this.fileContent.getBytes("UTF-8"));
    } catch (FileNotFoundException e) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_FILE_NOT_FOUND;
    } catch (SecurityException | IOException e) {
      log.error("", e);
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED;
    }

    return Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE;
  }

  @Override
  public boolean runEventHandler(BeforeExecuteCommandEventArgs args, IConfiguration configuration1)
          throws ConnectorException {
    if ("SaveFile".equals(args.getCommand())) {
      this.runCommand(args.getRequest(), args.getResponse(), configuration1);
      return false;
    }
    return true;
  }

  @Override
  protected void initParams(HttpServletRequest request, IConfiguration configuration)
          throws ConnectorException {
    super.initParams(request, configuration);
    this.setCurrentFolder(request.getParameter("currentFolder"));
    this.setType(request.getParameter("type"));
    this.fileContent = request.getParameter("content");
    this.fileName = request.getParameter("fileName");
  }

}
