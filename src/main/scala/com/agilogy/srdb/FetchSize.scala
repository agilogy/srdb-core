package com.agilogy.srdb

trait FetchSize
case class LimitedFetchSize(rows: Int) extends FetchSize
case object DefaultFetchSize extends FetchSize
