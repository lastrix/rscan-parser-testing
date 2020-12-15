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

package org.lastrix.rscan.api.reflection

import java.lang.annotation.Annotation

trait Configuration {
  def classLoader(classLoader: ClassLoader): Configuration

  def marker(annotation: Class[_ <: Annotation]): Configuration

  def resourceExtension(extension: String): Configuration

  def resourceBasePath(basePath: String): Configuration

  def allowNs(ns: String): Configuration

  def blacklistNs(ns: String): Configuration

  def classAnnotation(annotation: Class[_ <: Annotation]): Configuration

  def methodAnnotation(annotation: Class[_ <: Annotation]): Configuration

  def fieldAnnotation(annotation: Class[_ <: Annotation]): Configuration

  def implementorsOf(`class`: Class[_]): Configuration
}
