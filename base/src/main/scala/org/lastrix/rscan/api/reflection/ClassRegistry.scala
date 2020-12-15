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

import io.github.classgraph.{ClassGraph, ClassInfo, ScanResult}
import org.lastrix.rscan.api.annotation.Reflected
import org.slf4j.{Logger, LoggerFactory}

import java.lang.annotation.{Annotation, ElementType, Target}
import java.lang.reflect.{Constructor, Field, Method}
import java.net.URL
import java.util.ServiceLoader
import scala.collection.mutable
import scala.jdk.javaapi.CollectionConverters

object ClassRegistry {

  private val located: Located = new Locator().locate()

  def classesBy(annotation: Class[_ <: Annotation]): Seq[Class[_]] =
    located.classes.getOrElse(annotation, Seq.empty)

  def ctorsBy(annotation: Class[_ <: Annotation], declareClass: Class[_] = null): Seq[Constructor[_]] =
    if (declareClass == null) located.ctors.getOrElse(annotation, Seq.empty)
    else located.ctors.getOrElse(annotation, Seq.empty)
      .filter(_.getDeclaringClass == declareClass)

  def methodsBy(annotation: Class[_ <: Annotation], declareClass: Class[_] = null): Seq[Method] =
    if (declareClass == null) located.methods.getOrElse(annotation, Seq.empty)
    else located.methods.getOrElse(annotation, Seq.empty)
      .filter(_.getDeclaringClass == declareClass)

  def methodsByGrouped(annotation: Class[_ <: Annotation]): Map[Class[_], Seq[Method]] = {
    val map = new mutable.HashMap[Class[_], Seq[Method]]()
    located.methods.getOrElse(annotation, Seq.empty)
      .foreach(m => map.put(m.getDeclaringClass, map.getOrElse(m.getDeclaringClass, Seq.empty) :+ m))
    map.toMap
  }

  def fieldsBy(annotation: Class[_ <: Annotation], declareClass: Class[_] = null): Seq[Field] =
    if (declareClass == null) located.fields.getOrElse(annotation, Seq.empty)
    else located.fields.getOrElse(annotation, Seq.empty)
      .filter(_.getDeclaringClass == declareClass)

  def fieldsByGrouped(annotation: Class[_ <: Annotation]): Map[Class[_], Seq[Field]] = {
    val map = new mutable.HashMap[Class[_], Seq[Field]]()
    located.fields.getOrElse(annotation, Seq.empty)
      .foreach(f => map.put(f.getDeclaringClass, map.getOrElse(f.getDeclaringClass, Seq.empty) :+ f))
    map.toMap
  }

  def implementorsOf(`class`: Class[_]): Seq[Class[_]] = located.implementors.getOrElse(`class`, Seq.empty)

  def resourcesByExt(extension: String): Seq[URL] = located.resourcesByExt.getOrElse(extension, Seq.empty)

  ///////////////////////////////// Registration implementation ////////////////////////////////////////////////////////
  private sealed class Located
  (
    val classes: Map[Class[_ <: Annotation], Seq[Class[_]]],
    val ctors: Map[Class[_ <: Annotation], Seq[Constructor[_]]],
    val methods: Map[Class[_ <: Annotation], Seq[Method]],
    val fields: Map[Class[_ <: Annotation], Seq[Field]],
    val implementors: Map[Class[_], Seq[Class[_]]],
    val resourcesByExt: Map[String, Seq[URL]]
  )

  private trait ReadableConfiguration extends Configuration {
    def markerAnnotations: Seq[Class[_ <: Annotation]]

    def resourceExtensions: Set[String]

    def resourceBasePaths: Set[String]

    def classAnnotations: Set[Class[_ <: Annotation]]

    def methodAnnotations: Set[Class[_ <: Annotation]]

    def fieldAnnotations: Set[Class[_ <: Annotation]]

    def implementorsOf: Set[Class[_]]

    def classLoaders: Seq[ClassLoader]
  }

  private def isVerbose: Boolean =
    java.lang.Boolean.parseBoolean(System.getProperty("rscan.class.registry.verbose", "false"))

