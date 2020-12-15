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

import org.antlr.v4.runtime.atn.{ATN, ATNDeserializer}
import org.antlr.v4.runtime.dfa.DFA
import org.slf4j.LoggerFactory

object ATNCache {
  private val log = LoggerFactory.getLogger("api.parser.antlr4.ATNCache")
  private val MaxTokenCount = 8192 * 2 // 16k tokens must be enough
  private var cache: Map[Class[_], Item] = Map.empty

  def forClass(clazz: Class[_], inputTokenCount: Int): Cached = {
    if (inputTokenCount <= 0) {
      throw new IllegalArgumentException("Argument 'inputTokenCount' must be greater than zero")
    }
    this.synchronized {
      cache.get(clazz) match {
        case Some(info) => updateOrCreateNew(clazz, inputTokenCount, info)
        case None => createNewInfo(clazz, inputTokenCount)
      }
    }
  }

  private def updateOrCreateNew(clazz: Class[_], inputTokenCount: Int, info: Item): Cached = {
    if (info.tokenCount + inputTokenCount > MaxTokenCount) {
      createNewInfo(clazz, inputTokenCount)
    } else {
      info.tokenCount += inputTokenCount
      log.trace("Token count for {} = {}", clazz.getTypeName, info.tokenCount)
      Cached(info.atn, info.dfa)
    }
  }

  private def createNewInfo(clazz: Class[_], inputTokenCount: Int): Cached = {
    val atn = atnOf(clazz)
    val atnInfo = Item(atn, toDfa(atn), inputTokenCount)
    val entry = (clazz, atnInfo)
    cache = cache + entry
    Cached(atnInfo.atn, atnInfo.dfa)
  }

  def atnOf(clazz: Class[_]): ATN =
    new ATNDeserializer().deserialize(clazz.getDeclaredField("_serializedATN").get(null).toString.toCharArray)

  def toDfa(atn: ATN): Array[DFA] = {
    val _decisionToDFA = new Array[DFA](atn.getNumberOfDecisions)
    for (i <- 0 until atn.getNumberOfDecisions) {
      _decisionToDFA(i) = new DFA(atn.getDecisionState(i), i)
    }
    _decisionToDFA
  }

}

sealed case class Item(atn: ATN, dfa: Array[DFA], var tokenCount: Int)

sealed case class Cached(atn: ATN, dfa: Array[DFA]);
