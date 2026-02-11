package com.megaapp.zvonilnik.contacts

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactPhotoResolver {

    fun findPhotoUri(context: Context, phoneRaw: String): String? {
        val phone = normalize(phoneRaw)
        if (phone.isBlank()) return null

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phone)
        )

        val projection = arrayOf(
            ContactsContract.PhoneLookup.PHOTO_URI,
            ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI
        )

        context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val full = c.getString(0)
                val thumb = c.getString(1)
                return full ?: thumb
            }
        }
        return null
    }

    private fun normalize(s: String): String {
        // убираем наш префикс ":" если он есть + вычищаем мусор
        val noPrefix = s.removePrefix(":").trim()
        return noPrefix.replace(Regex("[^0-9+]"), "")
    }
}
