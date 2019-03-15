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

import java.io.ByteArrayInputStream
import java.net.URI
import java.nio.file.{ Path, Paths }

import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import scalaj.http.Http

import scala.util.{ Failure, Success, Try }
import scala.xml.{ Elem, XML }

trait Smithers2 {
  this: DebugEnhancedLogging =>
  val smithers2BaseUri: URI
  val smithers2ConnectionTimeoutMs: Int
  val smithers2ReadTimoutMs: Int
  val defaultDomain: String

  def getXmlFromPath(path: Path): Try[Elem] = {
    trace(path)
    val uri = path2Uri(path)
    debug(s"Smithers2 URI: $uri")
    for {
      response <- http("GET", uri)
      if response.code == 200
      xml <- checkResponseOk(response.body)
    } yield xml
  }

  def checkPathExists(path: Path): Try[(Path, Boolean)] = {
    trace(path)
    val uri = path2Uri(path)
    debug(s"Smithers2 URI: $uri")
    val result = for {
      response <- http("GET", uri)
      if response.code == 200
      _ <- checkResponseOk(response.body)
    } yield (path, true)
    result.recoverWith {
      case SpringfieldErrorException(errorCode, _, _) if errorCode == 404 => Success((path, false))
      case e => Failure(e)
    }
  }

  /**
   * Sets or clears the requireTicket flag on the specified audio/video file. The path must point to the
   * actual audio/video resource, not to a reference to the audio/video
   *
   * @param avFile        path to the audio/video
   * @param requireTicket true to set the flag, false to clear it
   * @return
   */
  def setRequireTicket(avFile: Path, requireTicket: Boolean): Try[Unit] = {
    trace(avFile, requireTicket)
    val uri = path2Uri(avFile.resolve("properties").resolve("private"))
    debug(s"Smithers2 URI: $uri")
    for {
      response <- http("PUT", uri, requireTicket.toString)
      if response.code == 200
      _ <- checkResponseOk(response.body)
    } yield ()
  }

  /**
   * Attempts to delete the item at `path`
   *
   * @param path the Springfield path of the item to delete
   * @return Success if the item was deleted, Failure with an error message otherwise
   */
  def deletePath(path: Path): Try[Unit] = {
    trace(path)
    val uri = path2Uri(path)
    debug(s"Smithers2 URI: $uri")
    for {
      response <- http("DELETE", uri)
      if response.code == 200
      _ <- checkResponseOk(response.body)
    } yield ()
  }

  def createUser(user: String, targetDomain: String): Try[Unit] = {
    trace(user, targetDomain)
    val uri = path2Uri(Paths.get("domain", targetDomain, "user", user, "properties"))
    debug(s"Smithers2 URI: $uri")
    for {
      response <- http("PUT", uri, <fsxml><properties/></fsxml>.toString)
      if response.code == 200
      _ <- checkResponseOk(response.body)
    } yield ()
  }

  def createCollection(name: String, title: String, description: String, targetUser: String, targetDomain: String): Try[Unit] = {
    trace(name, title, description, targetUser, targetDomain)
    val uri = path2Uri(Paths.get("domain", targetDomain, "user", targetUser, "collection", name, "properties"))
    debug(s"Smithers2 URI: $uri")
    for {
      response <- http("PUT", uri,
        <fsxml>
          <properties>
            <title>{ title }</title>
            <description>{ description }</description>
          </properties>
        </fsxml>.toString)
      if response.code == 200
      _ <- checkResponseOk(response.body)
    } yield ()
  }

