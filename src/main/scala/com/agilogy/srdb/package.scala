package com.agilogy

import java.sql.{ PreparedStatement, SQLException }

package object srdb {

  type ExceptionTranslator = (String, SQLException) => Exception

  type Argument = (PreparedStatement, Int) => Unit

  def withExceptionTranslator(et: ExceptionTranslator): Srdb = new Srdb(et)

  val Srdb = new Srdb(new DefaultExceptionTranslator)

}