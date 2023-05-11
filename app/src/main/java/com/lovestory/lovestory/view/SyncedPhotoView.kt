package com.lovestory.lovestory.view

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.lovestory.lovestory.database.PhotoDatabase
import com.lovestory.lovestory.database.entities.SyncedPhoto
import com.lovestory.lovestory.database.repository.SyncedPhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncedPhotoView(application:Application): ViewModel() {
    private lateinit var syncedPhotoRepository: SyncedPhotoRepository

    lateinit var listOfSyncPhotos : LiveData<List<SyncedPhoto>>
    lateinit var dayListOfSyncedPhotos :LiveData<List<SyncedPhoto>>
    lateinit var monthListOfSyncedPhotos : LiveData<List<SyncedPhoto>>
    lateinit var yearListOfSyncedPhotos : LiveData<List<SyncedPhoto>>

    lateinit var groupedSyncedPhotosByDate: LiveData<Map<String, List<SyncedPhoto>>>
    lateinit var syncedPhotosByDateAndArea : LiveData<Map<String, Map<Pair<String, String>, List<SyncedPhoto>>>>
    lateinit var sizesOfInnerElements : LiveData<List<List<Int>>>
    lateinit var cumOfSizeOfInnerElements : LiveData<List<List<Int>>>

    lateinit var daySyncedPhotosByDate : LiveData<Map<String, List<SyncedPhoto>>>

//    lateinit var checkedSyncedPhotosList : LiveData<Map<String, List<Boolean>>>

    private val _syncedPhoto = MutableLiveData<SyncedPhoto?>()
    val syncedPhoto: LiveData<SyncedPhoto?> = _syncedPhoto

//    var listOfSelectedPhotos = MutableLiveData<Set<String>>(setOf())
//    val listOfSelectedPhoto : LiveData<Set<String>> = _listOfSelectedPhotos

    init {
        val photoDatabase = PhotoDatabase.getDatabase(application)
        val syncedPhotoDao =  photoDatabase.syncedPhotoDao()
        syncedPhotoRepository = SyncedPhotoRepository(syncedPhotoDao)

        listOfSyncPhotos = syncedPhotoRepository.getAllSyncedPhotos
        dayListOfSyncedPhotos = syncedPhotoRepository.getDaySyncedPhotos
        monthListOfSyncedPhotos = syncedPhotoRepository.getMonthSyncedPhotos
        yearListOfSyncedPhotos = syncedPhotoRepository.getYearSyncedPhotos

        groupedSyncedPhotosByDate = Transformations.map(listOfSyncPhotos) { syncedPhotos ->
            syncedPhotos.groupBy { it.date.substring(0, 10) }
        }

//        checkedSyncedPhotosList = Transformations.map(listOfSyncPhotos){syncedPhotos ->
//            syncedPhotos.groupBy { it.date.substring(0, 10) }.mapValues { entry ->  entry.value.map{false}}
//        }

        syncedPhotosByDateAndArea = Transformations.map(listOfSyncPhotos){syncedPhotos->
            syncedPhotos.groupBy { it.date.substring(0, 10) }
                .mapValues { entry ->
                    entry.value.groupBy { Pair(it.area1, it.area2) } }
        }

        sizesOfInnerElements = Transformations.map(syncedPhotosByDateAndArea){syncedPhotosByDateAndAreaMap->
            syncedPhotosByDateAndAreaMap.map { entry ->
                entry.value.map { groupedByArea ->
                    groupedByArea.value.size/3
                }
            }
        }

        daySyncedPhotosByDate = Transformations.map(dayListOfSyncedPhotos){syncedPhoto->
            syncedPhoto.groupBy { it.date.substring(0, 10) }
        }

        cumOfSizeOfInnerElements = Transformations.map(sizesOfInnerElements){
            computeCumulativeSizes(it)
        }
    }

    fun computeCumulativeSizes(sizes: List<List<Int>>): List<List<Int>> {
        val cumulativeList = mutableListOf<MutableList<Int>>()

        var cumValue = 0
        for (i in sizes.indices) {
            cumulativeList.add(mutableListOf())
            for (j in 0 until sizes[i].size) {
                cumulativeList[i].add(cumValue)
                cumValue += sizes[i][j]
            }

            cumValue += 2
        }
        return cumulativeList
    }

    fun updateSyncedPhoto(newPhoto: SyncedPhoto) {
        _syncedPhoto.value = newPhoto
    }

    fun getAllSyncedPhotoIndex(photo: SyncedPhoto):Int{
        return listOfSyncPhotos.value!!.indexOf(photo)
    }

//    fun addListOfSelectedPhotos(id : String){
//        Log.d("[VIEW] SyncedPhotoView", "id : $id added")
//        listOfSelectedPhotos.value?.toMutableSet()?.apply { add(id) }
//        Log.d("[VIEW] SyncedPhotoView", "${listOfSelectedPhotos.value}")
//    }
//    fun removeListOfSelectedPhotos(id : String){
//        Log.d("[VIEW] SyncedPhotoView", "id : $id removed")
//        listOfSelectedPhotos.value?.toMutableSet()?.apply { remove(id) }
//        Log.d("[VIEW] SyncedPhotoView", "${listOfSelectedPhotos.value}")
//    }
//
//    fun checkListOfSelectedPhotos(id : String): Boolean? {
//        return listOfSelectedPhotos.value?.contains(id)
//    }
}