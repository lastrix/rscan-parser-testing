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

package org.lastrix.rscan

import org.antlr.v4.runtime.{CharStream, CharStreams}
import org.apache.commons.io.input.BOMInputStream
import org.apache.commons.io.{ByteOrderMark, FileUtils, IOUtils}
import org.apache.commons.lang3.StringUtils
import org.jetbrains.annotations.{NotNull, Nullable}
import org.mozilla.universalchardet.UniversalDetector

import java.io.InputStream
import java.nio.charset.Charset
import java.util.regex.Pattern

package object vfs {
  val SEPARATOR_CHAR = '/'
  val SEPARATOR = "/"
  val MULTIPLE_BACKSLASH_REPLACE: Pattern = Pattern.compile("\\\\+")
  val MULTIPLE_SLASH_REPLACE: Pattern = Pattern.compile("/+")

  /**
   * BOM-aware stream creation
   *
   * @param stream the input stream
   * @return the input stream with BOM sequence removed if present
   */
  def asBOMLessStream(stream: InputStream) =
    new BOMInputStream(stream, ByteOrderMark.UTF_8, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_32BE, ByteOrderMark.UTF_32LE)

  /**
   * Convert arbitrary path to valid virtual one. Remove any excessive slashes on the way, blank input produces single slash output
   *
   * @param path the raw path
   * @return the virtual path
   */
  @NotNull
  def toVirtualPath(@Nullable path: String): String = {
    if (StringUtils.isBlank(path)) return SEPARATOR

    var result = MULTIPLE_BACKSLASH_REPLACE.matcher(path.replace('\\', SEPARATOR_CHAR)).replaceAll("/")
    result = MULTIPLE_SLASH_REPLACE.matcher(result).replaceAll("/")
    if (SEPARATOR == result || StringUtils.isBlank(result)) return SEPARATOR

    if (!result.isEmpty && result.charAt(0) == SEPARATOR_CHAR)
      result = result.substring(1)

    if (!result.isEmpty && result.charAt(result.length() - 1) == SEPARATOR_CHAR)
      result = result.substring(0, result.length() - 1)

    if (StringUtils.isBlank(path)) SEPARATOR else result
  }

  /**
   * Get CharStream from virtual file
   *
   * @param file the virtual file
   * @return the Antlr4 CharStream
   */
  def toCharStream(file: VirtualFile): CharStream = {
    val text = toPlainText(file)
    CharStreams.fromString(text, file.absoluteName)
  }

  /**
   * Load file content as plain text
   * (detects encoding, although you may set it thru setting 'classOf[Charset].getSimpleName'
   * VirtualFile#customData property)
   *
   * @param file the virtual file
   * @return file text content
   */
  def toPlainText(file: VirtualFile): String = {
    val charset = detectCharset(file)
    toPlainTextWithCharset(file, charset)
  }

  /**
   * Detect file charset, the value is cached inside customData property
   * of name 'classOf[Charset].getSimpleName'
   * UTF-8 encoding is enforced if nothing else detected
   *
   * @param file the virtual file
   * @return the detected charset
   */
  def detectCharset(file: VirtualFile): Charset = {
    file.getCustomData(classOf[Charset].getSimpleName) match {
      case Some(charsetName) => return Charset.forName(charsetName.asInstanceOf[String])
      case None =>
    }
    var is: InputStream = null
    try {
      is = file.source.inputStream()
      val bytes = IOUtils.toByteArray(is, Math.min(file.source.size, DEFAULT_BYTE_COUNT))
      val charset = detectCharset(bytes)
      file.customData(classOf[Charset].getSimpleName, charset.displayName)
      charset
    } finally if (is != null) is.close()
  }

  ////////////////////////////////////// Helpful utilities /////////////////////////////////////////////////////////////
  private val DEFAULT_BYTE_COUNT = 4096
  private val MAX_CHARSET_SIZE = 50 * FileUtils.ONE_KB.toInt

  private def toPlainTextWithCharset(file: VirtualFile, charset: Charset): String = {
    var is: InputStream = null
    try {
      is = asBOMLessStream(file.source.inputStream())
      IOUtils.toString(is, charset)
    }
    finally if (is != null) is.close()
  }

  private def detectCharset(buffer: Array[Byte]) = {
    val detector = new UniversalDetector(null)
    var charsetDetectionLength = buffer.length
    if (charsetDetectionLength > MAX_CHARSET_SIZE) charsetDetectionLength = MAX_CHARSET_SIZE
    detector.handleData(buffer, 0, charsetDetectionLength)
    detector.dataEnd()
    var encodingName = detector.getDetectedCharset
    if (encodingName == null) encodingName = "UTF-8"
    // ensure exist
    Charset.forName(encodingName)
  }

}
