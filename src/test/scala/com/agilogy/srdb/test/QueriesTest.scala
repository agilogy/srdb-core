package com.agilogy.srdb.test

import java.sql._

import com.agilogy.srdb._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FlatSpec

class QueriesTest extends FlatSpec with MockFactory {
  import Srdb._

  val conn = mock[Connection]
  val ps = mock[PreparedStatement]
  val rs = mock[ResultSet]

  val sql = "sql"

  private def expectPrepareStatement(conn: Connection) = {
    (conn.prepareStatement(_: String, _: Int)).expects(sql, Statement.NO_GENERATED_KEYS).returning(ps)
  }

  private def expectIterateIntResults(rs: ResultSet) = {
    (rs.next _).expects().returning(true)
    (rs.getInt(_: String)).expects("result").returning(4)
    (rs.next _).expects().returning(true)
    (rs.getInt(_: String)).expects("result").returning(88)
    (rs.next _).expects().returning(false)
  }

  private def expectClose(rs: ResultSet, ps: PreparedStatement) = {
    (rs.close _).expects()
    (ps.close _).expects()
  }

  behavior of "select"

  it should "execute simple queries" in {
    inSequence {
      expectPrepareStatement(conn)
      (ps.executeQuery _).expects().returning(rs)
      expectIterateIntResults(rs)
      expectClose(rs, ps)
    }
    val res = select(sql)(readSeq(_.getInt("result")))(conn)
    assert(res === Seq(4, 88))
  }

  it should "return no rows when no rows are found" in {
    inSequence {
      expectPrepareStatement(conn)
      (ps.executeQuery _).expects().returning(rs)
      (rs.next _).expects().returning(false)
      expectClose(rs, ps)
    }
    val res = select(sql)(readSeq(_.getString("name")))(conn)
    assert(res === Seq.empty)
  }

  behavior of "selects in general"

  it should "accept Argument instances as parameters" in {
    inSequence {
      expectPrepareStatement(conn)
      (ps.setString _).expects(1, "foo")
      (ps.setInt _).expects(2, 23)
      (ps.executeQuery _).expects().returning(rs)
      (rs.next _).expects().returning(false)
      expectClose(rs, ps)
    }
    //    val arg1: Argument = _.setString(_, "foo")
    //    val arg2: Argument = _.setInt(_, 23)
    val res = select(sql)(readSeq(_.getString("name")))(conn, _.setString(_, "foo"), _.setInt(_, 23))
    assert(res === Seq.empty)
  }

  it should "accept a single argument setter as parameter" in {
    inSequence {
      expectPrepareStatement(conn)
      (ps.setString _).expects(1, "foo")
      (ps.setInt _).expects(2, 23)
      (ps.executeQuery _).expects().returning(rs)
      (rs.next _).expects().returning(false)
      expectClose(rs, ps)
    }
    val args = (ps: PreparedStatement) => {
      ps.setString(1, "foo")
      ps.setInt(2, 23)
    }
    val res = select(sql)(readSeq(_.getString("name")))(conn, args)
    assert(res === Seq.empty)
  }

}
