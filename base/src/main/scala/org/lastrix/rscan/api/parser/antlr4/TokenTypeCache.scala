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

import org.antlr.v4.runtime.{Token, Vocabulary}

object TokenTypeCache {
  private var tokenTypeMap = Map.empty[Vocabulary, Map[String, Int]]

  def clear(): Unit = this.synchronized {
    tokenTypeMap = Map.empty
  }

  def tokenTypeMapForVocabulary(vocabulary: Vocabulary): Map[String, Int] = {
    this.synchronized {
      tokenTypeMap.get(vocabulary) match {
        case Some(map) => map
        case None =>
          val im = buildTokenTypeMap(vocabulary)
          tokenTypeMap += (vocabulary -> im)
          im
      }
    }
  }

  private def buildTokenTypeMap(vocabulary: Vocabulary): Map[String, Int] = {
    var m = Map.empty[String, Int]
    for (i <- 0 until vocabulary.getMaxTokenType) {
      val name = vocabulary.getLiteralName(i)
      if (name != null) m += (name -> i)

      val sName = vocabulary.getSymbolicName(i)
      if (sName != null) m += (sName -> i)
    }
    m += ("EOF" -> Token.EOF)
    m
  }

}
