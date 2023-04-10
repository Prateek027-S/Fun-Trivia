package com.prateekshah.funtrivia.network

import com.squareup.moshi.Json

data class DataResponse(
    @Json(name = "response_code") val responseCode: Int = 0,
    val results: MutableList<Question> = mutableListOf(Question())
) {
    data class Question(val category: String = "",
                        val type: String = "",
                        val difficulty: String = "",
                        val question: String = "",
                        @Json(name = "correct_answer") val correctAnswer: String = "",
                        @Json(name = "incorrect_answers") val incorrectAnswer: List<String> = listOf(String())
    )

    //For converting HashMap from Firestore to DataResponse.Question object
    companion object {
        fun fromJson(map: Map<String, Any>) = object {
            val category = map["category"]
            val type = map["type"]
            val difficulty = map["difficulty"]
            val question = map["question"]
            val correctAnswer = map["correctAnswer"]
            val incorrectAnswer = map["incorrectAnswer"]
            val data = Question(category as String, type as String, difficulty as String, question as String, correctAnswer as String, incorrectAnswer as List<String>)
        }.data
    }
}

/*
{"response_code":0,
    "results":[
    {"category":"Sports",
        "type":"multiple",
        "difficulty":"easy",
        "question":"Which of the following sports is not part of the triathlon?",
        "correct_answer":"Horse-Riding",
        "incorrect_answers":["Cycling","Swimming","Running"]
    },
    {"category":"Entertainment: Video Games",
        "type":"boolean",
        "difficulty":"easy",
        "question":"The song &quot;Megalovania&quot; by Toby Fox made its third appearence in the 2015 RPG &quot;Undertale&quot;.",
        "correct_answer":"True","incorrect_answers":["False"]
    },
    {"category":"History","type":"boolean","difficulty":"easy","question":"The Spitfire originated from a racing plane.",
        "correct_answer":"True",
        "incorrect_answers":["False"]},
    {"category":"Science: Computers",
        "type":"multiple",
        "difficulty":"easy",
        "question":"Which computer hardware device provides an interface for all other connected devices to communicate?",
        "correct_answer":"Motherboard",
        "incorrect_answers":["Central Processing Unit","Hard Disk Drive","Random Access Memory"]
        },{"category":"History","type":"multiple","difficulty":"easy","question":"Which of the following African countries was most successful in resisting colonization?","correct_answer":"Ethiopia","incorrect_answers":["C&ocirc;te d&rsquo;Ivoire","Congo","Namibia"]},{"category":"History","type":"multiple","difficulty":"easy","question":"In which year did the Invasion of Kuwait by Iraq occur?","correct_answer":"1990","incorrect_answers":["1992","1988","1986"]},{"category":"Science & Nature","type":"multiple","difficulty":"easy","question":"The human heart has how many chambers?","correct_answer":"4","incorrect_answers":["2","6","3"]},{"category":"Geography","type":"multiple","difficulty":"easy","question":"Which country is the home of the largest Japanese population outside of Japan?","correct_answer":"Brazil","incorrect_answers":["China","Russia","The United States"]},{"category":"Science: Mathematics","type":"multiple","difficulty":"easy","question":"How many sides does a trapezium have?","correct_answer":"4","incorrect_answers":["3","5","6"]},{"category":"Entertainment: Film","type":"multiple","difficulty":"easy","question":"Who directed the Kill Bill movies?","correct_answer":"Quentin Tarantino","incorrect_answers":["Arnold Schwarzenegger","David Lean","Stanley Kubrick"]}]}
*/