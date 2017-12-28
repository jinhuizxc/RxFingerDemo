package com.example.library;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;

import java.util.HashMap;

import rx.Observable;
import rx.Subscription;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

import static com.example.library.CodeException.FINGERPRINTERS_FAILED_ERROR;
import static com.example.library.CodeException.HARDWARE_MISSIING_ERROR;
import static com.example.library.CodeException.KEYGUARDSECURE_MISSIING_ERROR;
import static com.example.library.CodeException.NO_FINGERPRINTERS_ENROOLED_ERROR;
import static com.example.library.CodeException.PERMISSION_DENIED_ERROE;
import static com.example.library.CodeException.SYSTEM_API_ERROR;

/**
 * Created by jinhui on 2017/12/27.
 * Email:1004260403@qq.com
 */

public class RxFinger {

    private static final String TAG = RxFinger.class.getSimpleName();
    // 调用硬件的指纹管理
    private FingerprintManager fingerprintManager;
    //
    private KeyguardManager keyguardManager;

    private Context context;

    private HashMap<String, CompositeSubscription> mSubscriptionMap;
    private PublishSubject<Boolean> publishSubject;
    CancellationSignal mCancellationSignal;
    FingerprintManager.AuthenticationCallback callback;

    public RxFinger(Context context) {
        this.context = context;
    }

    // 初始化
    public PublishSubject<Boolean> begin(){
        if(publishSubject == null){
            publishSubject = PublishSubject.create();
        }
        if(Build.VERSION.SDK_INT < 23){
            publishSubject.onError(new FPerException(SYSTEM_API_ERROR));
        }else {
            initManager();
            confirmFinger();
            startListening(null);
        }
        return publishSubject;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void startListening(FingerprintManager.CryptoObject cryptoObject) {
        // 请求指纹识别的权限
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT)
                != PackageManager.PERMISSION_GRANTED) {
            throw new FPerException(PERMISSION_DENIED_ERROE);
        }
        fingerprintManager.authenticate(cryptoObject, null,0, callback, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void confirmFinger() {
        // 请求指纹识别的权限
        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT)
                != PackageManager.PERMISSION_GRANTED){
            publishSubject.onError(new FPerException(PERMISSION_DENIED_ERROE));
        }
        // 判断硬件是否支持指纹识别
        if(!fingerprintManager.isHardwareDetected()){
            publishSubject.onError(new FPerException(HARDWARE_MISSIING_ERROR));
        }
        // 判断是否开启锁屏密码
        if(!keyguardManager.isKeyguardSecure()){
            publishSubject.onError(new FPerException(KEYGUARDSECURE_MISSIING_ERROR));
        }
        // 判断是否有指纹录入
        if (!fingerprintManager.hasEnrolledFingerprints()){
            publishSubject.onError(new FPerException(NO_FINGERPRINTERS_ENROOLED_ERROR));
        }
    }

    /**
     * 需要api是23以上
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initManager() {
        mCancellationSignal = new CancellationSignal();
        fingerprintManager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
        keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        callback = new FingerprintManager.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                //多次指纹密码验证错误后，进入此方法；并且，不能短时间内调用指纹验证
                publishSubject.onError(new FPerException(FINGERPRINTERS_FAILED_ERROR));
                mCancellationSignal.cancel();
            }

            @Override
            public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                super.onAuthenticationHelp(helpCode, helpString);
            }

            @Override
            public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                publishSubject.onNext(true);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                publishSubject.onNext(false);
            }
        };
    }

    // 这个方法没用到
    public Observable.Transformer<Object, Boolean> ensure(){
        return new Observable.Transformer<Object, Boolean>() {
            @Override
            public Observable<Boolean> call(Observable<Object> objectObservable) {
                return null;
            }
        };
    }

    /**
     * 保存订阅后的subscription
     * @param o
     * @param subscription
     */
    public void addSubscription(Object o, Subscription subscription) {
        if (mSubscriptionMap == null){
            mSubscriptionMap = new HashMap<>();
        }
        String key = o.getClass().getName();
        if(mSubscriptionMap.get(key) != null){
            mSubscriptionMap.get(key).add(subscription);
        }else {
            CompositeSubscription compositeSubscription = new CompositeSubscription();
            compositeSubscription.add(subscription);
            mSubscriptionMap.put(key, compositeSubscription);
        }
    }


    /**
     * 取消订阅
     * @param o
     */
    public void unSubscribe(Object o) {
        if(mSubscriptionMap == null){
            return;
        }
        String key = o.getClass().getName();
        if(!mSubscriptionMap.containsKey(key)){
            return;
        }
        if(mSubscriptionMap.get(key) != null){
            mSubscriptionMap.get(key).unsubscribe();
        }
        mSubscriptionMap.remove(key);
    }


}
