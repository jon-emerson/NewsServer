package com.janknspank.common;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.List;

import org.junit.Test;

public class CsvReaderTest {
  @Test
  public void test() {
    String text = "hello,,there,\"foo foo, foo\"\n"
        + "yessir,\"\",\"\",\"\",\"\",yep\n"
        + "fhoo";
    CsvReader reader = new CsvReader(new StringReader(text));

    List<String> firstLine = reader.readLine();
    assertEquals(4, firstLine.size());
    assertEquals("hello", firstLine.get(0));
    assertEquals("", firstLine.get(1));
    assertEquals("there", firstLine.get(2));
    assertEquals("foo foo, foo", firstLine.get(3));

    List<String> secondLine = reader.readLine();
    assertEquals(6, secondLine.size());
    assertEquals("yessir", secondLine.get(0));
    assertEquals("", secondLine.get(1));
    assertEquals("", secondLine.get(2));
    assertEquals("", secondLine.get(3));
    assertEquals("", secondLine.get(4));
    assertEquals("yep", secondLine.get(5));

    List<String> thirdLine = reader.readLine();
    assertEquals(1, thirdLine.size());
    assertEquals("fhoo", thirdLine.get(0));

    List<String> fourthLine = reader.readLine();
    assertEquals((List<String>) null, fourthLine);
  }

  @Test
  public void test2() {
    String text = "\n\n";
    CsvReader reader = new CsvReader(new StringReader(text));

    List<String> firstLine = reader.readLine();
    assertEquals(1, firstLine.size());
    assertEquals("", firstLine.get(0));

    List<String> secondLine = reader.readLine();
    assertEquals(1, secondLine.size());
    assertEquals("", secondLine.get(0));

    List<String> thirdLine = reader.readLine();
    assertEquals((List<String>) null, thirdLine);
  }

  @Test
  public void test3() {
    String text = "\"this is a\n nested \n block \n with carriage returns\",yup\n\n";
    CsvReader reader = new CsvReader(new StringReader(text));

    List<String> firstLine = reader.readLine();
    assertEquals(2, firstLine.size());
    assertEquals("this is a\n nested \n block \n with carriage returns", firstLine.get(0));
    assertEquals("yup", firstLine.get(1));

    List<String> secondLine = reader.readLine();
    assertEquals(1, secondLine.size());
    assertEquals("", secondLine.get(0));

    List<String> thirdLine = reader.readLine();
    assertEquals((List<String>) null, thirdLine);
  }

  @Test
  public void test4() {
    String text = "162479,Abbott Laboratories Inc.,,https://angel.co/abbott-laboratories-inc,,,,01/30/13 10:18 PM,1,2.01,,,,\n"
        + "162480,Personal Branding Blog,,https://angel.co/personal-branding-blog,,,,01/30/13 10:19 PM,1,2.01,,,,\n";
    CsvReader reader = new CsvReader(new StringReader(text));

    List<String> firstLine = reader.readLine();
    assertEquals(14, firstLine.size());
    assertEquals("162479", firstLine.get(0));
    assertEquals("Abbott Laboratories Inc.", firstLine.get(1));
    assertEquals("", firstLine.get(2));
    assertEquals("https://angel.co/abbott-laboratories-inc", firstLine.get(3));
    assertEquals("", firstLine.get(4));
    assertEquals("", firstLine.get(5));
    assertEquals("", firstLine.get(6));
    assertEquals("01/30/13 10:18 PM", firstLine.get(7));
    assertEquals("1", firstLine.get(8));
    assertEquals("2.01", firstLine.get(9));
    assertEquals("", firstLine.get(10));
    assertEquals("", firstLine.get(11));
    assertEquals("", firstLine.get(12));
    assertEquals("", firstLine.get(13));

    List<String> secondLine = reader.readLine();
    assertEquals(14, secondLine.size());
    assertEquals("162480", secondLine.get(0));
    assertEquals("Personal Branding Blog", secondLine.get(1));
    assertEquals("", secondLine.get(2));
    assertEquals("https://angel.co/personal-branding-blog", secondLine.get(3));
    assertEquals("", secondLine.get(4));
    assertEquals("", secondLine.get(5));
    assertEquals("", secondLine.get(6));
    assertEquals("01/30/13 10:19 PM", secondLine.get(7));
    assertEquals("1", secondLine.get(8));
    assertEquals("2.01", secondLine.get(9));
    assertEquals("", secondLine.get(10));
    assertEquals("", secondLine.get(11));
    assertEquals("", secondLine.get(12));
    assertEquals("", secondLine.get(13));

    List<String> thirdLine = reader.readLine();
    assertEquals((List<String>) null, thirdLine);
  }
}
