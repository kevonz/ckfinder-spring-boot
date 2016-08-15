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
  protected void createXMLChildNodes(int errorNum, Element rootElement) {
    if (errorNum == Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE) {
      createImageInfoNode(rootElement);
    }
  }

  private void createImageInfoNode(Element rootElement) {
    Element element = getArguments().getDocument().createElement("ImageInfo");
    element.setAttribute("width", String.valueOf(getArguments().getImageWidth()));
    element.setAttribute("height", String.valueOf(getArguments().getImageHeight()));
    rootElement.appendChild(element);
  }

  @Override
  protected int getDataForXml() {
    if (!isTypeExists(getArguments().getType())) {
      getArguments().setType(null);
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_TYPE;
    }

    if (!getConfiguration().getAccessControl().checkFolderACL(getArguments().getType(),
            getArguments().getCurrentFolder(), getArguments().getUserRole(),
            AccessControl.CKFINDER_CONNECTOR_ACL_FILE_VIEW)) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED;
    }

    if (getArguments().getFileName() == null || getArguments().getFileName().isEmpty()
            || !FileUtils.isFileNameInvalid(getArguments().getFileName())
            || FileUtils.isFileHidden(getArguments().getFileName(), this.getConfiguration())) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }

    if (FileUtils.checkFileExtension(getArguments().getFileName(), getConfiguration().getTypes().get(getArguments().getType())) == 1) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }

    Path imageFile = Paths.get(getConfiguration().getTypes().get(getArguments().getType()).getPath()
            + getArguments().getCurrentFolder(),
            getArguments().getFileName());

    try {
      if (!(Files.exists(imageFile) && Files.isRegularFile(imageFile))) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_FILE_NOT_FOUND;
      }

      BufferedImage image;
      try (InputStream is = Files.newInputStream(imageFile)) {
        image = ImageIO.read(is);
      }
      getArguments().setImageWidth(image.getWidth());
      getArguments().setImageHeight(image.getHeight());
    } catch (SecurityException | IOException e) {
      log.error("", e);
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED;
    }

    return Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE;
  }

  @Override
  protected void initParams(HttpServletRequest request, IConfiguration configuration)
          throws ConnectorException {
    super.initParams(request, configuration);
    getArguments().setImageHeight(0);
    getArguments().setImageWidth(0);
    getArguments().setCurrentFolder(request.getParameter("currentFolder"));
    getArguments().setType(request.getParameter("type"));
    getArguments().setFileName(request.getParameter("fileName"));
  }

}
