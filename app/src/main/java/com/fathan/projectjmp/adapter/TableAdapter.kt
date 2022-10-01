package com.fathan.projectjmp.adapter

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.fathan.projectjmp.R
import com.fathan.projectjmp.databinding.RvLayoutBinding
import com.fathan.projectjmp.model.Note
import com.fathan.projectjmp.utils.Utils.convertStringToBitmap
import com.fathan.projectjmp.utils.Utils.convertToBitmap

class TableAdapter(private val onItemClickCallback: OnItemClickCallback): RecyclerView.Adapter<TableAdapter.NoteViewHolder>() {
    var listNotes = ArrayList<Note>()
        set(listNotes) {
            if (listNotes.size > 0) {
                this.listNotes.clear()
            }
            this.listNotes.addAll(listNotes)
        }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.rv_layout, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(listNotes[position])
        Log.d("TableAdapter", "onBindViewHolder: ${listNotes.size}")
    }

    override fun getItemCount(): Int = this.listNotes.size

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = RvLayoutBinding.bind(itemView)

        fun bind(note: Note) {
            binding.nameTv.text = note.name
            binding.locationTv.text = note.location
            binding.dateTv.text = note.date
            binding.emailTv.text = note.email
            binding.rollNoTv.text = note.id.toString()
//            if (note.imageUrl != null){
//            val uri = Uri.parse(note.imageUrl.toString())
            binding.imageUser.setImageURI(note.imageUrl?.toUri())
            Log.d("TableAdapter", "bind: ${note.imageUrl} + ${note.description} + ${note.gender}+ ${note.email} + ${note.location}")
//            binding.tvItemDescription.text = note.description
            binding.itemRow.setOnClickListener {
                onItemClickCallback.onItemClicked(note, adapterPosition)
            }
        }
    }

    fun addItem(note: Note) {
        this.listNotes.add(note)
        notifyItemInserted(this.listNotes.size - 1)
    }
    fun updateItem(position: Int, note: Note) {
        this.listNotes[position] = note
        notifyItemChanged(position, note)
    }
    fun removeItem(position: Int) {
        this.listNotes.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, this.listNotes.size)
    }
    interface OnItemClickCallback {
        fun onItemClicked(selectedNote: Note?, position: Int?)
    }
}

//class TableAdapter(private val listNote: ArrayList<Note>) : RecyclerView.Adapter<TableAdapter.ListViewHolder>() {
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
//        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.rv_layout, parent, false)
//        return ListViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
//        val (name, description, photo) = listHero[position]
//        holder.imgPhoto.setImageResource(photo)
//        holder.tvName.text = name
//        holder.tvDescription.text = description
//    }
//
//    override fun getItemCount(): Int = listHero.size
//
//    class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        var imgPhoto: ImageView = itemView.findViewById(R.id.img_item_photo)
//        var tvName: TextView = itemView.findViewById(R.id.tv_item_name)
//        var tvDescription: TextView = itemView.findViewById(R.id.tv_item_description)
//    }
//}