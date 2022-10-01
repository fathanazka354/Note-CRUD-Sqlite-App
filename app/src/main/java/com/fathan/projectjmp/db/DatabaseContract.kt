package com.fathan.projectjmp.db

import android.provider.BaseColumns

internal class DatabaseContract {

    internal class NoteColumns : BaseColumns {
        companion object {
            const val TABLE_NAME = "note"
            const val _ID = "_id"
            const val NAME = "name"
            const val DESCRIPTION = "description"
            const val EMAIL = "email"
            const val LOCATION = "location"
            const val GENDER = "gender"
            const val DATE = "date"
            const val IMAGE_URL = "image_url"
        }
    }
}