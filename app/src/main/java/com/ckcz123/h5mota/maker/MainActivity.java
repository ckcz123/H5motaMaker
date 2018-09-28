package com.ckcz123.h5mota.maker;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.ckcz123.h5mota.maker.lib.CustomToast;
import com.ckcz123.h5mota.maker.lib.HttpRequest;
import com.ckcz123.h5mota.maker.lib.MyWebServer;
import com.ckcz123.h5mota.maker.lib.Utils;
import com.tencent.smtt.sdk.QbSdk;

import org.json.JSONObject;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fi.iki.elonen.SimpleWebServer;
import me.weyye.hipermission.HiPermission;
import me.weyye.hipermission.PermissionCallback;
import me.weyye.hipermission.PermissionItem;

public class MainActivity extends AppCompatActivity {

    public static final String DOMAIN = "https://h5mota.com";
    public static final String LOCAL = "http://127.0.0.1:1056/";

    SimpleWebServer simpleWebServer;
    public String workingDirectory;
    public File makerDir;
    public File templateDir;
    private ListView listView;
    private List<String> items;
    private String templateVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        items = new ArrayList<>();

        listView = findViewById(R.id.listView);
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items));
        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            try {
                String name = items.get(i);
                workingDirectory = name;
                loadUrl(LOCAL+URLEncoder.encode(name, "utf-8")+"/editor-mobile.html", name);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        });
        listView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            String name = items.get(i);
            File file = new File(makerDir, name);
            new AlertDialog.Builder(MainActivity.this)
                .setItems(new String[]{"打开目录", "重命名项目", "删除项目"}, (dialogInterface, i1) -> {
                    try {
                        if (i1 ==0) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.parse(file.getAbsolutePath()), "resource/folder");
                            if (intent.resolveActivityInfo(getPackageManager(), 0) != null)
                                startActivity(intent);
                            else CustomToast.showErrorToast(MainActivity.this, "无法打开目录");
                        }
                        else if (i1 ==1) {
                            final EditText editText = new EditText(MainActivity.this);
                            editText.setText(name);
                            new AlertDialog.Builder(MainActivity.this).setTitle("重命名项目")
                                .setCancelable(true).setView(editText)
                                .setPositiveButton("确定", (dialogInterface1, i11) -> {
                                    String content = editText.getEditableText().toString();
                                    if (content.isEmpty()) return;
                                    File to = new File(makerDir, content);
                                    if (to.exists())
                                        CustomToast.showErrorToast(MainActivity.this, content+"已存在！");
                                    else if (file.renameTo(to)) {
                                        CustomToast.showSuccessToast(MainActivity.this, "重命名成功！");
                                        updateItems();
                                    }
                                    else
                                        CustomToast.showErrorToast(MainActivity.this, "无法重命名项目");
                                }).setNegativeButton("取消", null).create().show();
                        }
                        else if (i1 ==2) {
                            new AlertDialog.Builder(MainActivity.this).setTitle("提示")
                                .setMessage("确定要删除项目"+name+"么？").setCancelable(true)
                                .setPositiveButton("确定", (dialogInterface12, i112) -> {
                                    Utils.deleteFile(file);
                                    CustomToast.showSuccessToast(MainActivity.this, "删除成功！");
                                    updateItems();
                                }).setNegativeButton("取消", null).create().show();
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        CustomToast.showErrorToast(MainActivity.this, e.getMessage());
                    }
                }).setCancelable(true).create().show();
            return true;
        });

        List<PermissionItem> list=new ArrayList<>();
        list.add(new PermissionItem(Manifest.permission.WRITE_EXTERNAL_STORAGE, "存储权限", R.drawable.permission_ic_storage));
        list.add(new PermissionItem(Manifest.permission.READ_PHONE_STATE, "读取手机状态", R.drawable.permission_ic_phone));

        HiPermission.create(this)
                .title("权限申请").permissions(list).msg("你需要如下权限来使用本软件")
                .checkMutiPermission(new PermissionCallback() {
            @Override
            public void onClose() {
                Log.i("Main", "onClose");
            }

            @Override
            public void onFinish() {
                Log.i("Main", "onFinish");
                initSDCard();
            }

            @Override
            public void onDeny(String permission, int position) {
                Log.i("Main", "onDeny");
            }

            @Override
            public void onGuarantee(String permission, int position) {
                Log.i("Main", "onGuarantee");
            }
        });

        QbSdk.initX5Environment(this, new QbSdk.PreInitCallback() {
            @Override
            public void onCoreInitFinished() {

            }

            @Override
            public void onViewInitFinished(boolean b) {
                Log.e("@@","加载内核是否成功:"+b);
            }
        });

        new Thread(() -> {
            try {
                HttpRequest httpRequest=HttpRequest.get(DOMAIN+"/games/_client/")
                        .followRedirects(true).useCaches(false);
                String s=httpRequest.body();
                httpRequest.disconnect();
                JSONObject jsonObject=new JSONObject(s);
                final JSONObject android = jsonObject.getJSONObject("android-maker");
                String version = android.getString("version");
                if (!version.equals(BuildConfig.VERSION_NAME)) {
                    runOnUiThread(() -> {
                        try {
                            new AlertDialog.Builder(MainActivity.this).setTitle("存在版本更新！")
                                .setMessage(android.getString("text")).setCancelable(true)
                                .setPositiveButton("下载", (dialogInterface, i) -> {
                                    try {
                                        Intent intent=new Intent(MainActivity.this, TBSActivity.class);
                                        intent.putExtra("title", "版本更新");
                                        intent.putExtra("url", android.getString("url"));
                                        workingDirectory = null;
                                        startActivity(intent);
                                    }
                                    catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }).setNegativeButton("取消", null).create().show();
                        }
                        catch (Exception e) {e.printStackTrace();}
                    });
                }
                templateVersion = android.getString("template");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }

    private void initSDCard() {

        // check permission
        if (!HiPermission.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            return;

        makerDir = new File(Environment.getExternalStorageDirectory()+"/H5motaMaker/");
        templateDir = new File(makerDir, ".templates");
        if (!templateDir.exists() && !templateDir.mkdirs())
            CustomToast.showErrorToast(this, "无法创建目录");

        try {
            if (simpleWebServer!=null) {
                simpleWebServer.stop();
            }
            simpleWebServer = new MyWebServer(this, "127.0.0.1", 1056, makerDir, true);
            simpleWebServer.start();
        }
        catch (Exception e) {
            new AlertDialog.Builder(MainActivity.this).setTitle("错误")
                    .setMessage("本地服务器启动失败！")
                    .setCancelable(true).setPositiveButton("确定",null).create().show();
            e.printStackTrace();
            simpleWebServer=null;
        }

        updateItems();

    }

    private void updateItems() {
        items.clear();
        for (File file: makerDir.listFiles()) {
            if (new File(file, "index.html").exists() && new File(file, "main.js").exists()
                    && new File(file, "editor-mobile.html").exists() && new File(file, "libs").exists()) {
                items.add(file.getName());
            }
        }
        ((ArrayAdapter)listView.getAdapter()).notifyDataSetChanged();
        if (items.isEmpty()) {
            CustomToast.showInfoToast(this, "当前没有正在编辑的塔，请新建项目。", 2500);
        }
    }

    protected void onDestroy() {
        if (simpleWebServer!=null) {
            simpleWebServer.stop();
        }
        super.onDestroy();
    }

    double exittime=0;
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode==KeyEvent.KEYCODE_BACK && event.getAction()==KeyEvent.ACTION_DOWN) {
            if (System.currentTimeMillis()-exittime>2000) {
                Toast.makeText(this, "再按一遍退出程序", Toast.LENGTH_SHORT).show();
                exittime=System.currentTimeMillis();
            }
            else
            {
                exittime=0;
                finish();
            }
            return true;
        }
        return false;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        menu.add(Menu.NONE, 0, 0, "").setIcon(android.R.drawable.ic_menu_add)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, 1, 1, "").setIcon(android.R.drawable.ic_menu_delete)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, 2, 2, "").setIcon(android.R.drawable.ic_menu_set_as)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case 0: createNewProject(); break;
            case 1: {
                File clearFile = new File(makerDir, "clearStorage.html");
                if (!clearFile.exists()) {
                    Utils.copyFilesFassets(this, "clearStorage.html", makerDir+"/clearStorage.html");
                }
                loadUrl(LOCAL+"clearStorage.html", "垃圾存档清理工具");
                break;
            }
            case 2: inputLink(); break;
        }
        return true;
    }

    private void createNewProject() {
        if (!HiPermission.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                || templateDir == null || !templateDir.exists()) {
            showError("你没有SD卡的权限！");
            return;
        }

        String currVersion = null;
        File[] files = templateDir.listFiles((file, s) -> s.endsWith(".zip"));
        Arrays.sort(files, (f1, f2)->Long.compare(f2.lastModified(), f1.lastModified()));
        if (files.length>0) currVersion = files[0].getName();

        if (templateVersion == null) templateVersion = currVersion;
        if (templateVersion == null) {
            showError("模板不存在！");
            return;
        }

        final File zipFile = new File(templateDir, templateVersion);
        if (!zipFile.exists()) {
            int index = templateVersion.lastIndexOf('.');
            String versionName = index==-1?templateVersion:templateVersion.substring(0, index);
            new AlertDialog.Builder(this).setTitle("错误")
                .setMessage("你当前的模板不是最新版本("+versionName+")，点击确定下载最新的模板才能新建项目。")
                .setCancelable(true).setPositiveButton("确定", (dialogInterface, i) -> {

                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(DOMAIN+"/games/_client/"+templateVersion));

                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationUri(Uri.fromFile(zipFile));
                    request.setTitle("正在下载" + templateVersion + "...");
                    DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (downloadManager!=null) {
                        downloadManager.enqueue(request);
                        CustomToast.showInfoToast(MainActivity.this, "文件下载中，请在通知栏查看进度");
                    }
                    else CustomToast.showErrorToast(MainActivity.this, "无法下载文件");

                }).setNegativeButton("取消", null).create().show();
            return;
        }

        final EditText editText = new EditText(this);
        editText.setHint("请输入塔名...");
        new AlertDialog.Builder(this).setTitle("创建新塔")
            .setView(editText).setPositiveButton("确定", (dialogInterface, i) -> {
                String content = editText.getEditableText().toString();
                if (content.isEmpty()) return;
                File destination = new File(makerDir, content);
                if (destination.exists()) {
                    showError("该塔目录已存在！");
                    return;
                }
                if (destination.mkdirs() && Utils.unzip(zipFile, destination)) {
                    updateItems();
                    CustomToast.showSuccessToast(MainActivity.this, "新塔创建成功！");
                }
                else {
                    showError("无法创建新塔");
                }
            }).setNegativeButton("取消", null).setCancelable(true).create().show();

    }

    private void showError(String content) {
        new AlertDialog.Builder(this).setTitle("错误")
                .setMessage(content)
                .setCancelable(true).setPositiveButton("确定",null).create().show();
    }

    private void inputLink() {
        final EditText editText = new EditText(this);
        editText.setHint("请输入地址...");
        new AlertDialog.Builder(this).setTitle("浏览网页")
            .setView(editText).setPositiveButton("确定", (dialogInterface, i) -> {
                String content = editText.getEditableText().toString();
                if (!content.startsWith("http://") && !content.startsWith("https://"))
                    content = "http://"+content;
                Intent intent=new Intent(MainActivity.this, TBSActivity.class);
                intent.putExtra("title", "浏览网页");
                intent.putExtra("url", content);
                workingDirectory = null;
                startActivity(intent);
            }).setNegativeButton("取消", null).setCancelable(true).create().show();
    }

    public void loadUrl(String url, String title) {
        try {
            Intent intent=new Intent(MainActivity.this, TBSActivity.class);
            intent.putExtra("title", title);
            intent.putExtra("url", url);
            startActivity(intent);
        }
        catch (Exception e) {
            e.printStackTrace();
            CustomToast.showErrorToast(this, "无法打开网页！");
        }
    }

}
