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
package com.github.zhanhb.ckfinder.connector.plugins;

import com.github.zhanhb.ckfinder.connector.configuration.IConfiguration;
import com.github.zhanhb.ckfinder.connector.data.InitCommandEventArgs;
import com.github.zhanhb.ckfinder.connector.data.InitCommandEventHandler;
import com.github.zhanhb.ckfinder.connector.handlers.response.ImageResizeInfo;
import com.github.zhanhb.ckfinder.connector.handlers.response.PluginsInfos;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ImageResizeInitCommandEventHandler implements InitCommandEventHandler {

  private final Map<String, String> params;

  @Override
  public boolean runEventHandler(InitCommandEventArgs args, IConfiguration configuration) {
    log.debug("runEventHandler: {} {}", args, configuration);
    ImageResizeInfo.Builder builder = ImageResizeInfo.builder();
    for (Map.Entry<String, String> entry : params.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      builder.attr(key, value);
    }
    args.getRootElement().pluginsInfos(
            PluginsInfos.builder().pluginsInfo(builder.build()).build()
    );
    return false;
  }

}
