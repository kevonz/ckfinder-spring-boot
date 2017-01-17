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
package com.github.zhanhb.ckfinder.connector.utils;

import com.github.zhanhb.ckfinder.connector.handlers.arguments.ErrorNode;
import com.github.zhanhb.ckfinder.connector.handlers.arguments.XMLArguments;
import com.github.zhanhb.ckfinder.connector.handlers.response.Connector;
import com.github.zhanhb.ckfinder.connector.handlers.response.DetailError;
import com.github.zhanhb.ckfinder.connector.handlers.response.Error;
import com.github.zhanhb.ckfinder.connector.handlers.response.Errors;
import java.io.Writer;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

/**
 * Class to create XML document.
 */
public enum XMLCreator {
  INSTANCE;

  public void writeTo(Connector connector, Writer writer) {
    try {
      JAXBContext.newInstance(Connector.class).createMarshaller().marshal(connector, writer);
    } catch (JAXBException e) {
      throw new IllegalStateException("fail to instance xml transformer", e);
    }
  }

  /**
   * adds error node to root element with error code.
   *
   * @param rootElement XML root node.
   * @param errorNum error code number.
   * @param errorText error text.
   */
  public void addErrorCommandToRoot(Connector.Builder rootElement, int errorNum, String errorText) {
    rootElement.error(Error.builder()
            .number(errorNum)
            .value(errorText).build());
  }

  /**
   * save errors node to list.
   *
   * @param arguments
   * @param errorCode error code
   * @param name file name
   * @param path current folder
   * @param type resource type
   */
  public void appendErrorNodeChild(XMLArguments arguments, int errorCode, String name, String path, String type) {
    arguments.getErrorList().add(ErrorNode.builder().type(type).name(name).folder(path).errorCode(errorCode).build());
  }

  /**
   * checks if error list contains errors.
   *
   * @param arguments
   * @return true if there are any errors.
   */
  public boolean hasErrors(XMLArguments arguments) {
    return !arguments.getErrorList().isEmpty();
  }

  /**
   * add all error nodes from saved list to xml.
   *
   * @param arguments
   * @param rootElement XML root element
   */
  public void addErrors(XMLArguments arguments, Connector.Builder rootElement) {
    if (hasErrors(arguments)) {
      Errors.Builder errorsNode = Errors.builder();
      for (ErrorNode item : arguments.getErrorList()) {
        DetailError childElem = DetailError.builder()
                .code(item.getErrorCode())
                .name(item.getName())
                .type(item.getType())
                .folder(item.getFolder())
                .build();
        errorsNode.error(childElem);
      }
      rootElement.errors(errorsNode.build());
    }
  }

}
