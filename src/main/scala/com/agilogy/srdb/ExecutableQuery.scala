package com.agilogy.srdb

import java.sql.{ PreparedStatement, Connection }

trait ExecutableQuery[RT] {

  def apply(conn: Connection, args: Argument*): RT = apply(conn, args)

  def apply(conn: Connection, argumentsSetter: PreparedStatement => Unit): RT = apply[PreparedStatement => Unit](conn, argumentsSetter)

  def apply[T: ArgumentsSetter](conn: Connection, args: T): RT
}

