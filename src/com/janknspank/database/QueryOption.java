package com.janknspank.database;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.protobuf.ProtocolMessageEnum;

public class QueryOption {
  public static class Limit extends QueryOption {
    private final int limit;

    public Limit(int limit) {
      Preconditions.checkArgument(limit > 0);
      this.limit = limit;
    }

    public int getLimit() {
      return limit;
    }
  }

  public final static class LimitWithOffset extends Limit {
    private final int offset;

    public LimitWithOffset(int limit, int offset) {
      super(limit);
      this.offset = offset;
    }

    public int getOffset() {
      return offset;
    }
  }

  public abstract static class WhereOption extends QueryOption {
    private final String fieldName;

    private WhereOption(String fieldName) {
      this.fieldName = fieldName;
    }

    public String getFieldName() {
      return fieldName;
    }

    public abstract int getFieldCount();
  }

  public static class WhereLike extends WhereOption {
    private final String value;

    public WhereLike(String fieldName, String value) {
      super(fieldName);
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public int getFieldCount() {
      return 1;
    }
  }

  public static class WhereLikeIgnoreCase extends WhereLike {
    public WhereLikeIgnoreCase(String fieldName, String value) {
      super(fieldName, value);
    }
  }

  public static class WhereNotLike extends WhereLike {
    public WhereNotLike(String fieldName, String value) {
      super(fieldName, value);
    }
  }

  public static class WhereNotLikeIgnoreCase extends WhereNotLike {
    public WhereNotLikeIgnoreCase(String fieldName, String value) {
      super(fieldName, value);
    }
  }

  public static class WhereEquals extends WhereOption {
    private final Iterable<String> values;

    public WhereEquals(String fieldName, String value) {
      this(fieldName, ImmutableList.of(value));
    }

    public WhereEquals(String fieldName, Iterable<String> values) {
      super(fieldName);
      this.values = values;
    }

    public Iterable<String> getValues() {
      return values;
    }

    public int getFieldCount() {
      return Iterables.size(values);
    }
  }

  public final static class WhereNotEquals extends WhereEquals {
    public WhereNotEquals(String fieldName, String value) {
      super(fieldName, value);
    }

    public WhereNotEquals(String fieldName, Iterable<String> values) {
      super(fieldName, values);
    }
  }

  public final static class WhereEqualsIgnoreCase extends WhereEquals {
    public WhereEqualsIgnoreCase(String fieldName, String value) {
      super(fieldName, value);
    }

    public WhereEqualsIgnoreCase(String fieldName, Iterable<String> values) {
      super(fieldName, values);
    }
  }

  public static class WhereEqualsEnum extends WhereOption {
    private final Iterable<?> values;

    public WhereEqualsEnum(String fieldName, ProtocolMessageEnum value) {
      super(fieldName);
      this.values = ImmutableList.of(value);
    }

    public WhereEqualsEnum(String fieldName, Iterable<ProtocolMessageEnum> values) {
      super(fieldName);
      this.values = values;
    }

    @SuppressWarnings("unchecked")
    public Iterable<ProtocolMessageEnum> getValues() {
      return (Iterable<ProtocolMessageEnum>) values;
    }

    public int getFieldCount() {
      return Iterables.size(values);
    }
  }

  public final static class WhereNotEqualsEnum extends WhereEqualsEnum {
    public WhereNotEqualsEnum(String fieldName, ProtocolMessageEnum value) {
      super(fieldName, value);
    }

    public WhereNotEqualsEnum(String fieldName, Iterable<ProtocolMessageEnum> values) {
      super(fieldName, values);
    }
  }

  public static class WhereEqualsNumber extends WhereOption {
    private final Iterable<Number> values;

    public WhereEqualsNumber(String fieldName, Number value) {
      this(fieldName, ImmutableList.of(value));
    }

    public WhereEqualsNumber(String fieldName, Iterable<Number> values) {
      super(fieldName);
      this.values = values;
    }

    public Iterable<Number> getValues() {
      return values;
    }

    public int getFieldCount() {
      return Iterables.size(values);
    }
  }

  public final static class WhereNotEqualsNumber extends WhereEqualsNumber {
    public WhereNotEqualsNumber(String fieldName, Number value) {
      super(fieldName, value);
    }

    public WhereNotEqualsNumber(String fieldName, Iterable<Number> values) {
      super(fieldName, values);
    }
  }

  public abstract static class WhereInequality extends WhereOption {
    private final Number value;

    public WhereInequality(String fieldName, Number value) {
      super(fieldName);
      this.value = value;
    }

    public Number getValue() {
      return value;
    }

    public int getFieldCount() {
      return 1;
    }
  }

  public final static class WhereGreaterThan extends WhereInequality {
    public WhereGreaterThan(String fieldName, Number value) {
      super(fieldName, value);
    }
  }

  public final static class WhereGreaterThanOrEquals extends WhereInequality {
    public WhereGreaterThanOrEquals(String fieldName, Number value) {
      super(fieldName, value);
    }
  }

  public final static class WhereLessThan extends WhereInequality {
    public WhereLessThan(String fieldName, Number value) {
      super(fieldName, value);
    }
  }

  public final static class WhereLessThanOrEquals extends WhereInequality {
    public WhereLessThanOrEquals(String fieldName, Number value) {
      super(fieldName, value);
    }
  }

  public static class WhereTrue extends WhereOption {
    public WhereTrue(String fieldName) {
     super(fieldName);
    }

    public int getFieldCount() {
      return 1;
    }
  }

  public static class WhereNotTrue extends WhereOption {
    public WhereNotTrue(String fieldName) {
     super(fieldName);
    }

    public int getFieldCount() {
      return 1;
    }
  }

  public static class WhereFalse extends WhereOption {
    public WhereFalse(String fieldName) {
      super(fieldName);
    }

    public int getFieldCount() {
      return 1;
    }
  }

  public static class WhereNotFalse extends WhereOption {
    public WhereNotFalse(String fieldName) {
      super(fieldName);
    }

    public int getFieldCount() {
      return 1;
    }
  }

  public static class WhereNull extends WhereOption {
    public WhereNull(String fieldName) {
      super(fieldName);
    }

    public int getFieldCount() {
      return 1;
    }
  }

  public static class WhereNotNull extends WhereNull {
    public WhereNotNull(String fieldName) {
      super(fieldName);
    }
  }

  static class Sort extends QueryOption {
    private final String fieldName;

    Sort(String fieldName) {
      super();
      this.fieldName = fieldName;
    }

    public String getFieldName() {
      return fieldName;
    }
  }

  public final static class AscendingSort extends Sort {
    public AscendingSort(String fieldName) {
      super(fieldName);
    }
  }

  public final static class DescendingSort extends Sort {
    public DescendingSort(String fieldName) {
      super(fieldName);
    }
  }

  @SuppressWarnings("unchecked")
  static final <X extends QueryOption> List<X> getList(
      QueryOption[] options, Class<X> queryOption) {
    List<X> queryOptions = Lists.newArrayList();
    for (QueryOption option : options) {
      if (queryOption.isInstance(option)) {
        queryOptions.add((X) option);
      }
    }
    return queryOptions;
  }

  /**
   * Returns true if we know the where clause could not possibly match any
   * database rows.
   */
  static final boolean isWhereClauseEmpty(QueryOption[] options) {
    for (QueryOption option : getList(options, WhereOption.class)) {
      if (option instanceof WhereEquals
          && Iterables.isEmpty(((WhereEquals) option).getValues())) {
        return true;
      }
    }
    return false;
  }
}
