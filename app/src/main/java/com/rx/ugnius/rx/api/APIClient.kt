package com.rx.ugnius.rx.api

import com.rx.ugnius.rx.api.entities.Album
import com.rx.ugnius.rx.api.entities.Artist
import com.rx.ugnius.rx.api.entities.Track
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface APIClient {

    @GET("v1/artists/{artistId}")
    fun getArtist(@Path("artistId") artistId: String): Single<Artist>

    @GET("v1/artists/{artistId}/top-tracks")
    fun getArtistTopTracks(@Path("artistId") artistId: String, @Query("country") country: String): Observable<List<Track>>

    @GET("v1/artists/{artistId}/albums")
    fun getArtistAlbums(@Path("artistId") artistId: String): Observable<List<Album>>

    @GET("v1/albums/{albumId}/tracks")
    fun getAlbumsTracks(@Path("albumId") albumId: String): Observable<List<Track>>


    @GET("v1/artists/{artistId}/related-artists")
    fun getArtistRelatedArtists(@Path("artistId") artistId: String): Observable<List<Artist>>

}
