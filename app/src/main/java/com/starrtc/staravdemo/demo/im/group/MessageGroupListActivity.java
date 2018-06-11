package com.starrtc.staravdemo.demo.im.group;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import com.starrtc.staravdemo.R;
import com.starrtc.staravdemo.demo.BaseActivity;
import com.starrtc.staravdemo.demo.MLOC;
import com.starrtc.staravdemo.demo.database.CoreDB;
import com.starrtc.staravdemo.demo.database.HistoryBean;
import com.starrtc.staravdemo.demo.serverAPI.InterfaceUrls;
import com.starrtc.staravdemo.demo.ui.CircularCoverView;
import com.starrtc.staravdemo.utils.AEvent;
import com.starrtc.staravdemo.utils.ColorUtils;
import com.starrtc.staravdemo.utils.DensityUtils;
import com.starrtc.staravdemo.utils.StarListUtil;

public class MessageGroupListActivity extends BaseActivity implements AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {

    private ListView vList;
    private MyListAdapter myListAdapter;
    private ArrayList<HistoryBean> mDatas;
    private LayoutInflater mInflater;
    private SwipeRefreshLayout refreshLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_group_list);

        ((TextView)findViewById(R.id.title_text)).setText("群组列表");
        findViewById(R.id.title_left_btn).setVisibility(View.VISIBLE);
        findViewById(R.id.title_left_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        AEvent.addListener(AEvent.AEVENT_GROUP_GOT_LIST,this);
        findViewById(R.id.create_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MessageGroupListActivity.this,MessageGroupCreateActivity.class));
            }
        });
        refreshLayout = (SwipeRefreshLayout)findViewById(R.id.refresh_layout);
        //设置刷新时动画的颜色，可以设置4个
        refreshLayout.setColorSchemeResources(android.R.color.holo_blue_light, android.R.color.holo_red_light, android.R.color.holo_orange_light, android.R.color.holo_green_light);
        refreshLayout.setOnRefreshListener(this);



        mDatas = new ArrayList<>();
        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        myListAdapter = new MyListAdapter();
        vList = (ListView) findViewById(R.id.list);
        vList.setAdapter(myListAdapter);
        vList.setOnItemClickListener(this);
        vList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                switch (i) {
                    case SCROLL_STATE_IDLE:
                        if(StarListUtil.isListViewReachTopEdge(absListView)){
                            refreshLayout.setEnabled(true);
                        }else{
                            refreshLayout.setEnabled(false);
                        }
                        break;
                }
            }
            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}
        });

    }

    @Override
    public void onResume(){
        super.onResume();
        MLOC.hasNewGroupMsg = false;
        InterfaceUrls.demoRequestGroupList(MLOC.userId);
    }
    @Override
    public void onStart(){
        super.onStart();

    }

    @Override
    public void onRestart(){
        super.onRestart();
        AEvent.addListener(AEvent.AEVENT_GROUP_GOT_LIST,this);
    }

    @Override
    public void onStop(){
        AEvent.removeListener(AEvent.AEVENT_GROUP_GOT_LIST,this);
        super.onStop();
    }

    @Override
    public void dispatchEvent(String aEventID, boolean success, Object eventObj) {
        super.dispatchEvent(aEventID,success,eventObj);
        switch (aEventID){
            case AEvent.AEVENT_GROUP_GOT_LIST:
                refreshLayout.setRefreshing(false);
                mDatas.clear();
                if(success){
                    ArrayList<MessageGroupInfo> res = (ArrayList<MessageGroupInfo>) eventObj;
                    List<HistoryBean> historyList = MLOC.getHistoryList(CoreDB.HISTORY_TYPE_GROUP);
                    //删除已经不再的群
                    for(int i=historyList.size()-1;i>0;i--){
                        HistoryBean historyBean = historyList.get(i);
                        Boolean needRemove = true;
                        for(int j = 0;j<res.size();j++){
                            if(historyBean.getConversationId().equals(res.get(j).groupId)){
                                needRemove = false;
                                break;
                            }
                        }
                        if(needRemove){
                            historyList.remove(i);
                        }
                    }
                    //添加新加的群
                    for(int i=0;i<res.size();i++){
                        MessageGroupInfo groupInfo = res.get(i);
                        boolean needAdd = true;
                        for(int j = 0;j<historyList.size();j++){
                            if(groupInfo.groupId.equals(historyList.get(j).getConversationId())){
                                needAdd = false;
                                break;
                            }
                        }
                        if(needAdd){
                            HistoryBean historyBean = new HistoryBean();
                            historyBean.setType(CoreDB.HISTORY_TYPE_GROUP);
                            historyBean.setNewMsgCount(0);
                            historyBean.setConversationId(res.get(i).groupId);
                            historyBean.setGroupName(res.get(i).groupName);
                            historyBean.setGroupCreaterId(res.get(i).createrId);
                            historyBean.setLastMsg("");
                            historyBean.setLastTime("");
                            MLOC.setHistory(historyBean,true);
                            historyList.add(historyBean);
                        }
                    }
                    mDatas.addAll(historyList);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        myListAdapter.notifyDataSetChanged();
                    }
                });

                break;
            default:
                onResume();
                break;
        }
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        HistoryBean clickInfo = mDatas.get(position);

        MLOC.setHistory(clickInfo,true);

        Intent intent = new Intent(MessageGroupListActivity.this, MessageGroupActivity.class);
        intent.putExtra(MessageGroupActivity.TYPE,MessageGroupActivity.GROUP_ID);
        intent.putExtra(MessageGroupActivity.GROUP_ID,clickInfo.getConversationId());
        intent.putExtra(MessageGroupActivity.GROUP_NAME,clickInfo.getGroupName());
        intent.putExtra(MessageGroupActivity.CREATER_ID,clickInfo.getGroupCreaterId());
        startActivity(intent);
    }

    @Override
    public void onRefresh() {
        InterfaceUrls.demoRequestGroupList(MLOC.userId);
    }


    class MyListAdapter extends BaseAdapter{
        @Override
        public int getCount() {
            return mDatas.size();
        }

        @Override
        public Object getItem(int position) {
            return mDatas.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final MyListAdapter.ViewHolder viewIconImg;
            if(convertView == null){
                viewIconImg = new MyListAdapter.ViewHolder();
                convertView = mInflater.inflate(R.layout.item_group_list,null);
                viewIconImg.vRoomName = (TextView)convertView.findViewById(R.id.item_id);
                viewIconImg.vCreaterId = (TextView)convertView.findViewById(R.id.item_creater_id);
                viewIconImg.vTime = (TextView) convertView.findViewById(R.id.item_time);
                viewIconImg.vCount = (TextView) convertView.findViewById(R.id.item_count);
                viewIconImg.vHeadBg =  convertView.findViewById(R.id.head_bg);
                viewIconImg.vHeadImage = (ImageView) convertView.findViewById(R.id.head_img);
                viewIconImg.vHeadCover = (CircularCoverView) convertView.findViewById(R.id.head_cover);
                convertView.setTag(viewIconImg);
            }else{
                viewIconImg = (MyListAdapter.ViewHolder)convertView.getTag();
            }
            viewIconImg.vRoomName.setText(mDatas.get(position).getGroupName());
            viewIconImg.vCreaterId.setText(mDatas.get(position).getGroupCreaterId());
            viewIconImg.vTime.setText(mDatas.get(position).getLastTime());
            viewIconImg.vCount.setText(""+mDatas.get(position).getNewMsgCount());
            viewIconImg.vHeadBg.setBackgroundColor(ColorUtils.getColor(MessageGroupListActivity.this,mDatas.get(position).getConversationId()));
            viewIconImg.vHeadCover.setCoverColor(Color.parseColor("#FFFFFF"));
            int cint = DensityUtils.dip2px(MessageGroupListActivity.this,28);
            viewIconImg.vHeadCover.setRadians(cint, cint, cint, cint,0);
            viewIconImg.vHeadImage.setImageResource(R.drawable.icon_im_group_item);

            if(mDatas.get(position).getNewMsgCount()==0){
                viewIconImg.vCount.setVisibility(View.INVISIBLE);
            }else{
                viewIconImg.vCount.setText(""+mDatas.get(position).getNewMsgCount());
                viewIconImg.vCount.setVisibility(View.VISIBLE);
            }
            return convertView;
        }

        class  ViewHolder{
            private TextView vRoomName;
            public TextView vTime;
            public TextView vCount;
            private TextView vCreaterId;
            public View vHeadBg;
            public CircularCoverView vHeadCover;
            public ImageView vHeadImage;
        }
    }


}
