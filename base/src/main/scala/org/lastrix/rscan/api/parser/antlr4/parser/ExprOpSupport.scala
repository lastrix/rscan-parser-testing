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
import org.lastrix.rscan.model.operation.ROp
import org.lastrix.rscan.model.operation.raw.RawOpType
import org.lastrix.rscan.model.operation.std._

trait ExprOpSupport extends RScanParser {
  def opExpr(child: ROp): ROp =
    if (child.`type` == StdOpType.EXPR) child
    else opNode(child.statement, StdOpType.EXPR, child)

  def opExprList(list: java.util.List[ROp]): ROp =
    if (list.size() == 1) list.get(0)
    else opNode(StdOpType.EXPR_LIST, list)

  def opTernary(condition: ROp, opTrue: ROp, opFalse: ROp): ROp =
    RTernaryOp(evalStatement(condition, opFalse), opCondition(condition), opExpr(opTrue), opExpr(opFalse))

  def opBinary(`type`: BinaryType, list: java.util.List[ROp]): ROp =
    RBinaryOp(evalStatement(list), `type`, asSeq(list))

  def opBinary(`type`: BinaryType, left: ROp, right: ROp): ROp =
    RBinaryOp(evalStatement(left, right), `type`, Seq(left, right))

  def opUnary(start: Token, stop: Token, `type`: UnaryType, operation: ROp): ROp =
    RUnaryOp(evalStatement(start, stop), `type`, operation)

  def opSuper(token: Token): ROp = opNode(StdOpType.SUPER, token)

  def opParen(start: Token, stop: Token, child: ROp): ROp =
    opNode(evalStatement(start, stop), StdOpType.PARENTHESIZED, Seq(child))

  def opArrayAccessor(start: Token, stop: Token, expr: ROp): ROp =
    opNode(evalStatement(start, stop), StdOpType.ARRAY_ACCESSOR, Seq(expr))

  def opChain(list: java.util.List[ROp]): ROp =
    if (list.size() == 1) list.get(0)
    else opNode(StdOpType.CHAIN, list)

  def opChainOrAppend(main: ROp, op: ROp): ROp = {
    if (main.`type` == StdOpType.CHAIN) opNode(main.`type`, main.children :+ op)
    else opNode(StdOpType.CHAIN, Seq(main, op))
  }

  def opCall(start: Token, stop: Token, list: java.util.List[ROp]): ROp =
    opNode(evalStatement(start, stop), RawOpType.RAW_CALL, asSeq(list))

  def opEmptyCall(start: Token, stop: Token): ROp = opNode(evalStatement(start, stop), RawOpType.RAW_CALL)

}
