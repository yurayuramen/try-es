package tryes.lib

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object Global {


  implicit val actorSystem = ActorSystem()
  implicit val actorMaterializer = ActorMaterializer()
  implicit val context = actorSystem.dispatcher

  val objectMapper = new ObjectMapper().
    registerModule(DefaultScalaModule).
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false)
    //.enable(SerializationFeature.INDENT_OUTPUT)


  def shutdown(): Unit ={
    System.exit(0)
    /*
    actorSystem.terminate().onComplete{
      case _=>
        System.exit(0)
    }
    */
  }

}
