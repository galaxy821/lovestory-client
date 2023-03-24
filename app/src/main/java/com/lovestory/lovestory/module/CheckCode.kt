package com.lovestory.lovestory.module

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun loveStoryCheckCode(context : Context, onOpenDialogRequest :()->Unit, inputCode : String, myCode : String?){
    if(inputCode.length < 6){
        Toast.makeText(context,"6자리 코드를 입력해주세요.", Toast.LENGTH_SHORT).show()
    }
    else if(myCode == inputCode){
        Toast.makeText(context,"본인의 코드는 입력할 수 없습니다.", Toast.LENGTH_SHORT).show()
    }else{
        CoroutineScope(Dispatchers.Main).launch {
            val response = checkValidCode(inputCode)
            if(response.isSuccessful){
                onOpenDialogRequest()
            }else{
                Toast.makeText(context,"오류가 발생했습니다.\n잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            }

        }
    }


}