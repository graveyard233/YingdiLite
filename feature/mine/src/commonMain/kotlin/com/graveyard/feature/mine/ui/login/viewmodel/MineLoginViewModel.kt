package com.graveyard.feature.mine.ui.login.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

internal class MineLoginViewModel : ViewModel() {
    // Do not put the password in SavedStateHandle or rememberSaveable.
    var password by mutableStateOf("")
}
