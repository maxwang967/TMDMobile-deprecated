package morningstarwang.com.tmd_mobile.pojo

data class ThreeAxesData(var x:Float, var y:Float, var z:Float)
data class PostData(var mLAccList:List<ThreeAxesData>, var mGyrList:List<ThreeAxesData>, var mMagList:List<ThreeAxesData>, var mPressureList: List<Float>)