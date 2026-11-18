package com.vire.android.android

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

object ProfileImageLoader {
    private const val TAG = "ProfileImageLoader"

    fun load(source: String?, target: ImageView, placeholder: Int) {
        val value = source?.trim().orEmpty()
        target.tag = value
        target.setImageResource(placeholder)
        if (value.isBlank()) return

        when {
            value.startsWith("data:image", ignoreCase = true) -> loadBase64(value, target, placeholder)
            value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true) ->
                loadUri(Uri.parse(value), value, target, placeholder)
            value.startsWith("content://", ignoreCase = true) || value.startsWith("file://", ignoreCase = true) ->
                loadUri(Uri.parse(value), value, target, placeholder)
            else -> loadFromStorage(value, target, placeholder)
        }
    }

    private fun loadBase64(value: String, target: ImageView, placeholder: Int) {
        try {
            val bytes = Base64.decode(value.substringAfter("base64,"), Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap == null) {
                Log.w(TAG, "Could not decode profile image data")
                target.setImageResource(placeholder)
            } else if (target.tag == value) {
                target.setImageBitmap(bitmap)
            }
        } catch (error: IllegalArgumentException) {
            Log.w(TAG, "Invalid profile image data", error)
            target.setImageResource(placeholder)
        }
    }

    private fun loadUri(uri: Uri, source: String, target: ImageView, placeholder: Int) {
        Picasso.get()
            .load(uri)
            .placeholder(placeholder)
            .error(placeholder)
            .fit()
            .centerCrop()
            .into(target)
        target.tag = source
    }

    private fun loadFromStorage(source: String, target: ImageView, placeholder: Int) {
        try {
            val storage = FirebaseStorage.getInstance()
            val reference = if (source.startsWith("gs://", ignoreCase = true)) {
                storage.getReferenceFromUrl(source)
            } else {
                storage.reference.child(source.trimStart('/'))
            }
            reference.downloadUrl
                .addOnSuccessListener { downloadUrl ->
                    if (target.tag == source) loadUri(downloadUrl, source, target, placeholder)
                }
                .addOnFailureListener { error ->
                    Log.w(TAG, "Could not load profile image from storage", error)
                    if (target.tag == source) target.setImageResource(placeholder)
                }
        } catch (error: IllegalArgumentException) {
            Log.w(TAG, "Invalid profile image storage reference", error)
            target.setImageResource(placeholder)
        } catch (error: IllegalStateException) {
            Log.e(TAG, "Firebase Storage is not initialized", error)
            target.setImageResource(placeholder)
        }
    }
}
