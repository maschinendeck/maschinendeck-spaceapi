import cats.effect._
import io.circe.Json
import org.http4s.blaze.server._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{HttpApp, HttpRoutes}

import java.io.{File, PrintWriter}
import java.nio.file.Paths
import scala.concurrent.ExecutionContext.global

object Main extends IOApp {

  val location: Json = Json.obj(
    "address" -> Json.fromString("Karl-Marx-Straße 18, 54290 Trier, Germany"),
    "lat" -> Json.fromDoubleOrNull(49.75294),
    "lon" -> Json.fromDoubleOrNull(6.63386),
  )

  val contact: Json = Json.obj(
    "email" -> Json.fromString("kontakt@maschinendeck.org"),
    "irc" -> Json.fromString("irc://irc.freenode.net/maschinendeck"),
    "ml" -> Json.fromString("public-subscribe@maschinendeck.org"),
    "twitter" -> Json.fromString("@maschinendeck_"),
    "discord" -> Json.fromString("https://discord.gg/e5xYxA8"),
  )

  def info(isOpen: Boolean): Json = Json.obj(
    "space" -> Json.fromString("Maschinendeck"),
    "api" -> Json.fromString("0.13"),
    "logo" -> Json.fromString("https://raw.githubusercontent.com/maschinendeck/DesignPropositions/master/logo_maschinendeck_final.svg?sanitize=true"),
    "url" -> Json.fromString("https://maschinendeck.org"),
    "location" -> location,
    "contact" -> contact,
    "issue_report_channels" -> Json.arr(Json.fromString("twitter")),
    "state" -> Json.obj("open" -> Json.fromBoolean(isOpen)),
    "project" -> Json.arr(Json.fromString("https://wiki.maschinendeck.org")),
  )

  val homeDir: String = System.getProperty("user.home")
  val openStateFile: File = Paths.get(homeDir, ".maschinenstate.is-open").toFile

  def writeIsOpen(isOpen: Boolean): IO[Unit] = IO.delay {
    val txt = if (isOpen) "1" else "0"
    val pw = new PrintWriter(openStateFile)
    pw.write(txt)
    pw.close()
  }

  def readIsOpen(): IO[Boolean] = {
    val parseIsOpen: IO[Option[Boolean]] = IO.delay {
      if (openStateFile.exists()) {
        val source = scala.io.Source.fromFile(openStateFile)
        val txt = source.getLines().mkString
        source.close()

        txt match {
          case "1" => Some(true)
          case "0" => Some(false)
          case _ => None
        }
      } else None
    }

    for {
      isOpenOpt <- parseIsOpen
      isOpen <- {
        isOpenOpt match {
          case Some(isOpen) => IO.pure(isOpen)
          case None => writeIsOpen(false) >> IO.pure(false)
        }
      }
    } yield {
      isOpen
    }
  }

  def spaceApiService(isOpenRef: Ref[IO, Boolean]): HttpApp[IO] = {
    HttpRoutes.of[IO] {
      case GET -> Root =>
        isOpenRef.get.flatMap(isOpen => Ok(info(isOpen)))

      case POST -> Root / "status" / "open" / "set" =>
        isOpenRef.set(true) >> writeIsOpen(true)  >> Ok()

      case POST -> Root / "status" / "open" / "unset" =>
        isOpenRef.set(false) >> writeIsOpen(false) >> Ok()
    }.orNotFound
  }

  def run(args: List[String]): IO[ExitCode] = {
    for {
      isOpen <- readIsOpen()
      isOpenRef <- Ref.of[IO, Boolean](isOpen)
      _ <- {
        BlazeServerBuilder[IO](global)
          .bindHttp(5000, "localhost")
          .withHttpApp(spaceApiService(isOpenRef))
          .serve
          .compile
          .drain
      }
    } yield {
      ExitCode.Success
    }
  }

}