  private sealed class Locator extends ReadableConfiguration {
    private val graph: ClassGraph = new ClassGraph
    private var _classLoaders = List.empty[ClassLoader]
    private var _markerAnnotations = List.empty[Class[_ <: Annotation]]
    private var _resourceExtensions = Set.empty[String]
    private var _resourceBasePaths = Set.empty[String]
    private var _allowedNs = Set.empty[String]
    private var _blacklistedNs = Set.empty[String]
    private var _classAnnotations = Set.empty[Class[_ <: Annotation]]
    private var _methodAnnotations = Set.empty[Class[_ <: Annotation]]
    private var _fieldAnnotations = Set.empty[Class[_ <: Annotation]]
    private var _implementorsOf = Set.empty[Class[_]]

    // default settings
    _markerAnnotations :+= classOf[Reflected]

    // configure class graph
    graph.enableAllInfo.removeTemporaryFilesAfterScan()
    if (isVerbose) graph.verbose()

    ServiceLoader.load(classOf[Configurator], this.getClass.getClassLoader)
      .forEach(_.configure(this))

    for (cl <- _classLoaders) graph.addClassLoader(cl)
    classLoader(ClassRegistry.getClass.getClassLoader)
    if (!_classLoaders.contains(Thread.currentThread().getContextClassLoader))
      classLoader(Thread.currentThread().getContextClassLoader)
    if (!_classLoaders.contains(ClassLoader.getSystemClassLoader))
      classLoader(ClassLoader.getSystemClassLoader)

    for (aNs <- _allowedNs) graph.whitelistPackages(aNs)
    for (bNs <- _blacklistedNs) graph.blacklistPackages(bNs)
    for (rBp <- resourceBasePaths) graph.whitelistPaths(rBp)

    override def classLoader(classLoader: ClassLoader): Configuration = {
      _classLoaders :+= classLoader
      this
    }

    override def marker(annotation: Class[_ <: Annotation]): Configuration = {
      _markerAnnotations :+= annotation
      this
    }

    override def resourceExtension(extension: String): Configuration = {
      _resourceExtensions += extension
      this
    }

    override def resourceBasePath(basePath: String): Configuration = {
      _resourceBasePaths += basePath
      this
    }

    override def allowNs(ns: String): Configuration = {
      _allowedNs += ns
      this
    }

    override def blacklistNs(ns: String): Configuration = {
      _blacklistedNs += ns
      this
    }

    override def classAnnotation(annotation: Class[_ <: Annotation]): Configuration = {
      _classAnnotations += annotation
      this
    }

    override def methodAnnotation(annotation: Class[_ <: Annotation]): Configuration = {
      _methodAnnotations += annotation
      this
    }

    override def fieldAnnotation(annotation: Class[_ <: Annotation]): Configuration = {
      _fieldAnnotations += annotation
      this
    }

    override def implementorsOf(`class`: Class[_]): Configuration = {
      _implementorsOf += `class`
      this
    }

    override def classLoaders: Seq[ClassLoader] = _classLoaders

    override def markerAnnotations: Seq[Class[_ <: Annotation]] = _markerAnnotations

    override def resourceExtensions: Set[String] = _resourceExtensions

    override def resourceBasePaths: Set[String] = _resourceBasePaths

    override def classAnnotations: Set[Class[_ <: Annotation]] = _classAnnotations

    override def methodAnnotations: Set[Class[_ <: Annotation]] = _methodAnnotations

    override def fieldAnnotations: Set[Class[_ <: Annotation]] = _fieldAnnotations

    override def implementorsOf: Set[Class[_]] = _implementorsOf

    def locate(): Located = {
      var scan: ScanResult = null
      try {
        scan = graph.scan()
        new GraphParser(scan, this).parse()
      } finally {
        if (scan != null) scan.close()
      }
    }
  }

  private sealed class GraphParser(val scanResult: ScanResult, val cfg: ReadableConfiguration) {
    private val classes = mutable.Map[Class[_ <: Annotation], Seq[Class[_]]]()
    private val ctors = mutable.Map[Class[_ <: Annotation], Seq[Constructor[_]]]()
    private val methods = mutable.Map[Class[_ <: Annotation], Seq[Method]]()
    private val fields = mutable.Map[Class[_ <: Annotation], Seq[Field]]()
    private val implementors = mutable.Map[Class[_], Seq[Class[_]]]()
    private val resourcesByExt = mutable.Map[String, Seq[URL]]()
    private val classResolveMap = new mutable.HashMap[String, Class[_]]()

    def parse(): Located = {
      scanResult.getAllAnnotations.forEach(checkMarker)
      scanAnnotatedClasses()
      scanAnnotatedMethods()
      scanAnnotatedFields()
      scanImplementors()
      new Located(
        classes.toMap,
        ctors.toMap,
        methods.toMap,
        fields.toMap,
        implementors.toMap,
        resourcesByExt.toMap)
    }