  def createPresentation(title: String, description: String, isPrivate: Boolean, targetUser: String, targetDomain: String): Try[String] = {
    trace(title, description, targetUser, targetDomain)
    val uri = path2Uri(Paths.get("domain", targetDomain, "user", targetUser, "presentation"))
    for {
      response <- http("POST", uri,
        <fsxml>
          <properties>
            <title>{ title }</title>
            <description>{ description }</description>
          </properties>
          <videoplaylist id="1"><properties><private>{ isPrivate }</private></properties></videoplaylist>
        </fsxml>.toString)
      if response.code == 200
      xml <- checkResponseOk(response.body)
      _ = debug(s"Return xml = ${ xml.toString }")
      referId <- Try { xml \ "properties" \ "uri" }
    } yield referId.head.text
  }

  def getReferencedPaths(path: Path): Try[Seq[Path]] = {
    getXmlFromPath(path)
      .map(_ \\ "@referid")
      .map {
        _.map {
          n =>
            if (n.text.startsWith("/")) n.text.substring(1)
            else n.text
        }
          .map(Paths.get(_))
      }
      .map {
        paths =>
          paths.map(getReferencedPaths).collectResults.map {
            s =>
              if (s.isEmpty) Seq()
              else s.reduce(_ ++ _)
          }
            .map(_ ++ paths)
      }.flatten.map(_.distinct)
  }

  def getVideoRefIdForVideoInPresentation(presentation: Path, id: String): Try[String] = {
    getXmlFromPath(presentation)
      .flatMap(extractVideoRefFromPresentationForVideoId(id))
  }

  private[springfield] def extractVideoRefFromPresentationForVideoId(index: String)(presentationXml: Elem): Try[String] = Try {
    (presentationXml \\ "video")
      .collectFirst { case node if (node \ "@id").text == index => (node \\ "@referid").text }
      .map(relativizePathString)
      .getOrElse(throw new IllegalStateException(s"No videoReference found for index '$index' in the presentation"))
  }

  private[springfield] def relativizePathString(path: String): String = {
    if (path.startsWith("/") && path.length >= 2) path.substring(1)
    else path
  }

  def putSubtitlesToVideo(videoRefId: Path, languageCode: String, fileName: String): Try[Elem] = {
    val uri = path2Uri(videoRefId.resolve("properties").resolve(s"webvtt_$languageCode"))
    debug(s"Smithers2 URI: $uri")
    http("PUT", uri, fileName).flatMap(response => {
      if (response.code == 200) checkResponseOk(response.body)
      else Failure(new IllegalStateException(s"response code '${ response.code }' was not equal to 200, body = '${ response.body }'"))
    })
  }

  def addVideoRefToPresentation(videoReferId: Path, videoName: String, presentation: Path): Try[Unit] = {
    for {
      _ <- checkVideoReferId(videoReferId)
      presentationReferId <- getPresentationReferIdPath(presentation)
      _ <- checkNameLength(videoName)
      _ = debug(s"Resolved to presentation referid: $presentationReferId")
      uri = path2Uri(presentationReferId.resolve("videoplaylist/1/video/").resolve(videoName).resolve("attributes"))
      response <- http("PUT", uri,
        <fsxml>
          <attributes>
            <referid>{ "/" + getCompletePath(videoReferId).toString }</referid>
          </attributes>
        </fsxml>.toString)
      if response.code == 200
      _ <- checkResponseOk(response.body)
    } yield ()
  }

  def addPresentationRefToCollection(presentationPath: Path, presentationName: String, collection: Path): Try[Unit] = {
    val uri = path2Uri(collection.resolve("presentation").resolve(presentationName).resolve("attributes"))
    debug(s"PUT to $uri")
    for {
      presentationReferId <- getPresentationReferIdPath(presentationPath)
      _ <- checkCollection(collection)
      _ <- checkNameLength(presentationName)
      response <- http("PUT", uri,
        <fsxml>
          <attributes>
            <referid>{ "/" + getCompletePath(presentationReferId).toString }</referid>
          </attributes>
        </fsxml>.toString)
      if response.code == 200
      _ <- checkResponseOk(response.body)
    } yield ()
  }

