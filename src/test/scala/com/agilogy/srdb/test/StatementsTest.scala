package com.agilogy.srdb.test

import java.sql.{ ResultSet, PreparedStatement, Connection, Statement }

import com.agilogy.srdb._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FlatSpec

class StatementsTest extends FlatSpec with MockFactory {

  import Srdb._

  val sql = "sql"

  val conn = mock[Connection]
  val ps = mock[PreparedStatement]
  val rs = mock[ResultSet]

  behavior of "executeUpdate"

  it should "execute simple updates" in {
    inSequence {
      (conn.prepareStatement(_: String, _: Int)).expects(sql, Statement.NO_GENERATED_KEYS).returning(ps)
      (ps.executeUpdate _).expects().returning(34)
      (ps.close _).expects()
    }
    val res = update(sql)(conn)
    assert(res === 34)
  }

  behavior of "executeUpdateGeneratedKeys"

  it should "execute updates that return generated keys" in {
    inSequence {
      (conn.prepareStatement(_: String, _: Int)).expects(sql, Statement.RETURN_GENERATED_KEYS).returning(ps)
      (ps.executeUpdate _).expects()
      (ps.getGeneratedKeys _).expects().returning(rs)
      (rs.next _).expects().returning(true)
      (rs.getLong(_: Int)).expects(1).returning(34)
      (rs.close _).expects()
      (ps.close _).expects()
    }
    val res = updateGeneratedKeys(sql)(_.getLong(1))(conn)
    assert(res === 34)
  }

}
