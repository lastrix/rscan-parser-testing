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

import org.lastrix.rscan.model.operation.ROp
import org.lastrix.rscan.model.operation.std.{RModifierOp, RModifiersOp, StdOpType}

sealed trait RModifier {
  def text: String

  override def toString: String = text
}

class RTextModifier(override val text: String)
  extends RModifier

class RSpecModifier(override val text: String)
  extends RModifier

object modifiers {
  def from(op: ROp): Seq[RModifier] = {
    val modifiersSeq: Seq[RModifier] = operation.find(op, StdOpType.MODIFIERS) match {
      case Some(x: RModifiersOp) => x.modifiers.toSeq
      case _ => Seq.empty
    }
    val modifiersSeq2: Seq[RModifier] = operation.findAll(op, StdOpType.MODIFIER).map(x => x.asInstanceOf[RModifierOp].modifier)
    modifiersSeq ++ modifiersSeq2
  }
}
