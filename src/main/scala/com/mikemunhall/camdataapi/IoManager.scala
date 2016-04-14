package com.mikemunhall.camdataapi

import java.io.File
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Paths}
import java.io.{FileInputStream, FileOutputStream}
import akka.actor.{Actor, Props}
import akka.http.scaladsl.server.directives.FileInfo

object IoManager {
  // TODO: Externalize these
  val currentImagesPath = "/Users/mikemunhall/Documents/workspace/cam-data-api/images/current"
  val savedImagesPath = "/Users/mikemunhall/Documents/workspace/cam-data-api/images/current"
  val currentImagesWebPath = "/images/current"
  val savedImagesWebPath = "/images/saved"

  case class ImageRecord(path: String, name: String, createDate: String)
  case class ImageRecords(images: List[ImageRecord])
  case object GetRecords
  case class SaveRecord(metadata: FileInfo, file: File)

  def props = Props[IoManager]
}

class IoManager extends Actor {
  import IoManager._

  def receive = {
    case GetRecords =>
      val records = new File(currentImagesPath).listFiles.filter(_.isFile).toList.filter { file =>
        file.getName.endsWith(".jpg")
      }.map { file =>
        val f = Paths.get(file.getAbsolutePath())
        val attrs = Files.readAttributes(f, classOf[BasicFileAttributes])
        ImageRecord(currentImagesWebPath, file.getName, attrs.lastModifiedTime().toString())
      }

      sender() ! ImageRecords(records)

    case SaveRecord(metadata, file) =>
      val newFilePath = currentImagesPath + '/' + metadata.fileName
      val in = new FileInputStream(file.getPath())
      val out = new FileOutputStream(newFilePath)
      var c = 0
      while ({c = in.read; c != -1}) {
        out.write(c)
      }
      val f = Paths.get(newFilePath)
      val attrs = Files.readAttributes(f, classOf[BasicFileAttributes])

      sender() ! ImageRecord(currentImagesWebPath, metadata.fileName, attrs.lastModifiedTime().toString())
  }
}