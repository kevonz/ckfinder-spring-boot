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

import lombok.Builder;
import lombok.Getter;
import org.springframework.core.io.Resource;

@Builder(builderClassName = "Builder")
@Getter
public class WatermarkSettings {

  public static final String WATERMARK = "watermark";
  public static final String SOURCE = "source";
  public static final String TRANSPARENCY = "transparency";
  public static final String QUALITY = "quality";
  public static final String MARGIN_BOTTOM = "marginBottom";
  public static final String MARGIN_RIGHT = "marginRight";

  private final Resource source;
  private final float transparency;
  private final float quality;
  private final int marginBottom;
  private final int marginRight;

  @SuppressWarnings("PackageVisibleInnerClass")
  public static class Builder {

    Builder() {
      this.source = null;
      this.marginRight = 0;
      this.marginBottom = 0;
      this.quality = 90;
      this.transparency = 1.0f;
    }

  }

}
