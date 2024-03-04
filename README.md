# AndroidBuild


## 自定义View
##### SeekBar(丝滑拖动): [CustomSeekBar](https://github.com/Zhangsongsong/AndroidBuild/blob/main/app/src/main/java/com/zasko/androidbuild/views/CustomSeekBar.kt)



## 海外篇
##### 安装来源获取：
>implementation 'com.android.installreferrer:installreferrer:2.2'

```
fun init(application: MyApplication) {
    referrerClient = InstallReferrerClient.newBuilder(application).build()
    referrerClient.startConnection(object : InstallReferrerStateListener {
        override fun onInstallReferrerSetupFinished(responseCode: Int) {
            IKLog.d("$TAG onInstallReferrerSetupFinished code:${responseCode}")
            when (responseCode) {
                InstallReferrerResponse.OK -> {
                    val responseDetail = referrerClient.installReferrer
                    val url = responseDetail.installReferrer
                    IKLog.d("$TAG onInstallReferrerSetupFinished url:${url}")
                    val deepLinkString = "redshort://app?${url}"
                    val isFirstSetUp = MMKVComponent.getMMKV()?.decodeBool(Constants.FIRST_SET_UP, true) ?: true
                    if (isFirstSetUp) {
                        DeepLinkComponent.handleLinkedIntent(deepLinkString, isDelay = true)
                        MMKVComponent.getMMKV()?.encode(Constants.FIRST_SET_UP, false)
                    }
                }
            }
            referrerClient.endConnection()
        }

        override fun onInstallReferrerServiceDisconnected() {

        }

    })
}
```
