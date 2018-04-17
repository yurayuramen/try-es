package tryes.lib

import java.io._
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import com.typesafe.scalalogging.Logger

object Files {

  private[this] val logger = Logger(this.getClass)

  def using[T1 <: { def close():Any},T2](hasClose:T1)(func:T1=>T2):T2=
    try
      func(hasClose)
    finally
      try hasClose.close()
      catch{
        case _:Throwable=>
      }

  implicit class FileExt(file:File){
    def writeALLString(source:String,enc:String="UTF-8"): Unit =
    {
      val os=
        if(file.getName.endsWith(".gz"))
          new GZIPOutputStream(new FileOutputStream(file))
        else
          new FileOutputStream(file)

      using(new OutputStreamWriter(os,enc)){
        _.write(source)
      }
    }

    def readALLString(enc:String="UTF-8"): String ={

      if(file.getName.endsWith(".gz")) {
        val BufferSize = 1024
        using(new CharArrayWriter()){writer=>
          using(new InputStreamReader(new GZIPInputStream(new FileInputStream(file)),enc)) { reader =>
            def read(): Unit ={
              val array = Array.fill[Char](BufferSize)(0)
              val ret = reader.read(array, 0, BufferSize)
              if(ret != -1){
                writer.write(array,0,ret)
                read()
              }
            }
            read()
          }
          writer.flush()
          writer.toString
        }
      }
      else {
        val size = file.length().toInt
        val array:Array[Byte] = Array.fill(size)(0)
        using(new FileInputStream(file)) { is =>
          is.read(array, 0, size)
        }
        new String(array,enc)
      }
    }

  }




}
