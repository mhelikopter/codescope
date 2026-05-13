
package de.thkoeln.codescope.data.client
import de.thkoeln.codescope.domain.googleAuth.GoogleAuthCredential
import de.thkoeln.codescope.domain.user.User
import de.thkoeln.codescope.domain.user.UserRole
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
class AuthProviderImpl(
    private val googleAuth: GoogleAuth,
    private val firebaseAuth: FirebaseAuth,
    private val firestoreClient: IFirestoreClient
) : IAuthProvider {
    override suspend fun signInWithGoogle(): Result<User> = try {
        // Get credential from platform-specific implementation
        val credential = googleAuth.getGoogleIdToken()
        // Sign in with Firebase
        val user = signInWithGoogle(credential)
        if (user != null) {
            updateTokenOnServer(user) // Your custom logic
            val appUser = firestoreClient.getDocument("users/${user.uid}", User.serializer())
            Result.success(appUser!!)
        } else {
            Result.failure(Exception("Authentication failed"))
        }
    } catch (e: Exception) {
        Result.failure(Exception("Sign-in failed: ${e.message}"))
    }
    override suspend fun signOut(): Result<Unit> = try {
        firebaseAuth.signOut()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    override suspend fun getCurrentUser(): User? {
        val firebaseUser = firebaseAuth.currentUser ?: return null
        return firestoreClient.getDocument("users/${firebaseUser.uid}", User.serializer())
    }
    private suspend fun signInWithGoogle(credential: GoogleAuthCredential): FirebaseUser? {
        val firebaseCredential = GoogleAuthProvider.credential(
            idToken = credential.idToken,
            accessToken = credential.accessToken?.takeIf { it.isNotBlank() }
        )
        val result = firebaseAuth.signInWithCredential(firebaseCredential)
        return result.user
    }
    private suspend fun updateTokenOnServer(firebaseUser: FirebaseUser) {
        val path = "users/${firebaseUser.uid}"
        val existingUser = firestoreClient.getDocument(path, User.serializer())
        val roleToUse = existingUser?.role ?: UserRole.STUDENT
        // 1. Mapping FirebaseUser to User
        val user = User(
            id = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            role = roleToUse,
            name = firebaseUser.displayName ?: "",
        )
        // 2. Use the setDocument method we created earlier
        // We use 'set' (merge=true behavior usually preferred) or just overwrite
        // to ensure the user exists in the database.
        firestoreClient.setDocument(
            path = "users/${user.id}",
            data = user,
            strategy = User.serializer()
        )
    }
}