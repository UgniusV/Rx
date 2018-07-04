package com.dandy.ugnius.dandy.artist.view.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.dandy.ugnius.dandy.R
import com.dandy.ugnius.dandy.artist.model.entities.Artist
import com.dandy.ugnius.dandy.artist.common.secondOrNull
import kotlinx.android.synthetic.main.similar_artist_cell_entry.view.*

class SimilarArtistsAdapter(context: Context) : RecyclerView.Adapter<SimilarArtistsAdapter.ViewHolder>() {

    var entries = listOf<Artist>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    private val inflater = LayoutInflater.from(context)
    private val requestManager = Glide.with(context)

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(inflater.inflate(R.layout.similar_artist_cell_entry, parent, false))
    }

    override fun getItemCount() = entries.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder.itemView) {
            val entry = entries[position]
            entry.images.secondOrNull()?.url?.let { requestManager.load(it).into(photo) }
            title.text = entry.name
            heart.setOnClickListener { heart.playAnimation() }
        }
    }
}