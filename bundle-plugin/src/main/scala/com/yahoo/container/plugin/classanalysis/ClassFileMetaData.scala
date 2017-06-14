// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.container.plugin.classanalysis

/**
 * The result of analyzing a .class file.
 * @author  tonytv
 */
sealed case class ClassFileMetaData(name:String,
                                    referencedClasses : Set[String],
                                    exportPackage : Option[ExportPackageAnnotation])
