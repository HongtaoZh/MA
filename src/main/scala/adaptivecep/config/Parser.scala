package adaptivecep.config

/**
  * Created by anjaria on 31.07.17.
  * @see https://stackoverflow.com/a/3183991/665905
  */
class Parser extends App {
  val usage = """
    Usage: PublisherApp|BrokerApp|SubscriberApp --ip ip-address --port portNum [--name A|B|C|D]
              """

  if (args.length == 0) println(usage)

  val argList = args.toList
  type OptionMap = Map[Symbol, Any]

  def nextOption(map : OptionMap, list: List[String]) : OptionMap = {
    list match {
      case Nil => map
      case "--ip" :: value :: tail =>
        nextOption(map ++ Map('ip -> value), tail)
      case "--port" :: value :: tail =>
        nextOption(map ++ Map('port -> value.toInt), tail)
      case "--name" :: value :: tail =>
        nextOption(map ++ Map('name -> value), tail)
      case option :: tail => println("Unknown option " + option); Map()
    }
  }
  val options = nextOption(Map(),argList)
  println(options)
}
