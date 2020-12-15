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

package org.lastrix.rscan.model.tokens

import org.antlr.v4.runtime.{CharStream, CommonToken, Lexer, TokenSource}
import org.lastrix.rscan.vfs.VirtualPath

/**
 * Default RScan token with folding support
 *
 * @param path    the virtual path of target file
 * @param source  the source for this token
 * @param `type`  token type, see corresponding Lexer class
 * @param channel the token channel, DEFAULT is 0, COMMENTS should be 1
 * @param start   the start position of token in stream
 * @param stop    the stop position of token in stream
 */
class RScanToken(val path: VirtualPath, source: org.antlr.v4.runtime.misc.Pair[TokenSource, CharStream], `type`: Int, channel: Int = Lexer.DEFAULT_TOKEN_CHANNEL, start: Int = 0, stop: Int = 0)
  extends CommonToken(source, `type`, channel, start, stop) {

  private[this] var foldedTokens = List.empty[RScanToken]
  private[this] var _options = List.empty[(String, String)]

  def this(token: RScanToken) = {
    this(token.path, new org.antlr.v4.runtime.misc.Pair(token.getTokenSource, token.getInputStream), token.getType, token.getChannel, token.getStartIndex, token.getStopIndex)
    setText(token.text)
    setCharPositionInLine(token.charPositionInLine)
    setTokenIndex(token.getTokenIndex)
    setLine(token.line)
  }

  def setFoldedTokens(list: Iterable[RScanToken]): Unit = {
    foldedTokens = list.toList
  }

  def foldedTokens(): List[RScanToken] = foldedTokens

  def options: Seq[(String, String)] = _options

  def option(key: String): Option[String] = _options.find(_._1 == key).map(_._2)

  def option(key: String, value: String): Unit =
    indexOf(key) match {
      case Some(idx) => _options = _options.updated(idx, (key, value))
      case None => _options :+= ((key, value))
    }

  private def indexOf(key: String): Option[Int] =
    _options.find(_._1 == key).map(p => _options.indexOf(p))
}
