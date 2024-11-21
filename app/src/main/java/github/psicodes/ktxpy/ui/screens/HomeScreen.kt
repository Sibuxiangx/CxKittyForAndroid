/*
Copyright (C) 2022-2023  PsiCodes

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package github.psicodes.ktxpy.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import github.psicodes.ktxpy.R
import github.psicodes.ktxpy.activities.EditorActivity
import github.psicodes.ktxpy.activities.HomeActivity
import github.psicodes.ktxpy.activities.TermActivity
import github.psicodes.ktxpy.ui.layoutComponents.FullScreenMessage
import github.psicodes.ktxpy.ui.layoutComponents.ListComponent
import github.psicodes.ktxpy.ui.layoutComponents.MenuItem
import github.psicodes.ktxpy.ui.screens.destinations.AboutScreenDestination
import github.psicodes.ktxpy.utils.Keys
import github.psicodes.ktxpy.utils.SessionManager
import github.psicodes.ktxpy.viewModels.HomeScreenViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "CoroutineCreationDuringComposition")
@RootNavGraph(start = true)
@Destination
@Composable
fun HomeScreen(welcomeActivity: HomeActivity, navigator: DestinationsNavigator) {
    val mViewModel = ViewModelProvider(welcomeActivity)[HomeScreenViewModel::class.java]
    val scope = rememberCoroutineScope()
    val sessionsList by  SessionManager.sessionList
    val items = listOf(
        MenuItem("Terminal", "终端", R.drawable.terminal_icon) {
            welcomeActivity.startActivity(Intent(welcomeActivity, TermActivity::class.java))
            scope.launch { mViewModel.mDrawerState.value.close() }
        },

//        MenuItem("Samples", "Samples", R.drawable.sample_icon) {
//            navigator.navigate(SampleScreenDestination)
//            scope.launch { mViewModel.mDrawerState.value.close() }
//        },
        MenuItem("PythonShell", "模拟", R.drawable.interactive_mode_icon) {
            val intent = Intent(welcomeActivity, TermActivity::class.java)
            intent.putExtra(Keys.IS_SHELL_MODE_KEY, true)
            welcomeActivity.startActivity(intent)
            scope.launch { mViewModel.mDrawerState.value.close() }
        },
        MenuItem("Config", "配置", R.drawable.library_icon) {
            val mIntent = Intent()
            mIntent.setClass(welcomeActivity, EditorActivity::class.java)
            mIntent.putExtra(Keys.KEY_FILE_PATH, welcomeActivity.filesDir.resolve("cxkitty/config.yml").absolutePath)
            welcomeActivity.startActivity(mIntent)
            scope.launch { mViewModel.mDrawerState.value.close() }
        },
//        MenuItem("New File", "Create new file", R.drawable.create_file_icon) {
//            scope.launch {
//                mViewModel.showDialog()
//            }
//        },
//        MenuItem("Open File", "Open file", R.drawable.file_open_icon) {
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
//                ActivityCompat.requestPermissions(
//                    welcomeActivity,
//                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
//                     1
//                )
//                if (ContextCompat.checkSelfPermission(
//                        welcomeActivity,
//                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
//                    ) == PackageManager.PERMISSION_GRANTED
//                ) {
//                    navigator.navigate(FilePickerScreenDestination)
//                }
//            }
//            else {
//                if (Environment.isExternalStorageManager()){
//                    navigator.navigate(FilePickerScreenDestination)
//                }
//                else {
//                    Toast.makeText(welcomeActivity,"Please grant storage permission",Toast.LENGTH_SHORT).show()
//                    PermissionManageExternal.request(welcomeActivity)
//                }
//            }
//        },
        MenuItem("Info", "关于", R.drawable.about_icon) {
            navigator.navigate(AboutScreenDestination)
            scope.launch { mViewModel.mDrawerState.value.close() }
        }
    )
    ModalNavigationDrawer(
        drawerState = mViewModel.mDrawerState.value,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .sizeIn(
                        minWidth = 240.dp,
                        maxWidth = 290.dp
                    )
                    .fillMaxHeight()
            ) {
                Spacer(Modifier.height(12.dp))
                items.forEach { item ->
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = item.resID),
                                contentDescription = null
                            )
                        },
                        label = { Text(item.title) },
                        selected = false,
                        onClick = item.clickable,
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(title = {
                    Text(
                        text = "AutoMirei",
                        fontFamily = FontFamily(Font(resId = R.font.roboto_condensed_bold))
                    )
                },
                    modifier = Modifier
                        .padding(10.dp, 0.dp, 10.dp, 2.dp)
                    ,
                    navigationIcon = {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = "Menu",
                            modifier = Modifier.clickable { scope.launch { mViewModel.mDrawerState.value.open() } })
                    },
                    actions = {
                        Icon(Icons.Default.Info, contentDescription = "Info",
                            Modifier
                                .size(26.dp)
                                .clickable { navigator.navigate(AboutScreenDestination) })
                    })
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            mViewModel.showDialog()
                        }
                    },
                    content = { Icon(Icons.Filled.Add, contentDescription = "Plus button") },
                )
            }
        ) {
            if (sessionsList.isNotEmpty()) {
                LazyColumn(
                    Modifier.padding(it)
                ) {
                    items(sessionsList.size) { index ->
                        ListComponent(
                            session = sessionsList[index],
                            modifier = Modifier
                                .padding(5.dp)
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    throw NotImplementedError("Not implemented")
                                }
                        )
                        Divider(Modifier.fillMaxWidth(1f))
                    }
                }
            } else {
                FullScreenMessage(
                    icon = painterResource(id = R.drawable.file_icon),
                    title = "无会话",
                    message = "点击 \"+\" 按钮创建会话"
                )
            }
            if (mViewModel.mDialogState.value) {
                AlertDialog(
                    onDismissRequest = {
                        mViewModel.dismissDialog()
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { scope.launch {
                                val intent = Intent(welcomeActivity, TermActivity::class.java)
                                intent.putExtra(Keys.IS_SHELL_MODE_KEY, false)
                                intent.putExtra(Keys.KEY_NO_ENV_CHECK, true)
                                intent.putExtra(Keys.KEY_PRESCRIPT, "cd cxkitty && python main.py && echo '[Enter to Exit]' && read junk && exit")
                                welcomeActivity.startActivity(intent)
                                scope.launch { mViewModel.mDrawerState.value.close() }
                                mViewModel.dismissDialog()
                            }}
                        ) {
                            Text("启动")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { mViewModel.dismissDialog()}
                        ) {
                            Text("取消")
                        }
                    },
                    text = {
                        TextField(
                            value = mViewModel.mFileName.value,
                            onValueChange = { it:String->
                                scope.launch {
                                mViewModel.changeFileName(it)
                                }
                            },
                            maxLines = 1,
                            keyboardOptions = KeyboardOptions()
                        )
                    },
                    title = {
                        Text(text = "输入会话名称")
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.create_file_icon),
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}