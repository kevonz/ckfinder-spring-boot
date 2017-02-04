package com.github.zhanhb.ckfinder.connector.configuration;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 *
 * @author zhanhb
 */
public class CommandFactoryTest {

  /**
   * Test of enableDefaultCommands method, of class CommandFactory.
   */
  @Test
  public void testEnableDefaultCommands() {
    assertNotNull(new CommandFactory().enableDefaultCommands().getCommand("INIT"));
  }

}
