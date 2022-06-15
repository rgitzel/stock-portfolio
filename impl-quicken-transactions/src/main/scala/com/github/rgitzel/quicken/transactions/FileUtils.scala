package com.github.rgitzel.quicken.transactions

import java.io.File
import scala.io.Source
import scala.util.{Failure, Try}

object FileUtils {
  def fileLines(file: File): Try[List[String]] =
    Try {
      val bufferedSource = Source.fromFile(file)
      val lines = bufferedSource.getLines.toList
      bufferedSource.close
      lines
    }
      .recoverWith(t => Failure(new Exception(s"failed to read file '${file.getName}': ${t.getMessage}")))

  def linesFromResourcePathFile(name: String): Try[List[String]] =
    this.getClass.getClassLoader.getResource(name) match {
      case null =>
        throw new IllegalArgumentException(s"can't find file '${name}")
      case resource =>
        val file = new File(resource.toURI)
        fileLines(file)
    }

}
