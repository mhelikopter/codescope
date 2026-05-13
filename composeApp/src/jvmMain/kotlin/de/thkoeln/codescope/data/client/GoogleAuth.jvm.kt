package de.thkoeln.codescope.data.client

import de.thkoeln.codescope.BuildKonfig
import de.thkoeln.codescope.domain.googleAuth.GoogleAuthCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.*
import java.nio.charset.StandardCharsets

actual class GoogleAuth actual constructor(context: Any) {

    private val clientId = BuildKonfig.DESKTOP_CLIENT_ID
    private val clientSecret = BuildKonfig.DESKTOP_CLIENT_SECRET

    actual suspend fun getGoogleIdToken(): GoogleAuthCredential = withContext(Dispatchers.IO) {
        var serverSocket: ServerSocket? = null

        try {
            serverSocket = ServerSocket(0)
            val port = serverSocket.localPort

            // Loopback redirect: MUST be registered in Google Cloud Console as:
            // http://127.0.0.1
            // and/or with path, depending on your OAuth client config.
            val redirectUrl = "http://127.0.0.1:$port/callback"
            val redirectEncoded = URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8.name())

            // Added drive.readonly scope to allow downloading files
            val scope = URLEncoder.encode("openid email profile https://www.googleapis.com/auth/drive.readonly", StandardCharsets.UTF_8.name())

            val authUrl =
                "https://accounts.google.com/o/oauth2/v2/auth" +
                        "?client_id=${URLEncoder.encode(clientId, StandardCharsets.UTF_8.name())}" +
                        "&redirect_uri=$redirectEncoded" +
                        "&response_type=code" +
                        "&scope=$scope" +
                        "&access_type=offline" +
                        "&prompt=consent"

            openBrowser(authUrl)

            val socket = serverSocket.accept()

            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val requestLine = reader.readLine() ?: throw Exception("Empty callback request")

            val writer = PrintWriter(socket.getOutputStream())
            writer.println("HTTP/1.1 200 OK")
            writer.println("Content-Type: text/html")
            writer.println()
            writer.println(successHtml())
            writer.flush()

            writer.close()
            reader.close()
            socket.close()

            // requestLine: "GET /callback?code=...&scope=... HTTP/1.1"
            val uriPart = requestLine.split(" ").getOrNull(1) ?: throw Exception("Invalid callback request")
            val query = uriPart.substringAfter("?", "")
            val params = parseQuery(query)

            val error = params["error"]
            if (!error.isNullOrBlank()) {
                throw Exception("Google OAuth error: $error")
            }

            val code = params["code"] ?: throw Exception("No authorization code in callback")

            // Exchange code -> tokens
            exchangeCodeForToken(
                code = code,
                redirectUrl = redirectUrl
            )

        } finally {
            serverSocket?.close()
        }
    }

    private fun openBrowser(url: String) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        } else {
            throw Exception("Desktop browsing is not supported on this system.")
        }
    }

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split("&")
            .mapNotNull { part ->
                val idx = part.indexOf("=")
                if (idx <= 0) return@mapNotNull null
                val key = URLDecoder.decode(part.substring(0, idx), StandardCharsets.UTF_8.name())
                val value = URLDecoder.decode(part.substring(idx + 1), StandardCharsets.UTF_8.name())
                key to value
            }
            .toMap()
    }

    private fun exchangeCodeForToken(code: String, redirectUrl: String): GoogleAuthCredential {
        val url = URL("https://oauth2.googleapis.com/token")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }

        fun enc(s: String) = URLEncoder.encode(s, StandardCharsets.UTF_8.name())

        val body =
            "code=${enc(code)}" +
                    "&client_id=${enc(clientId)}" +
                    "&client_secret=${enc(clientSecret)}" +
                    "&redirect_uri=${enc(redirectUrl)}" +
                    "&grant_type=authorization_code"

        conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }

        val responseText = if (conn.responseCode in 200..299) {
            conn.inputStream.reader().readText()
        } else {
            val err = conn.errorStream?.reader()?.readText()
            throw Exception("Google token exchange failed HTTP ${conn.responseCode}: $err")
        }

        val idToken = extractJsonValue(responseText, "id_token")
        val accessToken = extractJsonValue(responseText, "access_token")

        // ✅ return empty credential instead of throwing -> matches android behavior
        return GoogleAuthCredential(
            idToken = idToken,
            accessToken = accessToken
        )
    }

    private fun extractJsonValue(json: String, key: String): String? {
        val regex = "\"$key\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        return regex.find(json)?.groupValues?.get(1)
    }

    private fun successHtml(): String = """
        <html>
          <body style="text-align:center; font-family:sans-serif; padding-top:50px;">
            <h2 style="color:green;">Login Successful</h2>
            <p>You can close this window and return to the app.</p>
            <script>window.close();</script>
          </body>
        </html>
    """.trimIndent()

    actual suspend fun signOut() {
        // No-op for desktop as we don't have a persistent session in the same way
    }
}