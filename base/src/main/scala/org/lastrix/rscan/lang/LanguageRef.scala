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

package org.lastrix.rscan.lang

import _root_.java.util.concurrent.ConcurrentHashMap

sealed class LanguageRef private(val name: String) extends Comparable[LanguageRef] {
  def resolve: Language = Language.resolve(name)

  override def hashCode(): Int = name.hashCode()

  override def equals(obj: Any): Boolean = obj match {
    case x: LanguageRef => name == x.name
    case _ => false
  }

  override def toString: String = "ref:" + name

  override def compareTo(t: LanguageRef): Int = name.compareTo(t.name)
}

/**
 * Caching is required to make references singletons
 */
object LanguageRef {
  private[this] val _map = new ConcurrentHashMap[String, LanguageRef]()

  def fromString(value: String): Set[LanguageRef] =
    value.split(';')
      .map(p => LanguageRef(p))
      .toSet

  def toString(items: Iterable[LanguageRef]): String =
    if (items.isEmpty) ""
    else items.map(_.name).reduce(_ + ";" + _)

  def apply(name: String): LanguageRef = _map.computeIfAbsent(name, key => new LanguageRef(key))
}
