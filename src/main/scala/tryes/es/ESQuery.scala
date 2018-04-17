package tryes.es

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.typesafe.scalalogging.Logger
import tryes.lib.{Global, WebClient}

import scala.concurrent.Future
import scala.util.Failure

object ESQuery{

  private val logger = Logger(this.getClass)

  object Results {
    case class QueryRoot(total: Int, hits: Hits)
    case class GetRoot(_index: String, _type: String, _id: String, _source: Source)
    case class Hits(total: Int, hits: Seq[Hit])
    case class Hit(_index: String, _type: String, _id: String, _score: Float, _source: Source)
    case class Source(kind: String, name: String, html: String)
  }
  val maxQueryTermsManySettings =
    Map(
      "min_term_freq" -> 1
      ,"max_query_terms" -> 20000
      ,"min_term_freq" -> 1
      ,"min_doc_freq" -> 1
    )
  val maxQueryTermsManyManySettings =
    Map(
      "min_term_freq" -> 1
      ,"max_query_terms" -> 1000000
      ,"min_term_freq" -> 1
      ,"min_doc_freq" -> 1
      ,"minimum_should_match" -> "5%"
    )
  val maxQueryTerms200Settings =
    Map(
      "max_query_terms" -> 200
    )

  val emptySettings =
    Map[String,Any]()

}


class ESQuery(host:String="localhost",port:Int=9200,protocol:String="http")(index:String,`type`:Option[String]=None,settings:Map[String,_]=ESQuery.maxQueryTerms200Settings) {


  import ESQuery.Results._
  import tryes.lib.Global.context
  import ESQuery.logger

  val mapper = new ObjectMapper().
    registerModule(DefaultScalaModule).
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false).
    enable(SerializationFeature.INDENT_OUTPUT)

  val urlPrefix = s"${protocol}://${host}:${port}"

  val client = WebClient()

  private[this] def getType(`type`:Option[String]): String =
    `type` match{
      case None =>
        this.`type` match{
          case None =>
            throw new IllegalArgumentException("typeが指定されていません")
          case Some(type2)=>
            type2
        }
      case Some(type2)=>
        type2
    }


  def put(id:String,storeInstance:Any,`type`:Option[String]=None):Future[(Int,String)] ={

    val type2=getType(`type`)
    WebClient().putHTML(s"${urlPrefix}/${index}/${type2}/${id}",storeInstance)
  }


  def bulk(`type`:Option[String]=None)(list:Seq[(String,_)])={

    val type2=getType(`type`)


    val inputString=
    list.map{case(id,entry)=>
      val json = Global.objectMapper.writeValueAsString(entry)
      s"""{ "index" : { "_index" : "${index}", "_type" : "${type2}", "_id" : "${id}" } }${"\n"}${json}${"\n"}"""
    }.mkString //("\n")

    //logger.debug(s"json:${inputString.slice(0,450)}")
    //logger.debug(s"json:${inputString.slice(inputString.length-5000,inputString.length)}")
    logger.debug(inputString)



    WebClient().postRAW(s"${urlPrefix}/_bulk",inputString).map{
      WebClient.toHTML
    }

  }




  def getByID(id:String,`type`:Option[String]=None)={
    val type2 = getType(`type`)
    val url = s"${urlPrefix}/${index}/${type2}/${id}"

    onComleteDebug(
    client.getHTML(url).map{case(status,body)=>
      mapper.readValue(body,classOf[GetRoot])
    })
  }

  //private[this] val FieldNames = Array("name")
  private[this] val FieldNames = Seq("html")


  def queryMoreLikeThisByID(fields:String*)(ids:String*)(maxQueryTerms:Int=25,`type`:Option[String]=None) ={

    val others = Map(
      "min_term_freq" -> 1
      ,"max_query_terms" -> maxQueryTerms
      ,"min_term_freq" -> 1
      ,"min_doc_freq" -> 1
    )


    val type2 = getType(`type`)

    val likeArray = ids.map{id=>
      Map( "_index" -> index , "_type" -> type2 , "_id" -> id )
    }

    val query = Map("query" -> Map(
        "more_like_this" -> Map(
          "fields"-> fields,
          "like" -> likeArray
        ).++(others) //more_like_this
      )
      ,"size" -> 10
    )
    moreLikeThisInternal(query,`type`)
  }

  def queryMoreLikeThisByLikeText(likeText:String,`type`:Option[String]=None) ={


    val query = Map(
      "query" -> Map(
        "more_like_this" -> Map(
          "fields" -> FieldNames,
          "like_text" -> likeText,
          "percent_terms_to_match" -> 0.7,
        ).++(settings)
      ) //query
      , "size" -> 100
    )
    moreLikeThisInternal(query,`type`)
  }

  private[this] def moreLikeThisInternal(query:Any,`type`:Option[String]) ={
    val type2 = getType(`type`)

    val url = s"${urlPrefix}/${index}/${`type`}/_search"

    //,"min_term_freq" -> 1
    //,"max_query_terms" -> 3000

    //println(query)


    val f = client.postHTML(url,query).map{case(_,body)=>

      val root = mapper.readValue(body, classOf[QueryRoot])

      Option(root.hits).fold{
        val hits=Hits(total=0,hits=Nil)
        root.copy(hits=hits)
      }{_=>
        root
      }

    }
    onComleteDebug(f)
  }

  /*
  def moreLikeThisByDocs(text:String) ={

    val query = Map("query" -> Map(
      "more_like_this" -> Map(
        "fields"-> FieldNames,
        "docs" -> Seq(
          "doc" -> { "html" -> text }
        )
      ).++(moreLikeThisCommonSettings)
    )
      ,"size" -> 10
    )
    moreLikeThisInternal(query)
  }
  */

  private[this] def onComleteDebug[T](future:Future[T]) ={
    future.onComplete{
      case Failure(th)=>
        println("*failure*")
        th.printStackTrace()
      case _ =>
        println("*success*")
    }
    future
  }



  def analyze(source:String)={
    val url = s"${urlPrefix}/${index}/_analyze?pretty"

    val query = Map("text" -> source)

    val f = client.postHTML(url,query).map{case(_,body)=>
        body
    }
    onComleteDebug(f)
  }


}

/*
{
  "analyzer" : "standard",
  "text" : "this is a test"
}

GET /_search
{
    "query": {
        "more_like_this" : {
            "fields" : ["title", "description"],
            "like" : "Once upon a time",
            "min_term_freq" : 1,
            "max_query_terms" : 12
        }
    }
}

 */