package com.example.administrator.ggcode.Fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemLongClickListener;
import com.example.administrator.ggcode.Adapter.JokeAdapter;
import com.example.administrator.ggcode.Bean.JokeBean;
import com.example.administrator.ggcode.Commons.Constants;
import com.example.administrator.ggcode.Commons.ShareUtils;
import com.example.administrator.ggcode.R;
import com.example.administrator.ggcode.net.QClitent;
import com.example.administrator.ggcode.net.QNewsService;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


public class JokeFragment extends Fragment {

    @BindView(R.id.tv_joke_load_again)
    TextView check_tv;
    @BindView (R.id.tb_joke)
    Toolbar tbJoke;
    @BindView (R.id.rv_joke)
    RecyclerView rvJoke;
    @BindView (R.id.srl_joke)
    SwipeRefreshLayout srlJoke;
    @BindView (R.id.ll_loading)
    LinearLayout llLoading;
    @BindView (R.id.ll_error)
    LinearLayout       llError;

    private JokeAdapter mAdapter;

    List<JokeBean.ResultBean.DataBean> mData;

    private int mCurrentCounter;
    private int mTotalCounter = 5;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.fragment_joke, container, false);
        ButterKnife.bind(this,view);

        //初始化数据
        mData=new ArrayList<>();
        mAdapter=new JokeAdapter();
        mAdapter.openLoadAnimation(BaseQuickAdapter.SLIDEIN_RIGHT);//设置RecyclerView item动画（右侧进入）

        //设置下拉刷新
        srlJoke.setColorSchemeColors(Color.GREEN,Color.RED,Color.BLUE);
        srlJoke.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateDate();
            }
        });
        LinearLayoutManager ll=new LinearLayoutManager(getActivity());
        ll.setOrientation(LinearLayoutManager.VERTICAL);
        rvJoke.setLayoutManager(ll);
        rvJoke.setAdapter(mAdapter);
        rvJoke.addOnItemTouchListener(new OnItemLongClickListener() {
            @Override
            public void onSimpleItemLongClick(BaseQuickAdapter adapter, View view, int position) {
                String content=mAdapter.getData().get(position).getContent();
                ShareUtils.share(getActivity(),content);
            }
        });

        mAdapter.setOnLoadMoreListener(new BaseQuickAdapter.RequestLoadMoreListener() {
            @Override
            public void onLoadMoreRequested() {
                rvJoke.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mCurrentCounter>=mTotalCounter){
                            //数据加载完成
                            mAdapter.loadMoreEnd();
                        }else {
                            if (mAdapter.getItem(0)==null){
                                return;
                            }
                            long unixtime=mAdapter.getItem(mAdapter.getItemCount()-2).getUnixtime();
                            QClitent.getInstance().create(QNewsService.class,Constants.BASE_JOKE_URL).//创建服务
                                    getAssignJokeData(unixtime,1,5,QNewsService.DESC).
                                    subscribeOn(Schedulers.io()).
                                    observeOn(AndroidSchedulers.mainThread()).
                                    subscribe(new Consumer<JokeBean>() {
                                        @Override
                                        public void accept(JokeBean jokeBean) throws Exception {
                                            List<JokeBean.ResultBean.DataBean> data
                                                    = jokeBean.getResult().getData();
                                            mAdapter.addData(data);
                                            mCurrentCounter = mTotalCounter;
                                            mTotalCounter += 5;
                                            mAdapter.loadMoreComplete();
                                        }
                                    }, new Consumer<Throwable>() {
                                        @Override
                                        public void accept(Throwable throwable) throws Exception {
                                            mAdapter.loadMoreFail();
                                        }
                                    });

                        }
                    }
                },1000);
            }
        });
        updateDate();
        return view;
    }

    private void updateDate() {
        srlJoke.setVisibility(View.VISIBLE);
        llLoading.setVisibility(View.VISIBLE);
        llError.setVisibility(View.GONE);

        srlJoke.setRefreshing(true);// 让SwipeRefreshLayout开启刷新
        QClitent.getInstance().create(QNewsService.class, Constants.BASE_JOKE_URL).// 创建服务
                getCurrentJokeData(1,8).//设置查询页数和每页条数
                subscribeOn(Schedulers.io()).//子线程请求
                observeOn(AndroidSchedulers.mainThread()).//主线程刷新
                subscribe(new Consumer<JokeBean>() {
            @Override
            public void accept(JokeBean jokeBean) throws Exception {
                // 成功获取数据
                llLoading.setVisibility(View.GONE);
                llError.setVisibility(View.GONE);
                mAdapter.setNewData(jokeBean.getResult().getData());    // 给适配器设置数据
                srlJoke.setRefreshing(false);       // 让SwipeRefreshLayout关闭刷新

            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                // 获取数据失败
                Toast.makeText(getActivity(), "获取数据失败!" + "访问次数上限", Toast.LENGTH_SHORT)
                        .show();
                srlJoke.setRefreshing(false);
                llError.setVisibility(View.VISIBLE);
                llLoading.setVisibility(View.GONE);
                srlJoke.setVisibility(View.GONE);
            }
        });
    }

    @OnClick(R.id.tv_joke_load_again)
    public void OnClick(View view){
        updateDate();
    }
}
