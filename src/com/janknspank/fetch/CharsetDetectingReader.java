package com.janknspank.fetch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.SequenceInputStream;

import org.mozilla.universalchardet.UniversalDetector;

import com.google.common.base.Charsets;

/**
 * A Reader that auto-detects the Charset of the underlying InputStream.  Useful
 * since some web sites (e.g. NYTimes from 8 years ago) don't accurately tell us
 * what charset their web pages are in.
 *
 * NOTE(jonemerson): My initial attempt at this method used IBM's ICU4J, but
 * that FAILED to detect Windows CP1252 on a fairly trivial document.
 * Mozilla's implementation works flawlessly, though.
 */
class CharsetDetectingReader extends Reader {
  private final InputStreamReader internalReader;

  public CharsetDetectingReader(InputStream inputStream) throws IOException {
    UniversalDetector detector = new UniversalDetector(null);

    // Read data until the UniversalDetector says it's had enough (isDone()
    // returns true).  Store whatever we get into a ByteArrayOutputStream so we
    // can include it in our response.
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    int readBytes = inputStream.read(buffer, 0, buffer.length);
    while (readBytes >= 0) {
      baos.write(buffer, 0, readBytes);
      detector.handleData(buffer, 0, readBytes);
      if (detector.isDone()) {
        break;
      }
      readBytes = inputStream.read(buffer, 0, buffer.length);
    }
    detector.dataEnd();

    // Get out detected encoding, falling back to UTF-8.
    String charset = (detector.getDetectedCharset() == null)
        ? Charsets.UTF_8.name() : detector.getDetectedCharset();

    // Return a Reader for the detected Charset, using the combined data from
    // what we read to detect the charset and what we haven't yet read.
    internalReader = new InputStreamReader(
        new SequenceInputStream(
            new ByteArrayInputStream(baos.toByteArray()),
            inputStream),
        charset);
  }

  @Override
  public void close() throws IOException {
    internalReader.close();
  }

  @Override
  public int read(char[] cbuf, int off, int len) throws IOException {
    return internalReader.read(cbuf, off, len);
  }
}
