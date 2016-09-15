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
package com.github.zhanhb.ckfinder.connector.configuration;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Base class for plugins.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Plugin {

  private final String name;
  private final boolean internal;

  /**
   * register event handlers for plugin.
   *
   * @param eventHandler available event handlers.
   */
  protected abstract void registerEventHandlers(Events.Builder eventHandler);

}
