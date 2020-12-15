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

import org.lastrix.rscan.model.exception.MissingChildOpException
import org.lastrix.rscan.model.operation.raw.RawOpType
import org.lastrix.rscan.model.operation.std.StdOpType

package object operation {
  def requireOneOf(op: ROp, childOpTypes: ROpType*): ROp =
    childOpTypes.flatMap(x => find(op, x)).headOption match {
      case Some(x) => x
      case None => throw new MissingChildOpException(op, childOpTypes)
    }

  def require(op: ROp, childOpType: ROpType): ROp = find(op, childOpType) match {
    case Some(child) => child
    case _ => throw new MissingChildOpException(op, childOpType);
  }

  def find(op: ROp, childOpType: ROpType): Option[ROp] = op.findChild(_.`type` == childOpType)

  def findAll(op: ROp, childOpType: ROpType): Seq[ROp] = op.filterChildren(_.`type` == childOpType)

  def typeParameters(op: ROp): Option[ROp] = find(op, StdOpType.TYPE_PARAMETERS)

  def parameters(op: ROp): Seq[ROp] = findAll(op, RawOpType.RAW_DECL_PARAMETER)

  def assertSingleChild(op: ROp): Unit = if (op.children.length != 1)
    throw new IllegalStateException(op.key.`type`.name + " operation requires single child")

  @scala.annotation.tailrec
  def evalDecl(op: ROp): Option[ROp] = op.`type` match {
    case StdOpType.INIT | StdOpType.EXPR =>
      assertSingleChild(op)
      evalDecl(op.children.head)
    case StdOpType.DECL => Some(op)
    case _ => None
  }

}
