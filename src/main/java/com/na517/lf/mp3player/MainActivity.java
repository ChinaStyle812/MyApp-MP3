package com.na517.lf.mp3player;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.na517.lf.model.BDSong;
import com.na517.lf.model.BDSongDetail;
import com.na517.lf.net.ResponseCallback;
import com.na517.lf.net.StringRequest;
import com.na517.lf.utils.AudioUtils;
import com.na517.lf.utils.adapter.SongListAdapter;
import com.na517.lf.view.SearchView;

import java.util.ArrayList;

public class MainActivity extends Activity implements SearchView.OnStartSearchListener, AdapterView.OnItemClickListener {

    private Context mContext;

    private SongListAdapter mAdapter;

    private ArrayList<BDSong> mSongList;

    private ListView mLvResult;

    private SearchView mSvSearch;

    private ProgressBar mPbLoad;

    private long mFirstClick = 0;

    private TextView mTvLocalTotal;

    private TextView mTvSong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        initView();
        initData();
    }

    private void initView() {
        mLvResult = (ListView) findViewById(R.id.lv_main_result);
        mSvSearch = (SearchView) findViewById(R.id.sv_main_search);
        mPbLoad = (ProgressBar) findViewById(R.id.pb_main_search_progress);
        mTvLocalTotal = (TextView) findViewById(R.id.tv_main_local_total);
        mTvSong = (TextView) findViewById(R.id.tv_play_pad_song);

        mTvSong.setFocusable(true);
        mTvSong.setFocusableInTouchMode(true);

        mSvSearch.setOnStartSearchListener(this);
        mLvResult.setOnItemClickListener(this);
    }

    private void initData() {
        mTvLocalTotal.setText(AudioUtils.getAudioList(mContext).size() + "首");
    }

    private void searchSongs(String content) {
        String searchStr = content.replace(" ", "%20");

//        String urlFormat = "http://box.zhangmen.baidu.com/x?op=12&count=1&title=%s$$%s";
//        String url = String.format(urlFormat, singer, song);
        //新接口
//        String newURL = "http://mp3.baidu.com/dev/api/?tn=getinfo&ct=0&word=%s&ie=utf-8&format=xml"; //json或XML
        String newURL = "http://mp3.baidu.com/dev/api/?tn=getinfo&ct=0&word=%s&ie=utf-8&format=json"; //json或XML
        String url = String.format(newURL, searchStr);

        StringRequest.start(url, new ResponseCallback() {
            @Override
            public void onSuccess(String result) {
                mPbLoad.setVisibility(View.GONE);
                Log.i("LF", "call onSuccess.");
                Log.i("LF", "result=" + result);
                try {
                    mSongList = (ArrayList<BDSong>) JSON.parseArray(result, BDSong.class);
//                    BDSongXMLParser parser = new BDSongXMLParser();
//                    ArrayList<BDSong> songList = (ArrayList<BDSong>) parser.parse(result);
                    if (mSongList == null || mSongList.size() == 0) {
                        Toast.makeText(mContext, "搜索歌曲失败，请稍后重试", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    for (int i=0; i<mSongList.size(); i++) {
                        Log.i("LF", "songId:" + mSongList.get(i).song_id);
                        Log.i("LF", "singer:" + mSongList.get(i).singer);
                        Log.i("LF", "album:" + mSongList.get(i).album);
                        Log.i("LF", "singerPicSmall:" + mSongList.get(i).singerPicSmall);
                        Log.i("LF", "singerPicLarge:" + mSongList.get(i).singerPicLarge);
                        Log.i("LF", "albumPicSmall:" + mSongList.get(i).albumPicSmall);
                        Log.i("LF", "albumPicLarge:" + mSongList.get(i).albumPicLarge);
                    }
                    mAdapter = new SongListAdapter(mContext, mSongList);
                    mLvResult.setAdapter(mAdapter);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("LF", "Exception:" + e.getMessage());
                }
            }

            @Override
            public void onError(String result) {
                mPbLoad.setVisibility(View.GONE);
            }

            @Override
            public void onStartLoading() {
                mPbLoad.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BDSong song = mSongList.get(position);

        String urlFormat = "http://ting.baidu.com/data/music/links?songIds=%s";
        String url = String.format(urlFormat, song.song_id);

        StringRequest.start(url, new ResponseCallback() {
            @Override
            public void onSuccess(String result) {
                mPbLoad.setVisibility(View.GONE);
                Log.i("LF", "call onSuccess.");
                Log.i("LF", "result=" + result);

                try {
                    JSONObject jsonObject = JSON.parseObject(result);
                    JSONObject jsonData = jsonObject.getJSONObject("data");
                    JSONArray jsonArray = jsonData.getJSONArray("songList");
                    if (jsonArray.size() > 0) {
                        BDSongDetail songDetail = JSON.parseObject(jsonArray.get(0).toString(), BDSongDetail.class);
                        songDetail.xcode = jsonData.getString("xcode");
                        Intent i = new Intent(mContext, SongDetailActivity.class);
                        i.putExtra("SongDetail", songDetail);
                        startActivity(i);
                    }
                    else {
                        Toast.makeText(mContext, "没有获取到该歌曲信息", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e("LF", "Exception:" + e.getMessage());
                    Toast.makeText(mContext, "没有获取到该歌曲信息", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String result) {
                mPbLoad.setVisibility(View.GONE);
            }

            @Override
            public void onStartLoading() {
                mPbLoad.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void OnStartSearch(String content) {
        if (null == content || content.isEmpty()) {
            Toast.makeText(mContext, "请输入搜索内容", Toast.LENGTH_SHORT).show();
        }
        else {
//            Toast.makeText(mContext, "开始搜索：" + content, Toast.LENGTH_SHORT).show();
//            searchSongs(content);
            Intent intent = new Intent(mContext, TestActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            long secongClick = System.currentTimeMillis();
            if (secongClick - mFirstClick > 2000) {
                mFirstClick = secongClick;
                Toast.makeText(mContext, "再按一次退出程序", Toast.LENGTH_SHORT).show();
            }
            else {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }
        }

        return true;
    }
}
