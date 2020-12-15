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

import org.antlr.v4.runtime.misc.Pair
import org.antlr.v4.runtime.{CharStream, Lexer, Token, TokenSource}
import org.lastrix.rscan.vfs.{VirtualFile, VirtualPath}

import scala.annotation.tailrec
import scala.collection.mutable

abstract class AbstractRScanLexer(stream: CharStream, val file: VirtualFile)
  extends Lexer(stream) with RScanLexer {

  // enqueued token filter is top priority always
  addFilter(new TokenFilter {
    // we may ignore parameter because it should always be None
    override def nextToken(token: Option[Token]): Option[Token] = if (hasEnqueuedTokens) Some(dequeueToken) else None

    override def priority: Int = Int.MinValue
  })

  // ensure that we always get token from stream
  addFilter(new TokenFilter {
    override def nextToken(token: Option[Token]): Option[Token] = if (token.isDefined) token else Some(nextTokenNonFilter)

    override def priority: Int = Int.MaxValue
  })

  @tailrec
  override final def nextToken(): Token = nextTokenFromFilter match {
    case Some(t) =>
      if (t.getChannel == Token.DEFAULT_CHANNEL) _lastToken = t
      t
    case None => nextToken()
  }

  //////////////////////////////////// Attributes and overrides ////////////////////////////////////////////////////////
  private var _lastToken: Token = _

  def lastToken: Token = _lastToken

  override def path: VirtualPath = file.virtualPath

  override def tokenFactorySourcePair: Pair[TokenSource, CharStream] = _tokenFactorySourcePair

  override protected def LA(idx: Int): Int = _input.LA(idx)

  final def isStartOfFile: Boolean = getCharPositionInLine == 0 && getLine == 1

  override protected def nextTokenNonFilter: Token = super.nextToken()

  //////////////////////////////////// Token queue support /////////////////////////////////////////////////////////////
  private val tokenQueue = mutable.Queue[Token]()

  protected final def enqueueToken(token: Token): Unit = tokenQueue.enqueue(token)

  protected final def hasEnqueuedTokens: Boolean = tokenQueue.nonEmpty

  protected final def dequeueToken: Token = tokenQueue.dequeue()

}




