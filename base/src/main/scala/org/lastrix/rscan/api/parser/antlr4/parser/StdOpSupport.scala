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
import org.lastrix.rscan.model.operation.std._
import org.lastrix.rscan.model.{RModifier, Statement}

import scala.jdk.javaapi.CollectionConverters

trait StdOpSupport extends RScanParser {
  /**
   * Create language root operation, this one is used to separate
   * languages inside multi-lang documents
   *
   * @param start the start token  for statement evaluation
   * @param stop  the stop token for statement evaluation
   * @param list  children list
   * @return operation
   */
  def opLang(start: Token, stop: Token, list: java.util.List[ROp]): RLangOp = RLangOp(
    Statement(file.virtualPath, 1, 0, endLine(stop), endLinePosition(stop)),
    language,
    asSeq(list)
  )

  def opEmptyLang(): RLangOp = RLangOp(Statement(file.virtualPath), language, Seq.empty)

  def opBlock(start: Token, stop: Token, list: java.util.List[ROp]): ROp = opNode(evalStatement(start, stop), StdOpType.BLOCK, asSeq(list))

  def opName(child: ROp): ROp = opNode(StdOpType.NAME, child)

  def opName(children: java.util.List[ROp]): ROp = opNode(StdOpType.NAME, children)

  def opNone(token: Token): ROp = opNode(StdOpType.NONE, token)

  def opReturn(token: Token): ROp = opNode(StdOpType.RETURN, token)

  def opReturn(start: Token, stop: Token, child: ROp): ROp = opNode(evalStatement(start, stop), StdOpType.RETURN, Seq(child))

  def opContinue(token: Token): ROp = opNode(StdOpType.CONTINUE, token)

  def opBreak(token: Token): ROp = opNode(StdOpType.BREAK, token)

  def opThrow(start: Token, stop: Token, child: ROp): ROp = opNode(evalStatement(start, stop), StdOpType.THROW, Seq(child))

  def opModifiers(statement: Statement, modifiers: java.util.List[RModifier]): ROp =
    RModifiersOp(statement, CollectionConverters.asScala(modifiers).toSeq)

  def opModifier(token: Token, modifier: RModifier): ROp =
    RModifierOp(evalStatement(token), modifier)
}
