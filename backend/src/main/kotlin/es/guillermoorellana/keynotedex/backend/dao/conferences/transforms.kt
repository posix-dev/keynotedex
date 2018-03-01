package es.guillermoorellana.keynotedex.backend.dao.conferences

import org.jetbrains.squash.results.ResultRow
import org.jetbrains.squash.results.get

fun transformConference(it: ResultRow): Conference =
    Conference(
        name = it[ConferencesTable.name]
    )

fun Conference.toDto() = es.guillermoorellana.keynotedex.dto.Conference(
    name = name
)
