package com.fan.tiptop.dockthor.logic

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fan.tiptop.citiapi.data.CitiStationStatus
import com.fan.tiptop.citiapi.data.CitibikeStationInformationModel
import com.fan.tiptop.citiapi.database.CitibikeStationInformationDao
import kotlinx.coroutines.launch

class MainViewModel(val dao: CitibikeStationInformationDao) : ViewModel() {

    //LOG
    private val TAG = "DockThorViewModel"

    //DISPLAY
    val errorToDisplayLD = MutableLiveData("")
    val isLoadingLD = MutableLiveData(false)
    val citiStationStatusLD: MutableLiveData<List<CitiStationStatus>> = MutableLiveData()

    private val _contextualBarNotVisible = MutableLiveData(true)
    val contextualBarNotVisible: LiveData<Boolean>
        get() = _contextualBarNotVisible


    private val _navigateToSwitchFavStation = MutableLiveData(false)
    val navigateToSwitchFavStation: LiveData<Boolean>
        get() = _navigateToSwitchFavStation

    //MODEL
    private var _kernel: DockThorKernel = DockThorKernel.getInstance(dao)
    private val _selectedStationsId: MutableList<Int> = mutableListOf()

    //METHODS
    fun onSwipeRefresh() {
        refreshCitiStationStatusDisplay()
    }

    fun initializeMainViewModel() {
        initializeKernel()
    }

    private fun initializeKernel() {
        isLoadingLD.value = true
        viewModelScope.launch {
            _kernel.initialize { citiStationStatus: List<CitiStationStatus>, errorToDisplay: String ->
                citiStationStatusLD.value = citiStationStatus
                errorToDisplayLD.value = errorToDisplay
                isLoadingLD.value = false
            }
        }
    }

    private fun refreshCitiStationStatusDisplay() {
        isLoadingLD.value = true
        viewModelScope.launch {
            _kernel.updateCitistationList { citiStationStatus: List<CitiStationStatus>, errorToDisplay: String ->
                citiStationStatusLD.value = citiStationStatus
                errorToDisplayLD.value = errorToDisplay
                isLoadingLD.value = false
            }
        }
    }

    private fun toggleSelectedStation(station: CitiStationStatus) {
        if(!station.isFavorite)
        {
            return
        }
        val currentList: List<CitiStationStatus> = citiStationStatusLD.value ?: return
        citiStationStatusLD.value =
            toggleSelectedStatus(
                currentList,
                { x -> x.stationId == station.stationId }) { x -> !x.selected }
        val isSelected =
            getSelectedStatus(citiStationStatusLD.value!!) { x -> x.stationId == station.stationId }.first()
        updateSelectedStationIds(isSelected, station)
        _contextualBarNotVisible.value = _selectedStationsId.isEmpty()
    }

    private fun updateSelectedStationIds(
        isSelected: Boolean,
        station: CitiStationStatus
    ) {
        if (isSelected) {
            _selectedStationsId.add(station.stationId)
        } else {
            _selectedStationsId.remove(station.stationId)
        }
    }

    //MAIN FRAGMENT ACTION
    fun onSetStation(stationModel: CitibikeStationInformationModel) {
        viewModelScope.launch {
            dao.insert(stationModel)
            refreshCitiStationStatusDisplay()
        }
    }


    fun onAddFavStationClicked() {
        _navigateToSwitchFavStation.value = true;
    }

    fun onAddFavStationNavigated() {
        _navigateToSwitchFavStation.value = false;
    }

    fun onItemDeleted() {
        if (_selectedStationsId.isEmpty()) {
            return
        }
        viewModelScope.launch {
            dao.deleteByStationId((_selectedStationsId.distinct()))
            refreshCitiStationStatusDisplay()
            _contextualBarNotVisible.value = true
        }
    }

    fun onClearSelectedStation() {
        citiStationStatusLD.value =
            citiStationStatusLD.value?.let { toggleSelectedStatus(it, { true }) { false } }
        _selectedStationsId.clear()
        _contextualBarNotVisible.value = true;
    }

    fun onActionLongClick(station: CitiStationStatus) {
        toggleSelectedStation(station)
    }

    fun onActionClick(station: CitiStationStatus): Intent? {
        if (!contextualBarNotVisible.value!!) {
                toggleSelectedStation(station)
        } else {
            return _kernel.getActionViewIntent(station)
        }
        return null
    }

    //UTILS
    /* if condition on station selector is true, will copy the CitiStationStatus and apply actionOnSelected */
    private fun toggleSelectedStatus(
        currentList: List<CitiStationStatus>,
        stationSelector: (CitiStationStatus) -> Boolean,
        actionOnSelected: (CitiStationStatus) -> Boolean
    ): List<CitiStationStatus> {
        return currentList.map { x ->
            if (stationSelector(x)) {
                val newX = x.copy(); newX.selected = actionOnSelected(x); newX
            } else x
        }
    }

    private fun getSelectedStatus(
        currentList: List<CitiStationStatus>,
        filter: (CitiStationStatus) -> Boolean
    ): List<Boolean> {
        return currentList.filter { x -> filter(x) }.map { x -> x.selected }
    }

    fun containsInfoModel(stationModel: CitibikeStationInformationModel): Boolean {
        val matchingStation: CitiStationStatus? =
            citiStationStatusLD.value?.find { x -> x.stationId == stationModel.station_id.toInt() }
        return matchingStation != null
    }
}
