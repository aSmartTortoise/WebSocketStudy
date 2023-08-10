package com.wyj.websocketserver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

/**
 *  https://juejin.cn/post/6847009772198166536
 *  https://blog.csdn.net/gengkui9897/article/details/82863966
 *  
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private WebSocket mWebSocket;
    private SocketHandler mSocketHandler;
    private static final int MSG_START_SERVER = 1000;
    private static final int MSG_SEND = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_start).setOnClickListener(view -> {
            if (mSocketHandler == null) {
                HandlerThread handlerThread = new HandlerThread("webSocket server");
                handlerThread.start();
                mSocketHandler = new SocketHandler(handlerThread.getLooper(), MainActivity.this);
            }
            if (mWebSocket != null) {
                mSocketHandler.sendMessage(mSocketHandler.obtainMessage(MSG_START_SERVER));
            }
        });
    }

    private void startServer() {
        Log.d(TAG, "startServer: ");
        MockWebServer mMockWebServer = new MockWebServer();
        MockResponse response = new MockResponse()
                .withWebSocketUpgrade(new WebSocketListener() {
                    @Override
                    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                        super.onOpen(webSocket, response);
                        //有客户端连接时回调
                        Log.e(TAG, "onOpen 服务器收到客户端连接成功回调：");
                        mWebSocket = webSocket;
                        mWebSocket.send("我是服务器，你好呀");
                    }

                    @Override
                    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                        super.onMessage(webSocket, text);
                        Log.e(TAG, "onMessage 服务器收到消息：" + text);
                        if (mSocketHandler != null) {
                            Message message = mSocketHandler.obtainMessage(MSG_SEND);
                            message.obj = text;
                            mSocketHandler.sendMessageDelayed(message, 500L);
                        }
                    }

                    @Override
                    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                        super.onClosed(webSocket, code, reason);
                        Log.e(TAG, "onClosed onClosed：");
                    }
                });
        mMockWebServer.enqueue(response);
        String websocketUrl = "ws://" + mMockWebServer.getHostName() + ":" + mMockWebServer.getPort() + "/";
        Log.d(TAG, "startServer: webSocketUrl:" + websocketUrl);
        WSManager.getInstance().init(websocketUrl);
    }

    private void sendMsg(String content) {
        if (mWebSocket != null) {
            mWebSocket.send("我是服务器，各位客户端保持联系哈~~~");
        }
    }

    public static class SocketHandler extends Handler {
        private final WeakReference<MainActivity> reference;
        public SocketHandler(Looper looper, MainActivity activity) {
            super(looper);
            this.reference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            MainActivity activity = this.reference.get();
            if (activity != null && !activity.isFinishing()) {
                switch (msg.what) {
                    case MSG_SEND:
                        String content = (String) msg.obj;
                        activity.sendMsg(content);
                        break;
                    case MSG_START_SERVER:
                        activity.startServer();
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWebSocket != null) {
            mWebSocket.close(1001, "stop server, and close.");
        }
        if (mSocketHandler != null) {
            mSocketHandler.removeCallbacksAndMessages(null);
            mSocketHandler = null;
        }
    }
}