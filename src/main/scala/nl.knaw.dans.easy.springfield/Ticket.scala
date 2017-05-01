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
import java.nio.file.Path

import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.{ Failure, Success, Try }
import scalaj.http.Http

trait Ticket {
  this: DebugEnhancedLogging =>

  val lenny: URI
  val lennyConnectionTimeoutMs: Int
  val lennyReadTimeoutMs: Int

  def createTicket(path: Path, ticket: String, expireAfterSec: Long): Try[Unit] = {
    trace(path, ticket, expireAfterSec)
    val fsXml =
      <fsxml>
        <properties>
            <ticket>{ ticket }</ticket>
            <uri>/{ path }</uri>
            <role>DEFAULT ROLE</role>
            <ip>DEFAULT IP</ip>
            <expiry>{ System.currentTimeMillis / 1000 + expireAfterSec}</expiry>
        </properties>
      </fsxml>
    val response = Http(lenny.toASCIIString)
      .postData(fsXml.toString)
      .method("POST")
      .header("Content-Type", "text/xml")
      .timeout(connTimeoutMs = lennyConnectionTimeoutMs, readTimeoutMs = lennyReadTimeoutMs)
      .asBytes
    if (response.code == 200) Success(())
    else Failure(new RuntimeException(s"Unable to add ticket, response code = ${response.code}"))
  }

  def deleteTicket(ticket: String): Try[Unit] = {
    val response = Http(lenny.resolve(ticket).toASCIIString)
      .method("DELETE")
      .timeout(connTimeoutMs = lennyConnectionTimeoutMs, readTimeoutMs = lennyReadTimeoutMs)
      .asBytes
    response.code match {
      case 200 => Success(())
      case 404 => Failure(new RuntimeException(s"Could not find ticket $ticket"))
      case _ => Failure(new RuntimeException(s"Unable to remove ticket, response code = ${response.code}"))
    }
  }


}
