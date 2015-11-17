package com.agilogy.srdb.test

import java.sql.ResultSet
import scala.collection.mutable.ListBuffer

object TestReader {

  def read[T](readRow: ResultSet => T): ResultSet => Seq[T] = {
    rs =>
      val res = new ListBuffer[T]
      while (rs.next()) {
        res.append(readRow(rs))
      }
      res.toSeq
  }

}
