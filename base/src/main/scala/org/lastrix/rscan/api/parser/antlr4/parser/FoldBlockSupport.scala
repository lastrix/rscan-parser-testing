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

package org.lastrix.rscan.api.parser.antlr4.parser

import org.lastrix.rscan.model.operation.ROp
import org.lastrix.rscan.model.operation.std.RFoldOp
import org.lastrix.rscan.model.tokens.RScanToken

trait FoldBlockSupport extends RScanParser {

  import FoldBlockSupport._

  private var _folds = List.empty[RFoldOp]

  protected def addFold(op: RFoldOp): Unit = _folds :+= op

  def folds: List[RFoldOp] = _folds

  /**
   * Create folded block operation from token, the FOLD type is used by default
   *
   * @param token        the folded tokens token
   * @param foldRuleName the rule name to execute for parser
   * @return
   */
  def opFoldBlock(token: RScanToken, foldRuleName: String = DefaultFoldRuleName): ROp = {
    val head: RScanToken = token.foldedTokens().head
    val last = token.foldedTokens().last
    token.option(FoldRuleOption, foldRuleName)
    val op = RFoldOp(evalStatement(head, last), token)
    _folds :+= op
    op
  }
}

object FoldBlockSupport {
  val DefaultFoldRuleName = "startFoldBlock"
  val FoldRuleOption = "foldRule"
}
