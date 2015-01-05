package com.janknspank;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.google.common.io.CharStreams;
import com.janknspank.ArticleHandler.ArticleCallback;
import com.janknspank.dom.DomException;
import com.janknspank.dom.InterpretedData;
import com.janknspank.dom.Interpreter;
import com.janknspank.dom.parser.DocumentNode;
import com.janknspank.dom.parser.DocumentBuilder;
import com.janknspank.dom.parser.LenientXMLReader;
import com.janknspank.dom.parser.ParserException;
import com.janknspank.fetch.FetchException;
import com.janknspank.fetch.FetchResponse;
import com.janknspank.fetch.Fetcher;
import com.janknspank.proto.Core.Url;

public class Crawler {
  private final ArticleCallback callback;
  private final Fetcher fetcher = new Fetcher();

  public Crawler(ArticleCallback callback) {
    this.callback = callback;
  }

  public void crawl(Url url) {
    try {
      FetchResponse response = fetcher.fetch(url.getUrl());

      // TODO(jonemerson): Should we update the database if we were
      // redirected, so that it now points at the canonical URL?
      if (response.getStatusCode() == 200) {
        File file = writeToFile(url.getId() + ".html", response.getReader());

        // This guy is our top-level class for handling interpreted data and
        // a second-pass over the XML.
        ArticleHandler handler = new ArticleHandler(callback, url);

        // Get the interpreted data (paragraphs).
        DocumentNode documentNode = DocumentBuilder.build(new FileReader(file));
        InterpretedData interpretedData =
            new Interpreter(documentNode, url.getUrl()).getInterpretedData();
        handler.setInterpretedData(interpretedData);

        // Now send all the SAX events over to the ArticleHandler so it can read
        // the <meta> tags.
        // TODO(jonemerson): We should really build a <meta> tag reader that's
        // capable of working off of a DocumentNode object.
        XMLReader reader = new LenientXMLReader();
        reader.setContentHandler(handler);
        reader.parse(new InputSource(new FileReader(file)));
      }

    } catch (SAXException | IOException | IllegalArgumentException | DomException |
        FetchException | ParserException e) {
      e.printStackTrace();
    }
  }

  private File writeToFile(String filename, Reader reader) throws IOException {
    File file = new File("data/" + filename);
    FileWriter writer = new FileWriter(file);
    try {
      CharStreams.copy(reader, writer);
    } finally {
      writer.close();
    }
    return file;
  }
}
