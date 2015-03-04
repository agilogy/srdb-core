package com.agilogy.srdb

import java.sql._

import scala.util.control.NonFatal

class Srdb private[srdb](exceptionTranslator: ExceptionTranslator) {

  def select(query: String): ReadableQuery = new ReadableQuery {
    override def raw[T](readResultSet: Reader[T]): ExecutableQuery[T] = new ExecutableQuery[T] {
      override def apply(conn: Connection, setStatementParameters: (PreparedStatement) => Unit): T = {
        prepareStatement(conn, query, setStatementParameters, generatedKeys = false) {
          ps =>
            secure {
              ps.executeQuery()
            }
            val rs = secure(ps.getResultSet)
            try {
              secure(readResultSet(rs))
            } finally {
              rs.close()
            }
        }
      }
    }
  }

  def executeUpdate(statement: String): ExecutableQuery[Int] = new ExecutableQuery[Int] {
    override def apply(conn: Connection, setStatementParameters: (PreparedStatement) => Unit): Int = {
      prepareStatement(conn, statement, setStatementParameters) {
        ps =>
          secure {
            ps.executeUpdate()
          }
      }
    }
  }

  def executeUpdateGeneratedKeys[RT](statement: String)(readKey: ResultSet => RT): ExecutableQuery[RT] = new ExecutableQuery[RT] {
    override def apply(conn: Connection, setStatementParameters: (PreparedStatement) => Unit): RT = {
      prepareStatement(conn, statement, setStatementParameters, generatedKeys = true) {
        ps: PreparedStatement =>
          val result = secure {
            ps.executeUpdate()
          }
          val rs = secure {
            ps.getGeneratedKeys
          }
          try {
            if (rs.next) {
              secure {
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


  private def secure[T](f: => T): T = {
    try {
      f
    } catch {
      case e: SQLException => throw translateException(e)
    }
  }

  private def translateException(sqle: SQLException): Exception = {
    try {
      exceptionTranslator(sqle)
    } catch {
      case NonFatal(e) => sqle
    }
  }

  private def prepareStatement[T, DT](conn: Connection, query: String, setArguments: PreparedStatement => Unit, generatedKeys: Boolean = false)(f: (PreparedStatement => T)): T = {
    val prepareStatementFlag = if (generatedKeys) Statement.RETURN_GENERATED_KEYS else Statement.NO_GENERATED_KEYS
    val s = secure {
      conn.prepareStatement(query, prepareStatementFlag)
    }
    try {
      secure {
        setArguments(s)
      }
      f(s)
    } finally {
      s.close()
    }
  }

}
