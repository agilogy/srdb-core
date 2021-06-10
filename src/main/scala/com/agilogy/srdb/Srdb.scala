package com.agilogy.srdb

import java.sql._

import scala.util.control.NonFatal

import com.agilogy.srdb.exceptions.{ Context, _ }
import javax.sql.DataSource

class Srdb private[srdb] (exceptionTranslator: ExceptionTranslator) {

  def close(conn: Connection): Unit = {
    try {
      if (!conn.isClosed) conn.close()
    } catch {
      case t: Throwable => t.printStackTrace()
    }
  }

  def commitAndClose(conn: Connection): Unit = {
    try {
      conn.commit()
    } finally {
      close(conn)
    }
  }

  def rollbackAndClose(conn: Connection): Unit = {
    try {
      if (!conn.isClosed) conn.rollback()
    } finally {
      close(conn)
    }
  }

  def inTransaction[T](ds: DataSource)(f: Connection => T): T = {
    val conn = ds.getConnection
    try {
      conn.setAutoCommit(false)
      val res = f(conn)
      commitAndClose(conn)
      res
    } catch {
      case t: Throwable =>
        rollbackAndClose(conn)
        throw t
    }
  }

  def withSavepoint[T](conn: Connection)(f: => T)(catchBlock: PartialFunction[Exception, T]): T = {
    val savepoint = conn.setSavepoint()
    try {
      f
    } catch {
      case e: Exception if catchBlock.isDefinedAt(e) =>
        conn.rollback(savepoint)
        catchBlock(e)
    }
  }

  def select[T](query: String, fetchSize: FetchSize = DefaultFetchSize)(reader: ResultSet => T): ExecutableQuery[T] = new ExecutableQuery[T] {

    override def apply[AT: ArgumentsSetter](conn: Connection, args: AT): T = {
      prepareStatement(conn, query) {
        ps =>
          setArguments(ps, args, query)
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
    override def apply[AT: ArgumentsSetter](conn: Connection, args: AT): Int =
      prepareStatement(conn, statement) { ps =>
        setArguments(ps, args, statement)
        secure(Context.ExecuteUpdate, statement) { ps.executeUpdate() }
      }
  }

  def batchUpdate(statement: String): BatchUpdate = new BatchUpdate {

    override def apply[T: ArgumentsSetter](conn: Connection, args: List[T]): scala.Array[Int] =
      prepareStatement(conn, statement) { ps =>
        args.foreach { arg =>
          setArguments(ps, arg, statement)
          ps.addBatch()
        }
        secure(Context.ExecuteUpdate, statement) { ps.executeBatch() }
      }

  }

  def updateGeneratedKeys[RT](statement: String)(readKey: ResultSet => RT): ExecutableQuery[RT] = new ExecutableQuery[RT] {

    override def apply[AT: ArgumentsSetter](conn: Connection, args: AT): RT = {
      prepareStatement(conn, statement, generatedKeys = true) { ps: PreparedStatement =>
        setArguments(ps, args, statement)
        executeUpdate(statement, ps)
        val rs = secure(Context.GetGeneratedKeys, statement) {
          ps.getGeneratedKeys
        }
        try {
          if (rs.next) {
            secure(Context.ReadGeneratedKeys, statement) {
              readKey(rs)
            }
          } else {
            throw NoKeysGenerated(statement)
          }
        } finally {
          rs.close()
        }
      }
    }
  }

  private def executeUpdate[RT, AT: ArgumentsSetter](statement: String, ps: PreparedStatement) = {
    secure(Context.ExecuteUpdate, statement) {
      ps.executeUpdate()
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

  private def setArguments[AT: ArgumentsSetter](ps: PreparedStatement, arguments: AT, query: String): Unit = {
    secure(Context.SetArguments, query) {
      implicitly[ArgumentsSetter[AT]].set(ps, arguments)
    }
  }

  private def prepareStatement[T](conn: Connection, query: String, generatedKeys: Boolean = false)(f: PreparedStatement => T): T = {
    val prepareStatementFlag = if (generatedKeys) Statement.RETURN_GENERATED_KEYS else Statement.NO_GENERATED_KEYS
    val s = secure(Context.PrepareStatement(generatedKeys), query) {
      conn.prepareStatement(query, prepareStatementFlag)
    }
    try {
      f(s)
    } finally {
      s.close()
    }
  }

}
