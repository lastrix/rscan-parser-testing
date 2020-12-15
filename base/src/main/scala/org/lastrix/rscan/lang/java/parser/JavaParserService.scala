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

package org.lastrix.rscan.lang.java.parser

import org.antlr.v4.runtime.atn.ATN
import org.antlr.v4.runtime.dfa.DFA
import org.antlr.v4.runtime.{CharStream, TokenStream}
import org.lastrix.rscan.api.parser.AbstractParserService
import org.lastrix.rscan.api.parser.antlr4.lexer.AbstractRScanLexer
import org.lastrix.rscan.api.parser.antlr4.parser.AbstractRScanParser
import org.lastrix.rscan.lang.LanguageRef
import org.lastrix.rscan.lang.java.meta.JavaLanguage
import org.lastrix.rscan.vfs.VirtualFile

sealed class JavaParserService extends AbstractParserService {
  override val language: LanguageRef = JavaLanguage.ref

  override def newLexer(stream: CharStream, file: VirtualFile, atn: ATN, decisionToDFA: Array[DFA]): AbstractRScanLexer = new JavaLexer(stream, file, atn, decisionToDFA)

  override def newParser(file: VirtualFile, atn: ATN, decisionToDFA: Array[DFA], stream: TokenStream): AbstractRScanParser = new JavaParser(stream, file, atn, decisionToDFA)

  override def lexerClass: Class[_ <: AbstractRScanLexer] = classOf[JavaLexer]

  override def parserClass: Class[_ <: AbstractRScanParser] = classOf[JavaParser]

  override def isTokenTypeWithText(tokenTypeName: String): Boolean =
    "Identifier".equals(tokenTypeName) ||
      tokenTypeName.endsWith("Literal") ||
      tokenTypeName.startsWith("TemplateLiteral")

  override def hasFoldedTokens(tokenType: String): Boolean = tokenType match {
    case "FoldBlock" | "TemplateLiteral" => true
    case _ => false
  }
}
