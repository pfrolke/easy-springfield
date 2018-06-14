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
import java.util.UUID
import better.files.File

import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.io.StdIn
import scala.util.{ Failure, Success, Try }
import scala.xml.PrettyPrinter

object Command extends App
  with DebugEnhancedLogging
  with EasySpringfieldApp
  with Smithers2
  with ListUsers
  with GetStatus
  with CreateSpringfieldActions
  with Ticket {

  import scala.language.reflectiveCalls

  type FeedBackMessage = String

  val configuration = Configuration(File(System.getProperty("app.home")))
  val opts = new CommandLineOptions(args, properties, configuration.version)
  opts.verify()

  val result: Try[FeedBackMessage] = opts.subcommand match {
    case Some(cmd @ opts.listUsers) =>
      debug("Calling list-users")
      getUserList(cmd.domain()).map(_.mkString(", "))
    case Some(cmd @ opts.createUser) =>
      createUser(cmd.user(), cmd.targetDomain()).map(_ => s"User created: ${ cmd.user() }")
    case Some(cmd @ opts.createCollection) =>
      createCollection(cmd.collection(), cmd.title(), cmd.description(), cmd.user(), cmd.targetDomain()).map(_ => s"Collection created: ${ cmd.collection() }")
    case Some(cmd @ opts.createPresentation) =>
      createPresentation(cmd.title(), cmd.description(), cmd.requireTicket(), cmd.user(), cmd.targetDomain()).map(referid => s"Presentation created: $referid")
    case Some(cmd @ opts.createSpringfieldActions) =>
      val result = for {
        videos <- parseCsv(cmd.videosCsv())
        _ <- if (cmd.videosFolder.isSupplied) checkSourceVideosExist(videos, cmd.videosFolder())
             else Success(())
        parentsToCreate <- if (cmd.checkParentItems())
                             getParentPaths(videos)
                               .map(checkPathExists)
                               .collectResults
                               .map(_.filterNot(_._2).map(_._1))
                           else Success(Set[Path]())
        actions <- createSpringfieldActions(videos)
      } yield (new PrettyPrinter(160, 2).format(actions), parentsToCreate)
      result.map { case (s, ps) =>
        println(s)
        "XML generated." + (if (!cmd.videosFolder.isSupplied) " (Existence of files has NOT been checked!)"
                            else "") +
          (if (cmd.checkParentItems()) {
            "\nParent items have been checked: " +
              (if (ps.isEmpty) "OK"
               else "not existing yet:\n" + ps.mkString("\n"))
          }
           else "\nParent items have been NOT been checked.")
      }
    case Some(cmd @ opts.status) =>
      val TABS = "%-35s %-40s %-7s %-10s\n"
      val maybeList =
        if (cmd.user.toOption.isDefined) {
          getStatusSummaries(cmd.domain(), cmd.user())
            .map(_.map(s => TABS format(s.user, s.filename, s.requireTicket, s.status.toUpperCase)).mkString)
        }
        else {
          getUserList(cmd.domain())
            .map {
              _.map {
                user =>
                  getStatusSummaries(cmd.domain(), user)
                    .map(_.map(s => TABS format(s.user, s.filename, s.requireTicket, s.status.toUpperCase)).mkString)
                    .recover { case _ => TABS format(user, "*** COULD NOT RETRIEVE DATA ***", "") }.get
              }.mkString
            }
        }

      maybeList.map {
        list =>
          "\n" +
            (TABS format("USER", "VIDEO", "PRIVATE", "STATUS")) +
            (TABS format("=" * "USER".length, "=" * "VIDEO".length, "=" * "PRIVATE".length, "=" * "STATUS".length)) +
            list
      }
    case Some(cmd @ opts.setRequireTicket) =>
      for {
        videos <- getReferencedPaths(cmd.path()).map(_.filter(p => p.getNameCount > 1 && p.getName(p.getNameCount - 2).toString == "video"))
        _ <- approveAction(videos,
          s"""
             |WARNING: THIS ACTION COULD EXPOSE VIDEOS TO UNAUTHORIZED VIEWERS.
             |These videos will be set to require-ticket = ${ cmd.requireTicket() }
             |
             |(Note that you may have to clear your browser cache after making videos private to effectively test the result.)
           """.stripMargin)
        _ <- videos.map(setRequireTicket(_, cmd.requireTicket().toBoolean)).collectResults
      } yield s"Video(s) set to require-ticket = ${ cmd.requireTicket() }"
    case Some(cmd @ opts.createTicket) =>
      createTicket(getCompletePath(cmd.path()), cmd.ticket.toOption.getOrElse(UUID.randomUUID.toString), cmd.expiresAfterSeconds()).map(_ => "Ticket created.")
    case Some(cmd @ opts.deleteTicket) =>
      deleteTicket(cmd.ticket()).map(_ => "Ticket deleted.")
    case Some(cmd @ opts.delete) =>
      for {
        list <- if (cmd.withReferencedItems()) getReferencedPaths(cmd.path()).map(_ :+ getCompletePath(cmd.path()))
                else Success(Seq(cmd.path()))
        _ <- approveAction(list, """These items will be deleted.""")
        _ <- list.map(deletePath).collectResults
      } yield "Items deleted"
    case Some(cmd @opts.addVideoToPresentation) =>
      addVideoRefToPresentation(getCompletePath(cmd.video()), cmd.name(), cmd.presentation()).map(_ => "Video reference added.")
    case Some(cmd @opts.addPresentationToCollection) =>
      addPresentationRefToCollection(getCompletePath(cmd.presentation()), cmd.name(), cmd.collection()).map(_ => "Presentation reference added.")
    case _ => throw new IllegalArgumentException(s"Unknown command: ${ opts.subcommand }")
      Try { "Unknown command" }
  }

  result.map(msg => Console.err.println(s"OK: $msg"))
    .doIfFailure { case e => Console.err.println(s"FAILED: ${ e.getMessage }") }

  private def getUserList(domain: String): Try[Seq[String]] = {
    for {
      xml <- getXmlFromPath(Paths.get("domain", domain, "user"))
      users <- Try { listUsers(xml) }
    } yield users
  }

  private def getStatusSummaries(domain: String, user: String): Try[Seq[VideoStatusSummary]] = {
    for {
      xml <- getXmlFromPath(Paths.get("domain", domain, "user", user, "video"))
      summaries <- Try { getStatus(user, xml) }
      _ = debug(s"Retrieved status summaries: $summaries")
    } yield summaries
  }

  private def approveAction(list: Seq[Path], msg: String): Try[Seq[Path]] = {
    println("The following items will be processed:")
    list.foreach(println)
    println(msg)
    print("OK? (y/n): ")
    if (StdIn.readLine().toLowerCase == "y") Success(list)
    else Failure(new Exception("User aborted action"))
  }
}
