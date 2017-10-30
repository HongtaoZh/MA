package adaptivecep.simulation.adaptive.cep

import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicInteger
import javax.management.{Attribute, ObjectName}

/**
  * Created by Raheel on 05/07/2017.
  */
object SystemLoad {

  val runningQueries: AtomicInteger = new AtomicInteger(0)

  def addQueryToSystem(): Unit = {
    runningQueries.incrementAndGet()
  }

  def migrateQueryFromSystem() = {
    runningQueries.decrementAndGet()
  }

  def isSystemOverloaded(maxLoad: Double): Boolean = {
    getSystemLoad > maxLoad
  }

  def getSystemLoad: Double = {
    val bean = ManagementFactory.getOperatingSystemMXBean()
    val mbs = ManagementFactory.getPlatformMBeanServer
    val name = ObjectName.getInstance("java.lang:type=OperatingSystem")
    val list = mbs.getAttributes(name, Array("SystemCpuLoad"))
    if (!list.isEmpty) {
      list.get(0).asInstanceOf[Attribute].getValue.asInstanceOf[Double]
    } else {
      0
    }
  }

}
