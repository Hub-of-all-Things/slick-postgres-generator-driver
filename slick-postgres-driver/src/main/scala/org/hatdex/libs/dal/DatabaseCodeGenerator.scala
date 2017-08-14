/*
 * Copyright (C) $year HAT Data Exchange Ltd - All Rights Reserved
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Written by Andrius Aucinas <andrius.aucinas@hatdex.org>, 14/08/17 09:17
 */

package org.hatdex.libs.dal

import com.typesafe.config.Config
import org.hatdex.libs.dal.SlickPostgresDriver.api._
import slick.jdbc.meta.MTable
import slick.model.Model

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DatabaseCodeGenerator(config: Config) {
  protected def modelFuture(database: String, excludedTables: Seq[String]): Future[Model] = {
    Database.forConfig(database, config).run {
      MTable.getTables(None, None, None, Some(Seq("TABLE", "VIEW"))) //TABLE, and VIEW represent metadata, i.e. get database objects which are tables and views
        .map(_.filterNot(t => excludedTables contains t.name.name))
        .flatMap(SlickPostgresDriver.createModelBuilder(_, ignoreInvalidDefaults = false).buildModel)
    }
  }

  def generate(outputDir: String, packageName: String, className: String = "Tables",
    database: String = "devdb", excludedTables: Seq[String] = Seq("databasechangelog", "databasechangeloglock")): Future[Unit] = {

    modelFuture(database, excludedTables)
      .map(model => new TypemappedPgCodeGenerator(model))
      .map(_.writeToFile("org.hatdex.libs.dal.SlickPostgresDriver", outputDir, packageName, className, s"$className.scala"))
  }
}
