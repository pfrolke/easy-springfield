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

class ListCollectionsSpec extends TestSupportFixture with ListCollections {
  "listUsers" should "return Seq of collection names" in {
    val parent = <fsxml>
      <collection id="coll01"/>
      <video id="video01" />
      <collection id="coll02"/>
      <audio id="audio01" />
    </fsxml>

    listCollections(parent) should be(Seq("coll01", "coll02"))
  }

  it should "return an empty Seq if there are no collections" in {
    val parent = <fsxml>
      <video id="video01" />
      <audio id="audio01" />
    </fsxml>

    listCollections(parent) should be(empty)
  }

  it should "ignore properties listed before collections" in {
    val parent = <fsxml>
      <properties>
        <depth>10</depth>
        <start>0</start>
        <limit>-1</limit>
        <totalResultsAvailable>46</totalResultsAvailable>
        <totalResultsReturned>46</totalResultsReturned>
      </properties>
      <collection id="coll01"/>
      <video id="video01" />
      <collection id="coll02"/>
      <audio id="audio01" />    </fsxml>
    listCollections(parent) should be(Seq("coll01", "coll02"))
  }

  it should "just return empty Seq if no Springfield xml is passed to it" in {
    val parent = <html>
      <body>This is not springfield</body>
    </html>

    listCollections(parent) should be(empty)
  }
}
