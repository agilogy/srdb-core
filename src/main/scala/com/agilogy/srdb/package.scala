package com.agilogy

import java.sql.{Connection, PreparedStatement, ResultSet, SQLException}

import scala.collection.mutable.ListBuffer

package object srdb {

  type ExceptionTranslator = SQLException => Exception

  type Reader[T] = ResultSet => T
  type ArgumentsSetter = PreparedStatement => Unit
  type Argument = (PreparedStatement, Int) => Unit

  def withExceptionTranslator(et: ExceptionTranslator): Srdb = new Srdb(et)

  trait ExecutableQuery[RT] {

    private def argumentsSetter(args: Seq[Argument]): ArgumentsSetter = {
      ps =>
        args.zipWithIndex.foreach {
          case (arg, pos) => arg(ps, pos + 1)
        }
    }

    def apply(conn: Connection, args: Argument*): RT = apply(conn, argumentsSetter(args))

    def apply(conn: Connection, args: ArgumentsSetter): RT
  }

  trait ReadableQuery {
    def raw[T](readResultSet: Reader[T]): ExecutableQuery[T]

    def apply[T](rowReader: Reader[T]): ExecutableQuery[Seq[T]] = raw(asList(rowReader))

    def single[T](rowReader: Reader[T]): ExecutableQuery[Option[T]] = raw(asSingle(rowReader))
  }

  val Srdb = new Srdb(identity[SQLException])


  private def asList[T](reader: Reader[T]): (ResultSet) => Seq[T] = {
    rs =>
      val res = new ListBuffer[T]
      while (rs.next) {
        res.append(reader(rs))
      }
      res.toSeq
  }

  private def asSingle[T](reader: Reader[T]): (ResultSet) => Option[T] = {
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