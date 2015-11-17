package com.agilogy.srdb

import java.sql._

import scala.util.control.NonFatal

class Srdb private[srdb] (exceptionTranslator: ExceptionTranslator) {

  def select[T](query: String, fetchSize: FetchSize = DefaultFetchSize)(reader: ResultSet => T): ExecutableQuery[T] = new ExecutableQuery[T] {

    override def apply[AT: ArgumentsSetter](conn: Connection, args: AT): T = {
      prepareStatement(conn, query, args, generatedKeys = false) {
        ps =>
          fetchSize match {
            case LimitedFetchSize(fs) => secure(query)(ps.setFetchSize(fs))
            case _ =>
          }
          val rs = secure(query) {
            ps.executeQuery()
          }
          try {
            secure(query)(reader(rs))
          } finally {
            rs.close()
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

  def updateGeneratedKeys[RT](statement: String)(readKey: ResultSet => RT): ExecutableQuery[RT] = new ExecutableQuery[RT] {

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
