package roxy.music.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import roxy.music.app.network.NetworkBannerUiState
import roxy.music.app.network.ObserveNetworkBannerStateUseCase

@HiltViewModel
class NetworkBannerViewModel
@Inject
constructor(
    observeNetworkBannerStateUseCase: ObserveNetworkBannerStateUseCase,
) : ViewModel() {
    val bannerState: StateFlow<NetworkBannerUiState> =
        observeNetworkBannerStateUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = NetworkBannerUiState.Hidden,
            )
}
