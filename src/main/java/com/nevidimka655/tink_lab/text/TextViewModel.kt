package com.nevidimka655.tink_lab.text

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TextViewModel @Inject constructor(
    private val state: SavedStateHandle
): ViewModel() {



}