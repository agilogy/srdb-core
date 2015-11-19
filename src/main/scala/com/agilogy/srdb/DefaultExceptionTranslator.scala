package com.agilogy.srdb

import java.sql.SQLException

import com.agilogy.srdb.exceptions.{ DbExceptionWithCause, Context }

import scala.util.control.NonFatal

class DefaultExceptionTranslator extends ExceptionTranslator {
  override def apply(context: Context, sql: String, throwable: Throwable): Exception = {
    throwable match {
      case s: SQLException => DbExceptionWithCause(context, sql, s)
      case NonFatal(t) => DbExceptionWithCause(context, sql, t)
      case t => throw t
    }

  }
}

