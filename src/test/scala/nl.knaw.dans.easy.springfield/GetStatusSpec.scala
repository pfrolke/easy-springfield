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

class GetStatusSpec extends TestSupportFixture with GetStatus {

  "getStatus" should "return a list of Summary instances" in {
    val forUser = "testUser"
    val expectedFileName = "GV_AVA_doven_09.mp4"
    val expectedStatus = "done"
    val parent = <fsxml>
      <video id="11">
        <rawvideo id="1">
          <properties>
          </properties>
        </rawvideo>
        <rawvideo id="2">
          <properties>
            <filename>{ expectedFileName }</filename>
            <status>{ expectedStatus }</status>
          </properties>
        </rawvideo>
      </video>
    </fsxml>
    val result = getStatus(forUser, "video", parent)
    result should contain(AvStatusSummary(forUser, expectedFileName, expectedStatus, "",  requireTicket = true))
  }

  it should "return a list of Summary instances for audio as well" in {
    val forUser = "testUser"
    val expectedFileName = "GV_AVA_horenden_09.mp3"
    val expectedStatus = "done"
    val parent = <fsxml>
      <audio id="11">
        <rawaudio id="1">
          <properties>
          </properties>
        </rawaudio>
        <rawaudio id="2">
          <properties>
            <filename>{ expectedFileName }</filename>
            <status>{ expectedStatus }</status>
          </properties>
        </rawaudio>
      </audio>
    </fsxml>
    val result = getStatus(forUser, "audio", parent)
    result should contain(AvStatusSummary(forUser, expectedFileName, expectedStatus, "", requireTicket = true))
  }

  it should "return status 'waiting' if no status element is found" in {
    val forUser = "testUser"
    val expectedFileName = "GV_AVA_doven_09.mp4"
    val expectedStatus = "waiting"
    val parent = <fsxml>
      <video id="11">
        <rawvideo id="1">
          <properties>
          </properties>
        </rawvideo>
        <rawvideo id="2">
          <properties>
            <filename>{ expectedFileName }</filename>
          </properties>
        </rawvideo>
      </video>
    </fsxml>
    val result = getStatus(forUser, "video", parent)
    result should contain(AvStatusSummary(forUser, expectedFileName, expectedStatus, "", requireTicket = true))
  }

  it should "return status 'transcode' if no status element is found but a momar job reference is" in {
    val forUser = "testUser"
    val expectedFileName = "GV_AVA_doven_09.mp4"
    val expectedStatus = "transcode"
    val parent = <fsxml>
      <video id="11">
        <rawvideo id="1">
          <properties>
          </properties>
        </rawvideo>
        <rawvideo id="2">
          <properties>
            <filename>{ expectedFileName }</filename>
            <job>/domain/dans/service/momar/job/1</job>
          </properties>
        </rawvideo>
      </video>
    </fsxml>
    val result = getStatus(forUser, "video", parent)
    result should contain(AvStatusSummary(forUser, expectedFileName, expectedStatus, "/domain/dans/service/momar/job/1", requireTicket = true))
  }

  it should "return status 'transcode' if no status element is found but a willie job reference is" in {
    val forUser = "testUser"
    val expectedFileName = "GV_AVA_blinden_09.mp3"
    val expectedStatus = "transcode"
    val parent = <fsxml>
      <audio id="11">
        <rawaudio id="1">
          <properties>
          </properties>
        </rawaudio>
        <rawaudio id="2">
          <properties>
            <filename>{ expectedFileName }</filename>
            <job>/domain/dans/service/willie/job/1</job>
          </properties>
        </rawaudio>
      </audio>
    </fsxml>
    val result = getStatus(forUser, "audio", parent)
    result should contain(AvStatusSummary(forUser, expectedFileName, expectedStatus, "/domain/dans/service/willie/job/1", requireTicket = true))
  }

  it should "return requireTicket = false only if explicitly stated" in {
    val forUser = "testUser"
    val expectedFileName = "GV_AVA_doven_09.mp4"
    val expectedStatus = "waiting"
    val parent = <fsxml>
      <video id="11">
        <properties>
          <private>false</private>
        </properties>
        <rawvideo id="1">
          <properties>
          </properties>
        </rawvideo>
        <rawvideo id="2">
          <properties>
            <filename>{ expectedFileName }</filename>
          </properties>
        </rawvideo>
      </video>
    </fsxml>
    val result = getStatus(forUser, "video", parent)
    result should contain(AvStatusSummary(forUser, expectedFileName, expectedStatus, "", requireTicket = false))
  }
}
