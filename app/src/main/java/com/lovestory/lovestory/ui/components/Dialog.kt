package com.lovestory.lovestory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.maxkeppeker.sheets.core.models.base.SheetState
import java.time.LocalDate

@Composable
fun InputMeetDayDialog(
    onDismissRequest : ()-> Unit,
    properties: DialogProperties = DialogProperties(),
    content : @Composable () -> Unit
){
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        content()
    }
}

@Composable
fun CoupleSyncDialog(
    navHostController: NavHostController,
    onDismissRequest : ()->Unit,
    selectedMeetDates : MutableState<LocalDate>,
    calendarForMeetState : SheetState,
    userId : String?,
    code :String?
    ){
    InputMeetDayDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ){
        Column(
            modifier = Modifier
                .width(360.dp)
                .wrapContentHeight()
                .clip(RoundedCornerShape(25.dp))
                .background(color = Color.White),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,

            ) {
            Spacer(modifier = Modifier.height(20.dp))
            TextFieldForCalendar(
                selectedDates = selectedMeetDates,
                calendarState = calendarForMeetState,
                labelText = "처음 만난 날")
            Spacer(modifier = Modifier.height(20.dp))
            ButtonForCreateCouple(
                "시작하기",
                navHostController = navHostController,
                userId = userId,
                code = code,
                meetDay = selectedMeetDates.value.toString(),
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

    }
}