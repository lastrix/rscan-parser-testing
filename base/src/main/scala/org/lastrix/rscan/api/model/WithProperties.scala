/*
 * Copyright (C) 2019-2020.  rscan-parser-testing project
 *
 * This file is part of rscan-parser-testing project.
 *
 * rscan-parser-testing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * rscan-parser-testing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with rscan-parser-testing.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.lastrix.rscan.api.model

import java.time.Instant

trait WithProperties[T] {
  private var _props: Map[String, String] = Map.empty

  def prop(key: String, value: Any): T = {
    _props = _props + (key -> String.valueOf(value))
    this.asInstanceOf[T]
  }

  def prop(key: String): Option[String] = _props.get(key)

  def propInt(key: String): Option[Int] = prop(key) match {
    case Some(x) => Some(x.toInt)
    case None => None
  }

  def propLong(key: String): Option[Long] = prop(key) match {
    case Some(x) => Some(x.toLong)
    case None => None
  }

  def propInstant(key: String): Option[Instant] = prop(key) match {
    case Some(x) => Some(Instant.parse(x))
    case None => None
  }

  def propBoolean(key: String): Option[Boolean] = prop(key) match {
    case Some(x) => Some(x.toBoolean)
    case None => None
  }

  def propEnum[E <: Enum[E]](key: String, enumClass: Class[E]): Option[E] = prop(key) match {
    case Some(x) => Some(Enum.valueOf(enumClass, x))
    case None => None
  }
}
