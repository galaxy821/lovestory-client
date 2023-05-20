package com.lovestory.lovestory.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.lovestory.lovestory.R
import com.lovestory.lovestory.database.PhotoDatabase
import com.lovestory.lovestory.database.entities.PhotoForSync
import com.lovestory.lovestory.database.entities.PhotoForSyncDao
import com.lovestory.lovestory.database.repository.PhotoForSyncRepository
import com.lovestory.lovestory.module.photo.uploadPhoto
import com.lovestory.lovestory.ui.components.CheckableDisplayImageFromUri
import com.lovestory.lovestory.ui.components.CheckableDisplayImageFromUriWithPicker
import com.lovestory.lovestory.ui.components.DeletPhotoDialog
import com.lovestory.lovestory.ui.components.DropDownIcon
import com.lovestory.lovestory.view.PhotoForSyncView
import kotlinx.coroutines.*

@Composable
fun PhotoSyncScreen(navHostController: NavHostController, photoForSyncView: PhotoForSyncView){
    val notSyncedPhotos by photoForSyncView.listOfPhotoForSync.observeAsState(initial = listOf())
    val additionalNotSync by photoForSyncView.listOfAdditionPhotosForSync.observeAsState(initial = listOf())

    var isDropMenuForRemovePhoto = remember {mutableStateOf(false)}
    val showDeletePhotoDialog = remember { mutableStateOf(false) }

    val numOfCurrentUploadedPhoto = remember { mutableStateOf(0) }
    val numOfTotalUploadPhoto = remember { mutableStateOf(0) }
    val showUploadPhotoDialog = remember { mutableStateOf(false) }
    val onDismissRequest : () -> Unit = {showUploadPhotoDialog.value = false}

    val context = LocalContext.current

    val systemUiController = rememberSystemUiController()

    val photoDatabase: PhotoDatabase = PhotoDatabase.getDatabase(context)
    val photoForSyncDao : PhotoForSyncDao = photoDatabase.photoForSyncDao()
    val photoForSyncRepository = PhotoForSyncRepository(photoForSyncDao)

    LaunchedEffect(key1 = notSyncedPhotos) {
        photoForSyncView.checkPhotoList.value = MutableList<Boolean>(notSyncedPhotos.size) { true }

        if (notSyncedPhotos.size > photoForSyncView.checkPhotoList.value.size) {
            val newSize = notSyncedPhotos.size
            val oldSize = photoForSyncView.checkPhotoList.value.size
            val newCheckPhotoList = photoForSyncView.checkPhotoList.value.toMutableList()

            for (i in oldSize until newSize) {
                newCheckPhotoList.add(true)
            }

            photoForSyncView.checkPhotoList.value = newCheckPhotoList
        }
    }

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color(0xFFF3F3F3),
        )
    }

    val onChangeChecked: (Int) -> Unit = { index ->
        photoForSyncView.checkPhotoList.value = photoForSyncView.checkPhotoList.value.toMutableList().also {
            it[index] = !it[index]
        }
    }


    Box(
        modifier = Modifier
            .background(Color.White)
            .fillMaxSize(),
    ){
        AnimatedVisibility (visible = showDeletePhotoDialog.value, enter = fadeIn(), exit = fadeOut()) {
            DeletPhotoDialog(
                showDeletePhotoDialog = showDeletePhotoDialog,
                notSyncedPhotos = notSyncedPhotos,
                checkPhotoList = photoForSyncView.checkPhotoList.value,
                photoForSyncRepository = photoForSyncRepository,
                navHostController = navHostController,
                context = context,
            )
        }
        AnimatedVisibility(visible = showUploadPhotoDialog.value, enter = fadeIn(), exit = fadeOut()){
            Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside=false)) {
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(15.dp))
                        .background(color = Color.White)
                        .padding(vertical = 20.dp, horizontal = 10.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,

                    ){
                    LinearProgressIndicator(
                        progress = numOfCurrentUploadedPhoto.value.toFloat()/numOfTotalUploadPhoto.value.toFloat(),
                        color = Color(0xFFFCC5C5),
                        backgroundColor = Color(0xBBF3F3F3),
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .height(5.dp)
                    )
                    Text(
                        text = "사진 업로드 중 (${numOfCurrentUploadedPhoto.value} / ${numOfTotalUploadPhoto.value})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }



        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .padding(bottom = 10.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
            contentPadding = PaddingValues(top = 65.dp, bottom = 65.dp)
        ) {
            mapOf<String, List<Any>>( "camera" to listOf(notSyncedPhotos)  , "gallery" to listOf(additionalNotSync)).forEach {
                item (
                    span = {
                        GridItemSpan(
                            maxLineSpan
                        )
                    }

                ){
                    Column(
                        modifier = Modifier
                            .padding(vertical = 10.dp, horizontal = 15.dp),
//                        verticalArrangement = Arrangement.Center
                    ){
                        if(it.key =="camera"){
                            Text(text = "카메라로 찍은 사진 : ${notSyncedPhotos.size}장", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 5.dp))
                        }else if(it.key == "gallery"){
                            Divider(color = Color.LightGray, thickness = 1.dp)
                            Text(text = "갤러리에서 선택된 사진 : ${additionalNotSync.size}장", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp, bottom = 5.dp))
                        }
                    }
                }

                if(it.key =="camera"){
                    items(notSyncedPhotos.size) { index ->
                        if (index < notSyncedPhotos.size && index < photoForSyncView.checkPhotoList.value.size){
                            CheckableDisplayImageFromUri(
                                index = index,
                                checked = photoForSyncView.checkPhotoList.value[index],
                                imageInfo = notSyncedPhotos[index],
                                onChangeChecked = onChangeChecked,
                                navHostController = navHostController
                            )
                        }
                    }
                }

                else if(it.key == "gallery"){
                    items(additionalNotSync.size){ index ->
                        if(index < additionalNotSync.size){
                            CheckableDisplayImageFromUriWithPicker(
                                navHostController = navHostController,
                                index = index,
                                checked = false,
                                imageInfo = additionalNotSync[index],
                                onChangeChecked = {index:Int->{index}}
                            )
                        }


                    }
                }
            }
        }



        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxWidth()
        ){
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xFFF3F3F3))
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 20.dp)
            ){
                Row() {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_arrow_back_24),
                        contentDescription = null,
                        modifier = Modifier.clickable {navHostController.popBackStack() }
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = "사진 업로드",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                DropDownIcon(
                    isDropMenuForRemovePhoto = isDropMenuForRemovePhoto,
                    showDeletePhotoDialog = showDeletePhotoDialog,
                    notSyncedPhotos = notSyncedPhotos,
                    checkPhotoList = photoForSyncView.checkPhotoList.value,
                    context = context,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xFFF3F3F3))
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 20.dp)
            ){
                TextButton(
                    modifier = Modifier
                        .background(Color.Transparent)
                        .padding(10.dp),
                    onClick = {
                        val sendPhotos = getListOfCheckedPhoto(notSyncedPhotos, photoForSyncView.checkPhotoList.value)
                        if (sendPhotos.isNotEmpty()) {
                            numOfTotalUploadPhoto.value = sendPhotos.size
                            showUploadPhotoDialog.value = true
                            CoroutineScope(Dispatchers.IO).launch {
                                uploadPhoto(
                                    context = context,
                                    sendPhotos = sendPhotos,
                                    numOfCurrentUploadedPhoto =  numOfCurrentUploadedPhoto,
                                )
                                withContext(Dispatchers.Main){
                                    showUploadPhotoDialog.value = false
                                    Toast.makeText(context, "${sendPhotos.size}개의 사진을 업로드 했습니다.", Toast.LENGTH_SHORT).show()
                                    Log.d("PhotoSyncScreen", "notSyncedPhotos.value) :  ${notSyncedPhotos.size}")
                                    numOfCurrentUploadedPhoto.value = 0
                                    if(notSyncedPhotos.size == 1){
                                        navHostController.popBackStack()
                                    }
                                }
                            }
                        } else {
                            Toast
                                .makeText(context, "선택된 사진이 없습니다.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                ) {
                    Text(
                        text="선택 사진 업로드",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

fun getListOfCheckedPhoto (allPhotos : List<PhotoForSync>, checkPhotoList : List<Boolean>) : List<PhotoForSync>{
    var listOfCheck =  mutableListOf<PhotoForSync>()

    for(current in allPhotos.indices){
        if(checkPhotoList[current]){
            listOfCheck.add(allPhotos[current])
        }
    }
    return listOfCheck
}

fun getListOfNotCheckedPhoto (allPhotos : List<PhotoForSync>, checkPhotoList : List<Boolean>):List<PhotoForSync>{
    var listOfCheck =  mutableListOf<PhotoForSync>()

    for(current in allPhotos.indices){
        if(!checkPhotoList[current]){
            listOfCheck.add(allPhotos[current])
        }
    }
    return listOfCheck
}