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

import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.xml.Elem

trait GetProgressOfCurrentJobs {
  type JobRef = String
  type JobType = AvType.Value
  type Progress = String

  def getProgressOfCurrentJobs(queue: Elem, jobType: JobType): Map[JobRef, Progress] = {
    (queue \ "queue" \ "job")
      .flatMap(job => for {
        optId <- (job \ s"raw$jobType" \ "properties" \ "job").map(_.text).headOption
        optProgress <- (job \ "status" \ "properties" \ "details").map(_.text).headOption
      } yield (optId, optProgress))
      .toMap
  }
}
