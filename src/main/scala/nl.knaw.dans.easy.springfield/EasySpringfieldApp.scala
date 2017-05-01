/**
 * Copyright (C) 2017 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.springfield

import java.net.URI
import java.nio.file.{ Files, Paths }

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.configuration.PropertiesConfiguration

trait EasySpringfieldApp {
  this: DebugEnhancedLogging
    with Smithers2
    with ListUsers
    with CreateSpringfieldActions =>
  private val cfgPath = Seq(
    Paths.get("/usr/local/etc/dans/easy-springfield/"),
    Paths.get(System.getProperty("app.home")).resolve("cfg/"))

  private val cfg = cfgPath.find(Files.exists(_)).get
  val properties = new PropertiesConfiguration(cfg.resolve("application.properties").toFile)
  val smithers2BaseUri: URI = new URI(properties.getString("springfield.smithers2.base-uri"))
  val smithers2ConnectionTimeoutMs: Int = properties.getInt("springfield.smithers2.connection-timeout-ms")
  val smithers2ReadTimoutMs: Int = properties.getInt("springfield.smithers2.read-timeout-ms")
  val lenny: URI = new URI(properties.getString("springfield.lenny.uri"))
  val lennyConnectionTimeoutMs: Int = properties.getInt("springfield.lenny.connection-timeout-ms")
  val lennyReadTimeoutMs: Int = properties.getInt("springfield.lenny.read-timeout-ms")

  val defaultDomain: String = properties.getString("springfield.default-domain")
}
