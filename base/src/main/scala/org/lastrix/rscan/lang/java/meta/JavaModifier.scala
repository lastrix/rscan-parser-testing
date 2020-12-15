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

package org.lastrix.rscan.lang.java.meta

import org.lastrix.rscan.model.{RModifier, RSpecModifier, RTextModifier}

object JavaModifier {
  val Static: RModifier = new RTextModifier("static")
  val Transitive: RModifier = new RTextModifier("transitive")
  val Public: RModifier = new RTextModifier("public")
  val Protected: RModifier = new RTextModifier("protected")
  val Private: RModifier = new RTextModifier("private")
  val Abstract: RModifier = new RTextModifier("abstract")
  val Final: RModifier = new RTextModifier("final")
  val Strictfp: RModifier = new RTextModifier("strictfp")
  val Transient: RModifier = new RTextModifier("transient")
  val Volatile: RModifier = new RTextModifier("volatile")
  val Synchronized: RModifier = new RTextModifier("synchronized")
  val Native: RModifier = new RTextModifier("native")
  val Default: RModifier = new RTextModifier("default")


  val Wildcard: RModifier = new RSpecModifier("#wildcard")
  val VarArgs: RModifier = new RSpecModifier("#varargs")
  val VarType: RModifier = new RSpecModifier("#vartype")
  val Open: RModifier = new RSpecModifier("#open")

}
