package com.fathan.projectjmp.model

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Note(
    var id: Int = 0,
    var name: String? = null,
    var description: String? = null,
    var email: String? = null,
    var gender: Int? = null,
    var location: String? = null,
    var date: String? = null,
    var imageUrl: String? = null,
) : Parcelable