package com.agilogy.srdb

import java.sql._

import com.agilogy.srdb.exceptions.Context
import com.agilogy.srdb.exceptions._

import scala.util.control.NonFatal

class Srdb private[srdb] (exceptionTranslator: ExceptionTranslator) {

  def select[T](query: String, fetchSize: FetchSize = DefaultFetchSize)(reader: ResultSet => T): ExecutableQuery[T] = new ExecutableQuery[T] {

    override def apply[AT: ArgumentsSetter](conn: Connection, args: AT): T = {
      prepareStatement(conn, query, args, generatedKeys = false) {
        ps =>
          fetchSize match {
            case LimitedFetchSize(fs) => secure(Context.SetFetchSize(fs), query)(ps.setFetchSize(fs))
            case _ =>
          }
          val rs = secure(Context.ExecuteQuery, query) {
            ps.executeQuery()
          }
          try {
            secure(Context.ReadResultSet, query)(reader(rs))
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
          secure(Context.ExecuteUpdate, statement) {
            ps.executeUpdate()
          }
      }
    }
  }

  def updateGeneratedKeys[RT](statement: String)(readKey: ResultSet => RT): ExecutableQuery[RT] = new ExecutableQuery[RT] {

    override def apply[AT: ArgumentsSetter](conn: Connection, args: AT): RT = {
      prepareStatement(conn, statement, args, generatedKeys = true) {
        ps: PreparedStatement =>
          secure(Context.ExecuteUpdate, statement) {
            ps.executeUpdate()
          }
          val rs = secure(Context.GetGeneratedKeys, statement) {
            ps.getGeneratedKeys
          }
          try {
            if (rs.next) {
              secure(Context.ReadGeneratedKeys, statement) {
                readKey(rs)
              }
            } else {
              throw new NoKeysGenerated(statement)
            }
          } finally {
            rs.close()
          }
      }
    }
  }

  private def secure[T](context: Context, sql: String)(f: => T): T = {
    try {
      f
    } catch {
      case e: SQLException => throw translateException(context, sql, e)
    }
  }

  private def translateException(context: Context, sql: String, sqle: SQLException): Exception = {
    try {
      exceptionTranslator(context, sql, sqle)
    } catch {
      case NonFatal(e) =>
        println(s"ALERT! Exception translating the original exception $sqle")
        sqle.printStackTrace()
        throw e
    }
  }

  private def prepareStatement[T, AT: ArgumentsSetter](conn: Connection, query: String, arguments: AT, generatedKeys: Boolean = false)(f: (PreparedStatement => T)): T = {
    val prepareStatementFlag = if (generatedKeys) Statement.RETURN_GENERATED_KEYS else Statement.NO_GENERATED_KEYS
    val s = secure(Context.PrepareStatement(generatedKeys), query) {
      conn.prepareStatement(query, prepareStatementFlag)
    }
    try {
      secure(Context.SetArguments, query) {
        implicitly[ArgumentsSetter[AT]].set(s, arguments)
      }
      f(s)
    } finally {
      s.close()
    }
  }

}
