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

package org.lastrix.rscan.api.parser.antlr4

import org.antlr.v4.runtime._
import org.lastrix.rscan.vfs.VirtualFile
import org.slf4j.LoggerFactory

import java.util
import java.util.Collections

class RScanErrorListener(val file: VirtualFile, val tokenStream: TokenStream = null) extends BaseErrorListener {

  import RScanErrorListener._

  override def syntaxError(recognizer: Recognizer[_, _], offendingSymbol: Any, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException): Unit = {
    super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e)
    printParserDetails(recognizer.asInstanceOf[Parser])
    log.error(s"Syntax error: ${file.absoluteName}:$line:$charPositionInLine: $msg\r\n$offendingSymbol")
    //log.trace(s"SourceCode:\\r\\n${tokenStream.getText}")
    throw new IllegalStateException(e)
  }

  private def printParserDetails(parser: Parser): Unit = {
    val tokensStack: StringBuilder = new StringBuilder
    tokensStack.append("Tokens before:").append(System.lineSeparator)
    val tokenStream: TokenStream = parser.getTokenStream
    val lastToken: Token = parser.getCurrentToken
    for (i <- Math.max(0, lastToken.getTokenIndex - NumTokensToDump) to Math.min(lastToken.getTokenIndex, tokenStream.size() - 1))
      tokensStack.append('\t').append(tokenStream.get(i)).append(System.lineSeparator)

    tokensStack.append("Tokens after:").append(System.lineSeparator)
    for (i <- lastToken.getTokenIndex + 1 to Math.min(lastToken.getTokenIndex + NumTokensToDump, tokenStream.size() - 1))
      tokensStack.append('\t').append(tokenStream.get(i)).append(System.lineSeparator)

    println(tokensStack.toString())
    val stack: util.List[String] = parser.getRuleInvocationStack
    Collections.reverse(stack)
    stack.forEach(x => println("\t" + x))
  }
}

object RScanErrorListener {
  private val log = LoggerFactory.getLogger(getClass)
  private val NumTokensToDump = 10
}
