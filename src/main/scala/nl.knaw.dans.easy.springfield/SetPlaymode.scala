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

import java.nio.file.Path

import nl.knaw.dans.easy.springfield.Playmode.Playmode
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.{ Failure, Success, Try }

trait SetPlaymode extends DebugEnhancedLogging {
  this: Smithers2 =>

  /**
   * First checks if the provided path is a presentation or a presentation in collection. If it is the latter it will first attempt to
   * retrieve the referid to the presentation. It will than extract the playlist id('s) and assign the desired values to the playmode(s).
   *
   * @param presentationReferId path to the presentation
   * @param mode                string value of the desired playmode
   * @return Success if the operation succeeded, failure otherwise
   */
  def setPlayModeForPresentation(presentationReferId: Path, mode: Playmode): Try[Unit] = {
    for {
      referId <- extractPresentationFromCollection(presentationReferId)
      xml <- getXmlFromPath(referId)
      ids = extractVideoPlaylistIds(xml)
      _ <- setPlayModeForPlayLists(referId, mode, ids)
    } yield ()
  }

  /**
   * Sets the playmode for playlist in a presentation. The path must point to the
   * actual playlist resource.
   *
   * @param referId path to the playlist in the presentation
   * @param mode    {menu|continuous} the to be played mode
   */
  private def setPlayModeForPlayLists(referId: Path, mode: Playmode, ids: Seq[String]): Try[Unit] = {
    ids
      .map(id => setProperty(referId.resolve(s"videoplaylist").resolve(id).resolve("properties").resolve("play-mode"), mode.toString))
      .collectFirst { case Failure(exception) => Failure(new IllegalStateException(exception.getMessage)) }
      .getOrElse(Success(()))
  }
}
