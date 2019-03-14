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
import org.apache.commons.io.FileUtils

import scala.util.{ Success, Try }

trait AddSubtitles extends DebugEnhancedLogging {
  this: Smithers2 =>
  val springFieldDataDir: Path

  private def moveSubtitlesToDir(relativeDestination: Path, subtitles: Path, adjustedFileName: String, springFieldBaseDir: Path): Try[Unit] = Try {
    val resolvedDestination = springFieldBaseDir.resolve(relativeDestination)
    debug(s"copying sub titles '${ subtitles.getFileName }' to destination '${ resolvedDestination.resolve(adjustedFileName) }'")
    FileUtils.copyFile(subtitles.toFile, resolvedDestination.resolve(adjustedFileName).toFile)
  }

  def createLanguageAdjustedFileName(subTitlesPath: Path, language: String): String = {
    s"${ language }_${ subTitlesPath.getFileName }" // sub.vtt => nl_sub.vtt)
  }

  /**
   * This method copies a file containing subtitles to the desired video directory.
   * It also updates the Smithers2 XML through HTTP PUT call.
   *
   * @param subtitles  the path to a file containing the subtitles to be added to the video
   * @param videoRefId the path to the video where the subtitles will be added
   * @param language   a ISO639-1 language code of two characters
   * @return
   */
  def addSubtitlesToVideo(subtitles: Path, videoRefId: Path, language: String): Try[Unit] = {
    for {
      _ <- checkVideoReferId(videoRefId)
      adjustedFileName = createLanguageAdjustedFileName(subtitles, language)
      _ <- moveSubtitlesToDir(videoRefId, subtitles, adjustedFileName, springFieldDataDir)
      _ <- putSubtitlesToVideo(videoRefId, language, adjustedFileName)
    } yield ()
  }

  /**
   * Adds a list of subtitles to a presentation, recursive function loops over the list of subtitles with an index
   * First adds the subtitles to their video and than inserts a reference from the presentation to the subtitles file through a HTTP PUT call.
   *
   * @param videoNumber  the index of the video for the to be added subtitles
   * @param language     the language of the subtitles
   * @param presentation the path towards the presentation
   * @param subtitles    a list of paths to the to be added subtitles files
   * @return
   */
  def addSubtitlesToPresentation(videoNumber: Int, language: String, presentation: Path, subtitles: List[Path]): Try[Unit] = {
    subtitles match {
      case Nil => Success(())
      case head :: tail =>
        val relativePathToVideoProps = s"videoplaylist/1/video/$videoNumber"
        val pathToPresentation = presentation.resolve(relativePathToVideoProps)
        for {
          _ <- checkPresentation(presentation)
          videoRef <- getVideoRefIdForVideoInPresentation(presentation, String.valueOf(videoNumber))
          languageAdjustedFileName = createLanguageAdjustedFileName(head, language)
          _ <- addSubtitlesToVideo(head, Paths.get(videoRef), language) // first add the subtitles to the video, before adding it to the presentation
          _ <- putSubtitlesToPresentation(pathToPresentation, language, languageAdjustedFileName)
          _ = debug(s"added '$head' to presentation '$pathToPresentation'")
          _ <- addSubtitlesToPresentation(videoNumber + 1, language, presentation, tail)
        } yield ()
    }
  }
}
