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

import java.nio.file.{ Files, Path, Paths }

import org.apache.commons.csv.{ CSVFormat, CSVParser, CSVRecord }

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.{ Failure, Success, Try }
import scala.xml.Elem

case class Video(srcVideo: Path, targetDomain: String, targetUser: String, targetCollection: String, targetPresentation: String, targetVideo: String, requireTicket: Boolean = true)

trait CreateSpringfieldActions {
  val defaultDomain: String

  def parseCsv(file: Path): Try[Seq[Video]] = Try {
    val rawContent = Source.fromFile(file.toFile).mkString
    val format = CSVFormat.RFC4180
      .withRecordSeparator(',')
      .withHeader()
      .withSkipHeaderRecord(true)
    val parser = CSVParser.parse(rawContent, format)
    parser.getRecords.asScala
      .map { row: CSVRecord =>
        val targetDomain = Try { row.get("DOMAIN") }.getOrElse(defaultDomain)
        val srcVideo = Paths.get(row.get("SRC-VIDEO"))
        val targetVideo = Try { row.get("TARGET-VIDEO") }.getOrElse(srcVideo.getFileName.toString)

        Video(
          srcVideo = srcVideo,
          targetDomain = targetDomain,
          targetUser = row.get("USER"),
          targetCollection = row.get("COLLECTION"),
          targetPresentation = row.get("PRESENTATION"),
          targetVideo = targetVideo,
          requireTicket = row.get("REQUIRE-TICKET").toBoolean)
        // TODO: validate input
      }
  }

  def checkSourceVideosExist(videos: Seq[Video], srcFolder: Path): Try[Unit] = {
    val incorrect = videos.map(_.srcVideo).filterNot(v => !v.isAbsolute && Files.isRegularFile(srcFolder.resolve(v)))
    if (incorrect.isEmpty) Success(())
    else Failure(new Exception(s"Error in following items: [${ incorrect.mkString(", ") }]. Possible errors: file not found (or a directory), path is absolute. " +
      s"Paths resolved against source folder: $srcFolder"))
  }

  def getParentPaths(videos: Seq[Video]): Set[Path] = {
    (videos.map(v => Paths.get("domain", v.targetDomain)) ++
      videos.map(v => Paths.get("domain", v.targetDomain, "user", v.targetUser)) ++
      videos.map(v => Paths.get("domain", v.targetDomain, "user", v.targetUser, "collection", v.targetCollection))).toSet
  }

  def createSpringfieldActions(videos: Seq[Video]): Try[Elem] = Try {
    <actions xmlns="http://easy.dans.knaw.nl/external/springfield-actions/">
      { createAddPresentations(videos) }
    </actions>
  }

  def createAddPresentations(videos: Seq[Video]): Seq[Elem] = {
    videos.groupBy(_.targetPresentation).values.map(createAddPresentation).toSeq
  }

  /**
   * Creates one add element for a presentation
   *
   * @param videos the videos in this presentation
   */
  def createAddPresentation(videos: Seq[Video]): Elem = {
    <add target={s"domain/${ videos.head.targetDomain }/user/${ videos.head.targetUser }/collection/${ videos.head.targetCollection }"}>
      <presentation name={videos.head.targetPresentation}>
        <video-playlist require-ticket={videos.head.requireTicket.toString}>
          {videos.sortBy(_.targetVideo).map(v => createAddVideo(v.srcVideo, v.targetVideo))}
        </video-playlist>
      </presentation>
    </add>
  }

  def createAddVideo(srcVideo: Path, fileName: String): Elem = {
      <video src={srcVideo.toString} target={fileName}/>
  }
}
