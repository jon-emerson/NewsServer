package com.janknspank.bizness;

import java.util.Map;

import com.janknspank.proto.ExpressionProto.Expression;

public class Expressions {
  private static Map<String, Expression> __EXPRESSION_MAP = null;

  private synchronized static Map<String, Expression> getExpressionMap() {
    if (__EXPRESSION_MAP == null) {
      
    }
    return __EXPRESSION_MAP;
  }

  public static Expression getById(String expressionId) {
    return getExpressionMap().get(expressionId);
  }
}
