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

package org.lastrix.rscan.api.parser.antlr4.lexer

import org.antlr.v4.runtime.{Lexer, Token}
import org.lastrix.rscan.model.tokens.RScanToken

import scala.annotation.tailrec

trait FoldAwareLexer extends RScanLexer {
  addFilter(new TokenFilter {
    @tailrec
    override def nextToken(token: Option[Token]): Option[Token] = token match {
      case Some(x: RScanToken) if foldTokenEnabled && x.getChannel == Token.DEFAULT_CHANNEL && x.getType == foldOpen => Some(buildFoldToken(x))
      case Some(_) => token
      case None => nextToken(Some(nextTokenNonFilter))
    }

    override def priority: Int = Int.MaxValue - 1
  })

  final val foldBlock: Int = tokenType("FoldBlock")

  def foldTokenEnabled: Boolean = true

  def foldOpen: Int

  def foldClose: Int

  protected def buildFoldToken(start: RScanToken): RScanToken = {
    val (token, list) = createFoldBlockBody
    val result = new RScanToken(path, tokenFactorySourcePair, foldBlock, Lexer.DEFAULT_TOKEN_CHANNEL, start.getStartIndex, token.getStopIndex)
    result.setLine(start.getLine)
    result.setCharPositionInLine(start.getCharPositionInLine)
    result.setFoldedTokens(start +: list)
    result
  }

  private def createFoldBlockBody: (RScanToken, List[RScanToken]) = {
    var current: RScanToken = null
    var list = List.empty[RScanToken]
    while (current == null || current.getType != Token.EOF && (current.getType != foldClose || current.getChannel != Token.DEFAULT_CHANNEL)) {
      current = nextToken().asInstanceOf[RScanToken]
      list :+= current
    }
    if (current.getType == Token.EOF) throw new IllegalStateException()
    (current, list)
  }
}
