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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;

/**
 * Class to handle <code>FileUpload</code> command.
 *
 */
@Slf4j
@SuppressWarnings("ProtectedField")
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
  final void execute(HttpServletResponse response) throws ConnectorException {
    try {
      String errorMsg = this.getErrorCode() == Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE ? "" : (this.getErrorCode() == Constants.Errors.CKFINDER_CONNECTOR_ERROR_CUSTOM_ERROR ? getArguments().getCustomErrorMsg()
              : ErrorUtils.INSTANCE.getErrorMsgByLangAndCode(getArguments().getLangCode(), this.getErrorCode(), this.getConfiguration()));
      errorMsg = errorMsg.replace("%1", this.getNewFileName());
      String path = "";

      if (!isUploaded()) {
        getArguments().setNewFileName("");
        getArguments().setCurrentFolder("");
      } else {
        path = getConfiguration().getTypes().get(getArguments().getType()).getUrl()
                + getArguments().getCurrentFolder();
      }
      PrintWriter writer = response.getWriter();
      if (getArguments().getResponseType() != null && getArguments().getResponseType().equals("txt")) {
        writer.write(this.getNewFileName() + "|" + errorMsg);
      } else if (checkFuncNum()) {
        handleOnUploadCompleteCallFuncResponse(writer, errorMsg, path);
      } else {
        handleOnUploadCompleteResponse(writer, errorMsg);
      }
      writer.flush();
    } catch (IOException e) {
      throw new ConnectorException(
              Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED, e);
    }
  }

  /**
   * check if func num is set in request.
   *
   * @return true if is.
   */
  protected boolean checkFuncNum() {
    return getArguments().getCkFinderFuncNum() != null;
  }

  /**
   * return response when func num is set.
   *
   * @param out response.
   * @param errorMsg error message
   * @param path path
   * @throws IOException when error occurs.
   */
  protected void handleOnUploadCompleteCallFuncResponse(Writer out,
          String errorMsg,
          String path)
          throws IOException {
    getArguments().setCkFinderFuncNum(getArguments().getCkFinderFuncNum().replaceAll(
            "[^\\d]", ""));
    out.write("<script type=\"text/javascript\">");
    out.write("window.parent.CKFinder.tools.callFunction("
            + getArguments().getCkFinderFuncNum() + ", '"
            + path
            + FileUtils.backupWithBackSlash(this.getNewFileName(), "'")
            + "', '" + errorMsg + "');");
    out.write("</script>");
  }

  /**
   *
   * @param writer out put stream
   * @param errorMsg error message
   * @throws IOException when error occurs
   */
  protected void handleOnUploadCompleteResponse(Writer writer,
          String errorMsg) throws IOException {
    writer.write("<script type=\"text/javascript\">");
    writer.write("window.parent.OnUploadCompleted(");
    writer.write("'" + FileUtils.backupWithBackSlash(this.getNewFileName(), "'") + "'");
    writer.write(", '"
            + (this.getErrorCode()
            != Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE ? errorMsg
                    : "") + "'");
    writer.write(");");
    writer.write("</script>");
  }

  /**
   * initializing parametrs for command handler.
   *
   * @param request request
   * @param configuration connector configuration.
   * @throws ConnectorException when error occurs.
   */
  @Override
  protected void initParams(HttpServletRequest request, IConfiguration configuration)
          throws ConnectorException {
    super.initParams(request, configuration);
    getArguments().setCkFinderFuncNum(request.getParameter("CKFinderFuncNum"));
    this.setCkEditorFuncNum(request.getParameter("CKEditorFuncNum"));
    getArguments().setResponseType(request.getParameter("response_type") != null ? request.getParameter("response_type") : request.getParameter("responseType"));
    getArguments().setLangCode(request.getParameter("langCode"));

    if (this.getErrorCode() == Constants.Errors.CKFINDER_CONNECTOR_ERROR_NONE) {
      this.setUploaded(uploadFile(request));
    }

  }

  /**
   * uploads file and saves to file.
   *
   * @param request request
   * @return true if uploaded correctly.
   */
  private boolean uploadFile(HttpServletRequest request) {
    if (!getConfiguration().getAccessControl().checkFolderACL(getArguments().getType(),
            getArguments().getCurrentFolder(), getArguments().getUserRole(),
            AccessControl.CKFINDER_CONNECTOR_ACL_FILE_UPLOAD)) {
      this.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_UNAUTHORIZED);
      return false;
    }
    return fileUpload(request);
  }

  /**
   *
   * @param request http request
   * @return true if uploaded correctly
   */
  private boolean fileUpload(HttpServletRequest request) {
    try {
      Collection<Part> parts = request.getParts();
      for (Part part : parts) {
        String path = getConfiguration().getTypes().get(getArguments().getType()).getPath() + getArguments().getCurrentFolder();
        getArguments().setFileName(getFileItemName(part));
        if (validateUploadItem(part, path)) {
          return saveTemporaryFile(path, part);
        }
      }
      return false;
    } catch (ConnectorException e) {
      this.setErrorCode(e.getErrorCode());
      if (this.getErrorCode() == Constants.Errors.CKFINDER_CONNECTOR_ERROR_CUSTOM_ERROR) {
        getArguments().setCustomErrorMsg(e.getMessage());
      }
      return false;
    } catch (Exception e) {
      String message = e.getMessage().toLowerCase();
      if (message.contains("sizelimit") || message.contains("size limit")) {
        log.info("", e);
        this.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_UPLOADED_TOO_BIG);
        return false;
      }
      log.error("", e);
      this.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED);
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
  private boolean saveTemporaryFile(String path, Part item)
          throws Exception {
    Path file = Paths.get(path, this.getNewFileName());

    if (!ImageUtils.isImageExtension(file)) {
      item.write(file.toString());
      if (getConfiguration().getEvents() != null) {
        AfterFileUploadEventArgs args = new AfterFileUploadEventArgs(getArguments().getCurrentFolder(), file);
        getConfiguration().getEvents().runAfterFileUpload(args, getConfiguration());
      }
      return true;
    } else if (ImageUtils.checkImageSize(item, this.getConfiguration())
            || getConfiguration().isCheckSizeAfterScaling()) {
      ImageUtils.createTmpThumb(item.getInputStream(), file, getFileItemName(item), this.getConfiguration());
      if (!getConfiguration().isCheckSizeAfterScaling()
              || FileUtils.isFileSizeInRange(getConfiguration().getTypes().get(getArguments().getType()), Files.size(file))) {
        if (getConfiguration().getEvents() != null) {
          AfterFileUploadEventArgs args = new AfterFileUploadEventArgs(getArguments().getCurrentFolder(), file);
          getConfiguration().getEvents().runAfterFileUpload(args, getConfiguration());
        }
        return true;
      } else {
        Files.deleteIfExists(file);
        this.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_UPLOADED_TOO_BIG);
        return false;
      }
    } else {
      this.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_UPLOADED_TOO_BIG);
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
  private String getFinalFileName(String path, String name) {
    Path file = Paths.get(path, name);
    int number = 0;

    String nameWithoutExtension = FileUtils.getFileNameWithoutExtension(name, false);
    Pattern p = Pattern.compile("^(AUX|COM\\d|CLOCK\\$|CON|NUL|PRN|LPT\\d)$", Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(nameWithoutExtension);
    boolean protectedName = m.find();

    while (true) {
      if (Files.exists(file) || protectedName) {
        number++;
        @SuppressWarnings("StringBufferWithoutInitialCapacity")
        StringBuilder sb = new StringBuilder();
        sb.append(FileUtils.getFileNameWithoutExtension(name, false));
        sb.append("(").append(number).append(").");
        sb.append(FileUtils.getFileExtension(name, false));
        getArguments().setNewFileName(sb.toString());
        file = Paths.get(path, this.getNewFileName());
        protectedName = false;
        this.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_UPLOADED_FILE_RENAMED);
      } else {
        return this.getNewFileName();
      }
    }
  }

  /**
   * validates uploaded file.
   *
   * @param item uploaded item.
   * @param path file path
   * @return true if validation
   */
  private boolean validateUploadItem(Part item, String path) {

    if (item.getSubmittedFileName() != null && item.getSubmittedFileName().length() > 0) {
      getArguments().setFileName(getFileItemName(item));
    } else {
      this.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_UPLOADED_INVALID);
      return false;
    }
    getArguments().setNewFileName(getArguments().getFileName());

    getArguments().setNewFileName(UNSAFE_FILE_NAME_PATTERN.matcher(this.getNewFileName()).replaceAll("_"));

    if (getConfiguration().isDisallowUnsafeCharacters()) {
      getArguments().setNewFileName(this.getNewFileName().replace(';', '_'));
    }
    if (getConfiguration().isForceAscii()) {
      getArguments().setNewFileName(FileUtils.convertToASCII(this.getNewFileName()));
    }
    if (!getArguments().getNewFileName().equals(getArguments().getFileName())) {
      this.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_UPLOADED_INVALID_NAME_RENAMED);
    }

    if (FileUtils.isDirectoryHidden(getArguments().getCurrentFolder(), getConfiguration())) {
      this.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST);
      return false;
    }
    if (!FileUtils.isFileNameInvalid(getArguments().getNewFileName())
            || FileUtils.isFileHidden(this.getNewFileName(), getConfiguration())) {
      this.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_NAME);
      return false;
    }
    final ResourceType resourceType = getConfiguration().getTypes().get(getArguments().getType());
    int checkFileExt = FileUtils.checkFileExtension(this.getNewFileName(), resourceType);
    if (checkFileExt == 1) {
      this.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_EXTENSION);
      return false;
    }
    if (getConfiguration().isCheckDoubleFileExtensions()) {
      getArguments().setNewFileName(FileUtils.renameFileWithBadExt(resourceType, this.getNewFileName()));
    }

    try {
      Path file = Paths.get(path, getFinalFileName(path, this.getNewFileName()));
      if (!(ImageUtils.isImageExtension(file) && getConfiguration().isCheckSizeAfterScaling())
              && !FileUtils.isFileSizeInRange(resourceType, item.getSize())) {
        this.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_UPLOADED_TOO_BIG);
        return false;
      }

      if (getConfiguration().isSecureImageUploads() && ImageUtils.isImageExtension(file)
              && !ImageUtils.isValid(item)) {
        this.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_UPLOADED_CORRUPT);
        return false;
      }

      if (!FileUtils.isExtensionHtml(file.getFileName().toString(), getConfiguration())
              && FileUtils.detectHtml(item)) {
        this.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_UPLOADED_WRONG_HTML_FILE);
        return false;
      }
    } catch (SecurityException | IOException e) {
      log.error("", e);
      this.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED);
      return false;
    }

    return true;
  }

  /**
   * set response headers. Not user in this command.
   *
   * @param request request
   * @param response response
   */
  @Override
  public void setResponseHeader(HttpServletRequest request, HttpServletResponse response) {
    response.setCharacterEncoding("utf-8");
    response.setContentType("text/html");
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

  /**
   * check request for security issue.
   *
   * @param reqParam request param
   * @return true if validation passed
   * @throws ConnectorException if validation error occurs.
   */
  @Override
  protected boolean isRequestPathValid(String reqParam)
          throws ConnectorException {
    if (reqParam == null || reqParam.isEmpty()) {
      return true;
    }
    if (Pattern.compile(Constants.INVALID_PATH_REGEX).matcher(reqParam).find()) {
      this.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_NAME);
      return false;
    }
    return true;
  }

  @Override
  protected boolean isHidden()
          throws ConnectorException {
    if (FileUtils.isDirectoryHidden(getArguments().getCurrentFolder(), getConfiguration())) {
      this.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_REQUEST);
      return true;
    }
    return false;
  }

  @Override
  protected boolean isConnectorEnabled()
          throws ConnectorException {
    if (!getConfiguration().isEnabled()) {
      this.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_CONNECTOR_DISABLED);
      return false;
    }
    return true;
  }

  @Override
  protected boolean isCurrFolderExists(HttpServletRequest request)
          throws ConnectorException {
    String tmpType = request.getParameter("type");
    if (isTypeExists(tmpType)) {
      Path currDir = Paths.get(getConfiguration().getTypes().get(tmpType).getPath()
              + getArguments().getCurrentFolder());
      if (Files.exists(currDir) && Files.isDirectory(currDir)) {
        return true;
      } else {
        this.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_FOLDER_NOT_FOUND);
        return false;
      }
    }
    return false;
  }

  @Override
  protected boolean isTypeExists(String type) {
    ResourceType testType = getConfiguration().getTypes().get(type);
    if (testType == null) {
      this.setErrorCode(Constants.Errors.CKFINDER_CONNECTOR_ERROR_INVALID_TYPE);
      return false;
    }
    return true;
  }

  /**
   * @return the ckEditorFuncNum
   */
  protected String getCkEditorFuncNum() {
    return getArguments().getCkEditorFuncNum();
  }

  /**
   * @param ckEditorFuncNum the ckEditorFuncNum to set
   */
  protected void setCkEditorFuncNum(String ckEditorFuncNum) {
    getArguments().setCkEditorFuncNum(ckEditorFuncNum);
  }

  /**
   * @return the newFileName
   */
  protected String getNewFileName() {
    return getArguments().getNewFileName();
  }

  /**
   * @return the uploaded
   */
  protected boolean isUploaded() {
    return getArguments().isUploaded();
  }

  /**
   * @param uploaded the uploaded to set
   */
  protected void setUploaded(boolean uploaded) {
    getArguments().setUploaded(uploaded);
  }

  /**
   * @return the errorCode
   */
  protected int getErrorCode() {
    return getArguments().getErrorCode();
  }

  /**
   * @param errorCode the errorCode to set
   */
  protected void setErrorCode(int errorCode) {
    getArguments().setErrorCode(errorCode);
  }

}
