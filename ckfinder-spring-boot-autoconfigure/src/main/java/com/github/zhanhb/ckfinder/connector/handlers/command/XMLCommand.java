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
import com.github.zhanhb.ckfinder.connector.handlers.arguments.ErrorNode;
import com.github.zhanhb.ckfinder.connector.handlers.arguments.XMLArguments;
import com.github.zhanhb.ckfinder.connector.utils.XMLCreator;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.w3c.dom.Element;

/**
 * Base class to handle XML commands.
 *
 * @param <T>
 */
public abstract class XMLCommand<T extends XMLArguments> extends Command<T> {

  public XMLCommand(Supplier<T> argumentsSupplier) {
    super(argumentsSupplier);
  }

  /**
   * sets response headers for XML response.
   *
   * @param request
   * @param response response
   * @param arguments
   */
  @Override
  public void setResponseHeader(HttpServletRequest request, HttpServletResponse response, T arguments) {
    response.setContentType("text/xml");
    response.setHeader("Cache-Control", "no-cache");
    response.setCharacterEncoding("utf-8");
  }

  /**
   * executes XML command. Creates XML response and writes it to response output
   * stream.
   *
   * @throws ConnectorException to handle in error handler.
   */
  @Override
  @SuppressWarnings("FinalMethod")
  final void execute(T arguments, HttpServletResponse response) throws ConnectorException {
    try (PrintWriter out = response.getWriter()) {
      createXMLResponse(arguments, getDataForXml(arguments));
      XMLCreator.INSTANCE.writeTo(arguments.getDocument(), out);
    } catch (ConnectorException e) {
      throw e;
    } catch (IOException e) {
      throw new ConnectorException(Constants.Errors.CKFINDER_CONNECTOR_ERROR_ACCESS_DENIED, e);
    }
  }

  /**
   * abstract method to create XML response in command.
   *
   * @param errorNum error code from method getDataForXml()
   * @throws ConnectorException to handle in error handler.
   */
  private void createXMLResponse(T arguments, int errorNum) throws ConnectorException, IOException {
    Element rootElement = arguments.getDocument().createElement("Connector");
    if (arguments.getType() != null && !arguments.getType().isEmpty()) {
      rootElement.setAttribute("resourceType", arguments.getType());
    }
    if (mustAddCurrentFolderNode(arguments)) {
      createCurrentFolderNode(arguments, rootElement);
    }
    XMLCreator.INSTANCE.addErrorCommandToRoot(arguments.getDocument(), rootElement, errorNum, getErrorMsg(arguments));
    createXMLChildNodes(errorNum, rootElement, arguments);
    arguments.getDocument().appendChild(rootElement);
  }

  /**
   * gets error message if needed.
   *
   * @param errorNum error code
   * @return error message
   */
  protected String getErrorMsg(T arguments) {
    return null;
  }

  /**
   * abstract method to create XML nodes for commands.
   *
   * @param errorNum error code
   * @param rootElement XML root node
   * @param arguments
   * @throws java.io.IOException
   */
  protected abstract void createXMLChildNodes(int errorNum, Element rootElement, T arguments) throws IOException;

  /**
   * gets all necessary data to create XML response.
   *
   * @param arguments
   * @return error code
   * {@link com.github.zhanhb.ckfinder.connector.configuration.Constants.Errors}
   * or
   * {@link com.github.zhanhb.ckfinder.connector.configuration.Constants.Errors#CKFINDER_CONNECTOR_ERROR_NONE}
   * if no error occurred.
   * @throws java.io.IOException
   */
  protected abstract int getDataForXml(T arguments) throws IOException;

  /**
   * creates <code>CurrentFolder</code> element.
   *
   * @param arguments
   * @param rootElement XML root node.
   */
  @SuppressWarnings("FinalMethod")
  protected final void createCurrentFolderNode(T arguments, Element rootElement) {
    Element element = arguments.getDocument().createElement("CurrentFolder");
    element.setAttribute("path", arguments.getCurrentFolder());
    element.setAttribute("url", getConfiguration().getTypes().get(arguments.getType()).getUrl()
            + arguments.getCurrentFolder());
    element.setAttribute("acl", String.valueOf(getConfiguration().getAccessControl().checkACLForRole(arguments.getType(), arguments.getCurrentFolder(), arguments.getUserRole())));
    rootElement.appendChild(element);
  }

  @Override
  protected void initParams(T arguments, HttpServletRequest request, IConfiguration configuration)
          throws ConnectorException {
    super.initParams(arguments, request, configuration);
    arguments.setDocument(XMLCreator.INSTANCE.createDocument());
  }

  /**
   * whether <code>CurrentFolder</code> element should be added to the XML
   * response.
   *
   * @return true if must.
   */
  protected boolean mustAddCurrentFolderNode(T arguments) {
    return arguments.getType() != null && arguments.getCurrentFolder() != null;
  }

  /**
   * save errors node to list.
   *
   * @param errorCode error code
   * @param name file name
   * @param path current folder
   * @param type resource type
   */
  @SuppressWarnings("FinalMethod")
  protected final void appendErrorNodeChild(T arguments, int errorCode, String name, String path, String type) {
    arguments.getErrorList().add(ErrorNode.builder().type(type).name(name).folder(path).errorCode(errorCode).build());
  }

  /**
   * checks if error list contains errors.
   *
   * @return true if there are any errors.
   */
  @SuppressWarnings("FinalMethod")
  protected final boolean hasErrors(T arguments) {
    return !arguments.getErrorList().isEmpty();
  }

  /**
   * add all error nodes from saved list to xml.
   *
   * @param errorsNode XML errors node
   */
  @SuppressWarnings("FinalMethod")
  protected final void addErrors(T arguments, Element errorsNode) {
    for (ErrorNode item : arguments.getErrorList()) {
      Element childElem = arguments.getDocument().createElement("Error");
      childElem.setAttribute("code", String.valueOf(item.getErrorCode()));
      childElem.setAttribute("name", item.getName());
      childElem.setAttribute("type", item.getType());
      childElem.setAttribute("folder", item.getFolder());
      errorsNode.appendChild(childElem);
    }
  }

}
