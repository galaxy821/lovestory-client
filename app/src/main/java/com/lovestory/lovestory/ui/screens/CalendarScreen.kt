package com.lovestory.lovestory.ui.screens

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
//import com.google.android.gms.maps.model.CameraPosition
//import com.google.android.gms.maps.model.LatLng
//import com.google.maps.android.compose.GoogleMap
//import com.google.maps.android.compose.Marker
//import com.google.maps.android.compose.MarkerState
//import com.google.maps.android.compose.rememberCameraPositionState
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.*
import com.lovestory.lovestory.model.*
import com.lovestory.lovestory.module.getSavedComment
import com.lovestory.lovestory.module.getToken
import com.lovestory.lovestory.module.saveComment
import com.lovestory.lovestory.resource.vitro
import com.lovestory.lovestory.ui.components.*
import com.lovestory.lovestory.ui.theme.LoveStoryTheme
import retrofit2.Response
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.compose.*
import com.lovestory.lovestory.R
import com.lovestory.lovestory.database.PhotoDatabase
import com.lovestory.lovestory.database.entities.SyncedPhoto
import com.lovestory.lovestory.database.repository.SyncedPhotoRepository
import com.lovestory.lovestory.graphs.CalendarStack
import com.lovestory.lovestory.graphs.MainScreens
import com.lovestory.lovestory.module.photo.getThumbnailForPhoto
import com.lovestory.lovestory.network.*
import com.lovestory.lovestory.view.SyncedPhotoView
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import okhttp3.Dispatcher
import java.time.DayOfWeek
import kotlin.math.roundToInt


