package com.agilogy.srdb

import java.sql.SQLException

class DefaultExceptionTranslator extends ExceptionTranslator {
  override def apply(sql: String, t: SQLException): Exception = new SQLException("Error executing SQL " + sql, t.getSQLState, t.getErrorCode, t)
}

