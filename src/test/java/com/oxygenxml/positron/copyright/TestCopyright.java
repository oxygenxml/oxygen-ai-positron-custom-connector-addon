package com.oxygenxml.positron.copyright;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.time.Year;

import org.apache.commons.io.IOUtils;

import junit.framework.TestCase;

public class TestCopyright extends TestCase{

  /**
   * <p><b>Description:</b> test copyright year in extension.xml.</p> 
   *
   * <p><b>Bug ID:</b> OPA-3331</p>
   * @author razvan_tudosie
   *
   * @throws Exception
   */
  public void testCopyrightYearInExtensionDotXml() throws Exception {
    try (Reader reader = new FileReader(new File("extension.xml"))) {
      testCopyRightYear(IOUtils.readLines(reader).toString());
    }
  }
  
  /**
   * Test copyright year.
   * 
   * @param content The content to look into.
   */
  private void testCopyRightYear(String content) {
    int year = Year.now().getValue();
    assertTrue(content.contains("Copyright " + year + " Syncro Soft SRL"));
    for (int i = 2015; i < year; i++) {
      if(content.contains("Copyright " + i + " Syncro Soft SRL")) {
        fail("Found copyright year " + i);
      }
    }
  }
  
}
