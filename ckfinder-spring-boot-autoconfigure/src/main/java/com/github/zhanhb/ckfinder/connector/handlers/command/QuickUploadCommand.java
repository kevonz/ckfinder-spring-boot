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

import com.github.zhanhb.ckfinder.connector.handlers.arguments.FileUploadArguments;
import com.github.zhanhb.ckfinder.connector.utils.FileUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Class to handle <code>QuickUpload</code> command.
 */
public class QuickUploadCommand extends FileUploadCommand {

  @Override
  protected void handleOnUploadCompleteResponse(Writer writer, String errorMsg, FileUploadArguments arguments) throws IOException {
    if (arguments.getResponseType() != null && arguments.getResponseType().equalsIgnoreCase("json")) {
      handleJSONResponse(writer, errorMsg, null, arguments);
    } else {
      writer.write("<script type=\"text/javascript\">");
      writer.write("window.parent.OnUploadCompleted(");
      writer.write("" + arguments.getErrorCode() + ", ");
      if (arguments.isUploaded()) {
        writer.write("'" + getConfiguration().getTypes().get(arguments.getType()).getUrl()
                + arguments.getCurrentFolder()
                + FileUtils.backupWithBackSlash(FileUtils.encodeURIComponent(arguments.getNewFileName()), "'")
                + "', ");
        writer.write("'" + FileUtils.backupWithBackSlash(arguments.getNewFileName(), "'")
                + "', ");
      } else {
        writer.write("'', '', ");
      }
      writer.write("''");
      writer.write(");");
      writer.write("</script>");
    }
  }

  @Override
  protected void handleOnUploadCompleteCallFuncResponse(Writer writer,
          String errorMsg, String path, FileUploadArguments arguments)
          throws IOException {
    if (arguments.getResponseType() != null && arguments.getResponseType().equalsIgnoreCase("json")) {
      handleJSONResponse(writer, errorMsg, path, arguments);
    } else {
      writer.write("<script type=\"text/javascript\">");
      arguments.setCkEditorFuncNum(arguments.getCkEditorFuncNum().replaceAll("[^\\d]", ""));
      writer.write(("window.parent.CKEDITOR.tools.callFunction("
              + arguments.getCkEditorFuncNum() + ", '"
              + path
              + FileUtils.backupWithBackSlash(FileUtils.encodeURIComponent(arguments.getNewFileName()), "'")
              + "', '" + errorMsg + "');"));
      writer.write("</script>");
    }
  }

  @Override
  protected boolean checkFuncNum(FileUploadArguments arguments) {
    return arguments.getCkEditorFuncNum() != null;
  }

  @Override
  void setResponseHeader(HttpServletRequest request, HttpServletResponse response, FileUploadArguments arguments) {
    if (arguments.getResponseType() != null && arguments.getResponseType().equalsIgnoreCase("json")) {
      response.setContentType("application/json;charset=UTF-8");
    } else {
      response.setContentType("text/html;charset=UTF-8");
    }
  }

  /**
   * Writes JSON object into response stream after uploading file which was
   * dragged and dropped in to CKEditor 4.5 or higher.
   *
   * @param writer the response stream
   * @param errorMsg string representing error message which indicates that
   * there was an error during upload or uploaded file was renamed
   * @param path path to uploaded file
   */
  private void handleJSONResponse(Writer writer, String errorMsg, String path,
          FileUploadArguments arguments) throws IOException {

    Gson gson = new GsonBuilder().serializeNulls().create();
    Map<String, Object> jsonObj = new HashMap<>(6);

    jsonObj.put("fileName", arguments.getNewFileName());
    jsonObj.put("uploaded", arguments.isUploaded() ? 1 : 0);

    if (arguments.isUploaded()) {
      if (path != null && !path.isEmpty()) {
        jsonObj.put("url", path + FileUtils.backupWithBackSlash(FileUtils.encodeURIComponent(arguments.getNewFileName()), "'"));
      } else {
        jsonObj.put("url",
                getConfiguration().getTypes().get(arguments.getType()).getUrl()
                + arguments.getCurrentFolder()
                + FileUtils.backupWithBackSlash(FileUtils
                        .encodeURIComponent(arguments.getNewFileName()),
                        "'"));
      }
    }

    if (errorMsg != null && !errorMsg.isEmpty()) {
      Map<String, Object> jsonErrObj = new HashMap<>(3);
      jsonErrObj.put("number", arguments.getErrorCode());
      jsonErrObj.put("message", errorMsg);
      jsonObj.put("error", jsonErrObj);
    }

    writer.write(gson.toJson(jsonObj));
  }

}
