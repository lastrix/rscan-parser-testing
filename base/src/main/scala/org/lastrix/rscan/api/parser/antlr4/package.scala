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

import org.antlr.v4.runtime._
import org.antlr.v4.runtime.atn.PredictionMode
import org.lastrix.rscan.api.parser.antlr4.parser.{AbstractRScanParser, FoldBlockSupport}
import org.lastrix.rscan.api.util.PathUtil
import org.lastrix.rscan.model.operation._
import org.lastrix.rscan.model.operation.std._
import org.lastrix.rscan.vfs.VirtualFile
import org.slf4j.LoggerFactory

import java.io.File
import java.time.{Duration, Instant}
import java.util
import java.util.concurrent.{ForkJoinPool, RecursiveTask}
import scala.jdk.javaapi.CollectionConverters

package object antlr4 {
  private val log = LoggerFactory.getLogger("api.parser.antlr4")
  private val enableParallel = java.lang.Boolean.parseBoolean(System.getProperty("rscan.parser.parallel", "false"))
  private val enableProfiling = java.lang.Boolean.parseBoolean(System.getProperty("rscan.parser.profiling", "false"))
  private val cacheDir = {
    val dirName = System.getProperty("rscan.parser.caching.folder")
    if (dirName == null) None
    else {
      val dir = new File(dirName)
      PathUtil.ensureDirectory(dir)
      Some(dir)
    }
  }
  private val parserPool = ForkJoinPool.commonPool()

  def asTokenStream(tokens: List[Token]): TokenStream = {
    val s = new CommonTokenStream(new ListTokenSource(asArrayList(tokens)))
    s.fill()
    s
  }

  def asArrayList(tokens: List[Token]): util.ArrayList[Token] =
    new util.ArrayList[Token](CollectionConverters.asJava(tokens))

  /**
   * Parse file using supplied ParserService, no language recognition or any other
   * analysis is performed.
   *
   * @param file    the input file to read source code from
   * @param config  the parser configuration
   * @param service the parser service for lexer and parser creation
   * @return RLangOperation - the root of parsed file operation tree
   */
  def parseFile(file: VirtualFile, config: Antlr4Config = Antlr4Config())(implicit service: ParserService): RLangOp =
    parseText(file, None, config)

  /**
   * Parse file part represented as optional text parameter. If no text supplied,
   * then file reading occur.
   *
   * @param file    the input file to read source code from if text is None
   * @param text    the text to parse
   * @param config  the parser configuration, it's recommended to set offsets if text is not None
   * @param service the parser service for lexer and parser creation
   * @return RLangOperation - the root of parsed source code
   */
  def parseText(file: VirtualFile, text: Option[String], config: Antlr4Config = Antlr4Config())(implicit service: ParserService): RLangOp = {
    cacheDir match {
      case Some(dir) => parseTextNoCaching(file, text, config) // NOPE :D
      case None => parseTextNoCaching(file, text, config)
    }
  }

  private def targetFileFor(file: VirtualFile, dir: File): File = {
    val f = new File(dir, file.absoluteName)
    PathUtil.ensureDirectory(f.getParentFile)
    f
  }

  private def parseTextNoCaching(file: VirtualFile, text: Option[String], config: Antlr4Config = Antlr4Config())(implicit service: ParserService): RLangOp = {
    val start = Instant.now()
    val parser =
      if (enableParallel) new ParallelParser(service, file, config, text)
      else new SequentialParser(service, file, config, text)
    val root = parser.parse()
    val elapsed = Duration.between(start, Instant.now())
    val msg = config.additionalSuccessMsg match {
      case Some(x) => x
      case None => ""
    }
    log.trace(s"Source from ${file.virtualPath} parsed successfully in ${elapsed.toMillis} ms$msg")
    root
  }

  /**
   * Abstract for parsing source code.
   * The main goal of whole process is to split source code via FoldBlock tokens
   * into chunks, thus allowing to narrow lookahead of parser significantly.
   *
   * Two approaches to handle children jobs available via SequentialParser and ParallelParser,
   * the main difference between them: first one enqueues jobs into single queue and then pulls
   * till queue is empty, the other one overrides #enqueue method to call #parseItem in parallel mode
   */
  private trait Parser {
    def parserService: ParserService

    def config: Antlr4Config

    def file: VirtualFile

    def text: Option[String] = None

    def parse(): RLangOp

    protected final def parseInitial(): ParsedResult = {
      // caching this won't give any significant performance increase
      val lexer = parserService.newLexer(file, LexerConfig(text, config.offsetLine, config.offsetLinePosition))
      lexer.removeErrorListeners()
      lexer.addErrorListener(new RScanErrorListener(file))
      val stream = new CommonTokenStream(lexer)
      stream.fill()
      config.onLexerComplete(lexer)
      val parserCallback: AbstractRScanParser => ROp = config.initialRule match {
        case Some(rule) => _.invokeRule[ROp](rule)
        case None => _.invokeDefaultRule[RLangOp]
      }
      parseTokens(stream, parserCallback)
    }

    protected final def parseOperation(op: ROp): ParsedResult = op match {
      case x: RFoldOp =>
        if (x.parent == null) {
          log.trace("Unable to process parentless fold block: " + x.key)
          ParsedResultEmpty
        }
        else parseTokens(asTokenStream(x.token.foldedTokens()), p => {
          p.options(x.token.options)
          val o = p.invokeRuleFor(x)
          x.parent.synchronized {
            x.parent.replaceChild(x, o)
          }
          o
        })
      case _ => throw new IllegalArgumentException("Only RFoldOp permitted")
    }

    private def parseTokens(tokenStream: TokenStream, parse: AbstractRScanParser => ROp): ParsedResult = {
      val parser = parserService.newParser(file, tokenStream)
      parser.setBuildParseTree(false)
      parser.getInterpreter.setPredictionMode(PredictionMode.SLL)
      parser.removeErrorListeners()
      parser.addErrorListener(new RScanErrorListener(file, tokenStream))
      parser.setProfile(enableProfiling)
      val root = parse(parser)
      if (enableProfiling) printProfileInfo(parser, tokenStream)
      ParsedResult(root, foldOpsFrom(parser))
    }

    private def foldOpsFrom(parser: AbstractRScanParser): List[RFoldOp] = parser match {
      case x: FoldBlockSupport => x.folds
      case _ => List.empty
    }

    ////////////////////// Profiling support /////////////////////////////////////////////////////////////////////////////
    // TODO: refactoring required, make messages more versatile
    private def printProfileInfo(parser: AbstractRScanParser, tokenStream: TokenStream): Unit = {
      // do the actual parsing
      val parseInfo = parser.getParseInfo
      val atn = parser.getATN
      for (di <- parseInfo.getDecisionInfo if di.ambiguities.size() > 0) {
        val ds = atn.decisionToState.get(di.decision)
        val ruleName = parser.ruleName(ds.ruleIndex)
        log.debug("Ambiguity in rule '" + ruleName + "' -> {}", di)
        log.debug("=========================")
        log.debug(tokenStream.getText)
        log.debug("=========================")
      }
    }
  }

  sealed case class ParsedResult(op: ROp, children: List[RFoldOp])

  private val ParsedResultEmpty: ParsedResult = ParsedResult(null, List.empty)

  private sealed class SequentialParser
  (override val parserService: ParserService,
   override val file: VirtualFile,
   override val config: Antlr4Config,
   override val text: Option[String] = None)
    extends Parser {

    override def parse(): RLangOp = {
      val result = parseInitial()
      var jobs: List[RFoldOp] = result.children
      while (jobs.nonEmpty) {
        jobs = jobs.tail ++ parseOperation(jobs.head).children
      }
      result.op.asInstanceOf[RLangOp]
    }
  }

  private sealed class ParallelParser
  (override val parserService: ParserService,
   override val file: VirtualFile,
   override val config: Antlr4Config,
   override val text: Option[String] = None)
    extends Parser {

    private def supplyResult(op: Option[ROp]): ParsedResult = op match {
      case Some(o) => parseOperation(o)
      case None => parseInitial()
    }

    def parse(): RLangOp = {
      val task = new MapReduceParserTask(supplyResult)
      parserPool.execute(task)
      task.join().asInstanceOf[RLangOp]
    }
  }

  sealed class MapReduceParserTask
  (
    val supplier: Option[ROp] => ParsedResult,
    val op: Option[ROp] = None
  )
    extends RecursiveTask[ROp] {

    override def compute(): ROp = {
      val result = supplier.apply(op)
      val tasks = for (op <- result.children) yield taskFor(op)
      for (task <- tasks) task.join()
      result.op
    }

    private def taskFor(op: RFoldOp): MapReduceParserTask = {
      val task = new MapReduceParserTask(supplier, Some(op))
      task.fork()
      task
    }
  }

}
