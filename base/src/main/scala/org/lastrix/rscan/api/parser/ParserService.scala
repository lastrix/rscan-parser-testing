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

import org.antlr.v4.runtime.TokenStream
import org.lastrix.rscan.api.parser.antlr4.lexer.AbstractRScanLexer
import org.lastrix.rscan.api.parser.antlr4.parser.AbstractRScanParser
import org.lastrix.rscan.lang.LanguageRef
import org.lastrix.rscan.vfs.VirtualFile

import java.util.ServiceLoader
import scala.jdk.javaapi.CollectionConverters

trait ParserService {
  def language: LanguageRef

  /**
   * Create new lexer for provided file
   *
   * @param file   the virtual file
   * @param config lexer configuration parameters
   * @return the new lexer for target file
   */
  def newLexer(file: VirtualFile, config: LexerConfig = LexerConfig()): AbstractRScanLexer

  /**
   * Create new parser for provided file and Token stream
   *
   * @param file   the virtual file (source)
   * @param stream the token stream to analyze
   * @return the Parser
   */
  def newParser(file: VirtualFile, stream: TokenStream): AbstractRScanParser

  def lexerClass: Class[_ <: AbstractRScanLexer]

  def parserClass: Class[_ <: AbstractRScanParser]

  def isTokenTypeWithText(tokenTypeName: String): Boolean

  def hasFoldedTokens(tokenType: String): Boolean
}

object ParserService {
  private[this] val _map: Map[LanguageRef, ParserService] =
    CollectionConverters.asScala(ServiceLoader.load[ParserService](classOf[ParserService], getClass.getClassLoader))
      .map(e => (e.language, e))
      .toMap

  def apply(language: LanguageRef): ParserService =
    _map.get(language) match {
      case Some(srv) => srv
      case None => throw new IllegalArgumentException("No Antlr4 service found for language: " + language.name)
    }
}
