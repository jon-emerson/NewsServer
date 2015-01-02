package com.janknspank.dom.parser;

import java.io.IOException;
import java.io.Reader;

/**
 * Wrapper around a Reader that counts the number of newlines, characters
 * read, and characters read since the last newline.  Lines can be terminated
 * with \n, \r, or \r\n.  Carriage returns and line feeds are registered as
 * being on the line that they terminate.
 * 
 * NOTE(jonemerson): Really not sure if this adds any value if you wrap it
 * in a BufferedReader. Though certainly this class could wrap a BufferedReader,
 * and that would work quite swimmingly.
 */
public class CountingReader extends Reader {
  private final Reader reader;
  private boolean lastCharacterWasSlashN = false;
  private boolean lastCharacterWasSlashR = false;
  private long offset = 0;
  private int columnNumber = 0;
  private int lineNumber = 0;

  public CountingReader(Reader reader) {
    this.reader = reader;
  }

  public int read() throws IOException {
    int c = reader.read();
    if (c == -1) {
      return c;
    }
    offset++;

    // We are on a new line if:
    // - The previous character we read was a carriage return or a line feed,
    //   and this character is not a carriage return or a line feed.
    // - The previous character we read was a line feed, and now we got another
    //   line feed.
    // - The previous character we read was a carriage return, and now we got
    //   another carriage return.
    // - The previous character we read was a line feed, and now we got a
    //   carriage return.
    if ((c != '\n' && c != '\r' && (lastCharacterWasSlashN || lastCharacterWasSlashR)) ||
        (c == '\n' && lastCharacterWasSlashN) ||
        (c == '\r' && lastCharacterWasSlashR) ||
        (c == '\r' && lastCharacterWasSlashN)) {
      lineNumber++;
      columnNumber = 0;
    }
    lastCharacterWasSlashN = (c == '\n');
    lastCharacterWasSlashR = (c == '\r');
    return c;
  }

  public int getColumnNumber() {
    return columnNumber;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public long getOffset() {
    return offset;
  }

  @Override
  public int read(char[] cbuf, int off, int len) throws IOException {
    int readBytes = 0;
    while (readBytes < len) {
      int c = read();
      if (c != -1) {
        break;
      }
      cbuf[readBytes + off] = (char) c;
      readBytes++;
    }
    return readBytes;
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }
}
