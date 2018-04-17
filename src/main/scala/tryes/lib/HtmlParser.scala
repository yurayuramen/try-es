package tryes.lib

import java.util.regex.{Matcher, Pattern}

object HtmlParser {

  /***
    * スクリプトタグとその中身を取り除く
    * @param html
    * @return
    */
  def stripScript(html:String):String={
    val ptnScript = Pattern.compile("<script.*?</script>",Pattern.DOTALL)
    ptnScript.matcher(html).replaceAll("")
  }

  /***
    * bodyタグの中身のみを取り出す
    *
    * @param html
    * @return
    */
  def extractBody(html:String):String={
    val ptnBody = Pattern.compile(".*?<body.*?>(.+)</body>.*?",Pattern.DOTALL)
    val matcher = ptnBody.matcher(html)
    if(matcher.matches())
      matcher.group(1)
    else
      ""
  }

  /***
    * titleタグの中身を取り出す
    *
    * @param html
    * @return
    */
  def extractTitle(html:String):String={
    val ptn = Pattern.compile(".*?<title.*?>(.*?)</title>.*?",Pattern.DOTALL)
    val matcher = ptn.matcher(html)
    if(matcher.matches())
      matcher.group(1)
    else
      ""
  }

  /****
    * HTMLタグ（開始タグ、終了タグ）を取り除く
    *
    * @param html
    * @return
    */
  def stripHtmlTag(html:String):String=
    html.
      //
      replaceAll("<a.+?>(.+?)</a>","$1").
      replaceAll("<[^/]{1}[^>]*?(/>|>)"," ").
      replaceAll("</[^>]+?>"," ")



  /*
  val ptnScript = Pattern.compile("<script.*?</script>",Pattern.DOTALL)
  val ptnBody = Pattern.compile(".*?<body.*?>(.+)</body>.*?",Pattern.DOTALL)
  //val ptnAnchor = Pattern.compile("<a.+?>(.+)</a>",Pattern.DOTALL)

  def extract(source:String) ={

    def matcherCheck(matcher:Matcher): Unit ={
      if(matcher.matches()) {
        println("**match!!**")
        (0 to matcher.groupCount()).foreach { groupNum =>
          val groupText = matcher.group(groupNum)
          println(s"${groupNum}:${groupText.length}\n---------\n${groupText.slice(0, 100)}\n---------\n${groupText.slice(groupText.length - 200, groupText.length)}\n========\n")
        }
      }else
        println("**unmatched**")
    }



    val excludeScript = source //ptnScript.matcher(source).replaceAll("")

    def body(): String ={
      val matcher = ptnBody.matcher(excludeScript)
      println(s"\n---------\n${excludeScript.substring(0,100)}\n---------\n${excludeScript.substring(excludeScript.length - 200,excludeScript.length)}\n========\n")
      //matcherCheck(matcher)

      if(matcher.matches()){
        (0 to matcher.groupCount()).foreach{groupNum=>
          val groupText = matcher.group(groupNum)
          println(s"${groupNum}:${groupText.length}\n---------\n${groupText.substring(0,100)}\n---------\n${groupText.substring(groupText.length - 200,groupText.length)}\n========\n")
        }

        val body = matcher.group(1)
        //body.replaceAll("<a.+?>(.+?)</a>","$0")

        //.replaceAll("</?[^>]+?/?>"," ")
        body.replaceAll("<a.+?>(.+?)</a>","$1").
          replaceAll("<[^/]{1}[^>]*?(/>|>)"," ").
          replaceAll("</[^>]+?>"," ")
        //body
      }
      else
        ""
    }

    def title():String={
      val matcherTitle = Pattern.compile(".*?<title.*?>(.*?)</title>.*?",Pattern.DOTALL).matcher(source)
      matcherCheck(matcherTitle)
      if(matcherTitle.matches())
        matcherTitle.group(1)
      else
        ""
    }




    (title(),body())
  }

  */
}
