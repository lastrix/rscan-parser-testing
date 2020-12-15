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

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.{Arguments, MethodSource}
import org.lastrix.rscan.lang.java.meta.JavaLanguage
import org.lastrix.rscan.test.Antlr4TestUtils.Suite
import org.lastrix.rscan.test.{AbstractJUnitAntlr4Test, Antlr4TestUtils}


class JavaParserTest extends AbstractJUnitAntlr4Test {

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource(Array("testFiles"))
  override def testLexer(suite: Suite): Unit = {
    super.testLexer(suite)
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource(Array("testFiles"))
  override def testParser(suite: Suite): Unit = {
    super.testParser(suite)
  }
}


object JavaParserTest {
  private val TestDataDirectory = "/test/java/unit"

  def testFiles: java.util.stream.Stream[Arguments] = Antlr4TestUtils.filesFromResourceDirectory(JavaLanguage.ref, TestDataDirectory, getClass, Seq("java"))
}


