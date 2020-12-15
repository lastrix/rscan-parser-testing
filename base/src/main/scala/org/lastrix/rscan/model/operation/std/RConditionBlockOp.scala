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

import org.jetbrains.annotations.{NotNull, Nullable}
import org.lastrix.rscan.model.Statement
import org.lastrix.rscan.model.operation.ROp.LINE_SEPARATOR
import org.lastrix.rscan.model.operation.{ROp, ROpKey}

sealed class RConditionBlockOp private
(
  key: ROpKey,
  val conditionalType: ConditionalType,
  private var _condition: ROp,
  private var _body: ROp,
  private var _completeBody: ROp
)
  extends ROp(key, Seq(_condition, _body) ++ (if (_completeBody == null) Seq.empty else Seq(_completeBody))) {


  override def replaceChild(original: ROp, replacement: ROp): Unit = {
    super.replaceChild(original, replacement)
    if (_condition == original) _condition = replacement
    else if (_body == original) _body = replacement
    else if (_completeBody == original) _completeBody = replacement
  }

  def condition: ROp = _condition

  def body: ROp = _body

  def completeBody: ROp = _completeBody

  override def prettyPrint(sb: StringBuilder, prefix: String): Unit = {
    sb.append(prefix).append(key).append(LINE_SEPARATOR)
    sb.append(prefix).append('\t').append("Type: ").append(conditionalType).append(LINE_SEPARATOR)
    appendChildren(sb, prefix)
  }
}

object RConditionBlockOp {
  @NotNull
  def apply(@NotNull statement: Statement,
            @NotNull conditionalType: ConditionalType,
            @NotNull condition: ROp,
            @NotNull body: ROp,
            @Nullable completeBody: ROp = null): RConditionBlockOp =
    new RConditionBlockOp(ROpKey(StdOpType.BLOCK_CONDITIONAL, statement), conditionalType, condition, body, completeBody)
}
