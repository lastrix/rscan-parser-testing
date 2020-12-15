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

package org.lastrix.rscan.api.parser

import org.antlr.v4.runtime.atn.ATN
import org.antlr.v4.runtime.dfa.DFA
import org.antlr.v4.runtime.{CharStream, CharStreams, TokenStream}
import org.lastrix.rscan.api.parser.antlr4.lexer.AbstractRScanLexer
import org.lastrix.rscan.api.parser.antlr4.parser.AbstractRScanParser
import org.lastrix.rscan.api.parser.antlr4.{ATNCache, Cached}
import org.lastrix.rscan.model.tokens.RScanTokenFactory
import org.lastrix.rscan.vfs
import org.lastrix.rscan.vfs.VirtualFile

abstract class AbstractParserService extends ParserService {

  def newLexer(stream: CharStream, file: VirtualFile, atn: ATN, decisionToDFA: Array[DFA]): AbstractRScanLexer

  def newParser(file: VirtualFile, atn: ATN, decisionToDFA: Array[DFA], stream: TokenStream): AbstractRScanParser

  override final def newLexer(file: VirtualFile, config: LexerConfig): AbstractRScanLexer = {
    val charStream = config.text match {
      case Some(v) => CharStreams.fromString(v, file.absoluteName)
      case None => vfs.toCharStream(file)
    }
    val Cached(atn, dfa) = ATNCache.forClass(lexerClass, Math.max(1, (file.source.size / 16).toInt))
    val lexer = newLexer(charStream, file, atn, dfa)
    //    lexer match {
    //      case x: TemplateAwareLexer => x.templateMode(config.template)
    //      case _ =>
    //    }
    lexer.setTokenFactory(new RScanTokenFactory(file.virtualPath, config.offsetLine, config.offsetLinePos))
    lexer
  }

  override final def newParser(file: VirtualFile, stream: TokenStream): AbstractRScanParser = {
    val Cached(atn, dfa) = ATNCache.forClass(parserClass, stream.size())
    newParser(file, atn, dfa, stream)
  }
}
