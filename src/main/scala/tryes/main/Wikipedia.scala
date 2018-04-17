package tryes.main

import java.io.File
import java.net.URLEncoder

import com.typesafe.scalalogging.Logger
import tryes.lib.{Files, Global, WebClient}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.Source

case class WikipediaEntry(kind:String,id:String,name:String,status:Int,html:String)


object Wikipedia {

  private[this] val logger = Logger(this.getClass)


  def readFromInternet(csvFile:File,cacheFile:File) ={

    import Files._
    import Global.context
    def readCSV(): Seq[(String,String,String)] = {

      val data = csvFile.readALLString()

      Source.fromString(data).getLines().flatMap {
        _.split(",", 3) match {
          case Array(kind, id, name) =>
            Some(kind, id, name)
          case _ =>
            None
        }
      }.toSeq
    }
    val listIN = readCSV()
    val webClient = WebClient()

    val f=
    Future sequence listIN.map{case(kind,id,name)=>

      val url = s"https://ja.wikipedia.org/wiki/${URLEncoder.encode(name,"utf-8")}"

      logger.info(s"URL-START:${url}")
      webClient.getHTML(url).map{case(status,html)=>
        logger.info(s"URL-FIN:${status}:${url}")
        WikipediaEntry(kind,id,name,status,html)
      }
    }



    val listOUT = Await.result(f,Duration.Inf)

    val json = Global.objectMapper.writeValueAsString(listOUT)

    import Files._
    cacheFile.writeALLString(json)

  }


}
