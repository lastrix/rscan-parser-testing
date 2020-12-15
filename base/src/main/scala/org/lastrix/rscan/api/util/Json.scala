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

package org.lastrix.rscan.api.util

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, ScalaObjectMapper}

import java.time.Instant
import java.time.format.DateTimeFormatterBuilder

object Json {
  private val Mapper = new ObjectMapper() with ScalaObjectMapper
  Mapper.registerModule(DefaultScalaModule)
    .setSerializationInclusion(Include.NON_NULL)
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .registerModule(new JavaTimeModule().addSerializer(classOf[Instant], new InstantSerializerImpl))

  def apply(obj: AnyRef, pretty: Boolean = true): String =
    if (pretty) Mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj)
    else Mapper.writeValueAsString(obj)

  def apply[T](json: String)(implicit m: Manifest[T]): T = Mapper.readValue[T](json)

  private class InstantSerializerImpl
    extends InstantSerializer(
      InstantSerializer.INSTANCE,
      false,
      new DateTimeFormatterBuilder().appendInstant(3).toFormatter
    )

}
