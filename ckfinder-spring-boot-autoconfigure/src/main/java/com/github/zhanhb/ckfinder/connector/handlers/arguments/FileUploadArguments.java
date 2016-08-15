package com.github.zhanhb.ckfinder.connector.handlers.arguments;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author zhanhb
 */
@Getter
@Setter
public class FileUploadArguments extends Arguments {

  /**
   * Uploading file name request.
   */
  private String fileName;
  /**
   * File name after rename.
   */
  private String newFileName;
  /**
   * Function number to call after file upload is completed.
   */
  private String ckEditorFuncNum;
  /**
   * The selected response type to be used after file upload is completed.
   */
  private String responseType;
  /**
   * Function number to call after file upload is completed.
   */
  private String ckFinderFuncNum;
  /**
   * Language (locale) code.
   */
  private String langCode;
  /**
   * Flag informing if file was uploaded correctly.
   */
  private boolean uploaded;
  /**
   * Error code number.
   */
  private int errorCode;
  /**
   * Custom error message.
   */
  private String customErrorMsg;

  public FileUploadArguments() {
    this.errorCode = 0;
    this.fileName = "";
    this.newFileName = "";
    setType("");
    this.uploaded = false;
  }

}
