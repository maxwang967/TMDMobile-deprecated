package com.morningstarwang.tmdmobile.fragment

import android.os.Message
import android.support.v4.app.Fragment

abstract class BaseFragment : Fragment() {
    abstract fun doPredict()
    abstract fun isRadioButtonCheckedAtLeastOne(): Boolean
    abstract fun getCurrentMatrixCount(realDataFlag: Int, predictDataFlag: Int): Int
    abstract fun setCurrentMatrixCount(realDataFlag: Int, predictDataFlag: Int, count: Int)
    interface OnHandlerMessageListener {
        fun onSendMessage(message: Message)
    }

    interface OnTextChangeListener{
        fun onChangeWindowSize(text: String)
    }
}