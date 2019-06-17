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

import java.nio.file.{ Path, Paths }

import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.Try

trait SetTitle extends DebugEnhancedLogging {
  this: Smithers2 =>

  def setTitle(video: String, title: String, presentation: Path): Try[Unit] = {
    for {
      xml <- getXmlFromPath(presentation)
      videoPath <- extractVideoRefFromPresentationForVideoId(video)(xml)
      titlePath = toTitlePath(videoPath)
      _ <- setProperty(titlePath, title)
    } yield ()
  }

  def toTitlePath(videoPath: String): Path = {
    Paths.get(videoPath).resolve("properties").resolve("title")
  }
}
