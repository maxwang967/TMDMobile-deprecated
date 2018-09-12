package morningstarwang.com.tmd_mobile

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.util.Log.e
import kotlinx.android.synthetic.main.activity_main.*
import morningstarwang.com.tmd_mobile.pojo.ThreeAxesData
import morningstarwang.com.tmd_mobile.util.SharedPrefsStrListUtil
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), SensorEventListener {
    var mLAccList: MutableList<ThreeAxesData> = ArrayList()
    var mGyrList: MutableList<ThreeAxesData> = ArrayList()
    var mMagList: MutableList<ThreeAxesData> = ArrayList()


    private var handler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {     //此处的object 要加，否则无法重写 handlerMessage
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            when (msg?.what) {
                MSG_PERIOD -> {
                    tvPeriod.text = msg.obj.toString()
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
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val mLAccSensor: Sensor? = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val mGyrSensor: Sensor? = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val mMagSensor: Sensor? = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        swDataCollection.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {//开启数据采集
                mSensorManager.registerListener(this, mLAccSensor, 10000)
                mSensorManager.registerListener(this, mGyrSensor, 10000)
                mSensorManager.registerListener(this, mMagSensor, 10000)
            } else {//关闭数据采集
                mSensorManager.unregisterListener(this, mLAccSensor)
                mSensorManager.unregisterListener(this, mGyrSensor)
                mSensorManager.unregisterListener(this, mMagSensor)
            }
        }
        swModeDetection.setOnCheckedChangeListener { _, isChecked ->
            val task: TimerTask = object : TimerTask() {
                override fun run() {
                    val currentPeriod = tvPeriod.text.toString().toInt()
                    val msg = Message()
                    msg.what = MSG_PERIOD
                    msg.obj = (currentPeriod + 1).toString()
                    e("currentPeriod", (currentPeriod + 1).toString())
                    //LAcc
                    if (mLAccList.size >= 100 * edtWindowSize.text.toString().toFloat()) {//数据充分,足够预测
                        val savedLAccList: MutableList<ThreeAxesData> = ArrayList()
                        for (i in 0..((100 * edtWindowSize.text.toString().toFloat() - 1).toInt())) {
                            savedLAccList.add(mLAccList[i])
                        }
                        mLAccList = ArrayList()
                        e("savedLAccList.size=", savedLAccList.size.toString())
                    }
                    //Gyr
                    if (mLAccList.size >= 100 * edtWindowSize.text.toString().toFloat()) {//数据充分,足够预测
                        val savedLAccList: MutableList<ThreeAxesData> = ArrayList()
                        for (i in 0..((100 * edtWindowSize.text.toString().toFloat() - 1).toInt())) {
                            savedLAccList.add(mLAccList[i])
                        }
                        mLAccList = ArrayList()
                        e("savedLAccList.size=", savedLAccList.size.toString())
                    }
                    //Mag
                    if (mLAccList.size >= 100 * edtWindowSize.text.toString().toFloat()) {//数据充分,足够预测
                        val savedLAccList: MutableList<ThreeAxesData> = ArrayList()
                        for (i in 0..((100 * edtWindowSize.text.toString().toFloat() - 1).toInt())) {
                            savedLAccList.add(mLAccList[i])
                        }
                        mLAccList = ArrayList()
                        e("savedLAccList.size=", savedLAccList.size.toString())
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
    }
}
