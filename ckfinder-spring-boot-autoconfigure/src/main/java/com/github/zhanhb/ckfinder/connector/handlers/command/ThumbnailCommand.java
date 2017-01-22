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
import com.github.zhanhb.ckfinder.connector.errors.ConnectorException;
import com.github.zhanhb.ckfinder.connector.handlers.arguments.ThumbnailArguments;
import com.github.zhanhb.ckfinder.connector.utils.AccessControl;
import com.github.zhanhb.ckfinder.connector.utils.FileUtils;
import com.github.zhanhb.ckfinder.connector.utils.ImageUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Class to handle <code>Thumbnail</code> command.
 */
@Slf4j
public class ThumbnailCommand extends Command<ThumbnailArguments> {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
          .withZone(ZoneId.of("GMT"));

  /**
   * Backup map holding mime types for images just in case if they aren't set in
   * a request
   */
  private static final Map<String, String> imgMimeTypeMap = new HashMap<>(57);

  static {
    imgMimeTypeMap.put(".art", "image/x-jg");
    imgMimeTypeMap.put(".bm", "image/bmp");
    imgMimeTypeMap.put(".bmp", "image/bmp");
    imgMimeTypeMap.put(".dwg", "image/vnd.dwg");
    imgMimeTypeMap.put(".dxf", "image/vnd.dwg");
    imgMimeTypeMap.put(".fif", "image/fif");
    imgMimeTypeMap.put(".flo", "image/florian");
    imgMimeTypeMap.put(".fpx", "image/vnd.fpx");
    imgMimeTypeMap.put(".g3", "image/g3fax");
    imgMimeTypeMap.put(".gif", "image/gif");
    imgMimeTypeMap.put(".ico", "image/x-icon");
    imgMimeTypeMap.put(".ief", "image/ief");
    imgMimeTypeMap.put(".iefs", "image/ief");
    imgMimeTypeMap.put(".jut", "image/jutvision");
    imgMimeTypeMap.put(".mcf", "image/vasa");
    imgMimeTypeMap.put(".nap", "image/naplps");
    imgMimeTypeMap.put(".naplps", "image/naplps");
    imgMimeTypeMap.put(".nif", "image/x-niff");
    imgMimeTypeMap.put(".niff", "image/x-niff");
    imgMimeTypeMap.put(".pct", "image/x-pict");
    imgMimeTypeMap.put(".pcx", "image/x-pcx");
    imgMimeTypeMap.put(".pgm", "image/x-portable-graymap");
    imgMimeTypeMap.put(".pic", "image/pict");
    imgMimeTypeMap.put(".pict", "image/pict");
    imgMimeTypeMap.put(".pm", "image/x-xpixmap");
    imgMimeTypeMap.put(".png", "image/png");
    imgMimeTypeMap.put(".pnm", "image/x-portable-anymap");
    imgMimeTypeMap.put(".ppm", "image/x-portable-pixmap");
    imgMimeTypeMap.put(".ras", "image/x-cmu-raster");
    imgMimeTypeMap.put(".rast", "image/cmu-raster");
    imgMimeTypeMap.put(".rf", "image/vnd.rn-realflash");
    imgMimeTypeMap.put(".rgb", "image/x-rgb");
    imgMimeTypeMap.put(".rp", "image/vnd.rn-realpix");
    imgMimeTypeMap.put(".svf", "image/vnd.dwg");
    imgMimeTypeMap.put(".svf", "image/x-dwg");
    imgMimeTypeMap.put(".tiff", "image/tiff");
    imgMimeTypeMap.put(".turbot", "image/florian");
    imgMimeTypeMap.put(".wbmp", "image/vnd.wap.wbmp");
    imgMimeTypeMap.put(".xif", "image/vnd.xiff");
    imgMimeTypeMap.put(".xpm", "image/x-xpixmap");
    imgMimeTypeMap.put(".x-png", "image/png");
    imgMimeTypeMap.put(".xwd", "image/x-xwindowdump");
  }

  public ThumbnailCommand() {
    super(ThumbnailArguments::new);
  }

  @Override
  public void setResponseHeader(HttpServletRequest request, HttpServletResponse response, ThumbnailArguments arguments) {
    response.setHeader("Cache-Control", "public");
    String mimetype = getMimeTypeOfImage(request.getServletContext(), response, arguments);
    if (mimetype != null) {
      response.setContentType(mimetype);
    }
    response.addHeader("Content-Disposition",
            ContentDisposition.getContentDisposition("attachment", arguments.getFileName()));
  }

  /**
   * Gets mime type of image.
   *
   * @param sc the {@code ServletContext} object.
   * @param response currect response object
   * @return mime type of the image.
   */
  private String getMimeTypeOfImage(ServletContext sc,
          HttpServletResponse response, ThumbnailArguments arguments) {
    String fileName = arguments.getFileName();
    if (fileName == null || fileName.length() == 0) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return null;
    }
    String tempFileName = fileName.substring(0,
            fileName.lastIndexOf('.') + 1).concat(
            FileUtils.getFileExtension(fileName).toLowerCase());
    String mimeType = sc.getMimeType(tempFileName);
    if (mimeType == null || mimeType.length() == 0) {
      mimeType = imgMimeTypeMap.get(arguments.getFileName().substring(arguments.getFileName().lastIndexOf('.')).toLowerCase());
    }

