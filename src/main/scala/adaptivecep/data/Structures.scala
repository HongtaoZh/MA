package adaptivecep.data

/**
  * Created by mac on 05/07/2017.
  */
object Structures {

  case class MachineLoad private(value: Double) extends AnyVal {
    def compareTo(other: MachineLoad) = {
      value - other.value
    }
  }

  object MachineLoad {
    def make(x: Double) =
      if (x >= 0 && x <= 1)
        MachineLoad(x)
      else
        throw new RuntimeException("X must be between 0 and 1")
  }

}
