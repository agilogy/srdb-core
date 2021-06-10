package com.agilogy.srdb

import java.sql.{ Connection, PreparedStatement }

trait ExecutableQuery[RT] {

  def apply(conn: Connection, args: Argument*): RT = apply(conn, args)

  def apply(conn: Connection, argumentsSetter: PreparedStatement => Unit): RT = apply[PreparedStatement => Unit](conn, argumentsSetter)

  def apply[T: ArgumentsSetter](conn: Connection, args: T): RT
}

trait BatchUpdate {
  def apply[T: ArgumentsSetter](conn: Connection, args: List[T]): scala.Array[Int]
}