    private def scanAnnotatedClasses(): Unit =
      for (annotation <- cfg.classAnnotations)
        CollectionConverters.asScala(scanResult.getClassesWithAnnotation(annotation.getTypeName))
          .map(findClass)
          .map {
            case Some(x) => classes.put(annotation, classes.getOrElse(annotation, Seq.empty) :+ x)
            case None =>
          }

    private def scanAnnotatedMethods(): Unit =
      for (annotation <- cfg.methodAnnotations)
        CollectionConverters.asScala(scanResult.getClassesWithMethodAnnotation(annotation.getTypeName))
          .map(findClass)
          .map {
            case Some(x) => scanAnnotatedClassMethods(annotation, x)
            case None =>
          }

    private def scanAnnotatedFields(): Unit =
      for (annotation <- cfg.fieldAnnotations)
        CollectionConverters.asScala(scanResult.getClassesWithFieldAnnotation(annotation.getTypeName))
          .map(findClass)
          .map {
            case Some(x) => scanAnnotatedClassFields(annotation, x)
            case None =>
          }

    private def scanAnnotatedClassMethods(annotation: Class[_ <: Annotation], targetClass: Class[_]): Unit = {
      targetClass.getDeclaredConstructors
        .filter(_.getAnnotation(annotation) != null)
        .foreach(c => ctors.put(annotation, ctors.getOrElse(annotation, Seq.empty) :+ c))
      targetClass.getDeclaredMethods
        .filter(_.getAnnotation(annotation) != null)
        .foreach(m => methods.put(annotation, methods.getOrElse(annotation, Seq.empty) :+ m))
    }

    private def scanAnnotatedClassFields(annotation: Class[_ <: Annotation], targetClass: Class[_]): Unit =
      targetClass.getDeclaredFields
        .filter(_.getAnnotation(annotation) != null)
        .foreach(f => fields.put(annotation, fields.getOrElse(annotation, Seq.empty) :+ f))

    private def scanImplementors(): Unit = for (item <- cfg.implementorsOf) {
      val seq = CollectionConverters.asScala(scanResult.getClassesImplementing(item.getTypeName))
        .flatMap(findClass)
        .toSeq
      implementors.put(item, seq)
    }

    private def checkMarker(info: ClassInfo): Unit = if (hasAnyAnnotation(cfg.markerAnnotations, info))
      findClass(info) match {
        case Some(aClass) =>
          val target: Target = aClass.getAnnotation(classOf[Target])
          if (target == null) log.warn("No target on marked annotation: ", info.getName)
          else for (v <- target.value()) registerByElementType(aClass.asInstanceOf[Class[_ <: Annotation]], v)
        case None =>
      }

    private def registerByElementType(aClass: Class[_ <: Annotation], elementType: ElementType): Unit = elementType match {
      case ElementType.TYPE => cfg.classAnnotation(aClass)
      case ElementType.FIELD => cfg.fieldAnnotation(aClass)
      case ElementType.METHOD | ElementType.CONSTRUCTOR => cfg.methodAnnotation(aClass)
      case ElementType.PARAMETER => throw new UnsupportedOperationException
      case x => log.error(s"Reflected annotation targets invalid element ${aClass.getTypeName}, $x")
    }

    private def hasAnyAnnotation(annotations: Seq[Class[_ <: Annotation]], info: ClassInfo): Boolean =
      CollectionConverters.asScala(info.getAnnotations)
        .exists(a => annotations.exists(_.getTypeName == a.getName))

    private def findClass(classInfo: ClassInfo): Option[Class[_]] = classResolveMap.get(classInfo.getName) match {
      case Some(x) => Some(x)
      case None => resolveClass(classInfo.getName) match {
        case Some(x) =>
          classResolveMap.put(classInfo.getName, x)
          Some(x)
        case None =>
          log.warn("Unable to resolve class by name: {}", classInfo.getName)
          None
      }
    }

    private def resolveClass(className: String): Option[Class[_]] = {
      for (classLoader <- cfg.classLoaders) {
        try {
          return Some(classLoader.loadClass(className))
        } catch {
          case _: ClassNotFoundException =>
        }
      }
      None
    }
  }

  val log: Logger = LoggerFactory.getLogger(getClass)
}
