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

package org.lastrix.rscan.vfs.source

import org.slf4j.{Logger, LoggerFactory}

import java.io.{IOException, InputStream, InputStreamReader, LineNumberReader}

/**
 * Interface for every source, provides raw data for anyone
 */
trait Source {

  import Source._

  def sourceName: String

  /**
   * Return origin of this source,
   * The name must be unique for each source.
   *
   * @return String
   */
  def origin: String

  /**
   * Return size stored data or -1 if not available
   *
   * @return Long
   */
  def size: Long

  /**
   * Return line count for this file
   *
   * @return Long
   */
  def getLineCount: Long = {
    var lnr: LineNumberReader = null
    try {
      lnr = new LineNumberReader(new InputStreamReader(inputStream()))
      lnr.skip(Long.MaxValue)
      return lnr.getLineNumber + 1L
    } catch {
      case e: IOException => log.warn(s"Unable to evaluate amount of lines for file: $sourceName", e)
    } finally if (lnr != null) lnr.close()
    0
  }

  /**
   * Open non-buffered input stream, do not apply any of transformation or stream truncation,
   * This stream should return data as is.
   *
   * @return InputStream
   */
  def inputStream(): InputStream
}

object Source {
  private val log: Logger = LoggerFactory.getLogger(getClass)
}
