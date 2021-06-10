package com.agilogy.srdb

import java.sql.PreparedStatement

trait ArgumentsSetter[T] {

  def set(ps: PreparedStatement, value: T): Unit
}

object ArgumentsSetter {

  implicit val preparedStatementSetArgumentSetter: ArgumentsSetter[PreparedStatement => Unit] =
    (ps: PreparedStatement, f: PreparedStatement => Unit) => f(ps)

  implicit val argsSeqArgumentSetter: ArgumentsSetter[Seq[Argument]] = { (ps: PreparedStatement, args: Seq[Argument]) =>
    args.zipWithIndex.foreach {
      case (arg, pos) => arg(ps, pos + 1)
    }
  }
}
