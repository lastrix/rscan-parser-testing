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

package org.lastrix.rscan.test

import org.antlr.v4.runtime.{BufferedTokenStream, ListTokenSource, Token}
import org.lastrix.rscan.api.parser.ParserService
import org.lastrix.rscan.api.parser.antlr4.lexer.AbstractRScanLexer
import org.lastrix.rscan.lang.LanguageRef
import org.lastrix.rscan.model.tokens.RScanToken
import org.lastrix.rscan.test.Antlr4TestUtils.tokenStatement
import org.lastrix.rscan.vfs.VirtualFile
import org.lastrix.rscan.vfs.source.FileSource

import java.io.File
import java.util
import scala.jdk.javaapi.CollectionConverters

sealed case class LexerOutput(tokens: Seq[LexerOutputToken])

sealed case class LexerOutputToken
(
  tokenType: String,
  statement: String,
  text: String = null,
  channel: String = null,
  children: LexerOutput = null
)

object LexerOutput {

  def build(file: File, language: LanguageRef): LexerOutput = {
    val vFile = new VirtualFile(file.getName, new FileSource(file, file.getName))
    implicit val ps: ParserService = ParserService(language)
    val lexer = ps.newLexer(vFile)
    val ts = new BufferedTokenStream(lexer)
    ts.fill()
    buildImpl(ts, lexer)
  }

  private def buildImpl(ts: BufferedTokenStream, lexer: AbstractRScanLexer)(implicit ps: ParserService): LexerOutput = {
    var result = List.empty[LexerOutputToken]
    ts.getTokens.forEach(t => {
      val tokenType = lexer.getVocabulary.getSymbolicName(t.getType)
      val text = if (ps.isTokenTypeWithText(tokenType)) t.getText else null
      t match {
        case token: RScanToken if ps.hasFoldedTokens(tokenType) && token.foldedTokens().nonEmpty =>
          result :+= LexerOutputToken(tokenType, tokenStatement(t), text, nullIfZero(t.getChannel), buildFold(token, lexer))
        case _ => result :+= LexerOutputToken(tokenType, tokenStatement(t), text, nullIfZero(t.getChannel))
      }
    })
    LexerOutput(result)
  }

  private def nullIfZero(intValue: Int): String = if (intValue == 0) null else String.valueOf(intValue)

  private def buildFold(t: RScanToken, lexer: AbstractRScanLexer)(implicit ps: ParserService): LexerOutput = {
    val source = new ListTokenSource(new util.ArrayList[Token](CollectionConverters.asJava(t.foldedTokens())))
    val ts = new BufferedTokenStream(source)
    ts.fill()
    buildImpl(ts, lexer)
  }

}
