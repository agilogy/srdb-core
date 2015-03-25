package com.agilogy

import java.sql.{ PreparedStatement, ResultSet, SQLException }

import scala.collection.mutable.ListBuffer

package object srdb {

  type ExceptionTranslator = SQLException => Exception

  type Reader[T] = ResultSet => T
  type Argument = (PreparedStatement, Int) => Unit

  def withExceptionTranslator(et: ExceptionTranslator): Srdb = new Srdb(et)

  val Srdb = new Srdb(identity[SQLException])

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