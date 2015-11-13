package com.agilogy

import java.sql.{ PreparedStatement, ResultSet, SQLException }

import com.agilogy.srdb.ExceptionTranslator

import scala.collection.mutable.ListBuffer

class DefaultExceptionTranslator extends ExceptionTranslator {
  override def apply(sql: String, t: SQLException): Exception = new SQLException("Error executing SQL " + sql, t.getSQLState, t.getErrorCode, t)
}

package object srdb {

  type ExceptionTranslator = (String, SQLException) => Exception

  type Reader[T] = ResultSet => T
  type Argument = (PreparedStatement, Int) => Unit

  def withExceptionTranslator(et: ExceptionTranslator): Srdb = new Srdb(et)

  val Srdb = new Srdb(new DefaultExceptionTranslator)

  def asList[T](reader: Reader[T]): (ResultSet) => Seq[T] = {
    rs =>
      val res = new ListBuffer[T]
      while (rs.next) {
        res.append(reader(rs))
      }
      res.toSeq
  }

  def asSingle[T](reader: Reader[T]): (ResultSet) => Option[T] = {
    rs =>
      if (!rs.next()) {
        None
      } else {
        val res = Some(reader(rs))
        if (rs.next()) throw new IllegalArgumentException("The query returned more than one row")
        res
      }
  }

}