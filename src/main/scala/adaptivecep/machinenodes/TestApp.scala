package adaptivecep.machinenodes

import scala.collection.mutable.ListBuffer

object TestApp extends App {

  def compress(str: String): String = {
    var count: Int = 1
    val res: StringBuilder = new StringBuilder()
    var lastChar: Option[Char] = Option.empty
    for (c <- str) {
      if (lastChar.isDefined && lastChar.get == c) {
        count += 1
      } else if (lastChar.isDefined) {
        if(count>1) res.append(count)
        res.append(c)
        lastChar = Some(c)
        count = 1
      } else {
        res.append(c)
        lastChar = Some(c)
      }
    }
    if(count>1) res.append(count)
    res.toString()
  }

  def twins(a: Array[String], b: Array[String]): Array[String] = {

    var res = List.empty[String]

    def evenTwin(str1: String, str2: String):Boolean = {
      val array1 = str1.toCharArray
      val array2 = str2.toCharArray

      for (i <- 0 until array1.length) {
        if(i%2 ==0){
          for(j <- i+2 until array1.length){
            if(j%2==0){
              val tempCopy = array1.toList.toArray
              tempCopy(i) = array1(j)
              tempCopy(j)= array1(i)
              if(tempCopy.toString == str2){
                return true
              }
            }
          }
        }
      }
      false

    }

    def oddTwin(str1: String, str2: String):Boolean = {
      true
    }

    def twin(str1: String, str2: String): String = {
      if(str1.length == str2.length && (evenTwin(str1,str2) || oddTwin(str1, str2))) "Yes" else "No"
    }

    for((st1, str2) <- a zip b){
      res = res :+ twin(st1, str2)
    }
    res.toArray
  }


  twins(List("cdab","dcba").toArray, List("abcd","abd").toArray).foreach(println(_))
}
