package es.guillermoorellana.keynotedex.web.comms

import arrow.core.Try
import arrow.core.orNull
import es.guillermoorellana.keynotedex.api.Api
import es.guillermoorellana.keynotedex.dto.Submission
import es.guillermoorellana.keynotedex.dto.User
import es.guillermoorellana.keynotedex.requests.SubmissionUpdateRequest
import es.guillermoorellana.keynotedex.requests.UserProfileUpdateRequest
import es.guillermoorellana.keynotedex.responses.ErrorResponse
import es.guillermoorellana.keynotedex.responses.SubmissionResponse
import es.guillermoorellana.keynotedex.responses.UserProfileResponse
import es.guillermoorellana.keynotedex.responses.UserResponse
import es.guillermoorellana.keynotedex.web.model.UserProfile
import es.guillermoorellana.keynotedex.web.model.toModel
import es.guillermoorellana.keynotedex.web.model.toUpdateRequest
import kotlinx.coroutines.await
import org.w3c.dom.url.URLSearchParams
import org.w3c.fetch.Response
import kotlinx.serialization.json.JSON as KJSON

object NetworkDataSource {

    private val networkService = NetworkService

    suspend fun register(userId: String, password: String, displayName: String, email: String) =
        networkService.post(
            Api.V1.Paths.register,
            URLSearchParams().apply {
                append("userId", userId)
                append("password", password)
                append("displayName", displayName)
                append("email", email)
            }
        )
            .map { parseUserProfileResponse(it).user.toModel() }
            .orNull()!!

    suspend fun userProfile(userId: String): Try<UserProfile> =
        networkService.get(Api.V1.Paths.user.replace("{userId}", userId), null)
            .map { parseUserProfileResponse(it).toModel() }

    suspend fun updateUserProfile(userProfile: UserProfile): UserProfile? {
        val userId = userProfile.user.userId
        val body = KJSON.stringify(UserProfileUpdateRequest.serializer(), userProfile.toUpdateRequest())
        return networkService.put(Api.V1.Paths.user.replace("{userId}", userId), body)
            .map { parseUserProfileResponse(it).toModel() }
            .orNull()
    }

    suspend fun checkSession() = networkService.get(Api.V1.Paths.login, null)
        .map { parseUserResponse(it).toModel() }
        .orNull()

    suspend fun login(userId: String, password: String): es.guillermoorellana.keynotedex.web.model.User? {
        val body = URLSearchParams().apply {
            append("userId", userId)
            append("password", password)
        }
        return networkService.post(Api.V1.Paths.login, body)
            .map { parseUserResponse(it).toModel() }
            .orNull()
    }

    suspend fun logoutUser() = networkService.post(Api.V1.Paths.logout, null)

    suspend fun getSubmission(submissionId: String) =
        networkService.get(Api.V1.Paths.submissions.replace("{submissionId?}", submissionId), null)
            .map { parseSubmissionResponse(it).toModel() }
            .orNull()

    suspend fun postSubmission(submission: Submission) =
        networkService.post(
            Api.V1.Paths.submissions.replace("{submissionId?}", ""),
            KJSON.stringify(SubmissionUpdateRequest.serializer(), SubmissionUpdateRequest(submission))
        )

    suspend fun updateSubmission(submission: Submission) =
        networkService.put(
            Api.V1.Paths.submissions.replace("{submissionId?}", ""),
            KJSON.stringify(SubmissionUpdateRequest.serializer(), SubmissionUpdateRequest(submission))
        )

    private suspend fun parseUserResponse(response: Response): User = when {
        response.ok -> KJSON.parse(UserResponse.serializer(), response.text().await()).user
        else -> {
            val errorResponse: ErrorResponse = KJSON.parse(ErrorResponse.serializer(), response.text().await())
            throw LoginOrRegisterFailedException(errorResponse)
        }
    }

    private suspend fun parseUserProfileResponse(response: Response): UserProfileResponse = when {
        response.ok -> KJSON.parse(UserProfileResponse.serializer(), response.text().await())
        else -> {
            val errorResponse: ErrorResponse = KJSON.parse(ErrorResponse.serializer(), response.text().await())
            throw LoginOrRegisterFailedException(errorResponse)
        }
    }

    private suspend fun parseSubmissionResponse(response: Response): Submission = when {
        response.ok -> KJSON.parse(SubmissionResponse.serializer(), response.text().await()).submission
        else -> {
            val errorResponse: ErrorResponse = KJSON.parse(ErrorResponse.serializer(), response.text().await())
            throw LoginOrRegisterFailedException(errorResponse)
        }
    }

    class LoginOrRegisterFailedException(message: ErrorResponse) : Throwable(message.message)
}