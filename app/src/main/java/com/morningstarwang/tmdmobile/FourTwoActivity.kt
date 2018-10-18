package com.morningstarwang.tmdmobile

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayout
import android.util.Log.e
import android.widget.TextView
import com.google.gson.Gson
import com.morningstarwang.tmdmobile.api.PostService
import com.morningstarwang.tmdmobile.pojo.PostData
import com.morningstarwang.tmdmobile.pojo.ThreeAxesData
import com.morningstarwang.tmdmobile.utils.SpeechUtils
import kotlinx.android.synthetic.main.activity_four.*
import kotlinx.android.synthetic.main.activity_four_two.*
import kotlinx.android.synthetic.main.activity_main.*
import kr.co.namee.permissiongen.PermissionFail
import kr.co.namee.permissiongen.PermissionGen
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import java.io.File
import java.io.FileWriter
import java.util.*
import kotlin.collections.ArrayList


@Suppress("DEPRECATION")
class FourTwoActivity : AppCompatActivity(), SensorEventListener {
    var path = ""
    var mLAcc = ThreeAxesData(0f, 0f, 0f)
    var mGyr = ThreeAxesData(0f, 0f, 0f)
    var mMag = ThreeAxesData(0f, 0f, 0f)
    var mPressure = 0f
    val mLAccList: MutableList<ThreeAxesData> = ArrayList()
    val mGyrList: MutableList<ThreeAxesData> = ArrayList()
    val mMagList: MutableList<ThreeAxesData> = ArrayList()
    val mPressureList: MutableList<Float> = ArrayList()
    private var mSensorManager: SensorManager? = null
    private var mLAccSensor: Sensor? = null
    private var mGyrSensor: Sensor? = null
    private var mMagSensor: Sensor? = null
    private var mPressureSensor: Sensor? = null
    var confusionMatrix = Array<Array<TextView?>>(4) { arrayOfNulls(4) }
    private var mTimer: Timer? = null
    private var mTimerTask: TimerTask? = null
    private var mDataTimer: Timer? = null
    private var mDataTimerTask: TimerTask? = null

    var realDataFlag = -1
    var timstamp = 0L
    var saveFile: File? = null
    var outStream: FileWriter? = null
    private var handler: Handler = @SuppressLint("HandlerLeak")

