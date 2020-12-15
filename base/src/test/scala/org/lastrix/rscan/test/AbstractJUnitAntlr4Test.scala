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

import org.antlr.v4.runtime.{CommonTokenStream, Token}
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.{assertEquals, assertFalse, assertTrue}
import org.lastrix.rscan.api.parser.antlr4.lexer.AbstractRScanLexer
import org.lastrix.rscan.api.parser.{ParserService, antlr4}
import org.lastrix.rscan.api.util.Json
import org.lastrix.rscan.api.util.PathUtil.assertFile
import org.lastrix.rscan.model.operation.std.RLangOp
import org.lastrix.rscan.model.operation.{ROp, ROpKey}
import org.lastrix.rscan.model.tokens.RScanToken
import org.lastrix.rscan.test.Antlr4TestUtils._
import org.lastrix.rscan.vfs.VirtualFile
import org.lastrix.rscan.vfs.source.FileSource

import java.io.File
import java.nio.charset.StandardCharsets
import scala.collection.mutable
import scala.jdk.javaapi.CollectionConverters

abstract class AbstractJUnitAntlr4Test {

  import AbstractJUnitAntlr4Test._

  def testLexer(suite: Suite): Unit = {
    implicit val ps: ParserService = ParserService(suite.language)
    for (fi <- suite.fileInfos) {
      assertFile(fi.file)
      val lexerOutput: LexerOutput = readFromFile[LexerOutput](testConfig(LexerSuiteDir, suite, fi.file))
      val lexer = ps.newLexer(asVirtualFile(fi))
      val stream = new CommonTokenStream(lexer)
      stream.fill()
      val tokens = CollectionConverters.asScala(stream.getTokens)
      assertTokens(lexer, lexerOutput, tokens)
    }
  }

  def testParser(suite: Suite): Unit = {
    implicit val ps: ParserService = ParserService(suite.language)
    for (fi <- suite.fileInfos) {
      assertFile(fi.file)
      val parserOutput: ParserOutput = readFromFile[ParserOutput](testConfig(ParserSuiteDir, suite, fi.file))
      val op = antlr4.parseFile(asVirtualFile(fi))
      assertTrue(op.isInstanceOf[RLangOp], "Must be RLangOp")
      assertEquals(parserOutput.languageName, op.asInstanceOf[RLangOp].language.name, "Languages are not equal")
      assertOperations(op.key, op.children, parserOutput.items)
    }
  }

  private def asVirtualFile(info: Antlr4TestUtils.FileInfo) =
    new VirtualFile(info.file.getName, new FileSource(info.file, info.file.getName))

  private def assertTokens(lexer: AbstractRScanLexer, lexerOutput: LexerOutput, tokens: mutable.Buffer[Token]): Unit = {
    val queue = new mutable.Queue[LexerOutputToken]()
    queue ++= lexerOutput.tokens
    for (t <- tokens) {
      assertTrue(queue.nonEmpty, "Queue should not be empty")
      val lot = queue.dequeue()
      val tokenType = lexer.getVocabulary.getSymbolicName(t.getType)
      assertEquals(lot.tokenType, tokenType, "Token types are not equal")
      assertEquals(lot.statement, tokenStatement(t), "Token types are not equal")
      if (lot.text != null) {
        assertEquals(lot.text, t.getText, "Texts are not equal")
      }
      if (lot.children != null) {
        assertTrue(t.isInstanceOf[RScanToken], "Must be RScanToken")
        val rt = t.asInstanceOf[RScanToken]
        assertTrue(rt.foldedTokens().nonEmpty, "Must have fold tokens")
        assertTokens(lexer, lot.children, rt.foldedTokens().toBuffer)
      } else {
        assertFalse(t.isInstanceOf[RScanToken] && t.asInstanceOf[RScanToken].foldedTokens().nonEmpty, "No fold tokens allowed")
      }
    }
    if (queue.nonEmpty && queue.head.tokenType.equals("EOF")) queue.dequeue()
    assertTrue(queue.isEmpty, "Queue must be empty")
  }

  private def assertOperations(ownerKey: ROpKey, children: Seq[ROp], items: Seq[ParserOutputItem]): Unit = {
    if (children.isEmpty) {
      assertTrue(items == null || items.isEmpty, s"No items in operation: $ownerKey")
      return
    } else if (items == null) {
      Assertions.fail(s"Missing child operations: $ownerKey")
    }
    val queue = new mutable.Queue[ParserOutputItem]()
    queue ++= items
    for (child <- children) {
      assertTrue(queue.nonEmpty, s"Must not be empty at: ${child.key}")
      val poi = queue.dequeue()
      assertEquals(poi.keyText, child.key.toString, "Key text is not equal")
      assertEquals(poi.specText, child.specText, "SpecText is not equal")
      assertOperations(child.key, child.children, poi.children)
    }
    assertTrue(queue.isEmpty, s"Queue must be empty for: $ownerKey")
  }
}

object AbstractJUnitAntlr4Test {
  def readFromFile[T](file: File)(implicit m: Manifest[T]): T = {
    assertFile(file)
    Json[T](FileUtils.readFileToString(file, StandardCharsets.UTF_8))
  }
}
