package com.morningstarwang.tmdmobile.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.*
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AlertDialog
import android.util.Log.e
import com.google.gson.Gson
import com.morningstarwang.tmdmobile.R
import com.morningstarwang.tmdmobile.pojo.UpdateData
import kotlinx.android.synthetic.main.activity_splash.*
import kr.co.namee.permissiongen.PermissionFail
import kr.co.namee.permissiongen.PermissionGen
import org.jetbrains.anko.*
import java.io.File
import java.lang.Exception
import java.net.URL


class SplashActivity : AppCompatActivity() {
    companion object {
        var isOnline = true
        const val MSG_OK = 0x1
        const val MSG_CHECK_AGAIN = 0x2
        const val versionCode = 4
        const val update_url = "https://raw.githubusercontent.com/morningstarwang/releases/master/android/ict/tmd/update.json"
    }

    var failCount = 0

    inline fun <reified T : Any> Gson.fromJson(json: String): T {
        return Gson().fromJson(json, T::class.java)
    }

    private var handler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            when (msg?.what) {
                MSG_OK -> {
                    requestPermission()
                    startActivity<MainActivity>()
                    finish()
                }
                MSG_CHECK_AGAIN -> {
                    failCount += 1
                    checkUpdate()
                }
            }

        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        initActionBar()

//        val builder = StrictMode.VmPolicy.Builder()
//        StrictMode.setVmPolicy(builder.build())
//        builder.detectFileUriExposure()
//        val msg = Message()
//        msg.what = MSG_OK
//        handler.sendMessageDelayed(msg, 3000)
        checkUpdate()
    }

    private fun checkUpdate() {
        tvTip.text = "正在与服务器通信..."
        doAsync {
            val updateData = getUpdateData(update_url)
            if (updateData == null) {
                uiThread {
                    if(failCount<3){
                        isOnline = false
                        toast("网络连接不可用，请检查您的网络设置。（三次失败后将进入离线模式）")
                        handler.sendEmptyMessageDelayed(MSG_CHECK_AGAIN, 5000)
                    }else{
                        handler.sendEmptyMessage(MSG_OK)
                    }
                }
            }else{
                isOnline = true
            }
            uiThread {
                tvTip.text = "正在验证软件版本..."
            }
            if (updateData?.versionCode!! > versionCode) {//有更新
                activityUiThreadWithContext { f ->
                    requestPermission()
                    val mDialog = AlertDialog.Builder(f)
                    mDialog.setTitle("软件更新")
                            .setMessage("我们做了如下工作：\n" + updateData.description)
                            .setCancelable(false)
                            .setPositiveButton("立即更新", DialogInterface.OnClickListener { dialog, which ->
                                tvTip.text = "正在下载APK，请稍候..."
                                if (!getAPK(updateData.url)) {
                                    if(failCount<=3){
                                        isOnline = false
                                        toast("网络连接不可用，请检查您的网络设置。（三次失败后将进入离线模式）")
                                        handler.sendEmptyMessageDelayed(MSG_CHECK_AGAIN, 5000)
                                    }else{
                                        handler.sendEmptyMessage(MSG_OK)
                                    }
                                }
                            })
                            .create().show()
                }
            } else {
                handler.sendEmptyMessage(MSG_OK)
            }

        }
    }

    private fun getUpdateData(path: String): UpdateData? {
        val url = URL(path)
        return try {
            val content = url.readText()
            e("content", content)
            var updateData: UpdateData = Gson().fromJson(content)
            e("updateData=", updateData.toString())
            updateData
        } catch (e: Exception) {
            null
        }
    }

    private fun getAPK(path: String): Boolean {
        val url = URL(path)
        try {
            var path = Environment.getExternalStorageDirectory().absolutePath + "/"
            path += "tmd_mobile/"
            e("url", url.toString())
            val file = File(path)
            if (!file.exists()) {
                file.mkdir()
            }
            doAsync {
                val output = File(path, "latest.apk")
                output.writeBytes(url.readBytes())
                activityUiThreadWithContext {
                    f ->
                    val intent = Intent(Intent.ACTION_VIEW)
                    //版本在7.0以上是不能直接通过uri访问的
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        //参数1 上下文, 参数2 Provider主机地址 和配置文件中保持一致   参数3  共享的文件
                        val apkUri = FileProvider.getUriForFile(f, "com.morningstarwang.tmdmobile", output)
                        //添加这一句表示对目标应用临时授权该Uri所代表的文件
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
                    } else {
                        intent.setDataAndType(Uri.fromFile(output),
                                "application/vnd.android.package-archive")
                    }
                    f.startActivity(intent)
                    f.finish()
                }
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun initActionBar() {
        val actionBar = supportActionBar
        actionBar!!.setLogo(R.mipmap.icon)
        actionBar.setDisplayUseLogoEnabled(true)
        actionBar.setDisplayShowHomeEnabled(true)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        PermissionGen.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }


    @PermissionFail(requestCode = 100)
    fun permissionDenied() {
        toast("App需要此权限才能正常运行！")
        requestPermission()
    }

    private fun requestPermission() {
        PermissionGen.with(this)
                .addRequestCode(100)
                .permissions(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .request()
    }

}
