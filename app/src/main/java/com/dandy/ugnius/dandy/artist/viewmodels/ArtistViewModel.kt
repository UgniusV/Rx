package com.dandy.ugnius.dandy.artist.viewmodels

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.dandy.ugnius.dandy.global.clients.APIClient
import com.dandy.ugnius.dandy.global.entities.Album
import com.dandy.ugnius.dandy.global.entities.Artist
import com.dandy.ugnius.dandy.global.entities.Error
import com.dandy.ugnius.dandy.global.entities.Track
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.subscribeBy
import java.text.SimpleDateFormat
import java.util.*

//Navigation between multiple artists fragments eventually will result in HTTP 429 (Too many requests)
//Later on this will be fixed but for now please proceed with caution
class ArtistViewModel(private val apiClient: APIClient) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()
    private val formatter = SimpleDateFormat("yyyy-mm-dd", Locale.getDefault())
    private var topTracksObservable: Observable<List<Track>>? = null

    //todo return immutable live data to the view
    var artist = MutableLiveData<Artist>()
    var topTracks = MutableLiveData<List<Track>>()
    var tracks = MutableLiveData<List<Track>>()
    var albums = MutableLiveData<List<Album>>()
    var similarArtists = MutableLiveData<List<Artist>>()
    var error = MutableLiveData<Error>()

    fun query(artistId: String, market: String, groups: String) {
        queryArtist(artistId)
        queryTopTracks(artistId, market)
        querySimilarArtists(artistId, market)
        queryAllTracksAndAlbums(artistId, groups, market)
    }

    private fun getTopTracksObservable(artistId: String, market: String): Observable<List<Track>> {
        if (topTracksObservable == null) {
            topTracksObservable = apiClient.getArtistTopTracks(artistId, market).cache()
        }
        return topTracksObservable!!
    }

    private fun queryArtist(artistId: String) {
        val disposable = apiClient.getArtist(artistId)
            .subscribeBy(
                onSuccess = { artist.postValue(it) },
                onError = { it.message?.let { error.postValue(Error(it)) } }
            )
        compositeDisposable.add(disposable)
    }

    private fun queryTopTracks(artistId: String, market: String) {
        val disposable = getTopTracksObservable(artistId, market)
            .subscribeBy(
                onNext = { topTracks.postValue(it) },
                onError = { it.message?.let { error.postValue(Error(it)) } }
            )
        compositeDisposable.add(disposable)
    }

    private fun querySimilarArtists(artistId: String, market: String) {

        fun queryArtistTopThreeTracks(artistId: String, market: String): Observable<List<Track>> {
            return apiClient.getArtistTopTracks(artistId, market)
                .flatMapIterable { it }
                .take(3)
                .toList()
                .toObservable()
        }

        val disposable = apiClient.getSimilarArtists(artistId)
            .flatMapIterable { it }
            .flatMap(
                { queryArtistTopThreeTracks(it.id, market) },
                { artist, tracks -> artist.also { it.tracks = tracks } }
            )
            .toSortedList { lhs: Artist, rhs: Artist ->
                when {
                    rhs.followers > lhs.followers -> 1
                    rhs.followers == lhs.followers -> 0
                    else -> -1
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { similarArtists.postValue(it) },
                onError = { it.message?.let { error.postValue(Error(it)) } }
            )
        compositeDisposable.add(disposable)
    }

    private fun queryAllTracksAndAlbums(artistId: String, groups: String, market: String) {
        val disposable = apiClient.getArtistAlbums(artistId, groups)
            .flatMapIterable { it }
            .flatMap(
                { apiClient.getAlbumsTracks(it.id) },
                { album, tracks ->
                    tracks.forEach { it.images = album.images }
                    album.also { it.tracks = tracks }
                }
            )
            .toSortedList { lhs, rhs ->
                val firstDate = formatter.format(formatter.parse(rhs.releaseDate))
                val secondDate = formatter.format(formatter.parse(lhs.releaseDate))
                firstDate.compareTo(secondDate)
            }
            .toObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .zipWith(
                getTopTracksObservable(artistId, market),
                BiFunction { albums: List<Album>, topTracks: List<Track> ->
                    this.tracks.postValue(LinkedHashSet<Track>(topTracks + albums.flatMap { it.tracks!! }).toList())
                    this.albums.postValue(LinkedHashSet<Album>(albums).toList())
                })
            .subscribeBy(onError = { it.message?.let { error.postValue(Error(it)) } })
        compositeDisposable.add(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }

}