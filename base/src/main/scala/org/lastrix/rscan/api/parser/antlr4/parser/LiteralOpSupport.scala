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
import org.lastrix.rscan.model.literal.{RBooleanLiteral, RLiteral, RNullLiteral, RUndefinedLiteral}
import org.lastrix.rscan.model.operation.ROp
import org.lastrix.rscan.model.operation.std.{RLiteralOp, RUnresolvedIdOp}

trait LiteralOpSupport extends RScanParser {
  def trueLiteral(token: Token): ROp = opLiteral(token, RBooleanLiteral.TRUE)

  def falseLiteral(token: Token): ROp = opLiteral(token, RBooleanLiteral.FALSE)

  def nullLiteral(token: Token): ROp = opLiteral(token, RNullLiteral.NULL)

  def undefinedLiteral(token: Token): ROp = opLiteral(token, RUndefinedLiteral.UNDEFINED)

  def opLiteral(token: Token, literal: RLiteral): RLiteralOp = RLiteralOp(evalStatement(token), literal)

  def opUnresolvedId(token: Token): ROp = opUnresolvedId(token, token.getText)

  def opUnresolvedId(token: Token, text: String): ROp = RUnresolvedIdOp(evalStatement(token), text)

}
