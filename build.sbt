lazy val root = (project in file(".")).
  settings(
    name := "ElectionLeader",
    version := "1.0",
    scalaVersion := "2.11.4"
  )



libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.0"
libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.4.0"
