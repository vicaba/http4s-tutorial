package tutorial

// https://www.youtube.com/watch?v=v_gv6LsWdT0&t=115s min 35

import cats.*
import cats.effect.*
import cats.implicits.*
import com.comcast.ip4s.{ipv4, port}
import org.http4s.circe.*
import org.http4s.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.*
import org.http4s.implicits.*
import org.http4s.server.*
import org.http4s.server.middleware.Logger
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}
import tutorial.MovieDb.*
import tutorial.DirectorDetailsDb.*

import java.time.Year
import scala.util.Try

object Http4sTutorial extends IOApp {

  // Request -> F[Option[Response]]
  // HttpRoutes[F]

  // HttpRoutes[F] = Request => F[Option[Response]]

  given QueryParamDecoder[Year] =
    QueryParamDecoder[Int].emap(yearInt => Try(Year.of(yearInt)).toEither.leftMap(e => ParseFailure(e.getMessage, "")))

  object DirectorQueryParamMatcher extends QueryParamDecoderMatcher[String]("director")

  object YearQueryParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[Year]("year")
  // Get /movies?director=Quentin%20Tarantino

  def movieRoutes[F[_]: Monad]: HttpRoutes[F] =
    val dsl = Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] {
      case GET -> Root / "movies" :? DirectorQueryParamMatcher(director) +& YearQueryParamMatcher(maybeYear) =>
        val moviesByDirector = findMoviesByDirector(director)

        println(director)

        maybeYear match
          case Some(validatedYear) =>
            validatedYear.fold(
              _ => BadRequest("The year was badly formatted"),
              year =>
                val moviesByDirectorAndYear = moviesByDirector.filter(_.year == year.getValue)
                Ok(moviesByDirectorAndYear.asJson)
            )
          case None => Ok(moviesByDirector.asJson)
      case GET -> Root / "movies" / UUIDVar(movieId) / "actors" =>
        findMovieById(movieId).map(_.actors) match
          case Some(actors) => Ok(actors.asJson)
          case _ => NotFound(s"No movie with id '$movieId' found")
    }

  object DirectorPath {
    def unapply(str: String): Option[Director] = {
      str.split(" ").toList match
        case firstName :: lastName :: Nil => Some(Director(firstName, lastName))
        case _ => None
    }
  }

  def directorRoutes[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] {
      case GET -> Root / "directors" / DirectorPath(director) => database.get(director) match
          case Some(details) => Ok(details.asJson)
          case None => NotFound(s"No director '$director' found")
    }
  }

  def allRoutes[F[_]: Monad]: HttpRoutes[F] = movieRoutes[F] <+> directorRoutes[F]

  def allRoutesComplete[F[_]: Monad]: HttpApp[F] = allRoutes[F].orNotFound

  override def run(args: List[String]): IO[ExitCode] = {
    val apis = Router(
      "/api" -> movieRoutes[IO],
      "/api/admin" -> directorRoutes[IO]
    ).orNotFound

    import org.typelevel.log4cats._
    import org.typelevel.log4cats.slf4j._

    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(allRoutesComplete)
      .build
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }
}
