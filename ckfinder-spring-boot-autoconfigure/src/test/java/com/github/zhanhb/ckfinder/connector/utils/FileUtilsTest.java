package com.github.zhanhb.ckfinder.connector.utils;

import com.github.zhanhb.ckfinder.connector.data.ResourceType;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author zhanhb
 */
@Slf4j
public class FileUtilsTest {

  /**
   * Test of renameFileWithBadExt method, of class FileUtils.
   */
  @Test
  public void testRenameFileWithBadExt() {
    log.debug("renameFileWithBadExt");
    ResourceType type = ResourceType.builder()
            .name("test")
            .allowedExtensions("html,htm")
            .deniedExtensions("exe,jsp").build();
    String fileName = "test.exe.html.jsp.jsp";
    String expResult = "test_exe.html_jsp.jsp";
    String result = FileUtils.renameFileWithBadExt(type, fileName);
    assertEquals(expResult, result);
  }

}
