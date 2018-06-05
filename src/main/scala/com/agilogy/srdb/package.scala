package com.agilogy

import java.sql.{ PreparedStatement, ResultSet }

import com.agilogy.srdb.exceptions.Context

import scala.collection.mutable.ListBuffer

package object srdb {

  type ExceptionTranslator = (Context, String, Throwable) => Exception

  type Argument = (PreparedStatement, Int) => Unit

  def withExceptionTranslator(et: ExceptionTranslator): Srdb = new Srdb(et)

  val Srdb = new Srdb(new DefaultExceptionTranslator)

  def readSeq[T](readRow: ResultSet => T): ResultSet => Seq[T] = {
    rs =>
      val res = new ListBuffer[T]
      while (rs.next()) {
        res.append(readRow(rs))
      }
      res
  }

}