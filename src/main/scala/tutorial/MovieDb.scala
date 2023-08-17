package tutorial

import java.util.UUID

object MovieDb {

  // Movie database
  type Actor = String
  case class Movie(id: String, title: String, year: Int, actors: List[Actor], director: String)

  private val snjl: Movie = Movie(
    "6bcbca1e-efd3-411d-9f7c-14b872444fce",
    "Zack Snyder's Justice League",
    2021,
    List("Henry Cavill", "Gal Gadot", "Ezra Miller", "Ben Affleck", "Ray Fisher", "Jason Momoa"),
    "Zack Snyder"
  )

  private val database: Map[String, Movie] = Map(snjl.id -> snjl)

  def findMovieById(movieId: UUID) = database.get(movieId.toString)

  def findMoviesByDirector(director: String): List[Movie] = database.values.filter(_.director == director).toList

}
