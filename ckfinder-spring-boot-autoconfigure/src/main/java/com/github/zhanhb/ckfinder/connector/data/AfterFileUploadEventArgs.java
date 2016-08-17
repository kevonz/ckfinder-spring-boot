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
package com.github.zhanhb.ckfinder.connector.data;

import java.nio.file.Path;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Event data for
 * {@link com.github.zhanhb.ckfinder.connector.configuration.Events.Builder#afterFileUploadEventHandler(com.github.zhanhb.ckfinder.connector.data.AfterFileUploadEventHandler) }
 * event.
 */
@Getter
@RequiredArgsConstructor
@ToString
public class AfterFileUploadEventArgs {

  private final String currentFolder;
  private final Path file;

}