    if (mimeType == null) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return null;
    }
    return mimeType;
  }

  @Override
  @SuppressWarnings("FinalMethod")
  final void execute(ThumbnailArguments arguments, HttpServletResponse response) throws ConnectorException {
    validate(arguments);
    createThumb(arguments);
    if (setResponseHeadersAfterCreatingFile(response, arguments)) {
      try (ServletOutputStream out = response.getOutputStream()) {
        FileUtils.printFileContentToResponse(arguments.getThumbFile(), out);
      } catch (IOException e) {
        log.error("", e);
        try {
          response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (IOException e1) {
          throw new ConnectorException(e1);
        }
      }
    } else {
      response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
    }
  }

  @Override
  protected void initParams(ThumbnailArguments arguments, HttpServletRequest request, IConfiguration configuration)
          throws ConnectorException {
    super.initParams(arguments, request, configuration);
    arguments.setFileName(request.getParameter("FileName"));
    try {
      arguments.setIfModifiedSince(request.getDateHeader("If-Modified-Since"));
    } catch (IllegalArgumentException e) {
      arguments.setIfModifiedSince(0);
    }
    arguments.setIfNoneMatch(request.getHeader("If-None-Match"));
  }

  /**
   * Validates thumbnail file properties and current user access rights.
   *
   * @throws ConnectorException when validation fails.
   */
  private void validate(ThumbnailArguments arguments) throws ConnectorException {
    if (!this.getConfiguration().isThumbsEnabled()) {
      arguments.throwException(Constants.Errors.CKFINDER_CONNECTOR_ERROR_THUMBNAILS_DISABLED);
    }
    try {
      checkTypeExists(arguments.getType());
    } catch (ConnectorException ex) {
      arguments.setType(null);
      throw ex;
    }

    if (!getConfiguration().getAccessControl().hasPermission(arguments.getType(),
            arguments.getCurrentFolder(), arguments.getUserRole(),
            AccessControl.CKFINDER_CONNECTOR_ACL_FILE_VIEW)) {
      arguments.throwException(Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED);
    }

    if (!FileUtils.isFileNameInvalid(arguments.getFileName())) {
      arguments.throwException(Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST);
    }

    if (FileUtils.isFileHidden(arguments.getFileName(), this.getConfiguration())) {
      arguments.throwException(Constants.Errors.CKFINDER_CONNECTOR_ERROR_FILE_NOT_FOUND);
    }

    log.debug("configuration thumbsPath: {}", getConfiguration().getThumbsPath());
    Path fullCurrentDir = Paths.get(getConfiguration().getThumbsPath(), arguments.getType(), arguments.getCurrentFolder());
    log.debug("typeThumbDir: {}", fullCurrentDir);

    try {
      String fullCurrentPath = fullCurrentDir.toAbsolutePath().toString();
      log.debug(fullCurrentPath);
      arguments.setFullCurrentPath(fullCurrentPath);
      if (!Files.exists(fullCurrentDir)) {
        Files.createDirectories(fullCurrentDir);
      }
    } catch (IOException | SecurityException e) {
      throw new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED, e);
    }

  }

  /**
   * Creates thumbnail file if thumbnails are enabled and thumb file doesn't
   * exists.
   *
   * @throws ConnectorException when thumbnail creation fails.
   */
  @SuppressWarnings({"BroadCatchBlock", "TooBroadCatch"})
  private void createThumb(ThumbnailArguments arguments) throws ConnectorException {
    log.debug("ThumbnailCommand.createThumb()");
    log.debug("{}", arguments.getFullCurrentPath());
    Path thumbFile = Paths.get(arguments.getFullCurrentPath(), arguments.getFileName());
    log.debug("thumbFile: {}", thumbFile);
    arguments.setThumbFile(thumbFile);
    try {
      if (!Files.exists(thumbFile)) {
        Path orginFile = Paths.get(getConfiguration().getTypes().get(arguments.getType()).getPath(),
                arguments.getCurrentFolder(), arguments.getFileName());
        log.debug("orginFile: {}", orginFile);
        if (!Files.exists(orginFile)) {
          arguments.throwException(Constants.Errors.CKFINDER_CONNECTOR_ERROR_FILE_NOT_FOUND);
        }
        try {
          ImageUtils.createThumb(orginFile, thumbFile, getConfiguration());
        } catch (Exception e) {
          try {
            Files.deleteIfExists(thumbFile);
          } catch (IOException ex) {
            e.addSuppressed(ex);
          }
          throw new ConnectorException(
                  Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED,
                  e);
        }
      }
    } catch (SecurityException e) {
      throw new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED, e);
    }

  }

  /**
   * Fills in response headers after creating file.
   *
   * @return true if continue returning thumb or false if break and send
   * response code.
   * @throws ConnectorException when access is denied.
   */
  private boolean setResponseHeadersAfterCreatingFile(HttpServletResponse response,
          ThumbnailArguments arguments) throws ConnectorException {
    // Set content size
    Path file = Paths.get(arguments.getFullCurrentPath(), arguments.getFileName());
    try {
      FileTime lastModifiedTime = Files.getLastModifiedTime(file);
      String etag = "W/\"" + Long.toHexString(lastModifiedTime.toMillis()) + "-" + Long.toHexString(Files.size(file)) + '"';
      Instant instant = lastModifiedTime.toInstant();
      response.setHeader("Etag", etag);
      response.setHeader("Last-Modified", FORMATTER.format(instant));

      if (etag.equals(arguments.getIfNoneMatch())
              || lastModifiedTime.toMillis() <= arguments.getIfModifiedSince() + 1000L) {
        return false;
      }

      response.setContentLengthLong(Files.size(file));

      return true;
    } catch (IOException | SecurityException e) {
      throw new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED, e);
    }
  }

}
