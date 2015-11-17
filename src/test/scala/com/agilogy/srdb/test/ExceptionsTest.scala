package com.agilogy.srdb.test

import java.sql._

import com.agilogy.srdb._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FlatSpec
import TestReader.read

class ExceptionsTest extends FlatSpec with MockFactory {

  import Srdb._

  val conn = mock[Connection]
  val ps = mock[PreparedStatement]
  val rs = mock[ResultSet]

  val sql = "select * from foo"

  it should "throw exceptions untranslated" in {
    inSequence {
      (conn.prepareStatement(_: String, _: Int)).expects(sql, Statement.NO_GENERATED_KEYS).throwing(new SQLException("ouch!", "12345", 123))
    }
    val res = intercept[SQLException](select(sql)(read(_.getString("name")))(conn))
    assert(res.getMessage === "Error executing SQL select * from foo")
  }

  it should "translate thrown exceptions" in {
    inSequence {
      (conn.prepareStatement(_: String, _: Int)).expects(sql, Statement.NO_GENERATED_KEYS).throwing(new SQLException("ouch!", "12345", 123))
    }
    val et: ExceptionTranslator = {
      case (_, _) => new RuntimeException("Translated!")
    }
    val db = withExceptionTranslator(et)
    val res = intercept[RuntimeException](db.select(sql)(read(_.getString("name")))(conn))
    assert(res.getMessage === "Translated!")
  }

}
