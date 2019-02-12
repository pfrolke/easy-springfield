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

import nl.knaw.dans.easy.springfield.AvType._

class GetProgressOfCurrentJobsSpec extends TestSupportFixture with GetProgressOfCurrentJobs {

  "getProgressOfCurrentJobs" should "return a map from jobref to a progress (a percentage completed)" in {
    val queueXml =
      <fsxml>
        <queue id="high">
          <properties>
            <info>queue with high priority</info>
            <priority>high</priority>
          </properties>
          <job>
            <rawaudio id="1" referid="/domain/dans/user/emarkus/audio/23/rawaudio/2">
              <properties>
                <reencode>false</reencode>
                <mount>dans</mount>
                <format>aac</format>
                <extension>m4a</extension>
                <wantedbitrate>800000</wantedbitrate>
                <batchfile>mp4_audio</batchfile>
                <filename>IM14HeeschH1v1.m4a</filename>
                <job>/domain/dans/service/willie/queue/high/job/42</job>
              </properties>
            </rawaudio>
            <status id="1">
              <properties>
                <message>Progress</message>
                <details>5</details>
                <uri />
              </properties>
            </status>
          </job>
        </queue>
      </fsxml>

    getProgressOfCurrentJobs(queueXml, audio) should contain only (
      "/domain/dans/service/willie/queue/high/job/42" -> "5"
      )
  }

  it should "do the same if there are multiple elements in the queue" in {
    val queueXml =
      <fsxml>
        <queue id="high">
          <properties>
            <info>queue with high priority</info>
            <priority>high</priority>
          </properties>
          <job>
            <rawaudio id="1" referid="/domain/dans/user/emarkus/audio/23/rawaudio/2">
              <properties>
                <reencode>false</reencode>
                <mount>dans</mount>
                <format>aac</format>
                <extension>m4a</extension>
                <wantedbitrate>800000</wantedbitrate>
                <batchfile>mp4_audio</batchfile>
                <filename>IM14HeeschH1v1.m4a</filename>
                <job>/domain/dans/service/willie/queue/high/job/42</job>
              </properties>
            </rawaudio>
            <status id="1">
              <properties>
                <message>Progress</message>
                <details>5</details>
                <uri />
              </properties>
            </status>
          </job>
         <job>
            <rawaudio id="1" referid="/domain/dans/user/emarkus/audio/29/rawaudio/2">
              <properties>
                <reencode>false</reencode>
                <mount>dans</mount>
                <format>aac</format>
                <extension>m4a</extension>
                <wantedbitrate>800000</wantedbitrate>
                <batchfile>mp4_audio</batchfile>
                <filename>IM14HeeschH1v1.m4a</filename>
                <job>/domain/dans/service/willie/queue/high/job/41</job>
              </properties>
            </rawaudio>
            <status id="1">
              <properties>
                <message>Progress</message>
                <details>95</details>
                <uri />
              </properties>
            </status>
          </job>
        </queue>
      </fsxml>

    getProgressOfCurrentJobs(queueXml, audio) should contain only(
      "/domain/dans/service/willie/queue/high/job/41" -> "95",
      "/domain/dans/service/willie/queue/high/job/42" -> "5",
    )
  }

  it should "leave out jobs without a status" in {
    val queueXml =
      <fsxml>
        <queue id="high">
          <properties>
            <info>queue with high priority</info>
            <priority>high</priority>
          </properties>
          <job>
            <rawaudio id="1" referid="/domain/dans/user/emarkus/audio/23/rawaudio/2">
              <properties>
                <reencode>false</reencode>
                <mount>dans</mount>
                <format>aac</format>
                <extension>m4a</extension>
                <wantedbitrate>800000</wantedbitrate>
                <batchfile>mp4_audio</batchfile>
                <filename>IM14HeeschH1v1.m4a</filename>
                <job>/domain/dans/service/willie/queue/high/job/42</job>
              </properties>
            </rawaudio>
            <status id="1">
              <properties>
                <message>Progress</message>
                <details>5</details>
                <uri />
              </properties>
            </status>
          </job>
         <job>
            <rawaudio id="1" referid="/domain/dans/user/emarkus/audio/29/rawaudio/2">
              <properties>
                <reencode>false</reencode>
                <mount>dans</mount>
                <format>aac</format>
                <extension>m4a</extension>
                <wantedbitrate>800000</wantedbitrate>
                <batchfile>mp4_audio</batchfile>
                <filename>IM14HeeschH1v1.m4a</filename>
                <job>/domain/dans/service/willie/queue/high/job/41</job>
              </properties>
            </rawaudio>
          </job>
        </queue>
      </fsxml>

    getProgressOfCurrentJobs(queueXml, audio) should contain only (
      "/domain/dans/service/willie/queue/high/job/42" -> "5"
      )
  }
}
