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

import org.antlr.v4.runtime.Token
import org.jetbrains.annotations.Nullable
import org.lastrix.rscan.model.operation.ROp
import org.lastrix.rscan.model.operation.std._

trait StmtOpSupport extends RScanParser {

  def opStmtList(list: java.util.List[ROp]): ROp = opNode(StdOpType.BLOCK, list)

  def opTry(start: Token, stop: Token, body: ROp, catchBlocks: java.util.List[ROp], finallyBlock: ROp): ROp =
    opTry(start, stop, body, catchBlocks, finallyBlock, null)

  def opTry(start: Token, stop: Token, body: ROp, catchBlocks: java.util.List[ROp], finallyBlock: ROp, completeBlock: ROp): ROp =
    opNode(
      evalStatement(start, stop),
      StdOpType.TRY,
      Seq(body) ++ asSeq(catchBlocks) ++ (if (finallyBlock == null) Seq.empty[ROp] else Seq(finallyBlock))
        ++ (if (completeBlock == null) Seq.empty[ROp] else Seq(completeBlock))
    )

  def opConditionalBlock(start: Token,
                         stop: Token,
                         conditionalType: ConditionalType,
                         condition: ROp,
                         body: ROp): ROp = opConditionalBlock(start, stop, conditionalType, condition, body, null)

  def opConditionalBlock(start: Token,
                         stop: Token,
                         conditionalType: ConditionalType,
                         condition: ROp,
                         body: ROp,
                         completeBody: ROp): ROp =
    RConditionBlockOp(evalStatement(start, stop), conditionalType, condition, body, completeBody)

  def opSwitch(start: Token, stop: Token, condition: ROp, body: ROp): ROp =
    opNode(evalStatement(start, stop), StdOpType.CASE, Seq(condition, body))

  def opCaseItem(start: Token, stop: Token, condition: ROp, body: java.util.List[ROp]): ROp = {
    val b = if (body == null || body.isEmpty) null else opNode(StdOpType.LIST, body)
    RConditionItemOp(evalStatement(start, stop), condition, b)
  }

  def opDefaultCaseItem(start: Token, stop: Token, body: java.util.List[ROp]): ROp = {
    val b = if (body == null || body.isEmpty) null else opNode(StdOpType.LIST, body)
    RConditionItemOp(evalStatement(start, stop), b)
  }

  def opIfStmt(start: Token, stop: Token, conditional: java.util.List[ROp], @Nullable unconditional: ROp): ROp =
    opNode(
      evalStatement(start, stop),
      StdOpType.IF,
      asSeq(conditional) ++ (if (unconditional == null) Seq.empty[ROp] else Seq(unconditional))
    )

  def opLabel(start: Token, stop: Token, label: String, op: ROp): ROp =
    RLabelOp(evalStatement(start, stop), label, Seq(op))

  def opAssign(left: ROp, assignType: AssignType, right: ROp): ROp =
    RAssignOp(evalStatement(left, right), assignType, left, right)

}
