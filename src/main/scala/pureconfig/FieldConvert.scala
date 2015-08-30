/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
/**
 * @author Mario Pastorelli
 */
package pureconfig

import pureconfig.conf._
import shapeless.Lazy

import scala.util.{ Failure, Try }

/**
 * Utility trait used as strategy for conversion of a type [[T]] from/to a configuration of type
 * [[RawConfig]]. The main difference between this and [[ConfigConvert]] is that this is used to
 * convert each fields and has only two instances, one for "simple" types, called [[FieldConvert.primitiveFieldConvert]]
 * and one for "complex" (recursive) types, called [[FieldConvert.configFieldConvert]].
 * [[FieldConvert.primitiveFieldConvert]] uses an instance of [[StringConvert)]] while
 * [[FieldConvert.configFieldConvert]] uses an instance of [[ConfigConvert]].
 */
trait FieldConvert[T] {
  def from(config: RawConfig, namespace: String): Try[T]
  def to(t: T, namespace: String): RawConfig
}

object FieldConvert {

  def apply[T](implicit conv: FieldConvert[T]): FieldConvert[T] = conv

  implicit def primitiveFieldConvert[T](implicit strConvert: Lazy[StringConvert[T]]) = new FieldConvert[T] {
    override def from(config: RawConfig, namespace: String): Try[T] = {
      for {
        rawV <- Try(config(namespace)).orElse(Failure(new NoSuchElementException(s"key $namespace not found in configuration $config")))
        v <- strConvert.value.from(rawV)
      } yield v
    }

    override def to(t: T, namespace: String): RawConfig = {
      Map(namespace -> strConvert.value.to(t))
    }
  }

  implicit def configFieldConvert[T](implicit confConvert: Lazy[ConfigConvert[T]]) = new FieldConvert[T] {
    override def from(config: RawConfig, namespace: String): Try[T] = confConvert.value.from(config, namespace)
    override def to(t: T, namespace: String): RawConfig = confConvert.value.to(t, namespace)
  }
}