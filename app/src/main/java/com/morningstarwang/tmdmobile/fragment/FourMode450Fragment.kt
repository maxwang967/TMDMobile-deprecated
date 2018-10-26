package com.morningstarwang.tmdmobile.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Message
import android.support.v7.widget.GridLayout
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import com.google.gson.Gson
import com.morningstarwang.tmdmobile.R
import com.morningstarwang.tmdmobile.activity.MainActivity
import com.morningstarwang.tmdmobile.pojo.PostData
import com.morningstarwang.tmdmobile.pojo.ThreeAxesData
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mAccList
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mGyrList
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mLAccList
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mMagList
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mPressureList
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mTimer
import com.morningstarwang.tmdmobile.utils.SensorManager.Companion.mTimerTask
import kotlinx.android.synthetic.main.fragment_four_mode450.*
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class FourMode450Fragment : BaseFragment() {

    override fun setCurrentMatrixCount(realDataFlag: Int, predictDataFlag: Int, count: Int) {
        confusionMatrix[realDataFlag][predictDataFlag]!!.text = count.toString()
    }

    override fun getCurrentMatrixCount(realDataFlag: Int, predictDataFlag: Int): Int {
        return confusionMatrix[realDataFlag][predictDataFlag]!!.text.toString().toInt()
    }


    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var handlerMessageListener: OnHandlerMessageListener? = null
    private var onTextChangeListener: OnTextChangeListener?= null
    var confusionMatrix = Array<Array<TextView?>>(8) { arrayOfNulls(8) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }


    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initConfusionMatrix()
        onTextChangeListener!!.onChangeWindowSize("4.5")
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_four_mode450, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnHandlerMessageListener) {
            handlerMessageListener = context
        }
        if (context is OnTextChangeListener){
            onTextChangeListener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        handlerMessageListener = null
        onTextChangeListener = null
    }

    override fun isRadioButtonCheckedAtLeastOne(): Boolean{
        return !( !rbCar4.isChecked &&
                !rbBus4.isChecked &&
                !rbTrain4.isChecked &&
                !rbSubway4.isChecked)
    }

    @SuppressLint("ResourceType")
    private fun initConfusionMatrix() {
        glConfusionMatrix4.removeAllViews()
        for (i in 0..3) {
            for (j in 0..3) {
                val textView = TextView(context)
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
                glConfusionMatrix4.addView(confusionMatrix[i][j], mLayoutParams)
            }
        }
    }

    override fun doPredict(){
        initConfusionMatrix()
        if (mTimer == null) {
            mTimer = Timer()
        }
        if (mTimerTask == null) {
            mTimerTask = object : TimerTask() {
                override fun run() {
                    val currentPeriod = activity.findViewById<TextView>(R.id.tvPeriod).text.toString().toInt()
                    val msg = Message()
                    Log.e("mLAccList.size=", mLAccList.size.toString())
                    Log.e("mGyrList.size=", mGyrList.size.toString())
                    Log.e("mMagList.size=", mMagList.size.toString())
                    Log.e("mPressureList.size=", mPressureList.size.toString())
                    msg.what = MainActivity.MSG_PERIOD
                    val objs: MutableList<Any> = ArrayList()
                    objs.add((currentPeriod + 1).toString())
                    Log.e("currentPeriod", (currentPeriod + 1).toString())
                    var mLAccOK = false
                    var mAccOK = false
                    var mGyrOK = false
                    var mMagOK = false
                    var mPressureOK: Boolean
                    var postLAccList: List<ThreeAxesData> = ArrayList()
                    var postAccList: List<ThreeAxesData> = ArrayList()
                    var postGyrList: List<ThreeAxesData> = ArrayList()
                    var postMagList: List<ThreeAxesData> = ArrayList()
                    var postPressureList: MutableList<Float> = ArrayList()

                    //LAcc
                    if (mLAccList.size >= 450) {//数据充分,足够预测
                        val savedLAccList: MutableList<ThreeAxesData> = ArrayList()
                        for (i in 0..449) {
                            savedLAccList.add(mLAccList[i])
                        }
                        Log.e("savedLAccList.size=", savedLAccList.size.toString())
                        mLAccList.clear()
                        mLAccOK = true
                        postLAccList = savedLAccList
                    }
                    //Acc
                    if (mAccList.size >= 450) {//数据充分,足够预测
                        val savedAccList: MutableList<ThreeAxesData> = ArrayList()
                        for (i in 0..449) {
                            savedAccList.add(mAccList[i])
                        }
                        Log.e("savedAccList.size=", savedAccList.size.toString())
                        mAccList.clear()
                        mAccOK = true
                        postAccList = savedAccList
                    }
                    //Gyr
                    if (mGyrList.size >= 450) {//数据充分,足够预测
                        val savedGyrList: MutableList<ThreeAxesData> = ArrayList()
                        for (i in 0..449) {
                            savedGyrList.add(mGyrList[i])
                        }
                        Log.e("savedGyrList.size=", savedGyrList.size.toString())
                        mGyrList.clear()
                        mGyrOK = true
                        postGyrList = savedGyrList
                    }
                    //Mag
                    if (mMagList.size >= 450) {//数据充分,足够预测
                        val savedMagList: MutableList<ThreeAxesData> = ArrayList()
                        for (i in 0..449){
                            savedMagList.add(mMagList[i])
                        }
                        Log.e("savedMagList.size=", savedMagList.size.toString())
                        mMagList.clear()
                        mMagOK = true
                        postMagList = savedMagList
                    }
                    //Pressure
                    mPressureOK = true
                    if (mPressureList.size >= 450) {//数据充分,足够预测
                        val savedPressureList: MutableList<Float> = ArrayList()
                        for (i in 0..449) {
                            savedPressureList.add(mPressureList[i])
                        }
                        mPressureList.clear()
                        Log.e("savedPressureList.size=", savedPressureList.size.toString())
                        mPressureOK = true
                        postPressureList = savedPressureList
                    }
                    if (mLAccOK && mAccOK && mMagOK && mGyrOK && mPressureOK) {//向服务器发送数据
                        val postData = PostData(postLAccList, postAccList, postGyrList, postMagList, postPressureList)
                        val postDataJson = Gson().toJson(postData)
                        objs.add(postDataJson)
                        msg.obj = objs
                        Log.e("here1", "here1")
                        handlerMessageListener!!.onSendMessage(msg)
                    }

                }
            }
        }
        if (mTimer != null && mTimerTask != null) {
            mTimer!!.schedule(mTimerTask, (1000 * activity.findViewById<EditText>(R.id.edtWindowSize).text.toString().toFloat()).toLong(), (1000 * activity.findViewById<EditText>(R.id.edtWindowSize).text.toString().toFloat()).toLong())
        }
    }


    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                FourMode450Fragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}
