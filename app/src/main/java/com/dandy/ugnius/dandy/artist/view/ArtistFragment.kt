package com.dandy.ugnius.dandy.artist.view

import android.content.Context
import android.graphics.Color.WHITE
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View.VISIBLE
import android.view.View.GONE
import android.support.v4.app.Fragment
import android.support.v4.view.PagerAdapter
import android.support.v7.graphics.Palette
import android.support.v7.widget.*
import com.dandy.ugnius.dandy.model.entities.Album
import com.dandy.ugnius.dandy.model.entities.Artist
import com.dandy.ugnius.dandy.model.entities.Track
import com.dandy.ugnius.dandy.artist.presenter.ArtistPresenter
import android.view.*
import com.App
import com.dandy.ugnius.dandy.*
import com.dandy.ugnius.dandy.model.clients.APIClient
import com.dandy.ugnius.dandy.artist.view.adapters.AlbumsAdapter
import com.dandy.ugnius.dandy.artist.view.adapters.SimilarArtistsAdapter
import com.dandy.ugnius.dandy.artist.view.adapters.TracksAdapter
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.view_artist.*
import com.dandy.ugnius.dandy.main.MainActivity
import java.text.NumberFormat
import java.util.Locale.US
import javax.inject.Inject
import com.dandy.ugnius.dandy.artist.view.decorations.ItemOffsetDecoration
import android.support.v7.widget.RecyclerView
import android.widget.ImageView



class ArtistFragment : Fragment(), ArtistView {

    @Inject lateinit var apiClient: APIClient
    private val formatter = NumberFormat.getNumberInstance(US)
    private val presenter by lazy { ArtistPresenter(apiClient, this) }
    private var searchItem: MenuItem? = null
    private var searchView: SearchView? = null

    private val tracksAdapter by lazy {
        TracksAdapter(
            context = context!!,
            onTrackClicked = { currentTrack, tracks -> (this::onArtistTrackClicked)(currentTrack, tracks) }
        )
    }
    private val albumsAdapter by lazy {
        AlbumsAdapter(
            context = context!!,
            onAlbumClicked = { album -> (this::onAlbumClicked)(album) }
        )
    }
    private val similarArtistsAdapter by lazy {
        SimilarArtistsAdapter(
            context = context!!,
            onTrackClicked = { currentTrack, tracks -> (this::onArtistTrackClicked)(currentTrack, tracks) },
            onArtistClicked = { artist -> (context as MainActivity).onArtistClicked(artist.id) }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        (activity?.applicationContext as App).mainComponent?.inject(this)
        return inflater.inflate(R.layout.view_artist, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        arguments?.getString("artistId")?.let { presenter.query(it, "ES", "album,single") }
        setHasOptionsMenu(true)
        (activity as MainActivity).setSupportActionBar(toolbar)
        collapsingAppBar.addOnOffsetChangedListener { appBarLayout, offset ->
            if (offset == -appBarLayout.totalScrollRange) {
                searchItem?.isVisible = true
                searchView?.visibility = VISIBLE
                (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
            } else if (offset == 0) {
                searchItem?.isVisible = false
                searchView?.visibility = GONE
                toolbar.collapseActionView()
                (activity as? MainActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }
        }
    }

    override fun setArtistInfo(artist: Artist) {
        followers?.text = String.format(getString(R.string.followers, formatter.format(artist.followers)))
        collapsingLayout?.title = artist.name
        background?.loadBitmap(artist.images.first(), context!!) {
            it.extractSwatch().subscribeBy(
                onSuccess = { setAccentColor(it) },
                onComplete = { artistPager.background = ColorDrawable(WHITE) }
            )
        }
    }

    override fun setTracksAndAlbums(tracks: List<Track>, albums: List<Album>) {
        albumsAdapter.entries = albums
        tracksAdapter.entries = tracks
        artistPager.offscreenPageLimit = ARTIST_PAGER_ENTRIES_COUNT
        artistPager.adapter = ArtistPagerAdapter(context!!)
        artistPagerTabs.setupWithViewPager(artistPager)
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                //todo pasearchint like a soccer mom gausiu 2 resultatus
                tracksAdapter.entries = tracks.filter { it.name.contains(newText, true) }
                return false
            }
        })
    }

    override fun setSimilarArtists(artists: List<Artist>) {
        similarArtistsAdapter.entries = artists
    }

    private fun onArtistTrackClicked(currentTrack: Track, tracks: List<Track>) {
        (context as? MainActivity)?.onArtistTrackClicked(currentTrack, tracks)
    }

    private fun onAlbumClicked(album: Album) = (context as? MainActivity)?.onAlbumClicked(album)

    override fun showError(message: String) {
        //show error
    }

    private fun setAccentColor(swatch: Palette.Swatch) {
        artistPager.shade(color = Utilities.whiteBlend(context!!, swatch.rgb, 0.3F), ratio = 0.2F)
        val adjustedColor = Drawables.lightenOrDarken(swatch.rgb, 0.4)
        artistPagerTabs?.let {
            it.setSelectedTabIndicatorColor(adjustedColor)
            it.setTabTextColors(adjustedColor, adjustedColor)
        }
        collapsingLayout?.let {
            it.setContentScrimColor(swatch.rgb)
            it.setStatusBarScrimColor(swatch.rgb)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater?) {
        (activity as MainActivity).menuInflater.inflate(R.menu.artist_toolbar_menu, menu)
        searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem?.actionView as SearchView
        searchView?.setIconifiedByDefault(false)
        val searchIcon = searchView?.findViewById(android.support.v7.appcompat.R.id.search_mag_icon) as ImageView
        searchIcon.setImageDrawable(null)
        searchView?.queryHint = context?.getString(R.string.search)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return if (item?.itemId == R.id.actionFavorite) {
            //add artist to favorites
            true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    private inner class ArtistPagerAdapter(context: Context) : PagerAdapter() {

        private val inflater = LayoutInflater.from(context)

        override fun getCount() = ARTIST_PAGER_ENTRIES_COUNT

        override fun isViewFromObject(view: android.view.View, `object`: Any) = view == `object`

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val artistRecycler = inflater.inflate(R.layout.artist_recycler, container, false) as RecyclerView
            with(artistRecycler) {
                when (position) {
                    0 -> {
                        adapter = tracksAdapter
                        layoutManager = LinearLayoutManager(context)
                    }
                    1 -> {
                        adapter = albumsAdapter
                        layoutManager = GridLayoutManager(context, 2)
                        addItemDecoration(ItemOffsetDecoration(context, 4))
                    }
                    2 -> {
                        adapter = similarArtistsAdapter
                        layoutManager = LinearLayoutManager(context)
                    }
                }
                container.addView(this)
                return artistRecycler
            }
        }

        override fun destroyItem(container: ViewGroup, position: Int, view: Any) {
            container.removeView(view as android.view.View)
        }

        override fun getPageTitle(position: Int) = when (position) {
            0 -> "Songs"
            1 -> "Albums"
            2 -> "Similar"
            else -> throw IllegalArgumentException("Invalid item count was specified")
        }
    }
}
