package morningstarwang.com.tmd_mobile

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log.e
import android.util.MutableInt
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import morningstarwang.com.tmd_mobile.api.PostService
import morningstarwang.com.tmd_mobile.pojo.PostData
import morningstarwang.com.tmd_mobile.pojo.ThreeAxesData
import morningstarwang.com.tmd_mobile.utils.SpeechUtils
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.jetbrains.anko.toast
import retrofit2.Call
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList
import retrofit2.Retrofit



class MainActivity : AppCompatActivity(), SensorEventListener {
    var mLAccList: MutableList<ThreeAxesData> = ArrayList()
    var mGyrList: MutableList<ThreeAxesData> = ArrayList()
    var mMagList: MutableList<ThreeAxesData> = ArrayList()
    var mPressureList: MutableList<Float> = ArrayList()
    var mPressure = 0f
    private var handler: Handler = @SuppressLint("HandlerLeak")

    object : Handler() {     //此处的object 要加，否则无法重写 handlerMessage
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            when (msg?.what) {
                MSG_PERIOD -> {
                    if(msg.obj!=null){
                        tvPeriod.text = (msg.obj as MutableList<*>)[0].toString()
                        val postDataJson = (msg.obj as MutableList<*>)[1].toString()
                        e("postDataJson=", postDataJson)
//                        val postData = (msg.obj as MutableList<*>)[1] as PostData
                        val retrofit = Retrofit.Builder()
                                .baseUrl("http://101.200.54.20:5000/")
                                .build()
                        val service = retrofit.create(PostService::class.java)
                        val body =  RequestBody.create(MediaType.parse("application/json"), postDataJson)
                        val call = service.predict(body)
//                        val call = service.predict(postData)
                        call.enqueue(object: retrofit2.Callback<ResponseBody>{
                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                val result = response.body()?.string().toString()
//                                toast(result)
                                tvResult.text = result
                                val speech = SpeechUtils.getInstance(applicationContext)
                                speech.speakText(result)
                            }

                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                toast("Network Error, the reason is " + t.message)
                                e("network_error_stacktrace", t.stackTrace.toString())
//                                val msg = Message()
//                                msg.what = MSG_PREDICT_FAIL
//                                msg.obj = t
//                                handler.sendMessage(msg)
                            }

                        })
                    }
                }
            }

        }
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                tvLAccX.text = event.values[0].toString()
                tvLAccY.text = event.values[1].toString()
                tvLAccZ.text = event.values[2].toString()
                mLAccList.add(ThreeAxesData(event.values[0], event.values[1], event.values[2]))
            }
            Sensor.TYPE_GYROSCOPE -> {
                tvGyrX.text = event.values[0].toString()
                tvGyrY.text = event.values[1].toString()
                tvGyrZ.text = event.values[2].toString()
                mGyrList.add(ThreeAxesData(event.values[0], event.values[1], event.values[2]))
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                tvMagX.text = event.values[0].toString()
                tvMagY.text = event.values[1].toString()
                tvMagZ.text = event.values[2].toString()
                mMagList.add(ThreeAxesData(event.values[0], event.values[1], event.values[2]))
            }
            Sensor.TYPE_PRESSURE -> {
                tvPressure.text = event.values[0].toString()
//                mPressure  = event.values[0]
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val mLAccSensor: Sensor? = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val mGyrSensor: Sensor? = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val mMagSensor: Sensor? = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val mPressureSensor: Sensor? = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        swDataCollection.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {//开启数据采集
                mSensorManager.registerListener(this, mLAccSensor, 0)
                mSensorManager.registerListener(this, mGyrSensor, 0)
                mSensorManager.registerListener(this, mMagSensor, 0)
                mSensorManager.registerListener(this, mPressureSensor, 0)
            } else {//关闭数据采集
                mSensorManager.unregisterListener(this, mLAccSensor)
                mSensorManager.unregisterListener(this, mGyrSensor)
                mSensorManager.unregisterListener(this, mMagSensor)
                mSensorManager.unregisterListener(this, mPressureSensor)
            }
        }
        swModeDetection.setOnCheckedChangeListener { _, isChecked ->
            val task: TimerTask = object : TimerTask() {
                override fun run() {
                    val currentPeriod = tvPeriod.text.toString().toInt()
                    val msg = Message()
                    msg.what = MSG_PERIOD
                    var objs:MutableList<Any> = ArrayList()
                    objs.add((currentPeriod + 1).toString())

                    e("currentPeriod", (currentPeriod + 1).toString())
                    var mLAccOK = false
                    var mGyrOK = false
                    var mMagOK = false
                    var mPressureOK = false
                    var postLAccList:List<ThreeAxesData> = ArrayList()
                    var postGyrList:List<ThreeAxesData> = ArrayList()
                    var postMagList:List<ThreeAxesData> = ArrayList()
                    var postPressureList:MutableList<Float> = ArrayList()
                    for (i in 1..(100 * edtWindowSize.text.toString().toFloat()).toInt()){
                        mPressure -= 2
                        postPressureList.add(mPressure)
                    }
                    e("mLAccList.size=", mLAccList.size.toString())
                    e("mGyrList.size=", mGyrList.size.toString())
                    e("mMagList.size=", mMagList.size.toString())
                    e("mPressureList.size=", mPressureList.size.toString())
                    //LAcc
                    if (mLAccList.size >= 100 * edtWindowSize.text.toString().toFloat()) {//数据充分,足够预测
                        val savedLAccList: MutableList<ThreeAxesData> = ArrayList()
                        for (i in 0..((100 * edtWindowSize.text.toString().toFloat() - 1).toInt())) {
                            savedLAccList.add(mLAccList[i])
                        }
                        mLAccList = ArrayList()
                        e("savedLAccList.size=", savedLAccList.size.toString())
                        mLAccOK = true
                        postLAccList = savedLAccList
                    }
                    //Gyr
                    if (mGyrList.size >= 100 * edtWindowSize.text.toString().toFloat()) {//数据充分,足够预测
                        val savedGyrList: MutableList<ThreeAxesData> = ArrayList()
                        for (i in 0..((100 * edtWindowSize.text.toString().toFloat() - 1).toInt())) {
                            savedGyrList.add(mGyrList[i])
                        }
                        mGyrList = ArrayList()
                        e("savedGyrList.size=", savedGyrList.size.toString())
                        mGyrOK = true
                        postGyrList = savedGyrList
                    }
                    //Mag
                    if (mMagList.size >= 100 * edtWindowSize.text.toString().toFloat()) {//数据充分,足够预测
                        val savedMagList: MutableList<ThreeAxesData> = ArrayList()
                        for (i in 0..((100 * edtWindowSize.text.toString().toFloat() - 1).toInt())) {
                            savedMagList.add(mMagList[i])
                        }
                        mMagList = ArrayList()
                        e("savedMagList.size=", savedMagList.size.toString())
                        mMagOK = true
                        postMagList = savedMagList
                    }
                    //Pressure
                    mPressureOK = true
//                    if (mPressureList.size >= 100 * edtWindowSize.text.toString().toFloat()) {//数据充分,足够预测
//                        val savedPressureList: MutableList<Float> = ArrayList()
//                        for (i in 0..((100 * edtWindowSize.text.toString().toFloat() - 1).toInt())) {
//                            savedPressureList.add(mPressureList[i])
//                        }
//                        mPressureList = ArrayList()
//                        e("savedPressureList.size=", savedPressureList.size.toString())
//                        mPressureOK = true
//                        postPressureList = savedPressureList
//                    }
                    if(mLAccOK && mMagOK && mGyrOK && mPressureOK){//向服务器发送数据
                        val postData = PostData(postLAccList, postGyrList, postMagList, postPressureList)
                        val postDataJson  = Gson().toJson(postData)

                        objs.add(postDataJson)
                        msg.obj = objs
                    }

                    handler.sendMessage(msg)

                }
            }
            val timer = Timer()
            if (isChecked) {//开始预测
                timer.schedule(task, 0, (1000 * edtWindowSize.text.toString().toFloat()).toLong())
            } else {//停止预测
                tvPeriod.text = "0"
                timer.cancel()
            }
        }
    }

    companion object {
        const val MSG_PERIOD = 0x1
        const val MSG_PREDICT_OK = 0x2
        const val MSG_PREDICT_FAIL = 0x3
    }
}
