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

import scala.xml.{ Elem, NodeSeq }

case class AvStatusSummary(user: String, filename: String, status: String, jobRef: String, requireTicket: Boolean)

trait GetStatus {

  def getStatus(forUser: String, avType: String, parent: Elem): Seq[AvStatusSummary] = {
    for {
      video <- parent \ avType
      requireTicket = video \ "properties" \ "private"
      raw2 <- video \ s"raw${ avType }"
      if raw2 \@ "id" == "2"
      filename <- raw2 \ "properties" \ "filename"
      status = raw2 \ "properties" \ "status"
      job = raw2 \ "properties" \ "job"
      screensJob = video \ "screens" \ "properties" \ "joburi"
    } yield
      AvStatusSummary(
        forUser,
        filename.text,
        calcStatus(job, screensJob, status),
        job.map(_.text).headOption.getOrElse(""),
        requireTicket = requireTicket.isEmpty || requireTicket.head.text.toBoolean)
  }

  private def calcStatus(transCodingJob: NodeSeq, screensJob: NodeSeq, status: NodeSeq): String = {
    if (screensJob.nonEmpty) "stills"
    else if (status.isEmpty) {
      if (transCodingJob.isEmpty) "waiting"
      else if (isTranscodingJob(transCodingJob.head.text)) "transcode"
      else "unkown"
    }
    else status.head.text
  }

  private def isTranscodingJob(jobRef: String): Boolean = {
    (jobRef contains "momar") || (jobRef contains "willie")
  }
}
