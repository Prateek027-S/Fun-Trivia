package com.prateekshah.funtrivia

import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.example.funtrivia.R
import com.prateekshah.funtrivia.model.QuizApiStatus

@BindingAdapter("quizApiStatus")
fun bindStatus(statusImageView: ImageView, status: QuizApiStatus?){
    when(status) {
        QuizApiStatus.LOADING -> {
            statusImageView.visibility = View.VISIBLE
            statusImageView.setImageResource(R.drawable.loading_animation)
        }
        QuizApiStatus.ERROR -> {
            statusImageView.visibility = View.VISIBLE
            statusImageView.setImageResource(R.drawable.ic_connection_error)
        }
        else -> {
            statusImageView.visibility = View.GONE
        }
    }
}