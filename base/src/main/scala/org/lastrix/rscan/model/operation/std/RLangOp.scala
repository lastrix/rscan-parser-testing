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
import org.lastrix.rscan.lang.LanguageRef
import org.lastrix.rscan.model.Statement
import org.lastrix.rscan.model.operation.ROp.LINE_SEPARATOR
import org.lastrix.rscan.model.operation.{ROp, ROpKey}

sealed class RLangOp private
(
  key: ROpKey,
  val language: LanguageRef,
  children: Iterable[ROp]
)
  extends ROp(key, children) {

  override def prettyPrint(sb: StringBuilder, prefix: String): Unit = {
    sb.append(prefix).append(key).append(LINE_SEPARATOR)
      .append(prefix).append("\t").append("Language: ").append(language.name).append(LINE_SEPARATOR)
    appendChildren(sb, prefix)
  }
}

object RLangOp {
  @NotNull
  def apply(@NotNull statement: Statement,
            @NotNull language: LanguageRef,
            @NotNull children: Seq[ROp]): RLangOp =
    new RLangOp(ROpKey(StdOpType.LANG, statement), language, children)

}
