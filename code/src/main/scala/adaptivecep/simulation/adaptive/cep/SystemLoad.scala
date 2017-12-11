package adaptivecep.simulation.adaptive.cep

import java.util.concurrent.atomic.AtomicInteger

/**
  * Created by Raheel on 05/07/2017.
  */
object SystemLoad {

  @volatile
  var runningOperators: AtomicInteger = new AtomicInteger(0)

  val maxOperators = 100d

  def newOperatorAdded(): Unit = {
    runningOperators.incrementAndGet()
  }

  def operatorRemoved(): Unit = {
    runningOperators.decrementAndGet()
  }

  def isSystemOverloaded(currentLoad: Double, maxLoad: Double): Boolean = {
    currentLoad > maxLoad
  }

  def getSystemLoad: Double = {
    runningOperators.get()/maxOperators
    /*
    using running queries as system load, as running a new actor on a machine doesn't
    have much impact on system load, hence comparison results donot show the impact of running
    new operator on the system
    val bean = ManagementFactory.getOperatingSystemMXBean()
    val mbs = ManagementFactory.getPlatformMBeanServer
    val name = ObjectName.getInstance("java.lang:type=OperatingSystem")
    val list = mbs.getAttributes(name, Array("SystemCpuLoad"))
    if (!list.isEmpty) {
      list.get(0).asInstanceOf[Attribute].getValue.asInstanceOf[Double]
    } else {
      0
    }
    */
  }

}
