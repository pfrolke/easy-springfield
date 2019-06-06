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

import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.{ Failure, Random, Success, Try }
import scala.xml.Elem

class Smithers2Spec extends TestSupportFixture
  with Smithers2
  with DebugEnhancedLogging {
  override val smithers2BaseUri: URI = new URI("http://localhost:8080/smithers2/")
  override val smithers2ConnectionTimeoutMs: Int = 100000
  override val smithers2ReadTimoutMs: Int = 100000
  override val defaultDomain: String = "dans"
  private val privateContinuousRefIdPath = "/domain/dans/user/utest/presentation/1"
  private val elem: Elem =
    <fsxml>
        <presentation id="3" referid="presentation/1">
            <videoplaylist id="1" >
            <video id="1" referid="/domain/dans/user/utest/video/5">
            </video>
            <video id="2" referid="/domain/dans/user/utest/video/7">
            </video>
          </videoplaylist>
          <videoplaylist id="some_playlist_id">
            </videoplaylist>
        </presentation>
      </fsxml>

  //overridden to mock away the rest call to Smithers2
  override def getXmlFromPath(path: Path): Try[Elem] = Try {
    path.getFileName.toString match {
      case "private_continuous" =>
        <fsxml>
          <presentation id="private_continuous" referid={privateContinuousRefIdPath}>
            <properties>
              <title/>
              <description/>
            </properties>
          </presentation>
        </fsxml>
      case "3" => elem
      case "test_presentation" =>  elem
      case _ => <empty/>
    }
  }

  "extractVideoRefFromPresentationForVideoId" should "retrieve a relative path to a video starting from domain" in {
    extractVideoRefFromPresentationForVideoId("1")(elem) shouldBe Success("domain/dans/user/utest/video/5")
    extractVideoRefFromPresentationForVideoId("2")(elem) shouldBe Success("domain/dans/user/utest/video/7")
  }

  it should "fail if a non existing index is given" in {
    extractVideoRefFromPresentationForVideoId("3")(elem) should matchPattern {
      case Failure(i: IllegalStateException) if i.getMessage == "No videoReference found for index '3' in the presentation" =>
    }
  }

  it should "fail if a malformed xml-elem is provided" in {
    val malFormedElem = <mal><formed><elem></elem></formed></mal>
    extractVideoRefFromPresentationForVideoId("1")(malFormedElem) should matchPattern {
      case Failure(i: IllegalStateException) if i.getMessage == "No videoReference found for index '1' in the presentation" =>
    }
  }

  it should "return the first video if two videos share the same reference id" in {
    val elemDuplicateId = <fsxml>
        <presentation id="3">
          <videoplaylist id="1">
            <video id="1" referid="/domain/dans/user/utest/video/5">
            </video>
            <video id="1" referid="/domain/dans/user/utest/video/7">
            </video>
          </videoplaylist>
        </presentation>
      </fsxml>
    extractVideoRefFromPresentationForVideoId("1")(elemDuplicateId) shouldBe Success("domain/dans/user/utest/video/5")
  }

  "getPresentationReferIdPath" should "succeed if the name path has more than 3 parts and presentation is the penultimate part" in {
    getPresentationReferIdPath(Paths.get("domain/dans/user/utest/presentation/1")) shouldBe a[Success[_]]
  }

  it should "fail if the path has more than 3 parts and presentation is the last part" in {
    val path = "domain/dans/user/utest/presentation"
    getPresentationReferIdPath(Paths.get(path)) should matchPattern {
      case Failure(iae: IllegalArgumentException) if iae.getMessage == createExceptionMessage(path) =>
    }
  }

  it should "fail if the  path has less than 4 parts and presentation is the penultimate part" in {
    val path = "utest/presentation/notANumber"
    getPresentationReferIdPath(Paths.get(path)) should matchPattern {
      case Failure(iae: IllegalArgumentException) if iae.getMessage == createExceptionMessage(path) =>
    }
  }

  "matches" should "mach regex" in {
    "12343FF".matches("\\d+") shouldBe false
    "1234311".matches("\\d+") shouldBe true
  }

  it should "succeed if the path has more than 3 parts and presentation is the penultimate part, and can resolve the presentation name to a referid" in {
    getPresentationReferIdPath(Paths.get("domain/dans/user/utest/presentation/private_continuous")) shouldBe Success(Paths.get(relativizePathString(privateContinuousRefIdPath)))
  }

  it should "fail if the path has more than 3 parts and presentation is the penultimate part, and cannot resolve the presentation name to a referid" in {
    getPresentationReferIdPath(Paths.get("domain/dans/user/utest/presentation/notANumber")) should matchPattern {
      case Failure(ise: IllegalStateException) if ise.getMessage == "No presentation referid found for presentation name 'notANumber'" =>
    }
  }

  "checkVideoReferId" should "succeed if the path has more than 3 parts and video is the penultimate part" in {
    checkVideoReferId(Paths.get("domain/dans/user/utest/video/1")) shouldBe Success(())
  }

  it should "fail if the path has more than 3 parts and video is the last part" in {
    val path = "domain/dans/user/utest/video"
    checkVideoReferId(Paths.get(path)) should matchPattern {
      case Failure(iae: IllegalArgumentException) if iae.getMessage == s"$path does not appear to be a video referid. Expected format: [domain/<d>/]user/<u>/video/<number>" =>
    }
  }

  it should "fail if the path has less than 4 parts and video is the penultimate part" in {
    val path = "test/video/1"
    checkVideoReferId(Paths.get(path)) should matchPattern {
      case Failure(iae: IllegalArgumentException) if iae.getMessage == s"$path does not appear to be a video referid. Expected format: [domain/<d>/]user/<u>/video/<number>" =>
    }
  }

  it should "succeed if the path has more than 3 parts and video is the penultimate part, even though last part is not a number" in {
    checkVideoReferId(Paths.get("domain/dans/user/utest/video/notANumber")) shouldBe a[Success[_]]
  }

  "checkCollection" should "succeed if the name path has more than 3 parts and collection is the penultimate part" in {
    checkCollection(Paths.get("domain/dans/user/utest/collection/1")) shouldBe a[Success[_]]
  }

  it should "fail if the path has more than 3 parts and collection is the last part" in {
    val path = "domain/dans/user/utest/collection"
    checkCollection(Paths.get(path)) should matchPattern {
      case Failure(iae: IllegalArgumentException) if iae.getMessage == s"$path does not appear to be a collection Springfield path. Expected format: [domain/<d>/]user/<u>/collection/<name>" =>
    }
  }

  it should "fail if the path has less than 4 parts and collection is the penultimate part" in {
    val path = "test/collection/1"
    checkCollection(Paths.get(path)) should matchPattern {
      case Failure(iae: IllegalArgumentException) if iae.getMessage == s"$path does not appear to be a collection Springfield path. Expected format: [domain/<d>/]user/<u>/collection/<name>" =>
    }
  }

  it should "succeed if the path has more than 3 parts and collection is the penultimate part, even though last part is not a number" in {
    checkCollection(Paths.get("domain/dans/user/utest/collection/notANumber")) shouldBe a[Success[_]]
  }

  "checkNameLenght" should "succeed if the name length is below 101" in {
    checkNameLength("below MAX_NAME_LENGTH") shouldBe Success(())
    checkNameLength(Random.nextString(MAX_NAME_LENGTH)) shouldBe Success(())
  }

  it should "fail if the name length is above 100" in {
    val overMaxNameLenght = Random.nextString(MAX_NAME_LENGTH + 1)
    checkNameLength(overMaxNameLenght) should matchPattern {
      case Failure(i: IllegalArgumentException) if i.getMessage == s"Name is longer than $MAX_NAME_LENGTH chars: $overMaxNameLenght" =>
    }
  }

  "getCompletePath" should "add domain to the path" in {
    val uncompletPath = Paths.get("not/a/complete/path")
    getCompletePath(uncompletPath) shouldBe Paths.get("domain/dans").resolve(uncompletPath)
  }

  it should "not alter an already complete path (relative)" in {
    val completePath = Paths.get("domain/dans/complete/path")
    getCompletePath(completePath) shouldBe completePath
  }

  it should "not alter an already complete path (absolute)" in {
    val completePath = Paths.get("/domain/dans/complete/path")
    getCompletePath(completePath) shouldBe completePath
  }

  it should "not alter an already complete path (relative), also if the second param is not equal to the default domain" in {
    val completePath = Paths.get("domain/notDans/complete/path")
    getCompletePath(completePath) shouldBe completePath
  }

  "getNumberOfVideos" should "return the number of videos" in {
    getNumberOfVideos(elem) shouldBe 2
  }

  it should "return 0 if there are no videos present" in {
    getNumberOfVideos(<empty/>) shouldBe 0
  }

  "validateNumberOfVideosIsSameAsNumberOfSubtitles" should "succeed if the number of subtitles '0' is equal to the number of videos in the presentation '0'" in {
    validateNumberOfVideosInPresentationIsEqualToNumberOfSubtitles(Paths.get("domain/dans/user/utest/presentation/1"), List()) shouldBe a[Success[_]]
  }

  it should "succeed if the number of subtitles '2' is equal to the number of videos in the presentation '2'" in {
    validateNumberOfVideosInPresentationIsEqualToNumberOfSubtitles(Paths.get("domain/dans/user/utest/presentation/3"), List(Paths.get("1"), Paths.get("2"))) shouldBe a[Success[_]]
  }

  it should "fail if the number of subtitles '2' is not equal to number of videos in the presentation '0'" in {
    validateNumberOfVideosInPresentationIsEqualToNumberOfSubtitles(Paths.get("domain/dans/user/utest/presentation/1"), List(Paths.get("1"), Paths.get("2"))) should matchPattern {
      case Failure(e: IllegalArgumentException) if e.getMessage == "The provided number of subtitles '2' did not match the number of videos in the presentation '0'" =>
    }
  }

  it should "fail if the number of subtitles '0' is not equal to number of videos in the presentation '2'" in {
    validateNumberOfVideosInPresentationIsEqualToNumberOfSubtitles(Paths.get("domain/dans/user/utest/presentation/3"), List()) should matchPattern {
      case Failure(e: IllegalArgumentException) if e.getMessage == "The provided number of subtitles '0' did not match the number of videos in the presentation '2'" =>
    }
  }

  "extractVideoPlaylistIds" should "return the ids of the playlists in the presentation" in {
    getXmlFromPath(Paths.get("3"))
      .map(extractVideoPlaylistIds) shouldBe Success(List("1", "some_playlist_id"))
  }

  it should "not return an empty list if no playlist ids are found" in {
    getXmlFromPath(Paths.get("private_continuous"))
      .map(extractVideoPlaylistIds) shouldBe Success(List())
  }

  "extractPresentationReferIdFromXML" should "return the path to the presentation" in {
    getXmlFromPath(Paths.get("private_continuous"))
      .flatMap(extractPresentationReferIdFromXML(_, "private_continuous")) shouldBe Success(Paths.get("/domain/dans/user/utest/presentation/1"))
  }

  it should "fail if no xml is found" in {
    getXmlFromPath(Paths.get("9"))
      .flatMap(extractPresentationReferIdFromXML(_, "3")) should matchPattern {
      case Failure(iae: IllegalArgumentException) if iae.getMessage == "No presentation with name 3" =>
    }
  }

  it should "fail if the id is not found" in {
    getXmlFromPath(Paths.get("3"))
      .flatMap(extractPresentationReferIdFromXML(_, "9")) should matchPattern {
      case Failure(iae: IllegalArgumentException) if iae.getMessage == "No presentation with name 9" =>
    }
  }

  "extractPresentationFromCollection" should "return the path to the presentation if it is not wrapped in a collection" in {
    val aDirectPresentationPath = Paths.get("domain/dans/user/user001/presentation/a_very_good_presentation")
    extractPresentationFromCollection(aDirectPresentationPath) shouldBe Success(aDirectPresentationPath)
  }

  "isCollection" should "true if the path is a collection" in {
    isCollection(Paths.get("domain/dans/user/user001/collection/c001")) shouldBe true
  }

  it should "return false if it is not  collection" in {
    isCollection(Paths.get("domain/dans/user/user001/collection/c001/longer/path")) shouldBe false
  }

  it should "return the referid if the presentation is wrapped in a collection" in {
    val presentationWrappedInCollection = Paths.get("domain/dans/user/user001/collection/c001/presentation/3")
    extractPresentationFromCollection(presentationWrappedInCollection) shouldBe Success(Paths.get("presentation/1"))
  }

  it should "fail if the referid cannot be found in the collection xml" in {
    val nonExistingPresentationIdPAth = Paths.get("domain/dans/user/user001/collection/c001/presentation/test_presentation")
    extractPresentationFromCollection(nonExistingPresentationIdPAth) should matchPattern {
      case Failure(iae: IllegalArgumentException) if iae.getMessage == "No presentation with name test_presentation" =>
    }
  }

  private def createExceptionMessage(path: String): String = s"$path does not appear to be a presentation referid or Springfield path. Expected format: [domain/<d>/]user/<u>/presentation/<number> OR [domain/<d>/]user/<u>/collection/<c>/presentation/<p>"
}
