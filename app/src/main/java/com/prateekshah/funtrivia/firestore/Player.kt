package com.prateekshah.funtrivia.firestore

data class Player(var AvgTimePerQuestion: Double? = null,
                  var CurrentQuestionNum: Long? = null,
                  var Forfeit: Boolean? = null,
                  var Name: String? = null,
                  var RoomId: String? = null,
                  var Score: Long? = null
)
