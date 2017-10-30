package adaptivecep.graph.nodes.traits

import org.wso2.siddhi._
/**
  * Created by manishaluthra on 14.10.2017.
  */
trait SiddhiEngine {

  val siddiMgr = new SiddhiManager
  val execPlanRuntime = siddiMgr.createExecutionPlanRuntime


}
