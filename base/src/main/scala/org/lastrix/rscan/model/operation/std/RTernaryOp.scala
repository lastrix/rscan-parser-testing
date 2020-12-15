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

package org.lastrix.rscan.model.operation.std

import org.jetbrains.annotations.NotNull
import org.lastrix.rscan.model.Statement
import org.lastrix.rscan.model.operation.{ROp, ROpKey}

sealed class RTernaryOp private
(
  key: ROpKey,
  private var _condition: ROp,
  private var _opTrue: ROp,
  private var _opFalse: ROp
)
  extends ROp(key, Seq(_condition, _opTrue, _opFalse)) {
  override def replaceChild(original: ROp, replacement: ROp): Unit = {
    //noinspection DuplicatedCode
    super.replaceChild(original, replacement)
    if (_condition == original) _condition = replacement
    else if (_opTrue == original) _opTrue = replacement
    else if (_opFalse == original) _opFalse = replacement
  }

  def condition: ROp = _condition

  def opTrue: ROp = _opTrue

  def opFalse: ROp = _opFalse
}

object RTernaryOp {
  @NotNull
  def apply(@NotNull statement: Statement,
            @NotNull condition: ROp,
            @NotNull opTrue: ROp,
            @NotNull opFalse: ROp): RTernaryOp =
    new RTernaryOp(ROpKey(StdOpType.TERNARY, statement), condition, opTrue, opFalse)
}
