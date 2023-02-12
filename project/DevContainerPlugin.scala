package com.andrzejressel.build

import sbt.*
import Keys.*
import java.nio.file.Paths
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.core.util.DefaultIndenter.SYSTEM_LINEFEED_INSTANCE;
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.SerializationFeature

object DevContainerPlugin extends AutoPlugin {

  object autoImport {
    val generateDevContainer = taskKey[Unit]("Generate dev container files")
  }

  val HOME = "/home/vscode"

  val project_name = "scalops"
  case class Volume(
      val name: String,
      val path: java.nio.file.Path
  )

  import autoImport.*

  override def projectSettings: Seq[Setting[?]] = Seq(
    generateDevContainer := {
      val state                = Keys.state.value
      val extracted: Extracted = Project.extract(state)
      import extracted.*
      val rootId   = (extracted.rootProject(extracted.structure.root))
      val root     = extracted.structure.allProjects.find(_.id == rootId).get
      val rootBase = root.base.toPath()
      val devcontainerFile =
        rootBase.resolve(".devcontainer/devcontainer.json").toAbsolutePath()

      val subprojectTargetPaths = extracted.structure.allProjects
        .map(_.base.toPath())
        .map(_.resolve("target"))

      val additionalPaths = Seq(
        Paths.get(s"$HOME/.ivy2"),
        Paths.get(s"$HOME/.cache"),
        Paths.get(".bsp"),
        Paths.get(".bloop"),
        Paths.get(".metals"),
        Paths.get("project", "project"),
        Paths.get("project", "target"),
        Paths.get("project", ".bloop")
      )

      val paths =
        (subprojectTargetPaths ++ additionalPaths)
          .map(_.toAbsolutePath())
          .sorted

      val volumes = paths.map(p =>
        Volume(
          s"${project_name}${p.toString().replace('/', '-')}",
          p
        )
      )

      val mounts = volumes.map {
        case Volume(name, path) => {
          s"source=${name},target=${path},type=volume"
        }
      }

      val postCreateCommand =
        volumes
          .map { case Volume(_, path) => s"sudo chown vscode ${path}" }
          .mkString(" && ")

      val devcontainerJson = Map(
        "name" -> "Scala",
        "build" -> Map(
          "dockerfile" -> "Dockerfile",
          "args" -> Map(
            "VERSION"      -> "0.205.9",
            "VARIANT"      -> "bullseye",
            "JAVA_VERSION" -> "17",
            "SBT_VERSION"  -> "1.8.2"
          )
        ),
        "customizations" -> Map(
          "vscode" -> Map(
            "extensions" -> Seq(
              "scalameta.metals"
            )
          )
        ),
        "mounts"            -> mounts,
        "postCreateCommand" -> postCreateCommand,
        "remoteUser"        -> "vscode"
      )

      val mapper = new ObjectMapper()
        .registerModule(DefaultScalaModule)
        .enable(SerializationFeature.INDENT_OUTPUT);
      val prettyPrinter = new DefaultPrettyPrinter();
      prettyPrinter.indentArraysWith(SYSTEM_LINEFEED_INSTANCE);
      mapper.setDefaultPrettyPrinter(prettyPrinter);

      val result = mapper
        .writeValueAsString(devcontainerJson)
        .replaceAll("\" :", "\":")

      IO.write(
        devcontainerFile.toFile(),
        result
      )

    }
  )

}