  def checkVideoReferId(videoReferId: Path): Try[Unit] = {
    if (videoReferId.getNameCount > 3 && videoReferId.getName(videoReferId.getNameCount - 2).toString == "video") Success(())
    else Failure(new IllegalArgumentException(s"$videoReferId does not appear to be a video referid. Expected format: [domain/<d>/]user/<u>/video/<number>"))
  }

  def getPresentationReferIdPath(presentation: Path): Try[Path] = { //TODO refine code
    if (isPresentationPath(presentation) && presentation.getFileName.toString.matches("\\d+")) Success(presentation)
    else if (isPresentationPath(presentation)) {
      logger.info(s"received a presentation path with a name, trying to resolve referid for ${ presentation.getFileName }")
      getXmlFromPath(presentation)
        .flatMap(xml => extractPresentationReferIdPath(presentation, xml))
    }
    else Failure(new IllegalArgumentException(s"$presentation does not appear to be a presentation referid or Springfield path. Expected format: [domain/<d>/]user/<u>/presentation/<number> OR [domain/<d>/]user/<u>/collection/<c>/presentation/<p>"))
  }

  private def isPresentationPath(presentation: Path): Boolean = {
    presentation.getNameCount > 3 && presentation.getName(presentation.getNameCount - 2).toString == "presentation"
  }

  private def extractPresentationReferIdPath(presentation: Path, xml: Elem): Try[Path] = Try {
    (xml \\ "presentation").map(_ \\ "@referid")
      .map(node => Paths.get(node.text))
      .collectFirst { case path: Path if isPresentationPath(path) => Paths.get(relativizePathString(path.toString)) }
      .getOrElse(throw new IllegalStateException(s"No presentation referid found for presentation name '${ presentation.getFileName }'"))
  }

  def checkCollection(collection: Path): Try[Unit] = {
    if (collection.getNameCount > 3 && collection.getName(collection.getNameCount - 2).toString == "collection") Success(())
    else Failure(new IllegalArgumentException(s"$collection does not appear to be a collection Springfield path. Expected format: [domain/<d>/]user/<u>/collection/<name>"))
  }

  def checkNameLength(name: String): Try[Unit] = {
    if (name.length <= MAX_NAME_LENGTH) Success(())
    else Failure(new IllegalArgumentException(s"Name is longer than $MAX_NAME_LENGTH chars: $name"))
  }

  def getCompletePath(path: Path): Path = {
    if (path.getName(0).toString == "domain") path
    else Paths.get("domain", defaultDomain).resolve(path)
  }

  def path2Uri(path: Path): URI = {
    new URI(smithers2BaseUri.getScheme,
      smithers2BaseUri.getUserInfo,
      smithers2BaseUri.getHost,
      smithers2BaseUri.getPort,
      Paths.get(smithers2BaseUri.getPath).resolve(getCompletePath(path)).toString, null, null)
  }

  def http(method: String, uri: URI, body: String = null) = Try {
    {
      if (body == null) Http(uri.toASCIIString)
      else Http(uri.toASCIIString).postData(body)
    }.method(method)
      .timeout(connTimeoutMs = smithers2ConnectionTimeoutMs, readTimeoutMs = smithers2ReadTimoutMs)
      .asBytes
  }

  def checkResponseOk(content: Array[Byte]): Try[Elem] = {
    /*
     * Never mind about the status codes. Springfield only returns 200 :-/
     */
    val xml = XML.load(new ByteArrayInputStream(content))
    xml.label match {
      case "error" =>
        val errorCode = xml.attribute("id").flatMap(_.headOption).map(_.text).getOrElse("<no error code>")
        val message = (xml \ "properties" \ "message").headOption.map(_.text).getOrElse("<no message>")
        val details = (xml \ "properties" \ "details").headOption.map(_.text).getOrElse("<no details>")
        Failure(SpringfieldErrorException(errorCode.toInt, message, details))
      case _ => Success(xml)
    }
  }
}
