package com.nevidimka655.tink_lab.key

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nevidimka655.astracrypt.resources.R
import com.nevidimka655.tink_lab.domain.model.DataItem
import com.nevidimka655.tink_lab.domain.model.DataType
import com.nevidimka655.tink_lab.domain.model.Key
import com.nevidimka655.tink_lab.key.menu.AeadTypeMenu
import com.nevidimka655.tink_lab.key.menu.DataTypeMenu
import com.nevidimka655.ui.compose_core.FilledTonalButtonWithIcon
import com.nevidimka655.ui.compose_core.ext.LocalWindowWidth
import com.nevidimka655.ui.compose_core.ext.isCompact
import com.nevidimka655.ui.compose_core.theme.spaces
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@Composable
fun KeyScreen(
    modifier: Modifier = Modifier,
    onRequestKeysetChannel: Flow<Unit>,
    navigateToTextMode: (keyset: String) -> Unit,
    navigateToFilesMode: (keyset: String) -> Unit
) {
    val vm: KeyViewModel = hiltViewModel()
    val context = LocalContext.current

    val dataType by vm.dataTypeState.collectAsStateWithLifecycle(DataType.Files)
    val aeadType by vm.aeadTypeState.collectAsStateWithLifecycle()
    val keysetPassword by vm.keysetPasswordState.collectAsStateWithLifecycle()
    var keysetPasswordErrorState by remember { mutableStateOf(false) }
    val keysetUriToLoadState by vm.keysetUriToLoadState.collectAsStateWithLifecycle()
    val isLoadMode = remember(keysetUriToLoadState) { keysetUriToLoadState.isNotEmpty() }

    LaunchedEffect(Unit) {
        onRequestKeysetChannel.collectLatest {
            val key: Key
            if (keysetUriToLoadState.isNotEmpty()) {
                val loadedKey = vm.load()
                if (loadedKey == null) {
                    keysetPasswordErrorState = true
                    Toast.makeText(
                        context, context.getString(R.string.t_invalidPass), Toast.LENGTH_SHORT
                    ).show()
                    delay(3.seconds)
                    keysetPasswordErrorState = false
                    return@collectLatest
                } else key = loadedKey
            } else key = vm.createKey()
            when (key.dataType) {
                DataType.Files -> navigateToFilesMode(key.rawKeyset)
                DataType.Text -> navigateToTextMode(key.rawKeyset)
            }
        }
    }

    val openContract = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        if (it != null) vm.setKeysetUriLoad(it)
    }

    val saveContract = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { if (it != null) vm.save(uri = it) }

    Screen(
        modifier = modifier,
        fileAeadList = vm.fileAeadList,
        textAeadList = vm.textAeadList,
        isLoadingMode = isLoadMode,
        isWrongPassword = keysetPasswordErrorState,
        dataTypeIndex = dataType.ordinal,
        aeadType = aeadType,
        onSelectDataType = { vm.setDataType(it) },
        onSelectAeadType = { vm.setAeadType(it) },
        onLoadClick = { openContract.launch(arrayOf("text/plain")) },
        onSaveClick = { saveContract.launch("ac_key_${abs(Random.nextInt())}.txt") },
        keysetKey = keysetPassword,
        onChangeKeysetKey = { vm.setKeysetPassword(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun Screen(
    modifier: Modifier = Modifier,
    fileAeadList: List<String> = listOf(),
    textAeadList: List<String> = listOf(),
    isLoadingMode: Boolean = false,
    isWrongPassword: Boolean = false,
    dataTypes: List<DataItem> = listOf(
        DataItem(R.string.files, DataType.Files), DataItem(R.string.text, DataType.Text)
    ),
    dataTypeIndex: Int = 0,
    aeadType: String = "TEST_AEAD_TYPE",
    onSelectDataType: (DataType) -> Unit = {},
    onSelectAeadType: (String) -> Unit = {},
    onLoadClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    keysetKey: String = "",
    onChangeKeysetKey: (String) -> Unit = {}
) = Box(
    modifier = modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState()),
    contentAlignment = Alignment.Center
) {
    val dataType = remember(dataTypeIndex) { dataTypes[dataTypeIndex] }
    ElevatedCard {
        val localWindowWidth = LocalWindowWidth.current
        val defaultVerticalArrangement = Arrangement.spacedBy(
            MaterialTheme.spaces.spaceMedium, Alignment.CenterVertically
        )
        val defaultHorizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.spaceSmall)
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(MaterialTheme.spaces.spaceMedium),
            verticalArrangement = defaultVerticalArrangement,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var showDataTypeMenu by remember { mutableStateOf(false) }

            @Composable
            fun dataTypeMenu(modifier: Modifier = Modifier.fillMaxWidth()) = DataTypeMenu(
                modifier = modifier,
                expanded = showDataTypeMenu,
                enabled = !isLoadingMode,
                text = stringResource(id = dataType.titleResId),
                label = stringResource(id = R.string.lab_dataType),
                onExpandedChange = { if (!isLoadingMode) showDataTypeMenu = it },
                items = dataTypes,
                onSelect = onSelectDataType
            )

            var showAeadTypeMenu by remember { mutableStateOf(false) }
            val aeadTypes = remember(dataType) {
                when (dataType.type) {
                    DataType.Files -> fileAeadList
                    DataType.Text -> textAeadList
                }
            }
            LaunchedEffect(aeadTypes) { onSelectAeadType(aeadTypes[0]) }

            @Composable
            fun aeadTypeMenu() = AeadTypeMenu(
                expanded = showAeadTypeMenu,
                enabled = !isLoadingMode,
                text = aeadType,
                label = stringResource(id = R.string.encryption_type),
                onExpandedChange = { if (!isLoadingMode) showAeadTypeMenu = it },
                items = aeadTypes,
                onSelect = onSelectAeadType
            )

            @Composable
            fun keysetKeyField() = KeysetKeyTextField(
                value = keysetKey,
                onValueChange = onChangeKeysetKey,
                label = stringResource(id = R.string.lab_keySetPassword),
                isError = isWrongPassword
            )

            @Composable
            fun toolbar(modifier: Modifier = Modifier) = Row(
                horizontalArrangement = defaultHorizontalArrangement,
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
            ) {
                ToolbarButton(
                    imageVector = Icons.Default.Add,
                    text = stringResource(id = R.string.load),
                    modifier = Modifier.weight(0.5f),
                    onClick = onLoadClick
                )
                ToolbarButton(
                    imageVector = Icons.Default.Save,
                    text = stringResource(id = R.string.save),
                    modifier = Modifier.weight(0.5f),
                    onClick = onSaveClick
                )
            }

            if (localWindowWidth.isCompact) {
                keysetKeyField()
                dataTypeMenu()
                aeadTypeMenu()
                if (!isLoadingMode) toolbar()
            } else {
                Row(horizontalArrangement = defaultHorizontalArrangement) {
                    val horizontalAlignment = Alignment.CenterHorizontally
                    Column(
                        modifier = Modifier.weight(0.5f),
                        verticalArrangement = defaultVerticalArrangement,
                        horizontalAlignment = horizontalAlignment
                    ) {
                        keysetKeyField()
                        dataTypeMenu()
                    }
                    Column(
                        modifier = Modifier.weight(0.5f),
                        verticalArrangement = defaultVerticalArrangement,
                        horizontalAlignment = horizontalAlignment
                    ) {
                        aeadTypeMenu()
                        if (!isLoadingMode) toolbar(
                            modifier = Modifier.height(TextFieldDefaults.MinHeight)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeysetKeyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean
) {
    var passwordToggleState by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        leadingIcon = { Icon(imageVector = Icons.Default.Key, null) },
        supportingText = {
            Text(
                text = "${value.length}",
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        },
        label = { Text(text = label) },
        trailingIcon = {
            IconButton(onClick = { passwordToggleState = !passwordToggleState }) {
                Icon(
                    imageVector = if (!passwordToggleState) {
                        Icons.Default.VisibilityOff
                    } else Icons.Default.Visibility, contentDescription = null
                )
            }
        },
        visualTransformation = if (!passwordToggleState) {
            PasswordVisualTransformation()
        } else VisualTransformation.None,
        singleLine = true,
        isError = isError,
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview
@Composable
private fun ToolbarButton(
    modifier: Modifier = Modifier,
    imageVector: ImageVector = Icons.Default.Save,
    text: String = "Button",
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) = FilledTonalButtonWithIcon(
    modifier = modifier, enabled = enabled, icon = imageVector, title = text, onClick = onClick
)