package tryes.main

import java.io.File

import tryes.lib.Global

object TryESMain extends App{

  case class Options(options:Seq[String],hasValueNames:Seq[String],noValueNames:Seq[String]){

    private[this] val params:Map[String,Seq[String]]=init()

    private[this] def init()={
      val names1= hasValueNames.filter{name=> noValueNames.find{name2 => name == name2 }.isDefined }
      val names2= noValueNames.filter{name=> hasValueNames.find{name2 => name == name2 }.isDefined }

      if(names1.length != 0 || names2.length != 0){
        throw new IllegalArgumentException("")
      }

      val iterator = options.iterator

      def loop(params:Map[String,Seq[String]]): Map[String,Seq[String]] ={
        if(iterator.hasNext){
          val name = iterator.next()
          val newTuple=
          noValueNames.find{_ == name} match{
            case Some(name)=>
              name -> Nil
            case _=>
              hasValueNames.find{_ == name} match{
                case Some(name)=>
                  if(iterator.hasNext){
                    val value = iterator.next()
                    params.get(name).fold{
                      name -> Seq(value)
                    }{values=>
                      name -> (values :+ value)
                    }
                  }
                  else
                    name -> Nil
              }//hasValueNames.find{_ == name} match{
          }//noValueNames.find{_ == name} match{
          loop(params + newTuple)
        }
        else
          params
      }
      loop(Map.empty)
    }


    def keys()=params.keys
    def exist(name:String):Boolean=get(name).isDefined
    def apply(name:String):String=params(name).head
    def get(name:String):Option[String]=params.get(name).flatMap{seq=>
      if(seq.isDefinedAt(0))
        Some(seq(0))
      else
        None
    }
    def getOrElse(name:String,default:String)=get(name).getOrElse(default)
    def applySeq(name:String):Seq[String]=params(name)
    def getSeq(name:String):Option[Seq[String]]=params.get(name)


  }

  val WikipediaInputFile = "--wikipedia-input-file"
  val WikipediaHtmlFile = "--wikipedia-html-file"

  def buildOptions(params:Seq[String])=
    Options(options=params,
      hasValueNames = Seq(WikipediaInputFile,WikipediaHtmlFile),
      noValueNames = Seq(""))

  args.toList match{
    case "wikipedia" :: others =>

      val options = buildOptions(others)

      val csvFile=new File(options.getOrElse(WikipediaInputFile,"./files/wikipedia.csv"))
      Wikipedia.readFromInternet(csvFile,new File(options.getOrElse(WikipediaHtmlFile, "./html/wikipedia.json.gz")))

    case "load" :: others =>
      val options = buildOptions(others)
      ESLoader.load(new File(options.getOrElse(WikipediaHtmlFile, "./html/wikipedia.json.gz")))


  }
  Global.shutdown()


}
