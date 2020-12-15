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
import org.lastrix.rscan.model.operation.{ROp, ROpKey, ROpType}

sealed class RNodeOp private
(
  key: ROpKey,
  children: Iterable[ROp]
) extends ROp(key, children) {
  override def specText: String = null
}

object RNodeOp {
  @NotNull
  def apply(@NotNull `type`: ROpType,
            @NotNull statement: Statement,
            @NotNull children: Iterable[ROp]): RNodeOp =
    new RNodeOp(ROpKey(`type`, statement), children)

  @NotNull
  def apply(@NotNull `type`: ROpType,
            @NotNull statement: Statement,
            @NotNull child: ROp): RNodeOp =
    apply(`type`, statement, Seq(child))

  @NotNull
  def discarded(@NotNull statement: Statement,
                @Nullable child: ROp = null): RNodeOp =
    apply(StdOpType.DISCARDED, statement, if (child == null) Seq.empty else Seq(child))
}
