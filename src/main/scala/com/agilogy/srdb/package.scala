package com.agilogy

import java.sql.{ PreparedStatement, SQLException }

import com.agilogy.srdb.exceptions.Context

package object srdb {

  type ExceptionTranslator = (Context, String, Throwable) => Exception

  type Argument = (PreparedStatement, Int) => Unit

  def withExceptionTranslator(et: ExceptionTranslator): Srdb = new Srdb(et)

  val Srdb = new Srdb(new DefaultExceptionTranslator)

}