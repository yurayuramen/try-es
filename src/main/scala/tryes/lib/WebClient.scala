package tryes.lib

import akka.actor.{ActorSystem, Terminated}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.api.libs.ws.{BodyWritable, InMemoryBody, StandaloneWSResponse}

import scala.concurrent.{ExecutionContext, Future}

object WebClient{

  private[this] lazy val client = new WebClientImpl()

  def apply():WebClient=client

  def toHTML(response:StandaloneWSResponse) =
    //import play.api.libs.ws.DefaultBodyReadables._
    response.status -> response.body

  private class WebClientImpl extends WebClient {

    import Global._


    private[this] val mapper = new ObjectMapper().registerModule(DefaultScalaModule)

    private[this] val wsClient = StandaloneAhcWSClient()

    private[this] implicit val urlBodyWritable = BodyWritable[Any]({ any =>

      val json = mapper.writeValueAsString(any)
      val byteString = ByteString.fromString(json)
      InMemoryBody(byteString)
    }, "application/json")

    private[this] implicit val urlBodyWritable2 = BodyWritable[String]({ source =>

      val byteString = ByteString.fromString(source)
      InMemoryBody(byteString)
    }, "application/json")



    override def get(url:String)=wsClient.url(url).get()
    override def put(url:String,body:Any)=wsClient.url(url).put(body)
    override def post(url:String,body:Any)=wsClient.url(url).post(body)
    override def postRAW(url:String,body:String)=wsClient.url(url).withBody(ByteString(body.getBytes("utf-8"))).execute("POST")

    override def getHTML(url:String)=wsClient.url(url).get().map(WebClient.toHTML)
    override def putHTML(url:String,body:Any)=wsClient.url(url).put(body).map(WebClient.toHTML)
    override def postHTML(url:String,body:Any)=wsClient.url(url).post(body).map(WebClient.toHTML)


  }




}

trait WebClient{

  def get(url:String):Future[StandaloneWSResponse]
  def put(url:String,body:Any):Future[StandaloneWSResponse]
  def post(url:String,body:Any):Future[StandaloneWSResponse]
  def getHTML(url:String):Future[(Int,String)]
  def putHTML(url:String,body:Any):Future[(Int,String)]
  def postHTML(url:String,body:Any):Future[(Int,String)]
  def postRAW(url:String,body:String):Future[StandaloneWSResponse]


}
