package es.guillermoorellana.conferences.backend

import io.ktor.locations.location

@location("/")
class Index()

@location("/user/{user}")
data class UserPage(val user: String)
