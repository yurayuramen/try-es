import com.fasterxml.jackson.core.`type`.TypeReference
import org.scalatest.{MustMatchers, WordSpec}
import tryes.es.ESQuery
import tryes.es.ESQuery.Results
import tryes.es.ESQuery.Results.TryMLT
import tryes.lib.{Global, WebClient}

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class MLTSpec extends WordSpec with MustMatchers {

  implicit val typeRefGet = new TypeReference[Results.GetRoot[TryMLT]] {}
  implicit val typeRefRoot = new TypeReference[Results.QueryRoot[TryMLT]] {}
  "by likeid" should{

    def mlt(field:String,id:String,maxQueryTerms:Int=25)={

      import Global.context

      val q = new ESQuery()("try-mlt","_doc")
      val fResult = q.queryMoreLikeThisByID(field)(id)(maxQueryTerms = maxQueryTerms).map{ root=>
        root.hits.fold(Nil:Seq[String]){_.hits.map{ item=>item._id}}
      }

      val list = Await.result(fResult,Duration.Inf)

      list.zipWithIndex.foreach{case(item,index)=>println(s"${id}:${field}:${index}:${item}")}
      //Global.shutdown()
    }

    "1" in{
      mlt("body.kuromoji","pref-13")
      mlt("body.neologd","pref-13")
      mlt("body_all_strip.kuromoji","pref-13")
      mlt("body_all_strip.neologd","pref-13")
    }

  }



  "by liketext" should{

    def mlt(field:String,id:String)={

      import Global.context

      val q = new ESQuery()("try-mlt","_doc")
      val fResult=
        q.getByID(id).flatMap{root=>


          val html = root._source.body_all_strip
          q.queryMoreLikeThisByLikeText(field)(html).map{root=>
            root.hits.hits.getOrElse(Nil).map{hit=>
              hit._id
            }
          }
        }

      val list = Await.result(fResult,Duration.Inf)

      list.zipWithIndex.foreach{case(item,index)=>println(s"${id}:${index}:${item}")}
      //Global.shutdown()
    }

    "1" in{
      mlt("body.kuromoji","pref-13")
    }

  }

}
