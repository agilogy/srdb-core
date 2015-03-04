package com.agilogy.srdb

import java.sql.PreparedStatement

trait ArgumentsSetter[T] {

  def set(ps:PreparedStatement, value:T):Unit
}

object ArgumentsSetter{
  
  implicit val preparedStatementSetArgumentSetter = new ArgumentsSetter[PreparedStatement => Unit] {
    override def set(ps: PreparedStatement, f: (PreparedStatement) => Unit): Unit = f(ps)
  }
  
  implicit val argsSeqArgumentSetter = new ArgumentsSetter[Seq[Argument]] {
    override def set(ps: PreparedStatement, args: Seq[Argument]): Unit = {
      args.zipWithIndex.foreach {
        case (arg, pos) => arg(ps, pos + 1)
      }
    }
  }
}