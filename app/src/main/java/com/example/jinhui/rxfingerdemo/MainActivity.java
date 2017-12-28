package com.example.jinhui.rxfingerdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.library.FPerException;
import com.example.library.RxFinger;

import rx.Subscriber;
import rx.Subscription;

/**
 * 学习下指纹识别是如何实现的！
 *
 * 参考链接：https://github.com/Zweihui/RxFingerPrinter
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private FingerView fingerView;
    private Button bt_open;
    // 指纹错误次数
    private int fingerErrorNum = 0;

    private RxFinger rxFinger;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fingerView = findViewById(R.id.fingerView);
        // 状态改变监听
        fingerView.setOnStateChangedListener(new FingerView.OnStateChangedListener() {
            @Override
            public void onChange(int state) {
                Log.e(TAG, "state =" + state);
                if (state == FingerView.STATE_CORRECT_PSD) {
                    fingerErrorNum = 0;
                    Toast.makeText(MainActivity.this, "指纹识别成功", Toast.LENGTH_SHORT).show();
                }
                if (state == FingerView.STATE_ERROR_PSD) {
                    Toast.makeText(MainActivity.this, "指纹识别失败，还剩" + (5-fingerErrorNum) + "次机会",
                            Toast.LENGTH_SHORT).show();
                    fingerView.setState(FingerView.STATE_NO_SCANING);
                }
            }
        });
        bt_open = findViewById(R.id.open);

        // new 对象
        rxFinger = new RxFinger(this);
        bt_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fingerErrorNum = 0;
                // 解除订阅
                rxFinger.unSubscribe(this);
                Subscription subscription = rxFinger.begin().subscribe(new Subscriber<Boolean>() {
                    // 复写onStart
                    @Override
                    public void onStart() {
                        super.onStart();
                        if(fingerView.getState() == FingerView.STATE_SCANING){
                            return;
                        }else if (fingerView.getState() == FingerView.STATE_CORRECT_PSD
                                || fingerView.getState() == FingerView.STATE_ERROR_PSD){
                            fingerView.setState(FingerView.STATE_NO_SCANING);
                        }else {
                            fingerView.setState(FingerView.STATE_SCANING);
                        }
                    }

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof FPerException){
                            Toast.makeText(MainActivity.this,((FPerException) e).getDisplayMessage(),Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        if (aBoolean){
                            fingerView.setState(FingerView.STATE_CORRECT_PSD);
                        }else {
                            fingerErrorNum++;
                            fingerView.setState(FingerView.STATE_ERROR_PSD);
                        }
                    }
                });
                // 添加订阅
                rxFinger.addSubscription(this, subscription);
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 解除订阅
        rxFinger.unSubscribe(this);
    }
}
