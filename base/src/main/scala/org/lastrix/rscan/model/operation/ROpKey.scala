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

package org.lastrix.rscan.model.operation

import org.lastrix.rscan.model.Statement

import java.util.concurrent.ConcurrentHashMap

sealed class ROpKey private
(
  val `type`: ROpType,
  val statement: Statement
) extends Comparable[ROpKey] {
  override def toString: String = s"${`type`.name} at ${if (statement.undefined) "<undefined>" else statement}"

  override lazy val hashCode: Int = `type`.hashCode() * 31 + statement.hashCode()

  override def equals(obj: Any): Boolean = obj match {
    case x: ROpKey => `type` == x.`type` && statement == x.statement
    case _ => false
  }

  override def compareTo(t: ROpKey): Int = statement.compareTo(t.statement) match {
    case x: Int if x != 0 => x
    case _ => `type`.name.compareTo(t.`type`.name)
  }
}

object ROpKey {
  private val Cache = new ConcurrentHashMap[ROpKey, ROpKey]()

  def clearCache(): Unit = Cache.clear()

  def apply(`type`: ROpType, statement: Statement): ROpKey =
    Cache.computeIfAbsent(new ROpKey(`type`, statement), _key => _key)

}
