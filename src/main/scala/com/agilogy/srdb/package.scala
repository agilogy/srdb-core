package com.agilogy

import java.sql.SQLException


package object srdb {

  type ExceptionTranslator = SQLException => Exception

  val Srdb = new Srdb(identity[SQLException])

}