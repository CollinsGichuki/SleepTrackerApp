/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.*
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.launch

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(val database: SleepDatabaseDao, application: Application) : AndroidViewModel(application) {
    //Hold the current night
    private var tonight = MutableLiveData<SleepNight?>()
    private val nights = database.getAllNights()

    //Navigation LiveData
    private val _navigateToSleepQuality = MutableLiveData<SleepNight>()
    val navigateToSleepQuality: LiveData<SleepNight>
        get() = _navigateToSleepQuality

    //Resets the navigation LiveData
    fun doneNavigating() {
        _navigateToSleepQuality.value = null
    }

    //Transform the nights data to a HTML formatted String
    val nightString = Transformations.map(nights) { nights ->
        formatNights(nights, application.resources)
    }

    init {
        initializeTonight()
    }

    private fun initializeTonight() {
        //Launch is a coroutine builder
        viewModelScope.launch {
            tonight.value = getTonightFromDatabase()
        }
    }

    private suspend fun getTonightFromDatabase(): SleepNight? {
        //Get the last entry
        var night = database.getTonight()

        if (night?.endTimeInMilli != night?.startTimeInMillis) {
            //Night has already ended
            night = null
        }
        return night
    }

    fun onStartTracking() {
        //Need this result to continue and update the UI
        viewModelScope.launch {
            //Create a new SleepNight which captures the currentTime as the start time
            val newNight = SleepNight()
            insert(newNight)

            //Update the night value
            tonight.value = getTonightFromDatabase()
        }
    }

    private suspend fun insert(night: SleepNight) {
        //Coroutine with Room uses Dispatchers.IO
        database.insert(night)
    }

    fun onStopTracking() {
        viewModelScope.launch {
            val oldNights = tonight.value ?: return@launch
            oldNights.endTimeInMilli = System.currentTimeMillis()
            update(oldNights)
            //navigate to SleepQuality Fragment
            _navigateToSleepQuality.value = oldNights
        }
    }

    private suspend fun update(night: SleepNight) {
        database.update(night)
    }

    fun onClear() {
        viewModelScope.launch {
            clear()
            tonight.value = null
        }
    }

    private suspend fun clear() {
        database.clear()
    }
}

