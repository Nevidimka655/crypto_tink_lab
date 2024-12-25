package com.nevidimka655.tink_lab

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nevidimka655.astracrypt.core.di.IoDispatcher
import com.nevidimka655.astracrypt.utils.Mapper
import com.nevidimka655.tink_lab.domain.model.DataType
import com.nevidimka655.tink_lab.domain.model.Key
import com.nevidimka655.tink_lab.domain.usecase.CreateLabKeyUseCase
import com.nevidimka655.tink_lab.domain.usecase.GetFileAeadListUseCase
import com.nevidimka655.tink_lab.domain.usecase.GetTextAeadListUseCase
import com.nevidimka655.tink_lab.domain.usecase.LoadKeyUseCase
import com.nevidimka655.tink_lab.domain.usecase.SaveKeyUseCase
import com.nevidimka655.tink_lab.domain.util.KeyReader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val KEYSET_PASSWORD = "kp"
private const val KEYSET_URI_LOAD = "kp_uri_load"

@HiltViewModel
class TinkLabKeyViewModel @Inject constructor(
    @IoDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
    private val state: SavedStateHandle,
    private val createLabKeyUseCase: CreateLabKeyUseCase,
    private val saveKeyUseCase: SaveKeyUseCase,
    private val loadKeyUseCase: LoadKeyUseCase,
    private val uriToStringMapper: Mapper<Uri, String>,
    getFileAeadListUseCase: GetFileAeadListUseCase,
    getTextAeadListUseCase: GetTextAeadListUseCase
) : ViewModel() {
    private val key = MutableStateFlow(Key())
    val keyState = key.asStateFlow()

    val keysetUriToLoadState = state.getStateFlow(KEYSET_URI_LOAD, "")
    val keysetPasswordState = state.getStateFlow(KEYSET_PASSWORD, "")

    val fileAeadList = getFileAeadListUseCase()
    val textAeadList = getTextAeadListUseCase()

    fun save(uri: Uri) = viewModelScope.launch(defaultDispatcher) {
        saveKeyUseCase(
            key = key.value,
            uriString = uriToStringMapper(uri),
            keysetPassword = keysetPasswordState.value
        )
    }

    fun load() = viewModelScope.async(defaultDispatcher) {
        val result = loadKeyUseCase(
            uriString = keysetUriToLoadState.value,
            keysetPassword = keysetPasswordState.value
        )
        if (result is KeyReader.Result.Success) {
            key.update { result.key }
            true
        } else false
    }

    suspend fun shuffleKeyset(
        dataType: DataType,
        aeadType: String
    ) = withContext(defaultDispatcher) {
        val newTinkLabKey = createLabKeyUseCase(
            keysetPassword = keysetPasswordState.value,
            dataType = dataType,
            aeadType = aeadType
        )
        key.update { newTinkLabKey }
    }

    fun setKeysetPassword(keysetPassword: String) {
        state[KEYSET_PASSWORD] = keysetPassword
    }

    fun setKeysetUriLoad(uri: Uri) {
        state[KEYSET_URI_LOAD] = uriToStringMapper(uri)
    }

}