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

import org.apache.commons.configuration.PropertiesConfiguration
import org.rogach.scallop.{ ScallopConf, ScallopOption, Subcommand, singleArgConverter }

class CommandLineOptions(args: Array[String], properties: PropertiesConfiguration, version: String) extends ScallopConf(args) {
  appendDefaultToDescription = true
  editBuilder(_.setHelpWidth(110))

  printedName = "easy-springfield"
  private val _________ = " " * printedName.length
  private val SUBCOMMAND_SEPARATOR = "---\n"
  val description: String = s"""Tools for managing a Springfield WebTV server."""
  val synopsis: String =
    s"""
       |$printedName list-users <domain>
       |$printedName create-user [-d, --target-domain <arg>] <username>
       |$printedName create-collection [-t, --title <arg>] [-d, --description <arg>] \\
       |    [--target-domain <arg>] <collection> <target-user>
       |$printedName create-presentation [-t, --title <arg>] [-d, --description <arg>] \\
       |    [-r, --require-ticket] [--target-domain <arg>] <target-user>
       |$printedName create-springfield-actions [-p, --check-parent-items] [-v, --videos-folder <arg>] \\
       |    <videos-csv> > springfield-actions.xml
       |$printedName status [-u, --user <arg>][-d, --domain <arg>]
       |$printedName set-require-ticket <springfield-path> {true|false}
       |$printedName create-ticket [-e,--expires-after-seconds <arg>] [-t, --ticket <arg>] \\
       |    <springfield-path>
       |$printedName delete-ticket <ticket>
       |$printedName delete [-r, --with-referenced-items] <springfield-path>
       |$printedName add-video-to-presentation <video> <name> <springfield-path>
       |$printedName add-presentation-to-collection <presentation> <name> <springfield-path>
     """.stripMargin

  version(s"$printedName v$version")

  banner(
    s"""
       |  $description
       |
       |Usage:
       |
       |$synopsis
       |
       |Options:
       |
       |""".stripMargin)


  private implicit val fileConverter = singleArgConverter[Path](s => Paths.get(resolveTildeToHomeDir(s)))

  private def resolveTildeToHomeDir(s: String): String =
    if (s.startsWith("~")) s.replaceFirst("~", System.getProperty("user.home"))
    else s

