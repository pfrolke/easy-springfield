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

import better.files.File
import org.apache.commons.configuration.PropertiesConfiguration

class ConfigurationSpec extends TestSupportFixture with CustomMatchers {

  "isAValidLanguageCode" should "return true if the given language is found" in {
    createConfig.isValidLanguageCode("el") shouldBe true //greek
  }

  it should "be case sensitive" in {
    val iso = createConfig
    iso.isValidLanguageCode("el") shouldBe true //greek
    iso.isValidLanguageCode("EL") shouldBe false //greek but wrong casing
  }

  it should "return false if the given language does not exists" in {
   createConfig.isValidLanguageCode("zz") shouldBe false
  }

  "getSupportedCodes" should "return a list of tuples with all currently supported ISO-639-1 language codes" in {
    createConfig.languages.length shouldBe 184 // number of entries in the iso-639-1.properties file
  }

  private def createConfig: Configuration = {
    Configuration("1.0", new PropertiesConfiguration(), File("src/test/resources/debug-config/iso-639-1.txt")
      .lines
      .toList)
  }
}
