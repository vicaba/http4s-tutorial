package tutorial

import scala.collection.mutable

object DirectorDetailsDb {
  case class Director(firstName: String, lastName: String):
    override def toString = s"$firstName $lastName"

  case class DirectorDetails(firstName: String, lastName: String, yearOfBirth: Int)

  val database: mutable.Map[Director, DirectorDetails] = mutable.Map(
    Director("Zack", "Snyder") -> DirectorDetails("Christopher", "Nolan", 1966),
    Director("Ridley", "Scott") -> DirectorDetails("Ridley", "Scott", 1937),
    Director("Ridley", "Scott") -> DirectorDetails("Stanley", "Kubrick", 1928)
  )
}