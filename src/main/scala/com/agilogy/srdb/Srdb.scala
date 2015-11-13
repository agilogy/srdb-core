package com.agilogy.srdb

import java.sql._

import scala.util.control.NonFatal

class Srdb private[srdb] (exceptionTranslator: ExceptionTranslator) {

  def select(query: String): ReadableQuery = new ReadableQuery {
    override def raw[T: Reader]: ExecutableQuery[T] = new ExecutableQuery[T] {
      val readResultSet = implicitly[Reader[T]]
      override def apply[AT: ArgumentsSetter](conn: Connection, args: AT): T = {
        prepareStatement(conn, query, args, generatedKeys = false) {
          ps =>
            val rs = secure(query) {
              ps.executeQuery()
            }
            try {
              secure(query)(readResultSet(rs))
            } finally {
              rs.close()
            }
        }
      }
    }
  }

  def update(statement: String): ExecutableQuery[Int] = new ExecutableQuery[Int] {
    override def apply[AT: ArgumentsSetter](conn: Connection, args: AT): Int = {
      prepareStatement[Int, AT](conn, statement, args) {
        ps =>
          secure(statement) {
            ps.executeUpdate()
          }
      }
    }
  }

  def updateGeneratedKeys[RT: Reader](statement: String): ExecutableQuery[RT] = new ExecutableQuery[RT] {
    val readKey = implicitly[Reader[RT]]
    override def apply[AT: ArgumentsSetter](conn: Connection, args: AT): RT = {
      prepareStatement(conn, statement, args, generatedKeys = true) {
        ps: PreparedStatement =>
          secure(statement) {
            ps.executeUpdate()
          }
          val rs = secure(statement) {
            ps.getGeneratedKeys
          }
          try {
            if (rs.next) {
              secure(statement) {
                readKey(rs)
              }
            } else {
              throw new IllegalArgumentException("The statement didn't generate any key\n  Statement: " + statement)
            }
          } finally {
            rs.close()
          }
      }
    }
  }

  private def secure[T](sql: String)(f: => T): T = {
    try {
      f
    } catch {
      case e: SQLException => throw translateException(sql, e)
    }
  }

  private def translateException(sql: String, sqle: SQLException): Exception = {
    try {
      exceptionTranslator(sql, sqle)
    } catch {
      case NonFatal(e) => sqle
    }
  }

  private def prepareStatement[T, AT: ArgumentsSetter](conn: Connection, query: String, arguments: AT, generatedKeys: Boolean = false)(f: (PreparedStatement => T)): T = {
    val prepareStatementFlag = if (generatedKeys) Statement.RETURN_GENERATED_KEYS else Statement.NO_GENERATED_KEYS
    val s = secure(query) {
      conn.prepareStatement(query, prepareStatementFlag)
    }
    try {
      secure(query) {
        implicitly[ArgumentsSetter[AT]].set(s, arguments)
      }
      f(s)
    } finally {
      s.close()
    }
  }

}
