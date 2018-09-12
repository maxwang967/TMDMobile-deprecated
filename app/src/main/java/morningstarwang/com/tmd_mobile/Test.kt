package morningstarwang.com.tmd_mobile

import android.util.Log

import java.util.TimerTask

import android.content.ContentValues.TAG

class Test {
    internal var task: TimerTask = object : TimerTask() {
        override fun run() {
            Log.d(TAG, "run: sds")
        }
    }
}
