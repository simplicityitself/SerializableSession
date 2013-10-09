package serializablesession

class TestController {

  def index = {
    session["Simple"] = "Something serial"
  }

  def fail = {
    session["Simple"] = new BadClass("WibbleWibbleWibbleWibbleWibbleWibbleWibbleWibble${System.currentTimeMillis()}")
    session["Simple2"] = new BadClass("WibbleWibbleWibbleWibbleWibbleWibbleWibbleWibble${System.currentTimeMillis()}")
    session["Simple3"] = new BadClass("WibbleWibbleWibbleWibbleWibbleWibbleWibbleWibble${System.currentTimeMillis()}")
    session["Simple4"] = new BadClass("WibbleWibbleWibbleWibbleWibbleWibbleWibbleWibble${System.currentTimeMillis()}")
    session["Simple4"] = new BadClass("WibbleWibbleWibbleWibbleWibbleWibbleWibbleWibble${System.currentTimeMillis()}")
    session["Simple5"] = new BadClass("WibbleWibbleWibbleWibbleWibbleWibbleWibbleWibble${System.currentTimeMillis()}")
    session["Simple6"] = new BadClass("WibbleWibbleWibbleWibbleWibbleWibbleWibbleWibble${System.currentTimeMillis()}")
    session["Simple7"] = new BadClass("WibbleWibbleWibbleWibbleWibbleWibbleWibbleWibble${System.currentTimeMillis()}")
  }
}

class BadClass {
  def someName
  BadClass(String name) {
    someName = name
  }
}
