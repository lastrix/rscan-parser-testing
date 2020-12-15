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

package org.lastrix.rscan.model

import org.lastrix.rscan.vfs.VirtualPath

sealed case class Statement
(
  path: VirtualPath,
  startLine: Int = 0,
  startLinePosition: Int = 0,
  endLine: Int = 0,
  endLinePosition: Int = 0,
  undefined: Boolean = false,
  virtual: Int = 0
) extends Comparable[Statement] {
  def ofVirtual(newVirtual: Int): Statement = Statement(
    path,
    startLine,
    startLinePosition,
    endLine,
    endLinePosition,
    undefined,
    newVirtual
  )


  def nextVirtual: Statement =
    Statement(path, startLine, startLinePosition, endLine, endLinePosition, undefined, virtual + 1)

  def inside(statement: Statement): Boolean = path != statement.path ||
    startLine >= statement.startLine && (startLine != statement.startLine || startLinePosition >= statement.startLinePosition) &&
      endLine <= statement.endLine && (endLine != statement.endLine || endLinePosition <= statement.endLinePosition)

  override def toString: String = s"$path[$startLine:$startLinePosition-$endLine:$endLinePosition${if (virtual == 0) "" else "@" + virtual}]${if (undefined) "*" else ""}"

  override def compareTo(t: Statement): Int = {
    path.compareTo(t.path) match {
      case x: Int if x != 0 => return x
      case _ =>
    }

    Integer.compare(startLine, t.startLine) match {
      case x: Int if x != 0 => return x
      case _ =>
    }

    Integer.compare(startLinePosition, t.startLinePosition) match {
      case x: Int if x != 0 => return x
      case _ =>
    }

    Integer.compare(endLine, t.endLine) match {
      case x: Int if x != 0 => return x
      case _ =>
    }

    Integer.compare(endLinePosition, t.endLinePosition) match {
      case x: Int if x != 0 => return x
      case _ =>
    }

    Integer.compare(virtual, t.virtual) match {
      case x: Int if x != 0 => return x
      case _ =>
    }

    java.lang.Boolean.compare(undefined, t.undefined)
  }
}

object Statement {
  val Undefined: Statement = Statement(VirtualPath.Empty, undefined = true)

  def from(stmts: Seq[Statement]): Statement = {
    val s = stmts.sorted
    val h = s.head
    val e = s.last
    Statement(
      h.path,
      h.startLine,
      h.startLinePosition,
      e.endLine,
      e.endLinePosition,
      h.undefined,
      h.virtual
    )
  }
}
