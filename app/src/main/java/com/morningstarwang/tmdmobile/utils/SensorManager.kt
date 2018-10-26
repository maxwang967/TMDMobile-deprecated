package com.morningstarwang.tmdmobile.utils

import android.hardware.Sensor
import android.hardware.SensorManager
import com.morningstarwang.tmdmobile.pojo.ThreeAxesData
import java.util.*

class SensorManager {
    companion object {
        var mode = 0
        var path = ""
        var mLAcc = ThreeAxesData(0f, 0f, 0f)
        var mAcc = ThreeAxesData(0f, 0f, 0f)
        var mGyr = ThreeAxesData(0f, 0f, 0f)
        var mMag = ThreeAxesData(0f, 0f, 0f)
        var mPressure = 0f
        val mLAccList: MutableList<ThreeAxesData> = ArrayList()
        val mAccList: MutableList<ThreeAxesData> = ArrayList()
        val mGyrList: MutableList<ThreeAxesData> = ArrayList()
        val mMagList: MutableList<ThreeAxesData> = ArrayList()
        val mPressureList: MutableList<Float> = ArrayList()
        var mSensorManager: SensorManager? = null
        var mLAccSensor: Sensor? = null
        var mAccSensor: Sensor? = null
        var mGyrSensor: Sensor? = null
        var mMagSensor: Sensor? = null
        var mPressureSensor: Sensor? = null


        var mDataTimer: Timer? = null
        var mDataTimerTask: TimerTask? = null
        var mTimer: Timer? = null
        var mTimerTask: TimerTask? = null
        var realDataFlag = -1
    }
}