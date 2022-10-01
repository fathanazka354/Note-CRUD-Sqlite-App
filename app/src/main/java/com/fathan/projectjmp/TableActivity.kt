package com.fathan.projectjmp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fathan.projectjmp.adapter.TableAdapter
import com.fathan.projectjmp.databinding.ActivityTableBinding
import com.fathan.projectjmp.databinding.RvLayoutBinding
import com.fathan.projectjmp.db.NoteHelper
import com.fathan.projectjmp.helper.MappingHelper
import com.fathan.projectjmp.model.Note
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class TableActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTableBinding
    private lateinit var adapter: TableAdapter
    val resultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.data != null) {
            // Akan dipanggil jika request codenya ADD
            when (result.resultCode) {
                AddUpdateActivity.RESULT_ADD -> {
                    val note = result.data?.getParcelableExtra<Note>(AddUpdateActivity.EXTRA_NOTE) as Note
                    adapter.addItem(note)
                    binding.rvTable.smoothScrollToPosition(adapter.itemCount - 1)
                    showSnackbarMessage("Satu item berhasil ditambahkan")
                }
                AddUpdateActivity.RESULT_UPDATE -> {
                    val note = result.data?.getParcelableExtra<Note>(AddUpdateActivity.EXTRA_NOTE) as Note
                    val position = result?.data?.getIntExtra(AddUpdateActivity.EXTRA_POSITION, 0) as Int
                    adapter.updateItem(position, note)
                    binding.rvTable.smoothScrollToPosition(position)
                    showSnackbarMessage("Satu item berhasil diubah")
                }
                AddUpdateActivity.RESULT_DELETE -> {
                    val position = result?.data?.getIntExtra(AddUpdateActivity.EXTRA_POSITION, 0) as Int
                    adapter.removeItem(position)
                    showSnackbarMessage("Satu item berhasil dihapus")
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTableBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        bindingImage = RvLayoutBinding.inflate(layoutInflater)

        supportActionBar?.title = "Notes"

        binding.rvTable.layoutManager = LinearLayoutManager(this)
        binding.rvTable.setHasFixedSize(true)

        adapter = TableAdapter(object : TableAdapter.OnItemClickCallback {
            override fun onItemClicked(selectedNote: Note?, position: Int?) {
                val intent = Intent(this@TableActivity, AddUpdateActivity::class.java)
                intent.putExtra(AddUpdateActivity.EXTRA_NOTE, selectedNote)
                intent.putExtra(AddUpdateActivity.EXTRA_POSITION, position)
                resultLauncher.launch(intent)
            }
        })
        binding.rvTable.adapter = adapter
        binding.fabAdd.setOnClickListener {
            val intent = Intent(this, AddUpdateActivity::class.java)
            resultLauncher.launch(intent)
        }
        loadNotesAsync()
        if (savedInstanceState == null) {
            // proses ambil data
            loadNotesAsync()
        } else {
            val list = savedInstanceState.getParcelableArrayList<Note>(EXTRA_STATE)
            if (list != null) {
                adapter.listNotes = list
            }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(EXTRA_STATE, adapter.listNotes)
    }

    private fun loadNotesAsync() {
        lifecycleScope.launch {
//            binding.progressbar.visibility = View.VISIBLE
            val noteHelper = NoteHelper.getInstance(applicationContext)
            noteHelper.open()
            val deferredNotes = async(Dispatchers.IO) {
                val cursor = noteHelper.queryAll()
                MappingHelper.mapCursorToArrayList(cursor)
            }
//            binding.progressbar.visibility = View.INVISIBLE
            val notes = deferredNotes.await()
            if (notes.size > 0) {
                adapter.listNotes = notes
            } else {
                adapter.listNotes = ArrayList()
                showSnackbarMessage("Tidak ada data saat ini")
            }
            noteHelper.close()
        }
    }

    private fun showSnackbarMessage(message: String) {
        Snackbar.make(binding.rvTable, message, Snackbar.LENGTH_SHORT).show()
    }

    companion object {
        private const val EXTRA_STATE = "EXTRA_STATE"

        const val CAMERA_X_RESULT = 200
    }

}