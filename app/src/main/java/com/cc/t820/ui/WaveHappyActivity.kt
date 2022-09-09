package com.cc.t820.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.res.ResourcesCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cc.t820.R
import com.cc.t820.data.Colors
import com.cc.t820.databinding.ActivityWaveHappyBinding
import com.cc.t820.exts.TAG
import com.cc.t820.exts.color
import com.cc.t820.exts.hints
import com.cc.t820.exts.isDarkMode
import com.cc.t820.utils.DataStoreManager
import com.cc.t820.utils.DataStoreManager.dataStoreT
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.flow.first



/**
 * MVI 雏形 | 响应式编程 + 单向数据流 + 唯一可信数据源
 * MVI的Model主要指UI状态（State）
 * 用户的任何操作都被包装成Intent后发送给Model层进行数据请求
 *
 * 1.用户操作以Intent的形式通知Model
 * 2.Model基于Intent更新State
 * 3.View接收到State变化刷新UI
 *
 * */
class WaveHappyActivity : AppCompatActivity() {

    private val mViewModel by viewModels<WaveHappyViewModel>()

    private val mBinding by lazy {
        ActivityWaveHappyBinding.inflate(layoutInflater)
    }

    companion object {
        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, WaveHappyActivity::class.java)
            context.startActivity(starter)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.i(TAG, "========= 销毁重建=======")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        Log.i(TAG, "========= 更改主题色,界面重新绘制了 =======")
        mViewModel.dispatch(WaveIntent.Init)
        initViewModel()
    }


    private fun initViewModel() {
        lifecycleScope.launchWhenStarted {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mViewModel.uiFlow.collect { WaveViewState ->
                    when (WaveViewState) {
                        is WaveViewState.InitView -> {
                            //进行初始化内容
                            if (isDarkMode) {
                                mBinding.leftView.bt1.isChecked = true
                            } else {
                                mBinding.leftView.bt2.isChecked = true
                            }
                            mBinding.leftView.tvMaterialButtonToggleGroup.addOnButtonCheckedListener(
                                materialButton2()
                            )
                            mBinding.leftView.materialButtonGroup.addOnButtonCheckedListener(
                                materialButton()
                            )
                            materialButtonSetClick()

                            mBinding.leftView.edtDelay.hints(mViewModel.mDelay)

                            assets.open("wavesColors.json").use { inputStream ->
                                JsonReader(inputStream.reader()).use { jsonReader ->
                                    val colorsType = object : TypeToken<List<Colors>>() {}.type
                                    mViewModel.colors = Gson().fromJson<MutableList<Colors>?>(
                                        jsonReader,
                                        colorsType
                                    ).filter {
                                        it.darkSuitable == isDarkMode
                                    } as MutableList<Colors>

                                }
                            }

                            getPoetryToken()

                            mBinding.leftView.edtDelay.editText?.addTextChangedListener(textW())

                            mBinding.leftView.chip.setOnClickListener {

                                mViewModel.mDelay =
                                    mBinding.leftView.edtDelay.editText?.text.toString().toInt()

                                if (mBinding.leftView.chip.isVisible)
                                   it.animate()
                                        .alpha(0f)
                                        .setDuration(1000)
                                        .setListener(object : AnimatorListenerAdapter() {
                                            override fun onAnimationEnd(animation: Animator) {
                                                it.isVisible=false
                                            }
                                        })
                                getPoetryToken()

                                mBinding.leftView.edtDelay.hints(mViewModel.mDelay)

                                mBinding.leftView.edtDelay.editText?.clearFocus()
                            }

                        }
                        is WaveViewState.PoetryToken -> {
                            setDataStoreToken(WaveViewState.dataBean.token)
                        }

                        is WaveViewState.PoetryInfo -> {
                            WaveViewState.poetryBean.apply {
                                mBinding.layoutMain.tvContent.text = this.content
                                mBinding.layoutMain.tvTitle.text = "「${this.origin.title}」"
                                mBinding.layoutMain.tvAuthor.text = this.origin.author

                                mBinding.leftView.tvTitle.text = this.origin.title
                                mBinding.leftView.tvAuthor.text = this.origin.author
                                mBinding.leftView.tvDynasty.text = "(${this.origin.dynasty})"
                                if (this.origin.content.isNotEmpty()) {
                                    mViewModel.mStr.clear()
                                    this.origin.content.forEach { text ->
                                        mViewModel.mStr.append(text).append("\n")
                                    }
                                    mBinding.leftView.tvContent.text = mViewModel.mStr
                                }

                            }
                            var mRandom = (0 until mViewModel.colors.size).random()


                            mBinding.layoutMain.wave1.setWaveColor(mViewModel.colors[mRandom].hex.color)
                            mBinding.layoutMain.wave2.setWaveColor(mViewModel.colors[mRandom].hex.color)
                            mBinding.layoutMain.wave3.setWaveColor(mViewModel.colors[mRandom].hex.color)
                            mBinding.leftView.tvColor.text = mViewModel.colors[mRandom].name
                            mBinding.leftView.tvColor.setTextColor(mViewModel.colors[mRandom].hex.color)
                        }

                    }
                }
            }
        }
    }

    private fun materialButton() =
        MaterialButtonToggleGroup.OnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.bt1 -> {
                        AppCompatDelegate.setDefaultNightMode(
                            AppCompatDelegate.MODE_NIGHT_YES
                        )
                    }
                    R.id.bt2 -> {
                        AppCompatDelegate.setDefaultNightMode(
                            AppCompatDelegate.MODE_NIGHT_NO
                        )
                    }
                    R.id.bt3 -> {
                        AppCompatDelegate.setDefaultNightMode(
                            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        )

                    }
                }
            }

        }

    private fun materialButton2() =
        MaterialButtonToggleGroup.OnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btZx -> {
                        setTextType(0)
                        val typeface = ResourcesCompat.getFont(this, R.font.jxzt)
                        mBinding.apply {
                            this.leftView.textNavHeader.typeface = typeface
                            this.leftView.tvColorCur.typeface = typeface
                            this.leftView.tvContent.typeface = typeface
                            this.leftView.tvColor.typeface = typeface
                            this.leftView.tvAuthor.typeface = typeface
                            this.leftView.tvTitle.typeface = typeface
                            this.leftView.tvDynasty.typeface = typeface
                            this.leftView.bt1.typeface = typeface
                            this.leftView.bt2.typeface = typeface
                            this.leftView.bt3.typeface = typeface

                            this.layoutMain.tvContent.typeface = typeface
                            this.layoutMain.tvTitle.typeface = typeface
                            this.layoutMain.tvAuthor.typeface = typeface
                        }
                    }
                    R.id.btWk -> {
                        setTextType(1)
                        val typeface = ResourcesCompat.getFont(this, R.font.wenkai)
                        mBinding.apply {
                            this.leftView.textNavHeader.typeface = typeface
                            this.leftView.tvColorCur.typeface = typeface
                            this.leftView.tvContent.typeface = typeface
                            this.leftView.tvColor.typeface = typeface
                            this.leftView.tvAuthor.typeface = typeface
                            this.leftView.tvTitle.typeface = typeface
                            this.leftView.tvDynasty.typeface = typeface
                            this.leftView.bt1.typeface = typeface
                            this.leftView.bt2.typeface = typeface
                            this.leftView.bt3.typeface = typeface

                            this.layoutMain.tvContent.typeface = typeface
                            this.layoutMain.tvTitle.typeface = typeface
                            this.layoutMain.tvAuthor.typeface = typeface
                        }
                    }

                }
            }

        }

    private fun textW() = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

        override fun afterTextChanged(s: Editable?) {
            if (mBinding.leftView.chip.isGone)
                mBinding.leftView.chip.animate()
                    .alpha(1f)
                    .setDuration(1000)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            mBinding.leftView.chip.visibility = View.VISIBLE
                        }
                    })
        }

    }

    private fun materialButtonSetClick() {
//
//        var s1 = 0
//        // 直到DataStore返回数据为止
//        runBlocking {
//            this@WaveHappyActivity.dataStoreT.data.first { setting ->
//                setting[DataStoreManager.TextType] ?: 0
//                true
//            }
//        }
//        if (s1 == 0) {
//            mBinding.leftView.btZx.isChecked = true
//        } else {
//            mBinding.leftView.btWk.isChecked = true
//        }

        lifecycleScope.launchWhenStarted {
            val s1 = this@WaveHappyActivity.dataStoreT.data.first()[DataStoreManager.TextType] ?: 0
            if (s1 == 0) {
                mBinding.leftView.btZx.isChecked = true
            } else {
                mBinding.leftView.btWk.isChecked = true
            }
        }

    }

    // 目前字体 0 江西拙楷  1
    private fun setTextType(int: Int) {
        lifecycleScope.launchWhenStarted {
            this@WaveHappyActivity.dataStoreT.edit { setting ->
                if (setting[DataStoreManager.TextType] != int)
                    setting[DataStoreManager.TextType] = int
            }
        }
    }

    private fun getPoetryToken() {

        lifecycleScope.launchWhenStarted {
            mViewModel.mToken =
                this@WaveHappyActivity.dataStoreT.data.first()[DataStoreManager.PoetryToken]

            if (mViewModel.mToken.isNullOrBlank()) {
                mViewModel.dispatch(WaveIntent.PoetryToken)
            } else {
                mViewModel.dispatch(WaveIntent.PoetryInfo(mViewModel.mToken!!))
            }
        }


//        runBlocking {
//            mViewModel.mToken= this@WaveHappyActivity.dataStoreT.data.first()[DataStoreManager.PoetryToken]
//
////            this@WaveHappyActivity.dataStoreT.data.first { setting ->
////                mViewModel.mToken = setting[DataStoreManager.PoetryToken]
////                true
////            }
//            if (mViewModel.mToken.isNullOrBlank()) {
//                mViewModel.dispatch(WaveIntent.PoetryToken)
//            } else {
//                mViewModel.dispatch(WaveIntent.PoetryInfo(mViewModel.mToken!!))
//            }
//        }
    }

    private fun setDataStoreToken(str: String) {
        lifecycleScope.launchWhenStarted {
            if (str.isNotBlank()) {
                this@WaveHappyActivity.dataStoreT.edit { setting ->
                    //写数据
                    setting[DataStoreManager.PoetryToken] = str
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mBinding.leftView.materialButtonGroup.removeOnButtonCheckedListener(materialButton())
        mBinding.leftView.tvMaterialButtonToggleGroup.removeOnButtonCheckedListener(materialButton2())
        mBinding.leftView.edtDelay.editText?.removeTextChangedListener(textW())
        Log.e(TAG, "onDestroy: ")
    }
}


