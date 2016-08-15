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
import com.github.zhanhb.ckfinder.connector.handlers.arguments.ImageResizeInfoArguments;
import com.github.zhanhb.ckfinder.connector.handlers.command.XMLCommand;
import com.github.zhanhb.ckfinder.connector.utils.AccessControl;
import com.github.zhanhb.ckfinder.connector.utils.FileUtils;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Element;

@Slf4j
public class ImageResizeInfoCommand extends XMLCommand<ImageResizeInfoArguments> implements BeforeExecuteCommandEventHandler {

  public ImageResizeInfoCommand() {
    super(ImageResizeInfoArguments::new);
  }

  @Override
  public boolean runEventHandler(BeforeExecuteCommandEventArgs args, IConfiguration configuration)
          throws ConnectorException {
    log.debug("runEventHandler: {} {}", args, configuration);
    if ("ImageResizeInfo".equals(args.getCommand())) {
      this.runCommand(args.getRequest(), args.getResponse(), configuration);
      return false;
    }
    return true;
  }

  @Override
  protected void createXMLChildNodes(int errorNum, Element rootElement, ImageResizeInfoArguments arguments) {
    if (errorNum == Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE) {
      createImageInfoNode(rootElement, arguments);
    }
  }

  private void createImageInfoNode(Element rootElement, ImageResizeInfoArguments arguments) {
    Element element = arguments.getDocument().createElement("ImageInfo");
    element.setAttribute("width", String.valueOf(arguments.getImageWidth()));
    element.setAttribute("height", String.valueOf(arguments.getImageHeight()));
    rootElement.appendChild(element);
  }

  @Override
  protected int getDataForXml(ImageResizeInfoArguments arguments) {
    if (!isTypeExists(arguments.getType())) {
      arguments.setType(null);
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_TYPE;
    }

    if (!getConfiguration().getAccessControl().checkFolderACL(arguments.getType(),
            arguments.getCurrentFolder(), arguments.getUserRole(),
            AccessControl.CKFINDER_CONNECTOR_ACL_FILE_VIEW)) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED;
    }

    if (arguments.getFileName() == null || arguments.getFileName().isEmpty()
            || !FileUtils.isFileNameInvalid(arguments.getFileName())
            || FileUtils.isFileHidden(arguments.getFileName(), this.getConfiguration())) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }

    if (FileUtils.checkFileExtension(arguments.getFileName(), getConfiguration().getTypes().get(arguments.getType())) == 1) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }

    Path imageFile = Paths.get(getConfiguration().getTypes().get(arguments.getType()).getPath()
            + arguments.getCurrentFolder(),
            arguments.getFileName());

    try {
      if (!(Files.exists(imageFile) && Files.isRegularFile(imageFile))) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_FILE_NOT_FOUND;
      }

      BufferedImage image;
      try (InputStream is = Files.newInputStream(imageFile)) {
        image = ImageIO.read(is);
      }
      arguments.setImageWidth(image.getWidth());
      arguments.setImageHeight(image.getHeight());
    } catch (SecurityException | IOException e) {
      log.error("", e);
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED;
    }

    return Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE;
  }

  @Override
  protected void initParams(ImageResizeInfoArguments arguments, HttpServletRequest request, IConfiguration configuration)
          throws ConnectorException {
    super.initParams(arguments, request, configuration);
    arguments.setImageHeight(0);
    arguments.setImageWidth(0);
    arguments.setCurrentFolder(request.getParameter("currentFolder"));
    arguments.setType(request.getParameter("type"));
    arguments.setFileName(request.getParameter("fileName"));
  }

}
