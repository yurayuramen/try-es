package tryes.main

import java.io.File

import com.fasterxml.jackson.core.`type`.TypeReference
import com.typesafe.scalalogging.Logger
import tryes.es.ESQuery
import tryes.lib.{Files, Global, HtmlParser}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

case class ESEntry(name:String,kind:String,html:String,body:String
                   ,html_tag_strip:String,html_script_strip:String,html_all_strip:String
                   ,body_tag_strip:String,body_script_strip:String,body_all_strip:String
                  )

object ESLoader {

  private[this] val logger = Logger(this.getClass)

  def load(cacheFile:File): Unit =
  {
    import Files._
    import Global.context
    val json = cacheFile.readALLString()

    println(json.indexOf(0))
    val json2=json.replace(0.toChar,' ')
    println(json2.indexOf(0))

    val list:Seq[WikipediaEntry] = Global.objectMapper.readValue(json2,new TypeReference[Seq[WikipediaEntry]] {})

    val list2 = list.map{anEntry=>
      
      val html = anEntry.html
      val html_tag_strip = HtmlParser.stripHtmlTag(html)
      val html_script_strip = HtmlParser.stripScript(html)
      val html_all_strip = HtmlParser.stripHtmlTag(html_script_strip)

      val body = HtmlParser.extractBody(html)
      val body_tag_strip = HtmlParser.stripHtmlTag(body)
      val body_script_strip = HtmlParser.stripScript(body)
      val body_all_strip = HtmlParser.stripHtmlTag(body_script_strip)

      val id = s"${anEntry.kind}-${anEntry.id}"

      /*
      implicit class StringExt(source:String){
        def shrink:String={
          source.slice(0,100)
        }

      }

      id -> ESEntry(name = anEntry.name , kind = anEntry.kind
        ,html=html.shrink,html_script_strip = html_script_strip.shrink, html_all_strip = html_all_strip.shrink, html_tag_strip=html_tag_strip.shrink
        ,body=body.shrink,body_script_strip = body_script_strip.shrink, body_all_strip = body_all_strip.shrink, body_tag_strip=body_tag_strip.shrink
      )
      */
      id -> ESEntry(name = anEntry.name , kind = anEntry.kind
        ,html=html,html_script_strip = html_script_strip,html_all_strip = html_all_strip,html_tag_strip=html_tag_strip
        ,body=body,body_script_strip = body_script_strip,body_all_strip = body_all_strip,body_tag_strip=body_tag_strip
      )

    }



    val es = new ESQuery()("try-mlt",Some("_doc"))

    def put(): Unit ={
      val z = list2.map{case(id,entry)=>
        es.put(id,entry)
      }
      val f = Future sequence z
      val result = Await.result(f,Duration.Inf)
      logger.info(s"${result.map{row=>row}.mkString("\n")}")
    }
    def putSingleThread(): Unit ={

      list2.foreach{case(id,entry)=>
        val f = es.put(id,entry)
        val result = Await.result(f,Duration.Inf)
        logger.info(s"${result}")

      }

    }
    def bulk(): Unit ={

      def run(list:Seq[(String,_)]): Unit ={

        val (listA,listB)=list.splitAt(10)
        val f = es.bulk()(listA)
        val result = Await.result(f,Duration.Inf)
        logger.info(s"status:${result._1}")
        logger.info(s"result:${result._2}")

        if(listB.length!=0)
          run(listB)
      }
      run(list2)

    }
    //bulk()
    //put()
    putSingleThread()
  }


}
