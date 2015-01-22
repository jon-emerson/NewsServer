package com.janknspank.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import com.janknspank.proto.Core.Article;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@SuppressWarnings("resource")
public class DatabaseTest {
  @Test
  public void testGetAll() throws Exception {
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ArgumentCaptor<String> statementCaptor = ArgumentCaptor.forClass(String.class);
    when(connection.prepareStatement(statementCaptor.capture())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(mock(ResultSet.class));
    Database database = new Database(connection);
    database.get(Article.class);
    assertEquals("SELECT * FROM Article", statementCaptor.getValue());
  }

  @Test
  public void testGetFirstWithOffset() throws Exception {
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ArgumentCaptor<String> statementCaptor = ArgumentCaptor.forClass(String.class);
    when(connection.prepareStatement(statementCaptor.capture())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(mock(ResultSet.class));
    Database database = new Database(connection);
    database.getFirst(Article.class,
        new QueryOption.WhereEquals("type", "cou"),
        new QueryOption.LimitWithOffset(10, 500));
    assertEquals("SELECT * FROM Article "
        + "WHERE type IN (?) "
        + "LIMIT 500, 1", statementCaptor.getValue());
    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(preparedStatement, atLeast(0)).setString(any(Integer.class), argumentCaptor.capture());
    List<String> arguments = argumentCaptor.getAllValues();
    assertEquals(1, arguments.size());
    assertEquals("cou", arguments.get(0));
  }

  @Test
  public void testGetCrazy() throws Exception {
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ArgumentCaptor<String> statementCaptor = ArgumentCaptor.forClass(String.class);
    when(connection.prepareStatement(statementCaptor.capture())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(mock(ResultSet.class));
    Database database = new Database(connection);
    database.get(Article.class,
        new QueryOption.WhereEquals("id", ImmutableList.of("X", "Y", "Z")),
        new QueryOption.WhereNotEquals("crawl_priority", "0"),
        new QueryOption.WhereNotLike("url", "%//twitter.com/%"),
        new QueryOption.DescendingSort("published_time"),
        new QueryOption.Limit(5));
    assertEquals("SELECT * FROM Article "
        + "WHERE id IN (?,?,?) "
        + "AND crawl_priority NOT IN (?) "
        + "AND url NOT LIKE ? "
        + "ORDER BY published_time DESC "
        + "LIMIT 5", statementCaptor.getValue());
    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(preparedStatement, atLeast(0)).setString(any(Integer.class), argumentCaptor.capture());
    List<String> arguments = argumentCaptor.getAllValues();
    assertEquals(5, arguments.size());
    assertEquals("X", arguments.get(0));
    assertEquals("Y", arguments.get(1));
    assertEquals("Z", arguments.get(2));
    assertEquals("0", arguments.get(3));
    assertEquals("%//twitter.com/%", arguments.get(4));
  }

  @Test
  public void testMessageUpdate() throws Exception {
    Message message = Article.newBuilder()
        .setDescription("Je ne connais pas")
        .setPublishedTime(32625L)
        .setTitle("Title de Article Amazement")
        .setUrl("http://www.nytimes.com/ouiouioui")
        .setUrlId("ID")
        .build();
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ArgumentCaptor<String> statementCaptor = ArgumentCaptor.forClass(String.class);
    when(connection.prepareStatement(statementCaptor.capture())).thenReturn(preparedStatement);
    when(preparedStatement.executeBatch()).thenReturn(new int[] { 1 });
    Database database = new Database(connection);
    database.update(message);
    assertEquals("UPDATE Article SET url_id=?, title=?, published_time=?, proto=? "
        + "WHERE url_id IN (?)", statementCaptor.getValue());
    ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Long> longCaptor = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<byte[]> byteArrayCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(preparedStatement, atLeast(0)).setString(any(Integer.class),
        (String) stringCaptor.capture());
    verify(preparedStatement, atLeast(0)).setLong(any(Integer.class),
        (Long) longCaptor.capture());
    verify(preparedStatement, atLeast(0)).setBytes(any(Integer.class),
        (byte[]) byteArrayCaptor.capture());
    List<String> stringArguments = stringCaptor.getAllValues();
    assertEquals(5, stringArguments.size()
        + longCaptor.getAllValues().size() + byteArrayCaptor.getAllValues().size());
    assertEquals("ID", stringArguments.get(0));
    assertEquals("Title de Article Amazement", stringArguments.get(1));
    assertEquals("ID", stringArguments.get(2));
    assertEquals(Long.valueOf(32625L), (Long) longCaptor.getValue());
  }
}
