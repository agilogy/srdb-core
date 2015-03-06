package com.agilogy.srdb

import java.sql._

import scala.util.control.NonFatal

class Srdb private[srdb](exceptionTranslator: ExceptionTranslator) {

  def select(query: String): ReadableQuery = new ReadableQuery {
    override def raw[T](implicit readResultSet: Reader[T]): ExecutableQuery[T] = new ExecutableQuery[T] {
      override def apply[AT:ArgumentsSetter](conn: Connection, args:AT): T = {
        prepareStatement(conn, query, args, generatedKeys = false) {
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
    override def apply[AT:ArgumentsSetter](conn: Connection, args:AT): Int = {
      prepareStatement(conn, statement, args) {
        ps =>
          secure {
            ps.executeUpdate()
          }
      }
    }
  }

  def executeUpdateGeneratedKeys[RT](statement: String)(readKey: ResultSet => RT): ExecutableQuery[RT] = new ExecutableQuery[RT] {
    override def apply[AT:ArgumentsSetter](conn: Connection, args:AT): RT = {
      prepareStatement(conn, statement, args, generatedKeys = true) {
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

  private def prepareStatement[T, DT, AT:ArgumentsSetter](conn: Connection, query: String, arguments: AT, generatedKeys: Boolean = false)(f: (PreparedStatement => T)): T = {
    val prepareStatementFlag = if (generatedKeys) Statement.RETURN_GENERATED_KEYS else Statement.NO_GENERATED_KEYS
    val s = secure {
      conn.prepareStatement(query, prepareStatementFlag)
    }
    try {
      secure {
        implicitly[ArgumentsSetter[AT]].set(s,arguments)
      }
      f(s)
    } finally {
      s.close()
    }
  }

}