@OptIn(MapsComposeExperimentalApi::class)
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun CalendarScreen(navHostController: NavHostController, syncedPhotoView : SyncedPhotoView) {

    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) } // Adjust as needed
    val endMonth = remember { currentMonth.plusMonths(100) } // Adjust as needed
    val daysOfWeek = remember { daysOfWeek() }

    var selectionSave by rememberSaveable { mutableStateOf(CalendarDay(date = LocalDate.now(), position = DayPosition.MonthDate))}
    var isPopupVisibleSave by rememberSaveable { mutableStateOf(false) }
    //Log.d("세이브", "$selectionSave, $isPopupVisibleSave")

    var selection by remember { mutableStateOf(CalendarDay(date = LocalDate.now(), position = DayPosition.MonthDate))}
    var isPopupVisible by remember { mutableStateOf(false) }
    //Log.d("세이브", "$selection, $isPopupVisible")
    //Log.d("셀렉션1", "${selection.date}")

    if(isPopupVisibleSave){
        selection = selectionSave
        isPopupVisible = true
    }

    val onOpenDialogRequest : ()->Unit = {
        isPopupVisible = true
        //isPopupVisibleSave = true
    }
    val onDismissRequest : () -> Unit = {isPopupVisible = false}

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth =  endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = daysOfWeek.first()
    )

    val coroutineScope = rememberCoroutineScope()
    val visibleMonth = rememberFirstCompletelyVisibleMonth(state)

    var coupleMemoryList by remember { mutableStateOf(emptyList<CoupleMemory>()) }
    val stringMemoryList = mutableListOf<StringMemory>()

    var latLng by remember { mutableStateOf(emptyList<LatLng>()) }
    val dataLoaded = remember { mutableStateOf(false) }
    val meetDate = remember { mutableStateListOf<String>() }

    val context = LocalContext.current
    val token = getToken(context)
    val dialogWidthDp = remember { mutableStateOf(0.dp) }

    lateinit var repository : SyncedPhotoRepository
    lateinit var repositoryDummy : SyncedPhotoRepository //나중에 월별로 받아오면 삭제할 부분
    val photoDate = remember { mutableStateListOf<String>() }
    val items = remember{ mutableStateListOf<MyItem>() }
    val drawable = ContextCompat.getDrawable(context, R.drawable.img) //마커 이미지로 변경
    val bitmap = (drawable as BitmapDrawable).bitmap

    var latLngExist by remember { mutableStateOf(false) }
    var photoExist by remember { mutableStateOf(false) }

    val syncedPhotosByDate by syncedPhotoView.groupedSyncedPhotosByDate.observeAsState(initial = mapOf())
    val allPhotoListState = rememberLazyListState()


    //해야 되는 게 코루틴 정리. 룸 db
    LaunchedEffect(key1 = true) {
        //shared Preference 에서 get Comment
        val data = withContext(Dispatchers.IO) {
            getSavedComment(context)
        }
        coupleMemoryList = data
        coupleMemoryList.forEach { CoupleMemory -> Log.d("쉐어드1", "$CoupleMemory") }

        //get Comment
        val getMemoryList: Response<List<GetMemory>> = getComment(token!!)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        for (getMemory in getMemoryList.body()!!) {
            val date = LocalDate.parse(getMemory.date, formatter)
            val comment = getMemory.comment
            val stringMemory = StringMemory(date.toString(), comment)
            stringMemoryList.add(stringMemory)
        }
        coupleMemoryList = convertToCoupleMemoryList(stringMemoryList)
        saveComment(context, coupleMemoryList)
        coupleMemoryList.forEach {
            if (!meetDate.contains(dateToString(it.date))) {
                meetDate.add(dateToString(it.date))
            }
        }

        //데이터베이스에서 get sync date
        val response = getPhotoTable(token!!)
        if(response.isSuccessful) {
            val photoDatabase = PhotoDatabase.getDatabase(context)
            val photoDao = photoDatabase.syncedPhotoDao()
            repositoryDummy = SyncedPhotoRepository(photoDao)

            val syncedPhoto = repositoryDummy.listOfGetAllSyncedPhoto()

            syncedPhoto.forEach{
                val inputString = it.date
                val index = inputString.indexOf("T")
                val extractedString = inputString.substring(0, index)
                if (!meetDate.contains(extractedString)) {
                    meetDate.add(extractedString)
                }
                if (!photoDate.contains(extractedString)) {
                    photoDate.add(extractedString)
                }
            }
        }
    }

    LaunchedEffect(isPopupVisible || isPopupVisibleSave) {
        if (isPopupVisible || isPopupVisibleSave) {
            //get Comment
            val getMemoryList: Response<List<GetMemory>> = getComment(token!!)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            for (getMemory in getMemoryList.body()!!) {
                val date = LocalDate.parse(getMemory.date, formatter)
                val comment = getMemory.comment
                val stringMemory = StringMemory(date.toString(), comment)
                stringMemoryList.add(stringMemory)
            }
            coupleMemoryList = convertToCoupleMemoryList(stringMemoryList)
            saveComment(context, coupleMemoryList)
            coupleMemoryList.forEach {
                if (!meetDate.contains(dateToString(it.date))) {
                    meetDate.add(dateToString(it.date))
                }
            }

            //get GPS
            val gps = getGps(token, dateToString(selection.date))
            if (gps.body() != null) {
                latLng = getLatLng(gps.body()!!)
                if(latLng.isNotEmpty()){
                    latLngExist = true
                }
            }

            //get syncedPhoto from database
            val response = getPhotoTable(token)
            if (response.isSuccessful) {
                val photoDatabase = PhotoDatabase.getDatabase(context)
                val photoDao = photoDatabase.syncedPhotoDao()
                repository = SyncedPhotoRepository(photoDao)

                val syncedPhoto = repository.getSyncedPhotosByDate(dateToString(selection.date))
                syncedPhoto.forEach {
                    items.add(
                        MyItem(
                            LatLng(it.latitude, it.longitude),
                            "PHOTO",
                            "PHOTO",
                            getThumbnailForPhoto(token, it.id)!!
                        )
                    )
                }
                if(syncedPhoto.isNotEmpty()){
                    photoExist = true
                }
            }

            if (latLng.isNotEmpty()) {
                latLng.forEach {
                    items.add(MyItem(it, "MARKER", "GPS", bitmap))
                }
            }
            dataLoaded.value = true
            items.forEach {
                Log.d("비트맵 정보","${it.icon}")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        //horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        SimpleCalendarTitle(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            currentMonth = visibleMonth.yearMonth,
            goToPrevious = {
                coroutineScope.launch {
                    state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.previousMonth)
                }
            },
            goToNext = {
                coroutineScope.launch {
                    state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.nextMonth)
                }
            },
        )
        Spacer(modifier = Modifier.height(10.dp))
        DaysOfWeekTitle(daysOfWeek = daysOfWeek)
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalCalendar(
            modifier = Modifier.wrapContentWidth(),//.background(color = Color.White, RoundedCornerShape(30.dp)),
            state = state,
            dayContent = { day ->
                Day(
                    day = day,
                    isPopupVisible = isPopupVisible,
                    isSelected = selection == day,
                    onOpenDialogRequest = onOpenDialogRequest,
                    meetDate = meetDate,
                ) { clicked ->
                    selection = clicked
                }
            }
        )
    }

    if ((isPopupVisible || isPopupVisibleSave) && dataLoaded.value) {
        var editedcomment by remember { mutableStateOf("") }
        val existingMemory = coupleMemoryList.firstOrNull { it.date == selection.date }
        if (existingMemory != null) {
            editedcomment = existingMemory.comment
        }
        CalendarDialog(
            selection = selection,
            onDismissRequest = {
                if(existingMemory != null) {
                    coupleMemoryList.find{ it.date == selection.date }?.comment = editedcomment
                    coroutineScope.launch{
                        val put : Response<Any> = putComment(token!!, dateToString(selection.date), editedcomment)
                        saveComment(context, coupleMemoryList)
                    }
                } else {
                    if ( editedcomment != ""){
                        val newMemory = CoupleMemory(date = selection.date, comment = editedcomment)
                        coupleMemoryList = coupleMemoryList.toMutableList().apply{add(newMemory)}
                        coroutineScope.launch{
                            val put : Response<Any> = putComment(token!!, dateToString(selection.date), editedcomment)
                            saveComment(context, coupleMemoryList)
                            meetDate.add(dateToString(selection.date))
                        }
                    }
                }
                isPopupVisible = false// Update coupleMemoryList when dialog is dismissed
                isPopupVisibleSave = false
                dataLoaded.value = false
                items.clear()
                latLng = emptyList()
                latLngExist = false
                photoExist = false
            }, //onDismissRequest,
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
            Column(
                modifier = Modifier
                    .width(screenWidth-40.dp)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color = Color.White),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(color = Color.Transparent)
                        .padding(start = 25.dp, end = 25.dp, top = 15.dp, bottom = 10.dp),//vertical = 15.dp, horizontal = 25.dp),
                    verticalAlignment = Alignment.Bottom
                ){
                    Text(
                        text = selection.date.dayOfMonth.toString(),
                        fontSize = 26.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        modifier = Modifier
                            .padding(bottom = 3.dp)
                            .weight(1f),
                        text = selection.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())+"요일",
                        fontSize = 16.sp,
                        color = Color.Black,
                    )
                    //Spacer(Modifier.weight(1f))
                    Button(
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                        onClick = {
                            coroutineScope.launch {
                                val date = selection.date
                                coupleMemoryList = coupleMemoryList.filterNot { it.date == date }
                                val delete: Any = deleteComment(token!!, dateToString(selection.date))
                                if(!photoDate.contains(dateToString(date))){
                                    meetDate.remove(dateToString(date))
                                }
                            }
                            saveComment(context, coupleMemoryList)
                            isPopupVisible = false
                            isPopupVisibleSave = false
                            dataLoaded.value = false
                        },
                        elevation = null,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .width(30.dp)
                            .height(30.dp),
                        //.padding(bottom = 5.dp),//wrapContentSize(),
                        shape = CircleShape,
                    ){
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_delete),
                            contentDescription = "Delete"
                        )
                    }

                    Button(
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                        onClick = {
                            if(existingMemory != null) {
                                coupleMemoryList.find{ it.date == selection.date }?.comment = editedcomment
                                //sendComment
                                coroutineScope.launch{
                                    val put : Response<Any> = putComment(token!!, dateToString(selection.date), editedcomment)
                                    saveComment(context, coupleMemoryList)
                                }
                            } else {
                                if ( editedcomment != ""){
                                    val newMemory = CoupleMemory(date = selection.date, comment = editedcomment)
                                    coupleMemoryList = coupleMemoryList.toMutableList().apply{add(newMemory)}
                                    coroutineScope.launch{
                                        val put : Response<Any> = putComment(token!!, dateToString(selection.date), editedcomment)
                                        saveComment(context, coupleMemoryList)
                                        meetDate.add(dateToString(selection.date))
                                    }
                                }
                            }
                            isPopupVisible = false
                            isPopupVisibleSave = false
                            items.clear()
                            latLng = emptyList()
                            latLngExist = false
                            photoExist = false
                        },
                        elevation = null,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .width(30.dp)
                            .height(30.dp)
                            .padding(bottom = 5.dp),//wrapContentSize(),
                        shape = CircleShape,
                    ){
                        Text(
                            text = "X",
                            fontSize = 22.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Divider(color = Color.Black, thickness = 1.dp, modifier = Modifier.padding(start = 20.dp, end = 20.dp))

                Spacer(modifier = Modifier.height(15.dp))

                EditableTextField(
                    initialValue = editedcomment,
                    onValueChanged = {editedcomment = it}
                )
                Log.d("박스","$latLngExist")
                if(latLngExist) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp)
                            .wrapContentHeight()
                            .background(color = Color.LightGray, RoundedCornerShape(12.dp))
                    ) {
                        var viewposition = LatLng(37.503735330931136, 126.95615523253305)
                        var cameraPositionState = rememberCameraPositionState {
                            position = CameraPosition.fromLatLngZoom(viewposition, 15f)
                        }
                        selectionSave = selection
                        if (!dataLoaded.value) {
                            //스켈레톤 추가
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(2f)
                                    .background(color = Color.Transparent)
                            )
                        } else if (dataLoaded.value && latLng.isNotEmpty()) {
                            viewposition = averageLatLng(latLng)
                            cameraPositionState = CameraPositionState(
                                position = CameraPosition.fromLatLngZoom(
                                    viewposition,
                                    15f
                                )
                            )
                            val zoomLevel = getZoomLevelForDistance(
                                getMaxDistanceBetweenLatLng(
                                    viewposition,
                                    latLng
                                )
                            ) - 1
                            cameraPositionState = remember {
                                CameraPositionState(
                                    position = CameraPosition.fromLatLngZoom(
                                        viewposition,
                                        zoomLevel
                                    )
                                )
                            }

                            GoogleMap(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(2f)
                                    .clip(RoundedCornerShape(12.dp)),
                                cameraPositionState = cameraPositionState,
                                onMapClick = {
                                    isPopupVisible = false
                                    isPopupVisibleSave = true
                                    navHostController.navigate(
                                        CalendarStack.Map.route + "/${
                                            dateToString(
                                                selection.date
                                            )
                                        }"
                                    ) {
                                        launchSingleTop = true
                                        Log.d("클릭", "클릭")
                                    }
                                },
                                uiSettings = uiSettings
                            ) {
                                Clustering(
                                    items = items,
                                    // Optional: Handle clicks on clusters, cluster items, and cluster item info windows
                                    onClusterClick = {
                                        Log.d("TAG", "Cluster clicked! $it") // 클러스터 클릭했을 때
                                        false
                                    },
                                    onClusterItemClick = {
                                        Log.d(
                                            "TAG",
                                            "Cluster item clicked! $it"
                                        ) // 클러스팅 되지 않은 마커 클릭했을 때
                                        false
                                    },
                                    onClusterItemInfoWindowClick = {
                                        Log.d(
                                            "TAG",
                                            "Cluster item info window clicked! $it"
                                        ) // 클러스팅 되지 않은 마커의 정보창을 클릭했을 때
                                    },
                                    // Optional: Custom rendering for clusters
                                    clusterContent = { cluster ->
                                        val size = 50.dp
                                        val scaledBitmap = cluster.items.first().icon.let {
                                            val density = LocalDensity.current.density
                                            val scaledSize = (size * density).toInt()
                                            Bitmap.createScaledBitmap(
                                                it,
                                                scaledSize,
                                                scaledSize,
                                                false
                                            )
                                        }!!.asImageBitmap()
                                        Surface(
                                            shape = RoundedCornerShape(percent = 10),
                                            contentColor = Color.White,
                                            border = BorderStroke(1.dp, Color.White),
                                            elevation = 10.dp
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Image(
                                                    bitmap = scaledBitmap,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(60.dp)
                                                )
                                                Text(
                                                    "%,d".format(cluster.size), //이 부분 왜 2배로 나오지..?
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Black,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    },
                                    // Optional: Custom rendering for non-clustered items
                                    clusterItemContent = { item ->
                                        val size = 50.dp
                                        val scaledBitmap = item.icon.let {
                                            val density = LocalDensity.current.density
                                            val scaledSize = (size * density).toInt()
                                            Bitmap.createScaledBitmap(
                                                it,
                                                scaledSize,
                                                scaledSize,
                                                false
                                            )
                                        }!!.asImageBitmap()
                                        Surface(
                                            shape = RoundedCornerShape(percent = 10),
                                            contentColor = Color.White,
                                            border = BorderStroke(1.dp, Color.White),
                                            elevation = 10.dp
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Image(
                                                    bitmap = scaledBitmap,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(60.dp)
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    if(photoExist){
                        val boxWidth = remember { mutableStateOf(0) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f)
                                .padding(start = 20.dp, end = 20.dp)
                                .background(color = Color.Transparent, RoundedCornerShape(12.dp))
                        ) {
                            val popupWidthDp = with(LocalDensity.current) {
                                LocalContext.current.resources.displayMetrics.widthPixels.dp
                            }
                            val filteredSyncedPhotosByDate = syncedPhotosByDate.filterKeys { key ->
                                key == dateToString(selection.date)
                            }
                            isPopupVisibleSave = true
                            PhotoForCalendar(
                                syncedPhotosByDate = filteredSyncedPhotosByDate,
                                token = token,
                                syncedPhotoView = syncedPhotoView,
                                navHostController = navHostController,
                                allPhotoListState = allPhotoListState,
                                widthDp = boxWidth.value.dp
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }else{
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f)
                            .padding(start = 20.dp, end = 20.dp),
                        contentAlignment = Alignment.Center
                    ){
                        Text(
                            text = "만난 기록이 없어요...",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

val uiSettings = MapUiSettings(
        compassEnabled = false,
        indoorLevelPickerEnabled = false,
        mapToolbarEnabled = false,
        myLocationButtonEnabled = false,
        rotationGesturesEnabled = false,
        scrollGesturesEnabled = false,
        scrollGesturesEnabledDuringRotateOrZoom = false,
        tiltGesturesEnabled = false,
        zoomControlsEnabled = false,
        zoomGesturesEnabled = false
    )




@Preview(showSystemUi = true)
@Composable
fun DefaultPreview() {
    val navController = rememberNavController()
    LoveStoryTheme {
        //CalendarScreen(navHostController = navController, onNavigateToMapScreen = )
    }
}

