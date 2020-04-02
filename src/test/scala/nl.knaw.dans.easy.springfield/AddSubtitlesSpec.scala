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
import java.nio.file.{ Path, Paths }

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class AddSubtitlesSpec extends AnyFlatSpec with Matchers with CustomMatchers with AddSubtitles with Smithers2 {
  override val springFieldDataDir: Path = Paths.get("/data/dansstreaming")
  override val smithers2BaseUri: URI = new URI("http://localhost:8080/smithers2/")
  override val smithers2ConnectionTimeoutMs: Int = 100000
  override val smithers2ReadTimoutMs: Int = 100000
  override val defaultDomain: String = "dans"

  "createLanguageAdjustedfileName" should "change a fileName called webvtt.vtt with language nl to nl_webvtt.vtt" in {
    createLanguageAdjustedFileName(Paths.get("/path/to/nowhere/webvtt.vtt"), "nl") shouldBe s"nl_webvtt.vtt"
  }

  it should "only add a underscore to the name when an empty language is provided" in {
    createLanguageAdjustedFileName(Paths.get("/path/to/nowhere/webvtt.vtt"), "") shouldBe s"_webvtt.vtt"
  }
}