  val listUsers = new Subcommand("list-users") {
    descr("Lists users in a given domain")
    val domain: ScallopOption[String] = trailArg(name = "domain",
      descr = "the domain of which to list the users",
      default = Some(properties.getString("springfield.default-domain")))
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(listUsers)

  val createUser = new Subcommand("create-user") {
    descr(
      """Creates a new user in the Springfield database. This does NOT generate a springfield-actions XML but
        |instead creates the user in Springfield right away.
      """.stripMargin.stripLineEnd)
    val user: ScallopOption[String] = trailArg(name = "user", descr = "User name for the new user")
    val targetDomain: ScallopOption[String] = opt(name = "target-domain", short = 'd',
      descr = "The target domain in which to create the user",
      default = Some(properties.getString("springfield.default-domain")))
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(createUser)

  val createCollection = new Subcommand("create-collection") {
    descr(
      """Creates a new collection in the Springfield database. This does NOT generate a springfield-actions XML but
        |instead creates the collection in Springfield right away.
      """.stripMargin.stripLineEnd)
    val collection: ScallopOption[String] = trailArg(name = "collection", descr = "Name for the collection")
    val user: ScallopOption[String] = trailArg(name = "target-user", descr = "Existing user under which to store the collection")
    val targetDomain: ScallopOption[String] = opt(name = "target-domain",
      descr = "The target domain in which to create the collection",
      default = Some(properties.getString("springfield.default-domain")))
    val title: ScallopOption[String] = opt(name = "title", short = 't',
      descr = "Title for the new collection", default = Some(""))
    val description: ScallopOption[String] = opt(name = "description", short = 'd',
      descr = "Description for the new collection", default = Some(""))
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(createCollection)

  val createPresentation = new Subcommand("create-presentation") {
    descr(
      """Creates a new, empty presentation in the Springfield database, to be populated with the add-video-to-presentation command.
      """.stripMargin.stripLineEnd)
    val user: ScallopOption[String] = trailArg(name = "target-user", descr = "Existing user under which to store the collection")
    val targetDomain: ScallopOption[String] = opt(name = "target-domain",
      descr = "The target domain in which to create the presentation",
      default = Some(properties.getString("springfield.default-domain")))
    val title: ScallopOption[String] = opt(name = "title", short = 't',
      descr = "Title for the new presentation", default = Some(""))
    val description: ScallopOption[String] = opt(name = "description", short = 'd',
      descr = "Description for the new presentation", default = Some(""))
    val requireTicket: ScallopOption[Boolean] = opt(name = "require-ticket", short = 'r')
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(createPresentation)

  val createSpringfieldActions = new Subcommand("create-springfield-actions") {
    descr(
      """Create Springfield Actions XML containing add-actions for A/V items specified in a CSV file
        |with lines describing videos with the following columns: SRC, DOMAIN, USER, COLLECTION, PRESENTATION, FILE,
        |REQUIRE-TICKET.
      """.stripMargin.stripLineEnd)
    val videosCsv: ScallopOption[Path] = trailArg(name = "video-csv",
      descr = "CSV file describing the videos",
      required = true)
    val videosFolder: ScallopOption[Path] = opt(name = "videos-folder", short = 'v' ,
      descr = "Folder relative to which to resolve the SRC column in the CSV")
    val checkParentItems: ScallopOption[Boolean] = opt(name = "check-parent-items", short = 'p',
      descr = "Check that parent items (domain, user, collection) exist")
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(createSpringfieldActions)

  val status = new Subcommand("status") {
    descr("Retrieves the status of content offered for ingestion into Springfield.")
    val domain: ScallopOption[String] = opt(name = "domain",
      descr = "limit to videos within this domain",
      default = Some("dans"))
    val user: ScallopOption[String] = opt(name = "user",
      descr = "limit to videos owned by this user")

    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(status)

  val setRequireTicket = new Subcommand("set-require-ticket") {
    descr("Sets or clears the 'require-ticket' flag for the specified presentation.")
    val path: ScallopOption[Path] = trailArg(name = "springfield-path", descr = "The parent of items to change")
    val requireTicket: ScallopOption[String] = trailArg(name = "require-ticket", descr = "true|false")
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(setRequireTicket)

  val createTicket = new Subcommand("create-ticket") {
    descr("""Creates and registers an authorization ticket for a specified presentation.
            |If no ticket is specificied a random one is generated.""".stripMargin)
    val path: ScallopOption[Path] = trailArg(name = "springfield-path", descr = "The presentation to create the ticket for")
    val expiresAfterSeconds: ScallopOption[Long] = opt(name = "expires-after-seconds", short = 'e', default = Some(60 * 5))
    val ticket: ScallopOption[String] = opt(name = "ticket", short = 't', default = None)
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(createTicket)

  val deleteTicket = new Subcommand("delete-ticket") {
    descr("Deletes a specified authorization ticket.")
    val ticket: ScallopOption[String] = trailArg(name = "ticket")
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(deleteTicket)

  val delete = new Subcommand("delete") {
    descr("Deletes the item at the specified Springfield path.")
    val path: ScallopOption[Path] = trailArg(name = "path",
      descr = "the path pointing item to remove")
    val withReferencedItems: ScallopOption[Boolean] = opt(name = "with-referenced-items", short = 'r',
      descr = "also remove items reference from <path>, recursively")
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(delete)

  val addVideoToPresentation = new Subcommand("add-video-to-presentation") {
    descr("Adds a video to a presentation under a specified name.")
    val video: ScallopOption[Path] = trailArg(name = "video",
      descr = "referid of the video")
    val name: ScallopOption[String] = trailArg(name = "name",
      descr = "name to assign to the video in the presentation")
    val presentation: ScallopOption[Path] = trailArg(name = "presentation",
      descr = "the presentation, either a Springfield path or a referid")
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(addVideoToPresentation)

  val addPresentationToCollection = new Subcommand("add-presentation-to-collection") {
    descr("Adds a presentation to a collection under a specified name.")
    val presentation: ScallopOption[Path] = trailArg(name = "presentation",
      descr = "referid of the presentation")
    val name: ScallopOption[String] = trailArg(name = "name",
      descr = "name to assign to the presentation in the collection")
    val collection: ScallopOption[Path] = trailArg(name = "collection",
      descr = "the Springfield path of the collection")
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(addPresentationToCollection)

  footer("")
}
