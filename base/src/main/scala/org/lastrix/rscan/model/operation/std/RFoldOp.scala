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
import org.lastrix.rscan.model.operation.{ROp, ROpKey, ROpType}
import org.lastrix.rscan.model.tokens.RScanToken

/**
 * Special operation for storing intermediate parser results,
 * improves parser performance by reducing required lookahead steps
 * significantly. The file size with such approach affects parsing time
 * linearly instead of exponentially.
 *
 * @param key   the operation key
 * @param token The token with all folded tokens
 */
sealed class RFoldOp private
(
  key: ROpKey,
  val token: RScanToken
) extends ROp(key)

object RFoldOp {
  @NotNull
  def apply(@NotNull statement: Statement,
            @NotNull token: RScanToken): RFoldOp =
    new RFoldOp(ROpKey(StdOpType.FOLD, statement), token)

  @NotNull
  def apply(@NotNull opType: ROpType,
            @NotNull statement: Statement,
            @NotNull token: RScanToken): RFoldOp =
    new RFoldOp(ROpKey(opType, statement), token)

}