    object : Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            when (msg?.what) {
                MSG_PERIOD -> {
                    if (msg.obj != null) {
                        tvPeriod42.text = (msg.obj as MutableList<*>)[0].toString()
                        val postDataJson = (msg.obj as MutableList<*>)[1].toString()
                        e("postDataJson=", postDataJson)
                        val retrofit = Retrofit.Builder()
                                .baseUrl("http://47.95.255.173:5000/")
                                .build()
                        val service = retrofit.create(PostService::class.java)
                        val body = RequestBody.create(MediaType.parse("application/json"), postDataJson)
                        val call = service.predict42(body)
                        call.enqueue(object : retrofit2.Callback<ResponseBody> {
                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                val result = response.body()?.string().toString()
                                tvResult42.text = result
                                var predictDataFlag = -1
                                /**
                                 * rbStill.isChecked -> dataFlag = 0
                                rbWalk.isChecked -> dataFlag = 1
                                rbRun.isChecked -> dataFlag = 2
                                rbBike.isChecked -> dataFlag = 3
                                rbCar.isChecked -> dataFlag = 4
                                rbBus.isChecked -> dataFlag = 5
                                rbTrain.isChecked -> dataFlag = 6
                                rbSubway.isChecked -> dataFlag = 7
                                 */
                                when (result) {
                                    "Car" -> predictDataFlag = 3
                                    "Bus" -> predictDataFlag = 2
                                    "Train" -> predictDataFlag = 1
                                    "Subway" -> predictDataFlag = 0
                                }
                                if (realDataFlag != -1 && predictDataFlag != -1) {
                                    var currentCount = confusionMatrix[realDataFlag][predictDataFlag]!!.text.toString().toInt()
                                    currentCount += 1
                                    confusionMatrix[realDataFlag][predictDataFlag]!!.text = currentCount.toString()
                                }
                                var correctCount = 0f
                                for (i in 0..3) {
                                    correctCount += confusionMatrix[i][i]!!.text.toString().toFloat()
                                }
                                val accuracy: Float
                                accuracy = if (tvPeriod42.text.toString() == "0") {
                                    0f
                                } else {
                                    correctCount / tvPeriod42.text.toString().toFloat()
                                }
                                tvAccuracy42.text = (accuracy * 100).toString() + "%"
                                val speech = SpeechUtils.getInstance(applicationContext)
                                speech.speakText(result)
                            }
                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                toast("Network Error, the reason is " + t.message)
                                e("network_error_stacktrace", t.stackTrace.toString())
                            }

                        })
                    }
                }
                MSG_STOP -> {
                    System.exit(0)
                }
            }

        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                mLAcc.x = event.values[0]
                mLAcc.y = event.values[1]
                mLAcc.z = event.values[2]
            }
            Sensor.TYPE_GYROSCOPE -> {
                mGyr.x = event.values[0]
                mGyr.y = event.values[1]
                mGyr.z = event.values[2]
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                mMag.x = event.values[0]
                mMag.y = event.values[1]
                mMag.z = event.values[2]
            }
            Sensor.TYPE_PRESSURE -> {
                mPressure = event.values[0]
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_four_two)
        title = "4-Mode-Detection-2min"
        requestPermission()
        initActionBar()
        wakeLocker()
        initSensors()
        initPredictions()
        initConfusionMatrix()
    }


    private fun requestPermission() {
        PermissionGen.with(this)
                .addRequestCode(100)
                .permissions(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .request()
    }


    private fun initActionBar() {
        val actionBar = supportActionBar
        actionBar!!.setLogo(R.mipmap.icon)
        actionBar.setDisplayUseLogoEnabled(true)
        actionBar.setDisplayShowHomeEnabled(true)
    }

    @SuppressLint("WakelockTimeout")
    private fun wakeLocker() {
        val pm =
                getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                FourTwoActivity::class.java.name)
        wakeLock.acquire()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        PermissionGen.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }


    @PermissionFail(requestCode = 100)
    fun permissionDenied() {
        toast("App needs the permission to run normally")
        handler.sendEmptyMessageDelayed(MSG_STOP, 2000)
    }

    @SuppressLint("ResourceType")
    private fun initConfusionMatrix() {
        glConfusionMatrix42.removeAllViews()
        for (i in 0..3) {
            for (j in 0..3) {
                val textView = TextView(this)
                textView.text = "0"
                textView.setTextColor(resources.getColor(android.R.color.black))
                confusionMatrix[i][j] = textView
            }
        }
        for (i in 0..3) {
            for (j in 0..3) {
                val mLayoutParams = GridLayout.LayoutParams()
                mLayoutParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1.0f)
                mLayoutParams.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1.0f)
                glConfusionMatrix42.addView(confusionMatrix[i][j], mLayoutParams)
            }
        }
    }

    private fun initPredictions() {
        btn_switch_to_42.onClick {
            startActivity<MainActivity>()
            finish()
        }
        swModeDetection42.setOnCheckedChangeListener { _, isChecked ->
            if (!swDataCollection42.isChecked) {
                swModeDetection42.isChecked = false
                toast("Please start data collection first!")
                return@setOnCheckedChangeListener
            }
            if (isChecked) {//开始预测
                initConfusionMatrix()
                if (mTimer == null) {
                    mTimer = Timer()
                }
                if (mTimerTask == null) {
                    mTimerTask = object : TimerTask() {
                        override fun run() {
                            val currentPeriod = tvPeriod42.text.toString().toInt()
                            val msg = Message()
                            e("mLAccList.size=", mLAccList.size.toString())
                            e("mGyrList.size=", mGyrList.size.toString())
                            e("mMagList.size=", mMagList.size.toString())
                            e("mPressureList.size=", mPressureList.size.toString())
                            msg.what = MSG_PERIOD
                            val objs: MutableList<Any> = ArrayList()
                            objs.add((currentPeriod + 1).toString())
                            e("currentPeriod", (currentPeriod + 1).toString())
                            var mLAccOK = false
                            var mGyrOK = false
                            var mMagOK = false
                            var mPressureOK: Boolean
                            var postLAccList: List<ThreeAxesData> = ArrayList()
                            var postGyrList: List<ThreeAxesData> = ArrayList()
                            var postMagList: List<ThreeAxesData> = ArrayList()
                            var postPressureList: MutableList<Float> = ArrayList()

                            //LAcc
                            if (mLAccList.size >= 100 * edtWindowSize42.text.toString().toFloat()) {//数据充分,足够预测
                                val savedLAccList: MutableList<ThreeAxesData> = ArrayList()
                                for (i in 0..((100 * edtWindowSize42.text.toString().toFloat() - 1).toInt())) {
                                    savedLAccList.add(mLAccList[i])
                                }
                                e("savedLAccList.size=", savedLAccList.size.toString())
                                mLAccList.clear()
                                mLAccOK = true
                                postLAccList = savedLAccList
                            }
                            //Gyr
                            if (mGyrList.size >= 100 * edtWindowSize42.text.toString().toFloat()) {//数据充分,足够预测
                                val savedGyrList: MutableList<ThreeAxesData> = ArrayList()
                                for (i in 0..((100 * edtWindowSize42.text.toString().toFloat() - 1).toInt())) {
                                    savedGyrList.add(mGyrList[i])
                                }
                                e("savedGyrList.size=", savedGyrList.size.toString())
                                mGyrList.clear()
                                mGyrOK = true
                                postGyrList = savedGyrList
                            }
                            //Mag
                            if (mMagList.size >= 100 * edtWindowSize42.text.toString().toFloat()) {//数据充分,足够预测
                                val savedMagList: MutableList<ThreeAxesData> = ArrayList()
                                for (i in 0..((100 * edtWindowSize42.text.toString().toFloat() - 1).toInt())) {
                                    savedMagList.add(mMagList[i])
                                }
                                e("savedMagList.size=", savedMagList.size.toString())
                                mMagList.clear()
                                mMagOK = true
                                postMagList = savedMagList
                            }
                            //Pressure
                            mPressureOK = true
                            if (mPressureList.size >= 100 * edtWindowSize42.text.toString().toFloat()) {//数据充分,足够预测
                                val savedPressureList: MutableList<Float> = ArrayList()
                                for (i in 0..((100 * edtWindowSize42.text.toString().toFloat() - 1).toInt())) {
                                    savedPressureList.add(mPressureList[i])
                                }
                                mPressureList.clear()
                                e("savedPressureList.size=", savedPressureList.size.toString())
                                mPressureOK = true
                                postPressureList = savedPressureList
                            }
                            if (mLAccOK && mMagOK && mGyrOK && mPressureOK) {//向服务器发送数据
                                val postData = PostData(postLAccList, postGyrList, postMagList, postPressureList)
                                val postDataJson = Gson().toJson(postData)
                                objs.add(postDataJson)
                                msg.obj = objs
                            }
                            handler.sendMessage(msg)
                        }
                    }
                }
                if (mTimer != null && mTimerTask != null) {
                    mTimer!!.schedule(mTimerTask, (1000 * edtWindowSize42.text.toString().toFloat()).toLong(), (1000 * edtWindowSize42.text.toString().toFloat()).toLong())
                }
            } else {//停止预测
                stopPredict()
            }
        }
    }

    private fun stopPredict() {
        tvPeriod42.text = "0"
        tvResult42.text = "N/A"
        if (mTimer != null) {
            mTimer!!.cancel()
            mTimer = null
        }
        if (mTimerTask != null) {
            mTimerTask!!.cancel()
            mTimerTask = null
        }
    }

    private fun initSensors() {
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mLAccSensor = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mGyrSensor = mSensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        mMagSensor = mSensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        mPressureSensor = mSensorManager!!.getDefaultSensor(Sensor.TYPE_PRESSURE)
        swDataCollection42.setOnCheckedChangeListener { _, isChecked ->
            if (swModeDetection42.isChecked) {
                toast("Please stop mode detection first!")
                swDataCollection42.isChecked = true
                return@setOnCheckedChangeListener
            }
            if (!rbCar42.isChecked &&
                    !rbBus42.isChecked &&
                    !rbTrain42.isChecked &&
                    !rbSubway42.isChecked) {
                toast("Please select a specific transportation mode!")
                swDataCollection42.isChecked = false
                return@setOnCheckedChangeListener
            }
            if (isChecked) {//开启数据采集
                timstamp = System.currentTimeMillis()
                path = Environment.getExternalStorageDirectory().absolutePath + "/"
                path += "tmd_mobile/"
                val file = File(path)
                if (!file.exists()) {
                    file.mkdir()
                }
                when {
                    rbCar42.isChecked -> realDataFlag = 3
                    rbBus42.isChecked -> realDataFlag = 2
                    rbTrain42.isChecked -> realDataFlag = 1
                    rbSubway42.isChecked -> realDataFlag = 0
                }
                startSensor(mSensorManager!!, mLAccSensor, mGyrSensor, mMagSensor, mPressureSensor)
                if (mDataTimer == null) {
                    mDataTimer = Timer()
                }
                if (mDataTimerTask == null) {
                    mDataTimerTask = object : TimerTask() {
                        override fun run() {
                            val mLAccAdd = mLAcc.copy()
                            val mGyrAdd = mGyr.copy()
                            val mMagAdd = mMag.copy()
                            val mPressureAdd = mPressure
                            mLAccList.add(mLAccAdd)
                            mGyrList.add(mGyrAdd)
                            mMagList.add(mMagAdd)
                            mPressureList.add(mPressureAdd)
                            val content = "${mLAccAdd.x},${mLAccAdd.y},${mLAccAdd.z},${mGyrAdd.x},${mGyrAdd.y},${mGyrAdd.z},${mMagAdd.x},${mMagAdd.y},${mMagAdd.z},$mPressureAdd\n"
                            var mode = ""
                            when (realDataFlag) {
                                3 -> mode = "Car"
                                2 -> mode = "Bus"
                                1 -> mode = "Train"
                                0 -> mode = "Subway"
                            }
                            saveFile = File(path, "$mode-$timstamp.txt")
                            outStream = FileWriter(saveFile, true)
                            outStream!!.write(content)
                            outStream!!.close()
                        }
                    }
                }
                if (mDataTimer != null && mDataTimerTask != null) {
                    mDataTimer!!.schedule(mDataTimerTask, 0, 5)
                }
            } else {//关闭数据采集
                stopSensor(mSensorManager!!, mLAccSensor, mGyrSensor, mMagSensor, mPressureSensor)
                if (mDataTimer != null) {
                    mDataTimer!!.cancel()
                    mDataTimer = null
                }
                if (mDataTimerTask != null) {
                    mDataTimerTask!!.cancel()
                    mDataTimerTask = null
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPredict()
        if (mSensorManager != null)
            stopSensor(mSensorManager!!, mLAccSensor, mGyrSensor, mMagSensor, mPressureSensor)

    }

    private fun stopSensor(mSensorManager: SensorManager, mLAccSensor: Sensor?, mGyrSensor: Sensor?, mMagSensor: Sensor?, mPressureSensor: Sensor?) {
        enableAllRadioButtons()
        mSensorManager.unregisterListener(this, mLAccSensor)
        mSensorManager.unregisterListener(this, mGyrSensor)
        mSensorManager.unregisterListener(this, mMagSensor)
        mSensorManager.unregisterListener(this, mPressureSensor)
    }

    private fun startSensor(mSensorManager: SensorManager, mLAccSensor: Sensor?, mGyrSensor: Sensor?, mMagSensor: Sensor?, mPressureSensor: Sensor?) {
        disableAllRadioButtons()
        mSensorManager.registerListener(this, mLAccSensor, 0)
        mSensorManager.registerListener(this, mGyrSensor, 0)
        mSensorManager.registerListener(this, mMagSensor, 0)
        mSensorManager.registerListener(this, mPressureSensor, 0)
    }

    private fun enableAllRadioButtons() {
        rbCar42.isEnabled = true
        rbBus42.isEnabled = true
        rbTrain42.isEnabled = true
        rbSubway42.isEnabled = true
    }

    private fun disableAllRadioButtons() {
        rbCar42.isEnabled = false
        rbBus42.isEnabled = false
        rbTrain42.isEnabled = false
        rbSubway42.isEnabled = false
        btn_switch_to_42.isEnabled = false
    }

    companion object {
        const val MSG_PERIOD = 0x1
        const val MSG_STOP = 0x2
    }
}
