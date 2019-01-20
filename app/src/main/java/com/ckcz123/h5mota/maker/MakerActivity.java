package com.ckcz123.h5mota.maker;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ckcz123.h5mota.maker.lib.CustomToast;
import com.tencent.smtt.export.external.interfaces.ConsoleMessage;
import com.tencent.smtt.export.external.interfaces.JsPromptResult;
import com.tencent.smtt.export.external.interfaces.JsResult;
import com.tencent.smtt.export.external.interfaces.SslError;
import com.tencent.smtt.export.external.interfaces.SslErrorHandler;
import com.tencent.smtt.sdk.URLUtil;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MakerActivity extends AppCompatActivity {

    WebView webView;
    MakerActivity activity;
    ProgressBar progressBar;

    private ValueCallback<Uri> mUploadMessage;
    public ValueCallback<Uri[]> uploadMessage;
    public static final int REQUEST_SELECT_FILE = 100;
    public static final int JSINTERFACE_SELECT_FILE = 200;
    private final static int FILECHOOSER_RESULTCODE = 2;

    private File LOG_FILE;
    private String log_file_name;
    private SimpleDateFormat simpleDateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity=this;

        // 0-竖屏；1-横屏
        if (MainActivity.orientationMode == 1)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        File log_dir = new File(MainActivity.makerDir, ".logs");
        if (!log_dir.exists() && !log_dir.mkdirs()) {
            LOG_FILE = null;
        }
        else {
            log_file_name = "log-"+
                    new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date())+".txt";
            LOG_FILE = new File(log_dir, log_file_name);
            try {
                LOG_FILE.createNewFile();
            }
            catch (Exception e) {
                LOG_FILE = null;
                log_file_name = null;
            }
        }
        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());


        //webView=new WebView(this);
        //setContentView(webView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        setContentView(R.layout.activity_webview);


        webView=findViewById(R.id.webview);
        progressBar=findViewById(R.id.progressBar);

        progressBar.setVisibility(View.VISIBLE);

        setTitle(getIntent().getStringExtra("title"));

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportZoom(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_INSET);
        webSettings.setAllowContentAccess(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setDatabaseEnabled(true);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        webView.addJavascriptInterface(new JSInterface(this, webView), "jsinterface");

        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                webView.loadUrl(url);
                return true;
            }
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                activity.setTitle(webView.getTitle());
            }
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result)  {
                new AlertDialog.Builder(activity)
                        .setTitle("JsAlert")
                        .setMessage(message)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm();
                            }
                        })
                        .setCancelable(false)
                        .show();
                return true;
            }

            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(activity)
                        .setTitle("Javascript发来的提示")
                        .setMessage(message)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.cancel();
                            }
                        })
                        .setCancelable(false)
                        .show();
                return true;
            }
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, final JsPromptResult result) {
                final EditText et = new EditText(activity);
                et.setText(defaultValue);
                new AlertDialog.Builder(activity)
                        .setTitle(message)
                        .setView(et)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm(et.getText().toString());
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.cancel();
                            }
                        })
                        .setCancelable(false)
                        .show();

                return true;
            }
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                // startActivityForResult(Intent.createChooser(i, "File Browser"), FILECHOOSER_RESULTCODE);
                startActivityForResult(i, FILECHOOSER_RESULTCODE);
            }
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                openFileChooser(uploadMsg);
            }
            protected void openFileChooser(ValueCallback uploadMsg, String acceptType) {
                openFileChooser(uploadMsg);
            }

            public boolean onShowFileChooser(WebView mWebView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams)
            {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                    uploadMessage = null;
                }

                uploadMessage = filePathCallback;

                Intent intent = fileChooserParams.createIntent();
                try
                {
                    startActivityForResult(intent, REQUEST_SELECT_FILE);
                } catch (ActivityNotFoundException e)
                {
                    uploadMessage = null;
                    return false;
                }
                return true;
            }

            public void onProgressChanged(WebView view, int progress) {
                progressBar.setProgress(progress);
            }

            public boolean onConsoleMessage(ConsoleMessage message) {
                String msg = message.message();
                if (msg.equals("[object Object]") || msg.equals("localForage supported!") || msg.equals("插件编写测试") || msg.equals("开始游戏"))
                    return false;
                ConsoleMessage.MessageLevel level = message.messageLevel();
                if (level != ConsoleMessage.MessageLevel.LOG && level != ConsoleMessage.MessageLevel.ERROR)
                    return false;
                try (PrintWriter printWriter = new PrintWriter(new FileWriter(LOG_FILE, true))){
                    String s = String.format("[%s] {%s, Line %s, Source %s} %s\r\n", simpleDateFormat.format(new Date()),
                            level.toString(), message.lineNumber(), message.sourceId(), msg);
                    printWriter.write(s);
                }
                catch (Exception e) {
                    Log.i("Console", "error", e);
                }
                return false;
            }

        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimetype);
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                String filename = URLUtil.guessFileName(url, contentDisposition, mimetype);
                new File(Environment.getExternalStorageDirectory() + "/H5motaMaker/").mkdirs();
                File file = new File(Environment.getExternalStorageDirectory() + "/H5motaMaker/" + filename);
                if (file.exists()) file.delete();
                request.setDestinationUri(Uri.fromFile(file));
                request.setTitle("正在下载" + filename + "...");
                request.setDescription("文件保存在" + file.getAbsolutePath());
                DownloadManager downloadManager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
                downloadManager.enqueue(request);

                CustomToast.showInfoToast(activity, "文件下载中，请在通知栏查看进度");
            } catch (Exception e) {
                if (url.startsWith("blob")) {
                    CustomToast.showErrorToast(activity, "无法下载文件！");
                    return;
                }
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        });

        webView.loadUrl(getIntent().getStringExtra("url"));
    }

    double exittime=0;

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode==KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack();
            }
            else {
                if (System.currentTimeMillis()-exittime>2000) {
                    Toast.makeText(this, "再按一遍返回主页面", Toast.LENGTH_SHORT).show();
                    exittime=System.currentTimeMillis();
                }
                else
                {
                    exittime=0;
                    webView.loadUrl("about:blank");
                    finish();
                }
            }
            return true;
        }
        return false;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode==RESULT_OK) {
            Uri result = intent == null? null: intent.getData();
            switch (requestCode) {
                case REQUEST_SELECT_FILE:
                    if (uploadMessage == null)
                        return;
                    uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
                    uploadMessage = null;
                    return;
                case FILECHOOSER_RESULTCODE:
                    if (null == mUploadMessage) return;
                    mUploadMessage.onReceiveValue(result);
                    mUploadMessage = null;
                    break;
                case JSINTERFACE_SELECT_FILE:
                    if (result == null) break;
                    ContentResolver contentResolver = getContentResolver();
                    String type = contentResolver.getType(result);
                    if (type!=null && type.startsWith("image")) {
                        try (InputStream inputStream = getContentResolver().openInputStream(result)) {
                            byte[] bytes = IOUtils.toByteArray(inputStream);
                            String base64 = Base64.encodeToString(bytes, Base64.DEFAULT);
                            webView.evaluateJavascript("core.readFileContent('data:"+type+";base64," + base64 +"')", null);
                        }
                        catch (Exception e) {
                            CustomToast.showErrorToast(this, "读取失败！");
                            e.printStackTrace();
                        }
                        return;
                    }
                    try (InputStream inputStream = getContentResolver().openInputStream(result);
                         BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                        String line;
                        StringBuilder builder = new StringBuilder();
                        while ((line=reader.readLine())!=null) builder.append(line);
                        webView.evaluateJavascript("core.readFileContent('" + builder.toString().replace('\'', '\"') +"')", null);
                    }
                    catch (Exception e) {
                        CustomToast.showErrorToast(this, "读取失败！");
                        e.printStackTrace();
                    }

            }
        }
    }

    public void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        menu.add(Menu.NONE, 0, 0, "刷新").setIcon(R.drawable.refresh).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, 1, 1, "上张地图").setIcon(R.drawable.down).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, 2, 2, "下张地图").setIcon(R.drawable.up).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        if (MainActivity.orientationMode == 1)
            menu.add(Menu.NONE, 3, 3, "进入游戏").setIcon(R.drawable.play).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, 4, 4, "控制台").setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, 5, 5, "重置地图").setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, 6, 6, "查看日志").setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, 7, 7, "打开目录").setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, 8, 8, "帮助文档").setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, 9, 9, "横竖屏切换").setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, 10, 10, "退出").setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case 0: webView.reload(); break;
            case 1: {
                String url = webView.getUrl();
                if (!url.endsWith("/editor.html") && !url.endsWith("/editor-mobile.html")) {
                    CustomToast.showErrorToast(this, "只有在造塔器页面才能切换地图！");
                    break;
                }
                webView.evaluateJavascript("try { document.getElementById('mid').onmousewheel(" +
                        "    {preventDefault:function(){}, detail: -1}" +
                        ")} catch (e) {}", null);
                break;
            }
            case 2: {
                String url = webView.getUrl();
                if (!url.endsWith("/editor.html") && !url.endsWith("/editor-mobile.html")) {
                    CustomToast.showErrorToast(this, "只有在造塔器页面才能切换地图！");
                    break;
                }
                webView.evaluateJavascript("try { document.getElementById('mid').onmousewheel(" +
                        "    {preventDefault:function(){}, detail: 1}" +
                        ")} catch (e) {}", null);
                break;
            }
            case 3: {
                try {
                    webView.loadUrl(MainActivity.LOCAL + URLEncoder.encode(MainActivity.workingDirectory, "utf-8")+"/index.html");
                }
                catch (Exception e) {
                    Log.i("Error", "error", e);
                }
                break;
            }
            case 4: {
                final EditText editText = new EditText(this);
                editText.setHint("请输入控制台命令...");
                new AlertDialog.Builder(this).setTitle("控制台命令")
                        .setView(editText).setPositiveButton("确定", (dialogInterface, i) -> {
                    String content = editText.getEditableText().toString();
                    webView.evaluateJavascript(content, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                            if (s==null) s = "null";
                            s+="\n\n可以通过查看日志来获得更详细的控制台输出信息。";
                            new AlertDialog.Builder(MakerActivity.this).setTitle("执行结果")
                                    .setMessage(s).setPositiveButton("确定",null)
                                    .setCancelable(true).create().show();
                        }
                    });
                }).setNegativeButton("取消", null).setCancelable(true).create().show();
                break;
            }
            case 5: {
                String url = webView.getUrl();
                if (!url.endsWith("/index.html")) {
                    CustomToast.showErrorToast(this, "只有在游戏内才能重置地图！");
                    break;
                }
                new AlertDialog.Builder(this).setTitle("确认重置？")
                        .setMessage("你想调用 core.resetMap() 来重置当前楼层地图吗？")
                        .setCancelable(true)
                        .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                webView.evaluateJavascript("try { core.resetMap(); } catch (e) {} ", null);
                            }
                        }).setNegativeButton("取消", null).create().show();
                break;
            }
            case 6: {
                File file = new File(MainActivity.makerDir, ".logs");
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(file.getAbsolutePath()), "resource/folder");
                if (intent.resolveActivityInfo(getPackageManager(), 0) != null)
                    startActivity(intent);
                else CustomToast.showErrorToast(this, "无法打开目录！");
                break;
            }
            case 7: {
                File file = new File(MainActivity.makerDir, MainActivity.workingDirectory);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(file.getAbsolutePath()), "resource/folder");
                if (intent.resolveActivityInfo(getPackageManager(), 0) != null)
                    startActivity(intent);
                else CustomToast.showErrorToast(this, "无法打开目录！");
                break;
            }
            case 8: loadDocuments(); break;
            case 9: {
                if (MainActivity.orientationMode == 0)
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                MainActivity.orientationMode = 1-MainActivity.orientationMode;
                break;
            }
            case 10: webView.loadUrl("about:blank");finish();break;
        }
        return true;
    }

    public void loadDocuments() {
        try {
            Intent intent = new Intent(this, WebActivity.class);
            if (Build.VERSION.SDK_INT >= 21)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            intent.putExtra("title", "查看文档");
            intent.putExtra("url", "https://h5mota.com/games/template/docs/");
            startActivity(intent);
        }
        catch (Exception e) {
            e.printStackTrace();
            CustomToast.showErrorToast(this, "无法查看文档！");
        }
    }

}
