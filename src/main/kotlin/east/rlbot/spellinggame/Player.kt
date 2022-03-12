package east.rlbot.spellinggame

class Player(val index: Int, objectiveCount: Int) {

    var boostLastFrame = 100
    var inputBuffer = ""
    var objectives = Array(objectiveCount) { i -> i }
    var nextObjective = objectiveCount
    var score = 0
}