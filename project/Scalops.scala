import sbt._

object Scalops {
  def create(): Project = {
    lazy val test = (project in file("test"))
    test
  }
}
