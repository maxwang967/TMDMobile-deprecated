package com.morningstarwang.tmdmobile.pojo

import android.os.Parcel
import android.os.Parcelable

data class ThreeAxesData(var x:Float, var y:Float, var z:Float)
data class PostData(var mLAccList:List<ThreeAxesData>, var mAccList:List<ThreeAxesData>, var mGyrList:List<ThreeAxesData>, var mMagList:List<ThreeAxesData>, var mPressureList: List<Float>)

data class UpdateData(var versionCode:Int, var description:String, var force:Int, var url: String)