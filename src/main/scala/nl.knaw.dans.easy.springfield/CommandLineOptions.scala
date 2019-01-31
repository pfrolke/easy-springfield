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
import org.rogach.scallop.{ singleArgConverter, ScallopConf, ScallopOption, Subcommand }

class CommandLineOptions(args: Array[String], properties: PropertiesConfiguration, version: String) extends ScallopConf(args) {
  appendDefaultToDescription = true
  editBuilder(_.setHelpWidth(110))

  printedName = "easy-springfield"
  private val SUBCOMMAND_SEPARATOR = "---\n"
  private val FIVE_MINUTES = 60 * 5
  val description: String = s"""Tools for managing a Springfield WebTV server."""
  val synopsis: String =
    s"""
       |$printedName list-users [<domain>]
       |$printedName list-collections <user> [<domain>]
       |$printedName create-user <user> [<domain>]
       |$printedName create-collection [-t, --title <arg>] [-d, --description <arg>] \\
       |    <collection> <user> [<domain>]
       |$printedName create-presentation [-t, --title <arg>] [-d, --description <arg>] \\
       |    [-r, --require-ticket] <user> [<domain>]
       |$printedName create-springfield-actions [-c, --check-parent-items] [-v, --videos-folder <arg>] \\
       |    <videos-csv> > springfield-actions.xml
       |$printedName status [-u, --user <arg>][-d, --domain <arg>]
       |$printedName set-require-ticket <springfield-path> {true|false}
       |$printedName create-ticket [-e,--expires-after-seconds <arg>] [-t, --ticket <arg>] \\
       |    <springfield-path>
       |$printedName delete-ticket <ticket>
       |$printedName delete [-r, --with-referenced-items] <springfield-path>
       |$printedName add-videoref-to-presentation <video> <name> <presentation>
       |$printedName add-presentationref-to-collection <presentation> <name> <collection>
       |$printedName add-subtitles-to-video --language <code> <video> <web-vtt-file>
       |$printedName add-subtitles-to-presentation --language <code> <presentation> <web-vtt-file>...
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
      required = false,
      default = Some(properties.getString("springfield.default-domain")))
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(listUsers)

  val listCollections = new Subcommand("list-collections") {
    descr("Lists the collections of a user in a given domain")
    val user: ScallopOption[String] = trailArg(name = "user",
      descr = "the user whose collections to list",
      required = true)
    val domain: ScallopOption[String] = trailArg(name = "domain",
      descr = "the domain containing the user",
      required = false,
      default = Some(properties.getString("springfield.default-domain")))
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(listCollections)

  val createUser = new Subcommand("create-user") {
    descr(
      """Creates a new user in the Springfield database. This does NOT generate a springfield-actions XML but
        |instead creates the user in Springfield right away.
      """.stripMargin.stripLineEnd)
    val user: ScallopOption[String] = trailArg(name = "user",
      descr = "user name for the new user",
      required = true)
    val domain: ScallopOption[String] = trailArg(name = "domain",
      descr = "the target domain in which to create the user",
      required = false,
      default = Some(properties.getString("springfield.default-domain")))
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(createUser)

  val createCollection = new Subcommand("create-collection") {
    descr(
      """Creates a new collection in the Springfield database. This does NOT generate a springfield-actions XML but
        |instead creates the collection in Springfield right away.
      """.stripMargin.stripLineEnd)
    val collection: ScallopOption[String] = trailArg(name = "collection",
      descr = "name for the collection",
      required = true)
    val user: ScallopOption[String] = trailArg(name = "user",
      descr = "existing user under which to store the collection",
      required = true)
    val domain: ScallopOption[String] = trailArg(name = "domain",
      descr = "the target domain in which to create the collection",
      required = false,
      default = Some(properties.getString("springfield.default-domain")))
    val title: ScallopOption[String] = opt(name = "title", short = 't',
      descr = "Title for the new collection",
      default = Some(""))
    val description: ScallopOption[String] = opt(name = "description", short = 'd',
      descr = "Description for the new collection",
      default = Some(""))
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(createCollection)

  val createPresentation = new Subcommand("create-presentation") {
    descr(
      """Creates a new, empty presentation in the Springfield database, to be populated with the add-video-to-presentation command.
      """.stripMargin.stripLineEnd)
    val user: ScallopOption[String] = trailArg(name = "user",
      descr = "existing user under which to store the collection",
      required = true)
    val domain: ScallopOption[String] = trailArg(name = "domain",
      descr = "the target domain in which to create the presentation",
      required = false,
      default = Some(properties.getString("springfield.default-domain")))
    val title: ScallopOption[String] = opt(name = "title", short = 't',
      descr = "title for the new presentation",
      default = Some(""))
    val description: ScallopOption[String] = opt(name = "description", short = 'd',
      descr = "description for the new presentation",
      default = Some(""))
    val requireTicket: ScallopOption[Boolean] = opt(name = "require-ticket", short = 'r',
      descr = "whether to require a ticket before playing the presentation (private audio/video) or not (public audio/video)"
    )
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
    val videosFolder: ScallopOption[Path] = opt(name = "videos-folder", short = 'v',
      descr = "folder relative to which to resolve the SRC column in the CSV")
    val checkParentItems: ScallopOption[Boolean] = opt(name = "check-parent-items", short = 'c',
      descr = "check that parent items (domain, user, collection) exist")
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
    val path: ScallopOption[Path] = trailArg(name = "springfield-path",
      descr = "the parent of items to change",
      required = true)
    val requireTicket: ScallopOption[String] = trailArg(name = "require-ticket",
      descr = "true or false: whether to require a ticket before playing the presentation (private audio/video) or not (public audio/video)",
      required = true)
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(setRequireTicket)

  val createTicket = new Subcommand("create-ticket") {
    descr(
      """Creates and registers an authorization ticket for a specified presentation.
        |If no ticket is specificied a random one is generated.""".stripMargin)
    val path: ScallopOption[Path] = trailArg(name = "springfield-path",
      descr = "the presentation to create the ticket for")
    val expiresAfterSeconds: ScallopOption[Long] = opt(name = "expires-after-seconds", short = 'e',
      default = Some(FIVE_MINUTES))
    val ticket: ScallopOption[String] = opt(name = "ticket", short = 't',
      descr = "the ticket to assign",
      default = None)
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(createTicket)

  val deleteTicket = new Subcommand("delete-ticket") {
    descr("Deletes a specified authorization ticket.")
    val ticket: ScallopOption[String] = trailArg(name = "ticket",
      descr = "the ticket to delete",
      required = true)
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(deleteTicket)

  val delete = new Subcommand("delete") {
    descr("Deletes the item at the specified Springfield path.")
    val path: ScallopOption[Path] = trailArg(name = "path",
      descr = "the path pointing item to remove",
      required = true)
    val withReferencedItems: ScallopOption[Boolean] = opt(name = "with-referenced-items", short = 'r',
      descr = "also remove items reference from <path>, recursively")
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(delete)

  val addVideoRefToPresentation = new Subcommand("add-videoref-to-presentation") {
    descr("Adds a videoref to a presentation under a specified name. The video must already exist in Springfield.")
    val video: ScallopOption[Path] = trailArg(name = "video",
      descr = "referid of the video",
      required = true)
    val name: ScallopOption[String] = trailArg(name = "name",
      descr = "name to assign to the video in the presentation",
      required = true)
    val presentation: ScallopOption[Path] = trailArg(name = "presentation",
      descr = "the presentation, either a Springfield path or a referid",
      required = true)
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(addVideoRefToPresentation)

  val addPresentationRefToCollection = new Subcommand("add-presentationref-to-collection") {
    descr("Adds a presentation to a collection under a specified name. The presentation must already exist in Springfield")
    val presentation: ScallopOption[Path] = trailArg(name = "presentation",
      descr = "referid of the presentation",
      required = true)
    val name: ScallopOption[String] = trailArg(name = "name",
      descr = "name to assign to the presentation in the collection",
      required = true)
    val collection: ScallopOption[Path] = trailArg(name = "collection",
      descr = "the Springfield path of the collection",
      required = true)
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(addPresentationRefToCollection)

  val addSubtitlesToVideo = new Subcommand("add-subtitles-to-video") {
    descr("Adds a subtitles file to an existing video.")
    val languageCode: ScallopOption[String] = opt(name = "language",
      descr = "the ISO 639-1 (two letter) language code")
    val video: ScallopOption[Path] = trailArg(name = "video",
      descr = "the referid of the video")
    val subtitles: ScallopOption[Path] = trailArg(name = "webvtt-file",
      descr = "path to the WebVTT subtitles file to add")
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(addSubtitlesToVideo)

  val addSubtitlesToPresentation = new Subcommand("add-subtitles-to-presentation") {
    descr(
      """
        | Adds one or more subtitles file(s) to an existing presentation. If the presentation contains multiple videos
        | the same number of WebVTT files must be specified; they will be added in the specified order to the respective videos.
      """.stripMargin)
    val languageCode: ScallopOption[String] = opt(name = "language",
      descr = "the ISO 639-1 (two letter) language code")
    val presentation: ScallopOption[Path] = trailArg(name = "presentation",
      descr = "referid of the presentation")
    val subtitles: ScallopOption[List[String]] = trailArg(name = "webvtt-file(s)", // TODO: change to List[Path] ? We shall need a valueconverter then, however.
      descr = "path to the WebVTT subtitles file(s) to add")
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(addSubtitlesToPresentation)

  footer("")
}
