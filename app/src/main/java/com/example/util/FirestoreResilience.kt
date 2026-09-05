package com.example.util

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Shared resilient Firestore read helpers.
 *
 * Implements server-first strategy with graceful cache degradation:
 * 1. Attempts the read with default Source (which tries server first).
 * 2. If it throws or times out (network lag, emulator socket pause, transient offline),
 *    it explicitly falls back to Source.CACHE.
 * 3. Logs when the cache fallback is used.
 */
object FirestoreResilience {
    private const val TAG = "FirestoreResilience"
    const val DEFAULT_TIMEOUT_MS = 6000L

    suspend fun getDocumentServerFirst(
        docRef: DocumentReference,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): DocumentSnapshot {
        var serverException: Exception? = null
        val serverSnap = withTimeoutOrNull(timeoutMs) {
            try {
                docRef.get().await()
            } catch (e: Exception) {
                serverException = e
                Log.w(TAG, "Server-first fetch error for ${docRef.path}: ${e.message}")
                null
            }
        }

        if (serverSnap != null) {
            return serverSnap
        }

        Log.w(
            TAG,
            "Falling back to Source.CACHE for document: ${docRef.path} (reason: ${serverException?.message ?: "timed out after ${timeoutMs}ms"})"
        )

        return try {
            val cacheSnap = docRef.get(Source.CACHE).await()
            Log.i(TAG, "Source.CACHE hit for ${docRef.path} (exists=${cacheSnap.exists()})")
            cacheSnap
        } catch (cacheEx: Exception) {
            Log.e(TAG, "Source.CACHE fallback also failed for ${docRef.path}: ${cacheEx.message}")
            throw serverException ?: cacheEx
        }
    }

    suspend fun getQuerySnapshotServerFirst(
        query: Query,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): QuerySnapshot {
        var serverException: Exception? = null
        val serverSnap = withTimeoutOrNull(timeoutMs) {
            try {
                query.get().await()
            } catch (e: Exception) {
                serverException = e
                Log.w(TAG, "Server-first query error: ${e.message}")
                null
            }
        }

        if (serverSnap != null) {
            return serverSnap
        }

        Log.w(
            TAG,
            "Falling back to Source.CACHE for query (reason: ${serverException?.message ?: "timed out after ${timeoutMs}ms"})"
        )

        return try {
            val cacheSnap = query.get(Source.CACHE).await()
            Log.i(TAG, "Source.CACHE hit for query (results=${cacheSnap.size()})")
            cacheSnap
        } catch (cacheEx: Exception) {
            Log.e(TAG, "Source.CACHE fallback also failed for query: ${cacheEx.message}")
            throw serverException ?: cacheEx
        }
    }
}

suspend fun getDocumentServerFirst(
    docRef: DocumentReference,
    timeoutMs: Long = FirestoreResilience.DEFAULT_TIMEOUT_MS
): DocumentSnapshot = FirestoreResilience.getDocumentServerFirst(docRef, timeoutMs)

suspend fun getQuerySnapshotServerFirst(
    query: Query,
    timeoutMs: Long = FirestoreResilience.DEFAULT_TIMEOUT_MS
): QuerySnapshot = FirestoreResilience.getQuerySnapshotServerFirst(query, timeoutMs)
