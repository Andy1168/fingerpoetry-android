package com.klisly.bookbox.ui.fragment.activity;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.klisly.bookbox.R;
import com.klisly.bookbox.adapter.PagerFragmentAdapter;
import com.klisly.bookbox.model.Topic;
import com.klisly.bookbox.ui.base.BaseMainFragment;
import com.klisly.bookbox.utils.ToastHelper;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ActivityFragment extends BaseMainFragment implements Toolbar.OnMenuItemClickListener {
    @Bind(R.id.tab_layout)
    TabLayout mTabLayout;
    @Bind(R.id.viewPager)
    ViewPager mViewPager;
    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    private List<Topic> topics;

    public static ActivityFragment newInstance() {
        return new ActivityFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_content, container, false);
        ButterKnife.bind(this, view);
        initView();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    private void initView() {
        mToolbar.setTitle(R.string.novel);
        initToolbarNav(mToolbar, false);
        mToolbar.inflateMenu(R.menu.menu_site_pop);
        mToolbar.setOnMenuItemClickListener(this);
        if(topics != null){
            for(Topic topic : topics){
                mTabLayout.addTab(mTabLayout.newTab().setText(topic.getName()));
            }
        }
        mViewPager.setAdapter(new PagerFragmentAdapter(getChildFragmentManager(), topics));
        mTabLayout.setupWithViewPager(mViewPager);
    }

    /**
     * 类似于 Activity的 onNewIntent()
     */
    @Override
    protected void onNewBundle(Bundle args) {
        super.onNewBundle(args);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_find_topic:
                ToastHelper.showShortTip(R.string.find_topic);
                break;
            case R.id.action_manage_topic:
                ToastHelper.showShortTip(R.string.manage_topic);
                break;
            case R.id.action_sort_method:
                ToastHelper.showShortTip(R.string.sort_method);
                break;
            case R.id.action_notify_setting:
                ToastHelper.showShortTip(R.string.notify_setting);
                break;
            default:
                break;
        }
        return true;
    }
}
