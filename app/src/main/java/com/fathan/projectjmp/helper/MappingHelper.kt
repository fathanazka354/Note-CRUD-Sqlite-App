package com.fathan.projectjmp.helper

import android.database.Cursor
import com.fathan.projectjmp.model.Note
import com.fathan.projectjmp.db.DatabaseContract

object MappingHelper {

    fun mapCursorToArrayList(notesCursor: Cursor?): ArrayList<Note> {
        val notesList = ArrayList<Note>()
        notesCursor?.apply {
            while (moveToNext()) {
                val id = getInt(getColumnIndexOrThrow(DatabaseContract.NoteColumns._ID))
                val title = getString(getColumnIndexOrThrow(DatabaseContract.NoteColumns.NAME))
                val description = getString(getColumnIndexOrThrow(DatabaseContract.NoteColumns.DESCRIPTION))
                val location = getString(getColumnIndexOrThrow(DatabaseContract.NoteColumns.LOCATION))
                val gender = getInt(getColumnIndexOrThrow(DatabaseContract.NoteColumns.GENDER))
                val email = getString(getColumnIndexOrThrow(DatabaseContract.NoteColumns.EMAIL))
                val date = getString(getColumnIndexOrThrow(DatabaseContract.NoteColumns.DATE))
                val image = getString(getColumnIndexOrThrow(DatabaseContract.NoteColumns.IMAGE_URL))
                notesList.add(Note(id, name =  title, description =  description, date =  date, location = location, gender = gender,email = email, imageUrl = image))
            }
        }
        return notesList
    }
}