package adaptivecep.placement

import adaptivecep.placement.manets.StarksAlgorithm

/**
  * Created by raheel arif
  * on 17/08/2017.
  */
object TransitionManager {

  def getPlacementStrategy(): PlacementStrategy = StarksAlgorithm
}
