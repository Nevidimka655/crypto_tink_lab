package com.nevidimka655.tink_lab

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nevidimka655.astracrypt.core.di.IoDispatcher
import com.nevidimka655.astracrypt.utils.Mapper
import com.nevidimka655.tink_lab.domain.model.DataType
import com.nevidimka655.tink_lab.domain.model.Key
import com.nevidimka655.tink_lab.domain.usecase.CreateLabKeyUseCase
import com.nevidimka655.tink_lab.domain.usecase.GetFileAeadListUseCase
import com.nevidimka655.tink_lab.domain.usecase.GetTextAeadListUseCase
import com.nevidimka655.tink_lab.domain.usecase.SaveKeyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TinkLabKeyViewModel @Inject constructor(
    @IoDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
    private val createLabKeyUseCase: CreateLabKeyUseCase,
    private val saveKeyUseCase: SaveKeyUseCase,
    private val uriToStringMapper: Mapper<Uri, String>,
    getFileAeadListUseCase: GetFileAeadListUseCase,
    getTextAeadListUseCase: GetTextAeadListUseCase
) : ViewModel() {
    private val key = MutableStateFlow(Key())
    val keyState = key.asStateFlow()

    val fileAeadList = getFileAeadListUseCase()
    val textAeadList = getTextAeadListUseCase()

    fun save(uri: Uri) = viewModelScope.launch(defaultDispatcher) {
        saveKeyUseCase(
            key = key.value,
            uriString = uriToStringMapper(uri)
        )
    }

    suspend fun shuffleKeyset(
        keysetPassword: String,
        dataType: DataType,
        aeadType: String
    ) = withContext(defaultDispatcher) {
        val newTinkLabKey = createLabKeyUseCase(
            keysetPassword = keysetPassword,
            dataType = dataType,
            aeadType = aeadType
        )
        key.update { newTinkLabKey }
    }

}