package com.example.bluecatapp.db

data class TodoDTO(val id : Long,
                   val task : String,
                   val date : String,
                   val location : String,
                   val done : Boolean
)
