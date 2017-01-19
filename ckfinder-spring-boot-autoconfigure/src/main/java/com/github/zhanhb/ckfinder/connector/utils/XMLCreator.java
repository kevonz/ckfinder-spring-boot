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

import com.github.zhanhb.ckfinder.connector.handlers.arguments.XMLArguments;
import com.github.zhanhb.ckfinder.connector.handlers.response.Connector;
import com.github.zhanhb.ckfinder.connector.handlers.response.DetailError;
import com.github.zhanhb.ckfinder.connector.handlers.response.Errors;
import java.io.Writer;
import java.util.List;
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
   * save errors node to list.
   *
   * @param arguments
   * @param errorCode error code
   * @param name file name
   * @param path current folder
   * @param type resource type
   */
  public void appendErrorNodeChild(XMLArguments arguments, int errorCode, String name, String path, String type) {
    arguments.getErrorList().add(DetailError.builder().type(type).name(name).folder(path).code(errorCode).build());
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
    List<DetailError> errorList = arguments.getErrorList();
    if (!errorList.isEmpty()) {
      rootElement.errors(Errors.builder().errors(errorList).build());
    }
  }

}
