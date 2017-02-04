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
import com.github.zhanhb.ckfinder.connector.data.AfterFileUploadEventArgs;
import com.github.zhanhb.ckfinder.connector.data.ResourceType;
import com.github.zhanhb.ckfinder.connector.errors.ConnectorException;
import com.github.zhanhb.ckfinder.connector.errors.ErrorUtils;
import com.github.zhanhb.ckfinder.connector.handlers.arguments.FileUploadArguments;
import com.github.zhanhb.ckfinder.connector.utils.AccessControl;
import com.github.zhanhb.ckfinder.connector.utils.FileUtils;
import com.github.zhanhb.ckfinder.connector.utils.ImageUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;

/**
 * Class to handle <code>FileUpload</code> command.
 *
 */
@Slf4j
public class FileUploadCommand extends Command<FileUploadArguments> implements IPostCommand {

  /**
   * Array containing unsafe characters which can't be used in file name.
   */
  private static final Pattern UNSAFE_FILE_NAME_PATTERN = Pattern.compile("[:*?|/]");

  /**
   * default constructor.
   */
  public FileUploadCommand() {
    super(FileUploadArguments::new);
  }

  /**
   * Executes file upload command.
   *
   * @throws ConnectorException when error occurs.
   */
  @Override
  @SuppressWarnings("FinalMethod")
  final void execute(FileUploadArguments arguments, HttpServletResponse response, IConfiguration configuration) throws ConnectorException {
    try {
      String errorMsg = arguments.getErrorCode() == Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE ? "" : (arguments.getErrorCode() == Constants.Errors.CKFINDER_CONNECTOR_ERROR_CUSTOM_ERROR ? arguments.getCustomErrorMsg()
              : ErrorUtils.INSTANCE.getErrorMsgByLangAndCode(arguments.getLangCode(), arguments.getErrorCode()));
      errorMsg = errorMsg.replace("%1", arguments.getNewFileName());
      String path = "";

      if (!arguments.isUploaded()) {
        arguments.setNewFileName("");
        arguments.setCurrentFolder("");
      } else {
        path = configuration.getTypes().get(arguments.getType()).getUrl()
                + arguments.getCurrentFolder();
      }
      PrintWriter writer = response.getWriter();
      if ("txt".equals(arguments.getResponseType())) {
        writer.write(arguments.getNewFileName() + "|" + errorMsg);
      } else if (checkFuncNum(arguments)) {
        handleOnUploadCompleteCallFuncResponse(writer, errorMsg, path, arguments, configuration);
      } else {
        handleOnUploadCompleteResponse(writer, errorMsg, arguments, configuration);
      }
      writer.flush();
    } catch (IOException | SecurityException e) {
      throw new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED, e);
    }
  }

  /**
   * check if func num is set in request.
   *
   * @param arguments
   * @return true if is.
   */
  protected boolean checkFuncNum(FileUploadArguments arguments) {
    return arguments.getCkFinderFuncNum() != null;
  }

  /**
   * return response when func num is set.
   *
   * @param out response.
   * @param errorMsg error message
   * @param path path
   * @param arguments
   * @param configuration connector configuration
   * @throws IOException when error occurs.
   */
  protected void handleOnUploadCompleteCallFuncResponse(Writer out, String errorMsg, String path, FileUploadArguments arguments, IConfiguration configuration) throws IOException {
    arguments.setCkFinderFuncNum(arguments.getCkFinderFuncNum().replaceAll("[^\\d]", ""));
    out.write("<script type=\"text/javascript\">");
    out.write("window.parent.CKFinder.tools.callFunction("
            + arguments.getCkFinderFuncNum() + ", '"
            + path
            + FileUtils.backupWithBackSlash(arguments.getNewFileName(), "'")
            + "', '" + errorMsg + "');");
    out.write("</script>");
  }

  /**
   *
   * @param writer out put stream
   * @param errorMsg error message
   * @param arguments
   * @param configuration connector configuration
   * @throws IOException when error occurs
   */
  protected void handleOnUploadCompleteResponse(Writer writer, String errorMsg, FileUploadArguments arguments, IConfiguration configuration) throws IOException {
    writer.write("<script type=\"text/javascript\">");
    writer.write("window.parent.OnUploadCompleted(");
    writer.write("'" + FileUtils.backupWithBackSlash(arguments.getNewFileName(), "'") + "'");
    writer.write(", '"
            + (arguments.getErrorCode()
            != Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE ? errorMsg
                    : "") + "'");
    writer.write(");");
    writer.write("</script>");
  }

  /**
   * initializing parametrs for command handler.
   *
   * @param arguments
   * @param request request
   * @param configuration connector configuration.
   */
  @Override
  protected void initParams(FileUploadArguments arguments, HttpServletRequest request, IConfiguration configuration) {
    try {
      super.initParams(arguments, request, configuration);
    } catch (ConnectorException ex) {
      arguments.setErrorCode(ex.getErrorCode());
    }
    arguments.setCkFinderFuncNum(request.getParameter("CKFinderFuncNum"));
    arguments.setCkEditorFuncNum(request.getParameter("CKEditorFuncNum"));
    arguments.setResponseType(request.getParameter("response_type") != null ? request.getParameter("response_type") : request.getParameter("responseType"));
    arguments.setLangCode(request.getParameter("langCode"));

    if (arguments.getErrorCode() == Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE) {
      arguments.setUploaded(uploadFile(request, arguments, configuration));
    }

  }

  /**
   * uploads file and saves to file.
   *
   * @param request request
   * @return true if uploaded correctly.
   */
  private boolean uploadFile(HttpServletRequest request, FileUploadArguments arguments, IConfiguration configuration) {
    if (!configuration.getAccessControl().hasPermission(arguments.getType(),
            arguments.getCurrentFolder(), arguments.getUserRole(),
            AccessControl.CKFINDER_CONNECTOR_ACL_FILE_UPLOAD)) {
      arguments.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED);
      return false;
    }
    return fileUpload(request, arguments, configuration);
  }

  /**
   *
   * @param request http request
   * @return true if uploaded correctly
   */
  private boolean fileUpload(HttpServletRequest request, FileUploadArguments arguments, IConfiguration configuration) {
    try {
      Collection<Part> parts = request.getParts();
      for (Part part : parts) {
        String path = Paths.get(configuration.getTypes().get(arguments.getType()).getPath(),
                arguments.getCurrentFolder()).toString();
        arguments.setFileName(getFileItemName(part));
        if (validateUploadItem(part, path, arguments, configuration)) {
          return saveTemporaryFile(path, part, arguments, configuration);
        }
      }
      return false;
    } catch (ConnectorException e) {
      arguments.setErrorCode(e.getErrorCode());
      if (arguments.getErrorCode() == Constants.Errors.CKFINDER_CONNECTOR_ERROR_CUSTOM_ERROR) {
        arguments.setCustomErrorMsg(e.getMessage());
      }
      return false;
    } catch (IOException | ServletException | RuntimeException e) {
      String message = e.getMessage();
      if (message != null
              && (message.toLowerCase().contains("sizelimit")
              || message.contains("size limit"))) {
        log.info("", e);
        arguments.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_UPLOADED_TOO_BIG);
        return false;
      }
      log.error("", e);
      arguments.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED);
      return false;
    }

  }

  /**
   * saves temporary file in the correct file path.
   *
   * @param path path to save file
   * @param item file upload item
   * @return result of saving, true if saved correctly
   * @throws Exception when error occurs.
   */
  private boolean saveTemporaryFile(String path, Part item, FileUploadArguments arguments, IConfiguration configuration)
          throws IOException, ConnectorException {
    Path file = Paths.get(path, arguments.getNewFileName());

    if (!ImageUtils.isImageExtension(file)) {
      item.write(file.toString());
      if (configuration.getEvents() != null) {
        AfterFileUploadEventArgs args = new AfterFileUploadEventArgs(arguments.getCurrentFolder(), file);
        configuration.getEvents().runAfterFileUpload(args, configuration);
      }
      return true;
    } else if (ImageUtils.checkImageSize(item, configuration)
            || configuration.isCheckSizeAfterScaling()) {
      ImageUtils.createTmpThumb(item, file, getFileItemName(item), configuration);
      if (!configuration.isCheckSizeAfterScaling()
              || FileUtils.isFileSizeInRange(configuration.getTypes().get(arguments.getType()), Files.size(file))) {
        if (configuration.getEvents() != null) {
          AfterFileUploadEventArgs args = new AfterFileUploadEventArgs(arguments.getCurrentFolder(), file);
          configuration.getEvents().runAfterFileUpload(args, configuration);
        }
        return true;
      } else {
        Files.deleteIfExists(file);
        arguments.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_UPLOADED_TOO_BIG);
        return false;
      }
    } else {
      arguments.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_UPLOADED_TOO_BIG);
      return false;
    }
  }

  /**
   * if file exists this method adds (number) to file.
   *
   * @param path folder
   * @param name file name
   * @return new file name.
   */
  private String getFinalFileName(String path, String name, FileUploadArguments arguments) {
    Path file = Paths.get(path, name);
    int number = 0;

    String nameWithoutExtension = FileUtils.getFileNameWithoutExtension(name, false);

    if (Files.exists(file) || isProtectedName(nameWithoutExtension)) {
      @SuppressWarnings("StringBufferWithoutInitialCapacity")
      StringBuilder sb = new StringBuilder();
      sb.append(FileUtils.getFileNameWithoutExtension(name, false)).append("(");
      int len = sb.length();
      do {
        number++;
        sb.append(number).append(").").append(FileUtils.getFileExtension(name, false));
        arguments.setNewFileName(sb.toString());
        sb.setLength(len);
        file = Paths.get(path, arguments.getNewFileName());
        arguments.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_UPLOADED_FILE_RENAMED);
      } while (Files.exists(file));
    }
    return arguments.getNewFileName();
  }

  /**
   * validates uploaded file.
   *
   * @param item uploaded item.
   * @param path file path
   * @return true if validation
   */
  private boolean validateUploadItem(Part item, String path, FileUploadArguments arguments, IConfiguration configuration) {

    if (item.getSubmittedFileName() != null && item.getSubmittedFileName().length() > 0) {
      arguments.setFileName(getFileItemName(item));
    } else {
      arguments.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_UPLOADED_INVALID);
      return false;
    }
    arguments.setNewFileName(arguments.getFileName());

    arguments.setNewFileName(UNSAFE_FILE_NAME_PATTERN.matcher(arguments.getNewFileName()).replaceAll("_"));

    if (configuration.isDisallowUnsafeCharacters()) {
      arguments.setNewFileName(arguments.getNewFileName().replace(';', '_'));
    }
    if (configuration.isForceAscii()) {
      arguments.setNewFileName(FileUtils.convertToASCII(arguments.getNewFileName()));
    }
    if (!arguments.getNewFileName().equals(arguments.getFileName())) {
      arguments.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_UPLOADED_INVALID_NAME_RENAMED);
    }

    if (FileUtils.isDirectoryHidden(arguments.getCurrentFolder(), configuration)) {
      arguments.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST);
      return false;
    }
    if (!FileUtils.isFileNameInvalid(arguments.getNewFileName())
            || FileUtils.isFileHidden(arguments.getNewFileName(), configuration)) {
      arguments.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_NAME);
      return false;
    }
    final ResourceType resourceType = configuration.getTypes().get(arguments.getType());
    int checkFileExt = FileUtils.checkFileExtension(arguments.getNewFileName(), resourceType);
    if (checkFileExt == 1) {
      arguments.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_EXTENSION);
      return false;
    }
    if (configuration.isCheckDoubleFileExtensions()) {
      arguments.setNewFileName(FileUtils.renameFileWithBadExt(resourceType, arguments.getNewFileName()));
    }

    try {
      Path file = Paths.get(path, getFinalFileName(path, arguments.getNewFileName(), arguments));
      if (!(ImageUtils.isImageExtension(file) && configuration.isCheckSizeAfterScaling())
              && !FileUtils.isFileSizeInRange(resourceType, item.getSize())) {
        arguments.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_UPLOADED_TOO_BIG);
        return false;
      }

      if (configuration.isSecureImageUploads() && ImageUtils.isImageExtension(file)
              && !ImageUtils.isValid(item)) {
        arguments.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_UPLOADED_CORRUPT);
        return false;
      }

      if (!FileUtils.isExtensionHtml(file.getFileName().toString(), configuration)
              && FileUtils.hasHtmlContent(item)) {
        arguments.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_UPLOADED_WRONG_HTML_FILE);
        return false;
      }
    } catch (SecurityException | IOException e) {
      log.error("", e);
      arguments.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED);
      return false;
    }

    return true;
  }

  /**
   * set response headers. Not user in this command.
   *
   * @param request request
   * @param response response
   * @param arguments
   */
  @Override
  void setResponseHeader(HttpServletRequest request, HttpServletResponse response, FileUploadArguments arguments) {
    response.setContentType("text/html;charset=UTF-8");
  }

  /**
   * save if uploaded file item name is full file path not only file name.
   *
   * @param item file upload item
   * @return file name of uploaded item
   */
  private String getFileItemName(Part item) {
    Pattern p = Pattern.compile("[^\\\\/]+$");
    Matcher m = p.matcher(item.getSubmittedFileName());

    return (m.find()) ? m.group(0) : "";
  }

  @Deprecated
  @Override
  protected boolean isCurrFolderExists(FileUploadArguments arguments, HttpServletRequest request, IConfiguration configuration) {
    try {
      String tmpType = request.getParameter("type");
      if (tmpType != null) {
        try {
          checkTypeExists(tmpType, configuration);
        } catch (ConnectorException ex) {
          arguments.setErrorCode(ex.getErrorCode());
          return false;
        }
        Path currDir = Paths.get(configuration.getTypes().get(tmpType).getPath(),
                arguments.getCurrentFolder());
        if (!Files.isDirectory(currDir)) {
          throw new ConnectorException(
                  Constants.Errors.CKFINDER_CONNECTOR_ERROR_FOLDER_NOT_FOUND);
        } else {
          return true;
        }
      }
      return true;
    } catch (ConnectorException ex) {
      arguments.setErrorCode(ex.getErrorCode());
      return false;
    }
  }

  private boolean isProtectedName(String nameWithoutExtension) {
    return Pattern.compile("^(AUX|COM\\d|CLOCK\\$|CON|NUL|PRN|LPT\\d)$",
            Pattern.CASE_INSENSITIVE).matcher(nameWithoutExtension).matches();
  }

}
