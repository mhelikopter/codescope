package de.thkoeln.codescope.data.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*

class FirestoreClientImpl(
    private val http: HttpClient,
    private val config: FirebaseJvmConfig,
    private val session: FirebaseSession,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : IFirestoreClient {

    private fun docUrl(path: String): String {
        val cleaned = path.trim('/')
        return "https://firestore.googleapis.com/v1/projects/${config.projectId}/databases/(default)/documents/$cleaned"
    }

    override suspend fun <T : Any> setDocument(
        path: String,
        data: T,
        strategy: KSerializer<T>
    ): Result<Unit> = try {
        val fields = encodeToFirestoreFields(data, strategy)
        val body = buildJsonObject { put("fields", fields) }

        val response = http.patch(docUrl(path)) {
            headers.append(HttpHeaders.Authorization, "Bearer ${session.requireIdToken()}")
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        if (response.status.isSuccess()) {
            println("JVM Firestore: Document at $path successfully written.")
            Result.success(Unit)
        } else {
            val errorBody = response.bodyAsText()
            println("JVM Firestore: Error writing document at $path: ${response.status} - $errorBody")
            Result.failure(Exception("Firestore error: ${response.status}"))
        }
    } catch (e: Exception) {
        println("JVM Firestore: Exception writing document at $path: ${e.message}")
        Result.failure(e)
    }

    override suspend fun <T> getDocument(path: String, strategy: KSerializer<T>): T? = try {
        val response = http.get(docUrl(path)) {
            headers.append(HttpHeaders.Authorization, "Bearer ${session.requireIdToken()}")
        }
        
        if (!response.status.isSuccess()) {
            println("JVM Firestore: Document $path not found or access denied: ${response.status}")
            null
        } else {
            val resp: JsonObject = response.body()
            val name = resp["name"]?.jsonPrimitive?.contentOrNull
            val fields = resp["fields"]?.jsonObject ?: return null

            val patched = injectIdIfMissing(fields, name)
            decodeFromFirestoreFields(patched, strategy)
        }
    } catch (e: Exception) {
        println("JVM Firestore: Fehler beim Laden von $path: ${e.message}")
        null
    }

    override suspend fun deleteDocument(path: String): Result<Unit> = try {
        val response = http.delete(docUrl(path)) {
            headers.append(HttpHeaders.Authorization, "Bearer ${session.requireIdToken()}")
        }
        if (response.status.isSuccess()) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Delete failed: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun <T> observeDocument(path: String, strategy: KSerializer<T>): Flow<T?> =
        flow { emit(getDocument(path, strategy)) }

    override fun <T> getCollection(path: String, strategy: KSerializer<T>): Flow<List<T>> =
        flow {
            try {
                val url = docUrl(path) + "?pageSize=1000"
                val response = http.get(url) {
                    headers.append(HttpHeaders.Authorization, "Bearer ${session.requireIdToken()}")
                }
                
                if (response.status.isSuccess()) {
                    val resp: JsonObject = response.body()
                    val docs = resp["documents"]?.jsonArray ?: JsonArray(emptyList())
                    val result = docs.mapNotNull { docEl ->
                        val doc = docEl.jsonObject
                        val name = doc["name"]?.jsonPrimitive?.contentOrNull
                        val fields = doc["fields"]?.jsonObject ?: return@mapNotNull null
                        val patched = injectIdIfMissing(fields, name)
                        decodeFromFirestoreFields(patched, strategy)
                    }
                    emit(result)
                } else {
                    println("JVM Firestore: Error listing collection $path: ${response.status}")
                    emit(emptyList<T>())
                }
            } catch (e: Exception) {
                println("JVM Firestore: Exception listing collection $path: ${e.message}")
                emit(emptyList<T>())
            }
        }

    override suspend fun <T> getDocumentsByQuery(
        collectionPath: String,
        field: String,
        value: Any,
        strategy: KSerializer<T>
    ): Result<List<T>> = runQuery(collectionPath, field, "EQUAL", value, strategy)

    override suspend fun <T> getDocumentsByQueryInArray(
        collectionPath: String,
        field: String,
        value: Any,
        strategy: KSerializer<T>
    ): Result<List<T>> = runQuery(collectionPath, field, "ARRAY_CONTAINS", value, strategy)

    override fun <T> observeCollectionByQuery(
        collectionPath: String,
        field: String,
        value: Any,
        strategy: KSerializer<T>
    ): Flow<List<T>> = flow {
        val result = getDocumentsByQuery(collectionPath, field, value, strategy)
        emit(result.getOrDefault(emptyList()))
    }

    override suspend fun updateDocument(path: String, fields: Map<String, Any>): Result<Unit> {
        return Result.failure(NotImplementedError("updateDocument: implement later (PATCH with updateMask)"))
    }

    private suspend fun <T> runQuery(
        collectionPath: String,
        field: String,
        op: String,
        value: Any,
        strategy: KSerializer<T>
    ): Result<List<T>> = try {
        val url =
            "https://firestore.googleapis.com/v1/projects/${config.projectId}/databases/(default)/documents:runQuery"

        val firestoreValue = anyToFirestoreValue(value)

        val body = buildJsonObject {
            put("structuredQuery", buildJsonObject {
                put("from", JsonArray(listOf(buildJsonObject {
                    put("collectionId", collectionPath.trim('/'))
                })))
                put("where", buildJsonObject {
                    put("fieldFilter", buildJsonObject {
                        put("field", buildJsonObject { put("fieldPath", field) })
                        put("op", op)
                        put("value", firestoreValue)
                    })
                })
            })
        }

        val response = http.post(url) {
            headers.append(HttpHeaders.Authorization, "Bearer ${session.requireIdToken()}")
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        if (response.status.isSuccess()) {
            val resp: JsonArray = response.body()
            val result = resp.mapNotNull { row ->
                val obj = row.jsonObject
                val doc = obj["document"]?.jsonObject ?: return@mapNotNull null
                val name = doc["name"]?.jsonPrimitive?.contentOrNull
                val fieldsObj = doc["fields"]?.jsonObject ?: return@mapNotNull null

                val patchedFields = injectIdIfMissing(fieldsObj, name)
                decodeFromFirestoreFields(patchedFields, strategy)
            }
            Result.success(result)
        } else {
            val errorBody = response.bodyAsText()
            println("JVM Firestore: Query failed at $collectionPath: ${response.status} - $errorBody")
            Result.failure(Exception("Query failed: ${response.status}"))
        }
    } catch (e: Exception) {
        println("JVM Firestore: Exception during query: ${e.message}")
        Result.failure(e)
    }

    // ---------- Firestore <-> Kotlinx Serialization ----------

    private fun <T> encodeToFirestoreFields(data: T, strategy: KSerializer<T>): JsonObject {
        val element = json.encodeToJsonElement(strategy, data)
        require(element is JsonObject) { "Top-level document must be a JSON object" }

        return buildJsonObject {
            for ((k, v) in element) put(k, jsonElementToFirestoreValue(v))
        }
    }

    private fun <T> decodeFromFirestoreFields(fields: JsonObject, strategy: KSerializer<T>): T {
        val jsonObj = buildJsonObject {
            for ((k, v) in fields) put(k, firestoreValueToJsonElement(v.jsonObject))
        }
        return json.decodeFromJsonElement(strategy, jsonObj)
    }

    private fun jsonElementToFirestoreValue(el: JsonElement): JsonObject = when (el) {
        is JsonNull -> buildJsonObject { put("nullValue", JsonNull) }
        is JsonPrimitive -> when {
            el.isString -> buildJsonObject { put("stringValue", JsonPrimitive(el.content)) }
            el.booleanOrNull != null -> buildJsonObject { put("booleanValue", JsonPrimitive(el.boolean)) }
            el.longOrNull != null -> buildJsonObject { put("integerValue", JsonPrimitive(el.long)) }
            el.doubleOrNull != null -> buildJsonObject { put("doubleValue", JsonPrimitive(el.double)) }
            else -> buildJsonObject { put("stringValue", JsonPrimitive(el.content)) }
        }
        is JsonArray -> buildJsonObject {
            put("arrayValue", buildJsonObject {
                put("values", JsonArray(el.map { jsonElementToFirestoreValue(it) }))
            })
        }
        is JsonObject -> buildJsonObject {
            put("mapValue", buildJsonObject {
                put("fields", buildJsonObject {
                    el.forEach { (k, v) -> put(k, jsonElementToFirestoreValue(v)) }
                })
            })
        }
    }

    private fun firestoreValueToJsonElement(valueObj: JsonObject): JsonElement = when {
        "stringValue" in valueObj -> JsonPrimitive(valueObj["stringValue"]!!.jsonPrimitive.content)
        "booleanValue" in valueObj -> JsonPrimitive(valueObj["booleanValue"]!!.jsonPrimitive.boolean)
        "integerValue" in valueObj -> {
            val raw = valueObj["integerValue"]!!.jsonPrimitive.content
            JsonPrimitive(raw.toLongOrNull() ?: 0L)
        }
        "doubleValue" in valueObj -> {
            val raw = valueObj["doubleValue"]!!.jsonPrimitive.content
            JsonPrimitive(raw.toDoubleOrNull() ?: 0.0)
        }
        "nullValue" in valueObj -> JsonNull
        "mapValue" in valueObj -> {
            val fields = valueObj["mapValue"]!!.jsonObject["fields"]?.jsonObject ?: JsonObject(emptyMap())
            buildJsonObject { fields.forEach { (k, v) -> put(k, firestoreValueToJsonElement(v.jsonObject)) } }
        }
        "arrayValue" in valueObj -> {
            val values = valueObj["arrayValue"]!!.jsonObject["values"]?.jsonArray ?: JsonArray(emptyList())
            JsonArray(values.map { firestoreValueToJsonElement(it.jsonObject) })
        }
        else -> JsonNull
    }

    private fun anyToFirestoreValue(value: Any): JsonObject {
        val el: JsonElement = when (value) {
            is String -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is Long -> JsonPrimitive(value)
            is Double -> JsonPrimitive(value)
            is Float -> JsonPrimitive(value.toDouble())
            else -> JsonPrimitive(value.toString())
        }
        return jsonElementToFirestoreValue(el)
    }

    private fun injectIdIfMissing(fields: JsonObject, docName: String?): JsonObject {
        if (fields.containsKey("id")) return fields
        val id = docName?.substringAfterLast("/") ?: return fields

        return buildJsonObject {
            // id als stringValue ergänzen
            put("id", buildJsonObject { put("stringValue", JsonPrimitive(id)) })
            fields.forEach { (k, v) -> put(k, v) }
        }
    }

}
