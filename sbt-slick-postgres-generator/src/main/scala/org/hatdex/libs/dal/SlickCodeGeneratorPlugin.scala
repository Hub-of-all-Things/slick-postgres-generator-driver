/*
 * Copyright (C) $year HAT Data Exchange Ltd - All Rights Reserved
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Written by Andrius Aucinas <andrius.aucinas@hatdex.org>, 14/08/17 09:17
 */

package org.hatdex.libs.dal

import java.io.File

import com.typesafe.config.ConfigFactory
import sbt.Keys._
import sbt._

import scala.concurrent.Await
import scala.concurrent.duration._
import com.typesafe.config.Config

object SlickCodeGeneratorPlugin extends AutoPlugin {
  // This plugin is automatically enabled for projects
  //  override def trigger = allRequirements

  // by defining autoImport, the settings are automatically imported into user's `*.sbt`
  object autoImport {
    // configuration points, like the built-in `version`, `libraryDependencies`, or `compile`
    val gentables = taskKey[Seq[File]]("Generates tables.")

    val codegenOutputDir = settingKey[String]("Directory to output the generated DAL file")
    val codegenPackageName = settingKey[String]("Package for the generated DAL file")
    val codegenClassName = settingKey[String]("Class name for the generated DAL file")
    val codegenExcludedTables = settingKey[Seq[String]]("List of tables excluded from generating code for")
    val codegenDatabase = settingKey[String]("Live database from which structures are retrieved")

    // default values for the tasks and settings
    lazy val codegenSettings: Seq[Def.Setting[_]] = Seq(
      gentables := {
        Generator(
          ConfigFactory.load(ConfigFactory.parseFile((resourceDirectory in Compile).value / "application.conf")), // puts reference.conf underneath,
          (codegenOutputDir in gentables).value,
          (codegenPackageName in gentables).value,
          (codegenClassName in gentables).value,
          (codegenDatabase in gentables).value,
          (codegenExcludedTables in gentables).value)
      },
      //      codegenOutputDir in gentables := (baseDirectory.value / "project").getPath,
      //      codegenPackageName in gentables := "dal",
      codegenClassName in gentables := "Tables",
      codegenDatabase in gentables := "devdb",
      codegenExcludedTables in gentables := Seq("databasechangelog", "databasechangeloglock"))
  }

  import autoImport._

  // a group of settings that are automatically added to projects.
  override lazy val projectSettings = inConfig(Compile)(codegenSettings)

  object Generator {
    def apply(config: Config, outputDir: String, packageName: String, className: String,
      database: String, excludedTables: Seq[String]): Seq[File] = {

      val eventuallyGenerated = new DatabaseCodeGenerator(config)
        .generate(outputDir, packageName, className, database, excludedTables)
      Await.result(eventuallyGenerated, 5.minutes)
      val fname = outputDir + "/" + packageName.replace('.', '/') + "/Tables.scala"
      Seq(file(fname))
    }
  }
}