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
import com.github.zhanhb.ckfinder.connector.data.PluginInfo;
import com.github.zhanhb.ckfinder.connector.data.PluginParam;
import com.github.zhanhb.ckfinder.connector.errors.ConnectorException;
import com.github.zhanhb.ckfinder.connector.handlers.arguments.ImageResizeArguments;
import com.github.zhanhb.ckfinder.connector.handlers.command.XMLCommand;
import com.github.zhanhb.ckfinder.connector.utils.AccessControl;
import com.github.zhanhb.ckfinder.connector.utils.FileUtils;
import com.github.zhanhb.ckfinder.connector.utils.ImageUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Element;

@Slf4j
public class ImageResizeCommand extends XMLCommand<ImageResizeArguments> implements BeforeExecuteCommandEventHandler {

  private static final String[] SIZES = {"small", "medium", "large"};

  private final PluginInfo pluginInfo;

  public ImageResizeCommand(PluginInfo pluginInfo) {
    super(ImageResizeArguments::new);
    this.pluginInfo = pluginInfo;
  }

  @Override
  public boolean runEventHandler(BeforeExecuteCommandEventArgs args, IConfiguration configuration)
          throws ConnectorException {
    if ("ImageResize".equals(args.getCommand())) {
      this.runCommand(args.getRequest(), args.getResponse(), configuration);
      return false;
    }
    return true;
  }

  @Override
  protected void createXMLChildNodes(int arg0, Element arg1, ImageResizeArguments arguments) {
  }

  @Override
  protected int getDataForXml(ImageResizeArguments arguments) {
    if (!isTypeExists(arguments, arguments.getType())) {
      arguments.setType(null);
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_TYPE;
    }

    if (!getConfiguration().getAccessControl().hasPermission(arguments.getType(),
            arguments.getCurrentFolder(), arguments.getUserRole(),
            AccessControl.CKFINDER_CONNECTOR_ACL_FILE_DELETE
            | AccessControl.CKFINDER_CONNECTOR_ACL_FILE_UPLOAD)) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED;
    }

    if (arguments.getFileName() == null || arguments.getFileName().isEmpty()) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_NAME;
    }

    if (!FileUtils.isFileNameInvalid(arguments.getFileName())
            || FileUtils.isFileHidden(arguments.getFileName(), getConfiguration())) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }

    if (FileUtils.checkFileExtension(arguments.getFileName(), getConfiguration().getTypes().get(arguments.getType())) == 1) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }

    Path file = Paths.get(getConfiguration().getTypes().get(arguments.getType()).getPath(),
            arguments.getCurrentFolder(),
            arguments.getFileName());
    try {
      if (!Files.isRegularFile(file)) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_FILE_NOT_FOUND;
      }

      if (arguments.isWrongReqSizesParams()) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
      }

      if (arguments.getWidth() != null && arguments.getHeight() != null) {

        if (!FileUtils.isFileNameInvalid(arguments.getNewFileName())
                && FileUtils.isFileHidden(arguments.getNewFileName(), getConfiguration())) {
          return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_NAME;
        }

        if (FileUtils.checkFileExtension(arguments.getNewFileName(),
                getConfiguration().getTypes().get(arguments.getType())) == 1) {
          return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_EXTENSION;
        }

        Path thumbFile = Paths.get(getConfiguration().getTypes().get(arguments.getType()).getPath(),
                arguments.getCurrentFolder(),
                arguments.getNewFileName());

        if (Files.exists(thumbFile) && !Files.isWritable(thumbFile)) {
          return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED;
        }
        if (!"1".equals(arguments.getOverwrite()) && Files.exists(thumbFile)) {
          return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ALREADY_EXIST;
        }
        int maxImageHeight = getConfiguration().getImgHeight();
        int maxImageWidth = getConfiguration().getImgWidth();
        if ((maxImageWidth > 0 && arguments.getWidth() > maxImageWidth)
                || (maxImageHeight > 0 && arguments.getHeight() > maxImageHeight)) {
          return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
        }

        try {
          ImageUtils.createResizedImage(file, thumbFile,
                  arguments.getWidth(), arguments.getHeight(), getConfiguration().getImgQuality());

        } catch (IOException e) {
          log.error("", e);
          return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED;
        }
      }

      String fileNameWithoutExt = FileUtils.getFileNameWithoutExtension(arguments.getFileName());
      String fileExt = FileUtils.getFileExtension(arguments.getFileName());
      for (String size : SIZES) {
        if (arguments.getSizesFromReq().get(size) != null
                && arguments.getSizesFromReq().get(size).equals("1")) {
          String thumbName = fileNameWithoutExt + ("_") + size + "." + fileExt;
          Path thumbFile = Paths.get(getConfiguration().getTypes().get(arguments.getType()).getPath(),
                  arguments.getCurrentFolder(), thumbName);
          for (PluginParam param : pluginInfo.getParams()) {
            if ((size + "Thumb").equals(param.getName())) {
              if (checkParamSize(param.getValue())) {
                String[] params = parseValue(param.getValue());
                try {
                  ImageUtils.createResizedImage(file, thumbFile, Integer.parseInt(params[0]),
                          Integer.parseInt(params[1]), getConfiguration().getImgQuality());
                } catch (IOException e) {
                  log.error("", e);
                  return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED;
                }
              }
            }
          }
        }
      }
    } catch (SecurityException e) {
      log.error("", e);
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED;
    }

    return Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE;
  }

  private String[] parseValue(String value) {
    StringTokenizer st = new StringTokenizer(value, "x");
    return new String[]{st.nextToken(), st.nextToken()};
  }

  private boolean checkParamSize(String value) {
    return Pattern.matches("(\\d)+x(\\d)+", value);
  }

  @Override
  @SuppressWarnings("CollectionWithoutInitialCapacity")
  protected void initParams(ImageResizeArguments arguments, HttpServletRequest request, IConfiguration configuration1)
          throws ConnectorException {
    super.initParams(arguments, request, configuration1);

    arguments.setSizesFromReq(new HashMap<>());
    arguments.setFileName(request.getParameter("fileName"));
    arguments.setNewFileName(request.getParameter("newFileName"));
    arguments.setOverwrite(request.getParameter("overwrite"));
    String reqWidth = request.getParameter("width");
    String reqHeight = request.getParameter("height");
    arguments.setWrongReqSizesParams(false);
    try {
      if (reqWidth != null && !reqWidth.isEmpty()) {
        arguments.setWidth(Integer.valueOf(reqWidth));
      } else {
        arguments.setWidth(null);
      }
    } catch (NumberFormatException e) {
      arguments.setWidth(null);
      arguments.setWrongReqSizesParams(true);
    }
    try {
      if (reqHeight != null && !reqHeight.isEmpty()) {
        arguments.setHeight(Integer.valueOf(reqHeight));
      } else {
        arguments.setHeight(null);
      }
    } catch (NumberFormatException e) {
      arguments.setHeight(null);
      arguments.setWrongReqSizesParams(true);
    }
    for (String size : SIZES) {
      arguments.getSizesFromReq().put(size, request.getParameter(size));
    }

  }

}
