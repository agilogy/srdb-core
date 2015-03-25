package com.agilogy.srdb

trait ReadableQuery {
  def raw[T: Reader]: ExecutableQuery[T]

  def apply[T: Reader]: ExecutableQuery[Seq[T]] = raw(asList(implicitly[Reader[T]]))

  def single[T: Reader]: ExecutableQuery[Option[T]] = raw(asSingle(implicitly[Reader[T]]))
}

