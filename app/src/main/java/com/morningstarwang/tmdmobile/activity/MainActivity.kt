package com.morningstarwang.tmdmobile.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*

import android.support.v7.app.AppCompatActivity
import android.util.Log.e
import android.view.View
import com.morningstarwang.tmdmobile.R
import com.morningstarwang.tmdmobile.api.PredictService
import com.morningstarwang.tmdmobile.fragment.*
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mAcc
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mAccList
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mAccSensor
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mDataTimer
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mDataTimerTask
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mGyr
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mGyrList
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mGyrSensor
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mLAcc
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mLAccList
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mLAccSensor
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mMag
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mMagList
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mMagSensor
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mPressure
import com.morningstarwang.tmdmobile.utils.SpeechUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_eight_mode.*
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mPressureList
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mPressureSensor
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mSensorManager
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mTimer
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mTimerTask
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mode
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.path
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.realDataFlag
import kotlinx.android.synthetic.main.fragment_eight_mode_pf.*
import kotlinx.android.synthetic.main.fragment_four_mode12000.*
import kotlinx.android.synthetic.main.fragment_four_mode450.*
import kr.co.namee.permissiongen.PermissionFail
import kr.co.namee.permissiongen.PermissionGen
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.textColor
import org.jetbrains.anko.toast
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import java.io.File
import java.io.FileWriter
import java.util.*


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), SensorEventListener, BaseFragment.OnHandlerMessageListener, BaseFragment.OnTextChangeListener {

    override fun onChangeWindowSize(text: String) {
        edtWindowSize.setText(text)
    }

    override fun onSendMessage(message: Message) {
        e("here2", "here2")
        handler.sendMessage(message)
    }


    /**
     * 当前模式：
     * 0 八分类 450窗口
     * 1 四分类 450窗口
     * 2 四分类 12000窗口
     * 3 八分类 450窗口 PF
     */

    var timstamp = 0L
    var saveFile: File? = null
    var outStream: FileWriter? = null
    var currentFragment: BaseFragment? = null
    private var handler: Handler = @SuppressLint("HandlerLeak")

    object : Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            when (msg?.what) {
                MSG_PERIOD -> {
                    if (msg.obj != null) {
                        e("here3", "here3")
                        tvPeriod.text = (msg.obj as MutableList<*>)[0].toString()
                        val postDataJson = (msg.obj as MutableList<*>)[1].toString()
                        e("postDataJson=", postDataJson)
                        var call: Call<ResponseBody>? = null
                        var url = ""
                        when(mode){
                            0 -> url = "http://47.95.255.173:5000/"
                            1 -> url = "http://47.95.255.173:5001/"
                            2 -> url = "http://47.95.255.173:5002/"
                            3 -> url = "http://47.95.255.173:5003/"
                        }
                        val retrofit = Retrofit.Builder()
                                .baseUrl(url)
                                .build()
                        val service = retrofit.create(PredictService::class.java)
                        val body = RequestBody.create(MediaType.parse("application/json"), postDataJson)
                        when (mode) {
                            0 -> call = service.predict8(body)
                            1 -> call = service.predict4(body)
                            2 -> call = service.predict42(body)
                            3 -> call = service.predict8pf(body)
                        }
                        call!!.enqueue(object : retrofit2.Callback<ResponseBody> {
                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                val result = response.body()?.string().toString()
                                tvResult.text = result
                                var predictDataFlag = -1
                                when (mode) {
                                    0 -> {
                                        when (result) {
                                            "Still" -> predictDataFlag = 0
                                            "Walk" -> predictDataFlag = 1
                                            "Run" -> predictDataFlag = 2
                                            "Bike" -> predictDataFlag = 3
                                            "Car" -> predictDataFlag = 4
                                            "Bus" -> predictDataFlag = 5
                                            "Train" -> predictDataFlag = 6
                                            "Subway" -> predictDataFlag = 7
                                        }
                                    }
                                    1 -> {
                                        when (result) {
                                            "Car" -> predictDataFlag = 3
                                            "Bus" -> predictDataFlag = 2
                                            "Train" -> predictDataFlag = 1
                                            "Subway" -> predictDataFlag = 0
                                        }
                                    }
                                    2 -> {
                                        when (result) {
                                            "Car" -> predictDataFlag = 3
                                            "Bus" -> predictDataFlag = 2
                                            "Train" -> predictDataFlag = 1
                                            "Subway" -> predictDataFlag = 0
                                        }
                                    }
                                    3 -> {
                                        when (result) {
                                            "Still" -> predictDataFlag = 0
                                            "Walk" -> predictDataFlag = 1
                                            "Run" -> predictDataFlag = 2
                                            "Bike" -> predictDataFlag = 3
                                            "Car" -> predictDataFlag = 4
                                            "Bus" -> predictDataFlag = 5
                                            "Train" -> predictDataFlag = 6
                                            "Subway" -> predictDataFlag = 7
                                        }
                                    }
                                }

                                if (realDataFlag != -1 && predictDataFlag != -1) {
                                    var currentCount = currentFragment!!.getCurrentMatrixCount(realDataFlag, predictDataFlag)
                                    currentCount += 1
                                    currentFragment!!.setCurrentMatrixCount(realDataFlag, predictDataFlag, currentCount)
                                }
                                var correctCount = 0f
                                when (mode) {
                                    0 -> {
                                        for (i in 0..7) {
                                            correctCount += currentFragment!!.getCurrentMatrixCount(i, i)
                                        }
                                    }
                                    1 -> {
                                        for (i in 0..3) {
                                            correctCount += currentFragment!!.getCurrentMatrixCount(i, i)
                                        }
                                    }
                                    2 -> {
                                        for (i in 0..3) {
                                            correctCount += currentFragment!!.getCurrentMatrixCount(i, i)
                                        }
                                    }
                                    3 -> {
                                        for (i in 0..7) {
                                            correctCount += currentFragment!!.getCurrentMatrixCount(i, i)
                                        }
                                    }
                                }

                                val accuracy: Float
                                accuracy = if (tvPeriod.text.toString() == "0") {
                                    0f
                                } else {
                                    correctCount / tvPeriod.text.toString().toFloat()
                                }
                                when (mode) {
                                    0 -> tvAccuracy.text = (accuracy * 100).toString() + "%"
                                    1 -> tvAccuracy4.text = (accuracy * 100).toString() + "%"
                                    2 -> tvAccuracy42.text = (accuracy * 100).toString() + "%"
                                    3 -> tvAccuracypf.text = (accuracy * 100).toString() + "%"
                                }
                                val speech = SpeechUtils.getInstance(applicationContext)
                                speech.speakText(result)
                            }

                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                toast("网络错误, 原因是： " + t.message)
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
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                mLAcc.x = event.values[0]
                mLAcc.y = event.values[1]
                mLAcc.z = event.values[2]
            }
            Sensor.TYPE_ACCELEROMETER -> {
                mAcc.x = event.values[0]
                mAcc.y = event.values[1]
                mAcc.z = event.values[2]
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
        setContentView(R.layout.activity_main)
        requestPermission()
        defaultFragment()
        checkButtonSelected()
        checkIsCnline()
        initActionBar()
        wakeLocker()
        initSensors()
        initPredictions()
        initButtons()
//        initConfusionMatrix()
    }

    private fun initButtons() {
        btn_switch_to_8_450.onClick {
            if (!checkProcessStarted()) {
                mode = 0
                switchFragment(EightMode450Fragment())
                checkButtonSelected()
                tvClassMode.text = "八分类4.5s(有气压)"
            } else {
                toast("请先停止数据采集和模式识别！")
            }

        }
        btn_switch_to_4_450.onClick {
            if (!checkProcessStarted()) {
                mode = 1
                switchFragment(FourMode450Fragment())
                checkButtonSelected()
                tvClassMode.text = "四分类4.5s"
            } else {
                toast("请先停止数据采集和模式识别！")
            }
        }
        btn_switch_to_4_12000.onClick {
            if (!checkProcessStarted()) {
                mode = 2
                switchFragment(FourMode12000Fragment())
                checkButtonSelected()
                tvClassMode.text = "四分类2min"
            } else {
                toast("请先停止数据采集和模式识别！")
            }
        }

        btn_switch_to_8_450_pf.onClick {
            if (!checkProcessStarted()) {
                mode = 3
                switchFragment(EightMode450PFFragment())
                checkButtonSelected()
                tvClassMode.text = "八分类4.5s(无气压)"
            } else {
                toast("请先停止数据采集和模式识别！")
            }

        }
    }

    private fun checkProcessStarted(): Boolean {
        return swDataCollection.isChecked or swModeDetection.isChecked
    }

    private fun switchFragment(targetFragment: BaseFragment) {
        val transaction = supportFragmentManager.beginTransaction()
        if (!targetFragment.isAdded) {
            if (currentFragment != null) {
                transaction.hide(currentFragment)
            }
            transaction.add(R.id.flModeFragmentContainer, targetFragment).commit()
        } else {
            if (currentFragment != null) {
                transaction.hide(currentFragment)
            }
            transaction.show(targetFragment).commit()
        }
        currentFragment = targetFragment
    }


    private fun checkButtonSelected() {
        when (mode) {
            0 -> {
                btn_switch_to_8_450.visibility = View.INVISIBLE
                btn_switch_to_4_450.visibility = View.VISIBLE
                btn_switch_to_4_12000.visibility = View.VISIBLE
                btn_switch_to_8_450_pf.visibility = View.VISIBLE
            }
            1 -> {
                btn_switch_to_8_450.visibility = View.VISIBLE
                btn_switch_to_4_450.visibility = View.INVISIBLE
                btn_switch_to_4_12000.visibility = View.VISIBLE
                btn_switch_to_8_450_pf.visibility = View.VISIBLE
            }
            2 -> {
                btn_switch_to_8_450.visibility = View.VISIBLE
                btn_switch_to_4_450.visibility = View.VISIBLE
                btn_switch_to_4_12000.visibility = View.INVISIBLE
                btn_switch_to_8_450_pf.visibility = View.VISIBLE
            }
            3 -> {
                btn_switch_to_8_450.visibility = View.VISIBLE
                btn_switch_to_4_450.visibility = View.VISIBLE
                btn_switch_to_4_12000.visibility = View.VISIBLE
                btn_switch_to_8_450_pf.visibility = View.INVISIBLE
            }
        }
    }

    private fun requestPermission() {
        PermissionGen.with(this)
                .addRequestCode(100)
                .permissions(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .request()
    }

    private fun defaultFragment() {
        mode = 0
        switchFragment(EightMode450Fragment())
        checkButtonSelected()
    }


    private fun checkIsCnline() {
        if (SplashActivity.isOnline) {
            tvStatus.text = "在线"
            tvStatus.textColor = resources.getColor(R.color.colorOK)
            swStatus.isChecked = true
        } else {
            tvStatus.text = "离线"
            tvStatus.textColor = resources.getColor(R.color.colorFail)
            swStatus.isChecked = false
            swModeDetection.isEnabled = false
        }
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
                MainActivity::class.java.name)
        wakeLock.acquire()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        PermissionGen.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }


    @PermissionFail(requestCode = 100)
    fun permissionDenied() {
        toast("App需要此权限才能正常运行！")
        requestPermission()
    }


    private fun initPredictions() {
        swModeDetection.setOnCheckedChangeListener { _, isChecked ->
            if (!swDataCollection.isChecked) {
                swModeDetection.isChecked = false
                toast("请先启动数据采集！")
                return@setOnCheckedChangeListener
            }
            if (isChecked) {//开始预测
                //调用Fragment中的方法
                e("predict", "predict")
                currentFragment!!.doPredict()
            } else {//停止预测
                stopPredict()
            }
        }
    }

    private fun stopPredict() {
        tvPeriod.text = "0"
        tvResult.text = "N/A"
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
        mLAccSensor = mSensorManager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        mAccSensor = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mGyrSensor = mSensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        mMagSensor = mSensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        mPressureSensor = mSensorManager!!.getDefaultSensor(Sensor.TYPE_PRESSURE)
        swDataCollection.setOnCheckedChangeListener { _, isChecked ->
            if (swModeDetection.isChecked) {
                toast("请先停止模式识别！")
                swDataCollection.isChecked = true
                return@setOnCheckedChangeListener
            }
            if (!currentFragment!!.isRadioButtonCheckedAtLeastOne()) {
                toast("请至少选择一种交通模式！")
                swDataCollection.isChecked = false
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
                when (mode) {
                    0 -> {
                        when {
                            rbStill.isChecked -> realDataFlag = 0
                            rbWalk.isChecked -> realDataFlag = 1
                            rbRun.isChecked -> realDataFlag = 2
                            rbBike.isChecked -> realDataFlag = 3
                            rbCar.isChecked -> realDataFlag = 4
                            rbBus.isChecked -> realDataFlag = 5
                            rbTrain.isChecked -> realDataFlag = 6
                            rbSubway.isChecked -> realDataFlag = 7
                        }
                    }
                    1 -> {
                        when {
                            rbCar4.isChecked -> realDataFlag = 3
                            rbBus4.isChecked -> realDataFlag = 2
                            rbTrain4.isChecked -> realDataFlag = 1
                            rbSubway4.isChecked -> realDataFlag = 0
                        }
                    }
                    2 -> {
                        when {
                            rbCar42.isChecked -> realDataFlag = 3
                            rbBus42.isChecked -> realDataFlag = 2
                            rbTrain42.isChecked -> realDataFlag = 1
                            rbSubway42.isChecked -> realDataFlag = 0
                        }
                    }
                    3 -> {
                        when {
                            rbStillpf.isChecked -> realDataFlag = 0
                            rbWalkpf.isChecked -> realDataFlag = 1
                            rbRunpf.isChecked -> realDataFlag = 2
                            rbBikepf.isChecked -> realDataFlag = 3
                            rbCarpf.isChecked -> realDataFlag = 4
                            rbBuspf.isChecked -> realDataFlag = 5
                            rbTrainpf.isChecked -> realDataFlag = 6
                            rbSubwaypf.isChecked -> realDataFlag = 7
                        }
                    }
                }

                startSensor(mSensorManager!!, mLAccSensor, mAccSensor, mGyrSensor, mMagSensor, mPressureSensor)
                if (mDataTimer == null) {
                    mDataTimer = Timer()
                }
                if (mDataTimerTask == null) {
                    mDataTimerTask = object : TimerTask() {
                        override fun run() {
                            val mLAccAdd = mLAcc.copy()
                            val mAccAdd = mAcc.copy()
                            val mGyrAdd = mGyr.copy()
                            val mMagAdd = mMag.copy()
                            val mPressureAdd = mPressure
                            mLAccList.add(mLAccAdd)
                            mAccList.add(mAccAdd)
                            mGyrList.add(mGyrAdd)
                            mMagList.add(mMagAdd)
                            mPressureList.add(mPressureAdd)
                            val content = "${mAccAdd.y},${mAccAdd.z},${mGyrAdd.x},${mLAccAdd.x},${mLAccAdd.y},${mLAccAdd.z},${mAccAdd.x},${mGyrAdd.y},${mGyrAdd.z},${mMagAdd.x},${mMagAdd.y},${mMagAdd.z},$mPressureAdd,${realDataFlag + 1}\n"
                            var modeName = ""
                            when (mode) {
                                0 -> {
                                    when (realDataFlag) {
                                        0 -> modeName = "Still"
                                        1 -> modeName = "Walk"
                                        2 -> modeName = "Run"
                                        3 -> modeName = "Bike"
                                        4 -> modeName = "Car"
                                        5 -> modeName = "Bus"
                                        6 -> modeName = "Train"
                                        7 -> modeName = "Subway"
                                    }
                                }
                                1 -> {
                                    when (realDataFlag) {
                                        3 -> modeName = "Car"
                                        2 -> modeName = "Bus"
                                        1 -> modeName = "Train"
                                        0 -> modeName = "Subway"
                                    }
                                }
                                2 -> {
                                    when (realDataFlag) {
                                        3 -> modeName = "Car"
                                        2 -> modeName = "Bus"
                                        1 -> modeName = "Train"
                                        0 -> modeName = "Subway"
                                    }
                                }
                                3 -> {
                                    when (realDataFlag) {
                                        0 -> modeName = "Still"
                                        1 -> modeName = "Walk"
                                        2 -> modeName = "Run"
                                        3 -> modeName = "Bike"
                                        4 -> modeName = "Car"
                                        5 -> modeName = "Bus"
                                        6 -> modeName = "Train"
                                        7 -> modeName = "Subway"
                                    }
                                }
                            }

                            saveFile = File(path, "$modeName-$timstamp-next.csv")
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
                stopSensor(mSensorManager!!, mLAccSensor, mAccSensor, mGyrSensor, mMagSensor, mPressureSensor)
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
            stopSensor(mSensorManager!!, mLAccSensor, mAccSensor, mGyrSensor, mMagSensor, mPressureSensor)
    }

    private fun stopSensor(mSensorManager: SensorManager, mLAccSensor: Sensor?, mAccSensor: Sensor?, mGyrSensor: Sensor?, mMagSensor: Sensor?, mPressureSensor: Sensor?) {
        enableAllRadioButtons()
        mSensorManager.unregisterListener(this, mLAccSensor)
        mSensorManager.unregisterListener(this, mAccSensor)
        mSensorManager.unregisterListener(this, mGyrSensor)
        mSensorManager.unregisterListener(this, mMagSensor)
        mSensorManager.unregisterListener(this, mPressureSensor)
    }

    private fun startSensor(mSensorManager: SensorManager, mLAccSensor: Sensor?, mAccSensor: Sensor?, mGyrSensor: Sensor?, mMagSensor: Sensor?, mPressureSensor: Sensor?) {
        disableAllRadioButtons()
        mSensorManager.registerListener(this, mLAccSensor, 0)
        mSensorManager.registerListener(this, mAccSensor, 0)
        mSensorManager.registerListener(this, mGyrSensor, 0)
        mSensorManager.registerListener(this, mMagSensor, 0)
        mSensorManager.registerListener(this, mPressureSensor, 0)
    }

    private fun enableAllRadioButtons() {
        when (mode) {
            0 -> {
                rbStill.isEnabled = true
                rbWalk.isEnabled = true
                rbRun.isEnabled = true
                rbBike.isEnabled = true
                rbCar.isEnabled = true
                rbBus.isEnabled = true
                rbTrain.isEnabled = true
                rbSubway.isEnabled = true
            }
            1 -> {
                rbCar4.isEnabled = true
                rbBus4.isEnabled = true
                rbTrain4.isEnabled = true
                rbSubway4.isEnabled = true
            }
            2 -> {
                rbCar42.isEnabled = true
                rbBus42.isEnabled = true
                rbTrain42.isEnabled = true
                rbSubway42.isEnabled = true
            }
            3 -> {
                rbStillpf.isEnabled = true
                rbWalkpf.isEnabled = true
                rbRunpf.isEnabled = true
                rbBikepf.isEnabled = true
                rbCarpf.isEnabled = true
                rbBuspf.isEnabled = true
                rbTrainpf.isEnabled = true
                rbSubwaypf.isEnabled = true
            }
        }


    }

    private fun disableAllRadioButtons() {
        when (mode) {
            0 -> {
                rbStill.isEnabled = false
                rbWalk.isEnabled = false
                rbRun.isEnabled = false
                rbBike.isEnabled = false
                rbCar.isEnabled = false
                rbBus.isEnabled = false
                rbTrain.isEnabled = false
                rbSubway.isEnabled = false
            }
            1 -> {
                rbCar4.isEnabled = false
                rbBus4.isEnabled = false
                rbTrain4.isEnabled = false
                rbSubway4.isEnabled = false
            }
            2 -> {
                rbCar42.isEnabled = false
                rbBus42.isEnabled = false
                rbTrain42.isEnabled = false
                rbSubway42.isEnabled = false
            }
            3 -> {
                rbStillpf.isEnabled = false
                rbWalkpf.isEnabled = false
                rbRunpf.isEnabled = false
                rbBikepf.isEnabled = false
                rbCarpf.isEnabled = false
                rbBuspf.isEnabled = false
                rbTrainpf.isEnabled = false
                rbSubwaypf.isEnabled = false
            }
        }
    }

    companion object {
        const val MSG_PERIOD = 0x1
        const val MSG_STOP = 0x2
    }
}
