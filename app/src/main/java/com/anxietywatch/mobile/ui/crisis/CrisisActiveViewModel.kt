package com.anxietywatch.mobile.ui.crisis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anxietywatch.mobile.data.remote.AnxietyWatchApi
import com.anxietywatch.mobile.data.remote.ProfileResponseDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CrisisProfileUiState {
    data object Loading : CrisisProfileUiState
    data class Loaded(val profile: ProfileResponseDto?) : CrisisProfileUiState
}

@HiltViewModel
class CrisisActiveViewModel @Inject constructor(
    api: AnxietyWatchApi,
) : ViewModel() {
    private val _profile = MutableStateFlow<CrisisProfileUiState>(CrisisProfileUiState.Loading)
    val profile: StateFlow<CrisisProfileUiState> = _profile.asStateFlow()

    init {
        viewModelScope.launch {
            _profile.value = CrisisProfileUiState.Loaded(runCatching { api.getProfile() }.getOrNull())
        }
    }
}
