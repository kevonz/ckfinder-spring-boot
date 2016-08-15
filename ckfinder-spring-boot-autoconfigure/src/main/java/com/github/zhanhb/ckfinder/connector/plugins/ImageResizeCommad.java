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
public class ImageResizeCommad extends XMLCommand<ImageResizeArguments> implements BeforeExecuteCommandEventHandler {

  private static final String[] SIZES = {"small", "medium", "large"};

  private final PluginInfo pluginInfo;

  public ImageResizeCommad(PluginInfo pluginInfo) {
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
  protected void createXMLChildNodes(int arg0, Element arg1) {
  }

  @Override
  protected int getDataForXml() {
    if (!isTypeExists(getArguments().getType())) {
      getArguments().setType(null);
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_TYPE;
    }

    if (!getConfiguration().getAccessControl().checkFolderACL(getArguments().getType(),
            getArguments().getCurrentFolder(), getArguments().getUserRole(),
            AccessControl.CKFINDER_CONNECTOR_ACL_FILE_DELETE
            | AccessControl.CKFINDER_CONNECTOR_ACL_FILE_UPLOAD)) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED;
    }

    if (getArguments().getFileName() == null || getArguments().getFileName().isEmpty()) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_NAME;
    }

    if (!FileUtils.isFileNameInvalid(getArguments().getFileName())
            || FileUtils.isFileHidden(getArguments().getFileName(), getConfiguration())) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }

    if (FileUtils.checkFileExtension(getArguments().getFileName(), getConfiguration().getTypes().get(getArguments().getType())) == 1) {
      return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
    }

    Path file = Paths.get(getConfiguration().getTypes().get(getArguments().getType()).getPath() + getArguments().getCurrentFolder(),
            getArguments().getFileName());
    try {
      if (!(Files.exists(file) && Files.isRegularFile(file))) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_FILE_NOT_FOUND;
      }

      if (getArguments().isWrongReqSizesParams()) {
        return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
      }

      if (getArguments().getWidth() != null && getArguments().getHeight() != null) {

        if (!FileUtils.isFileNameInvalid(getArguments().getNewFileName())
                && FileUtils.isFileHidden(getArguments().getNewFileName(), getConfiguration())) {
          return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_NAME;
        }

        if (FileUtils.checkFileExtension(getArguments().getNewFileName(),
                getConfiguration().getTypes().get(getArguments().getType())) == 1) {
          return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_EXTENSION;
        }

        Path thumbFile = Paths.get(getConfiguration().getTypes().get(getArguments().getType()).getPath() + getArguments().getCurrentFolder(),
                getArguments().getNewFileName());

        if (Files.exists(thumbFile) && !Files.isWritable(thumbFile)) {
          return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED;
        }
        if (!"1".equals(getArguments().getOverwrite()) && Files.exists(thumbFile)) {
          return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ALREADY_EXIST;
        }
        int maxImageHeight = getConfiguration().getImgHeight();
        int maxImageWidth = getConfiguration().getImgWidth();
        if ((maxImageWidth > 0 && getArguments().getWidth() > maxImageWidth)
                || (maxImageHeight > 0 && getArguments().getHeight() > maxImageHeight)) {
          return Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST;
        }

        try {
          ImageUtils.createResizedImage(file, thumbFile,
                  getArguments().getWidth(), getArguments().getHeight(), getConfiguration().getImgQuality());

        } catch (IOException e) {
          log.error("", e);
          return Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED;
        }
      }

      String fileNameWithoutExt = FileUtils.getFileNameWithoutExtension(getArguments().getFileName());
      String fileExt = FileUtils.getFileExtension(getArguments().getFileName());
      for (String size : SIZES) {
        if (getArguments().getSizesFromReq().get(size) != null
                && getArguments().getSizesFromReq().get(size).equals("1")) {
          String thumbName = fileNameWithoutExt.concat("_").concat(size).concat(".").concat(fileExt);
          Path thumbFile = Paths.get(getConfiguration().getTypes().get(getArguments().getType()).getPath().concat(getArguments().getCurrentFolder()).concat(thumbName));
          for (PluginParam param : pluginInfo.getParams()) {
            if (size.concat("Thumb").equals(param.getName())) {
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
  protected void initParams(HttpServletRequest request, IConfiguration configuration1)
          throws ConnectorException {
    super.initParams(request, configuration1);

    getArguments().setSizesFromReq(new HashMap<>());
    getArguments().setFileName(request.getParameter("fileName"));
    getArguments().setNewFileName(request.getParameter("newFileName"));
    getArguments().setOverwrite(request.getParameter("overwrite"));
    String reqWidth = request.getParameter("width");
    String reqHeight = request.getParameter("height");
    getArguments().setWrongReqSizesParams(false);
    try {
      if (reqWidth != null && !reqWidth.isEmpty()) {
        getArguments().setWidth(Integer.valueOf(reqWidth));
      } else {
        getArguments().setWidth(null);
      }
    } catch (NumberFormatException e) {
      getArguments().setWidth(null);
      getArguments().setWrongReqSizesParams(true);
    }
    try {
      if (reqHeight != null && !reqHeight.isEmpty()) {
        getArguments().setHeight(Integer.valueOf(reqHeight));
      } else {
        getArguments().setHeight(null);
      }
    } catch (NumberFormatException e) {
      getArguments().setHeight(null);
      getArguments().setWrongReqSizesParams(true);
    }
    for (String size : SIZES) {
      getArguments().getSizesFromReq().put(size, request.getParameter(size));
    }

  }

}
