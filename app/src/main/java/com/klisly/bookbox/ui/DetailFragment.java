package com.klisly.bookbox.ui;

import android.app.NotificationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import com.andexert.library.RippleView;
import com.klisly.bookbox.Constants;
import com.klisly.bookbox.R;
import com.klisly.bookbox.api.ArticleApi;
import com.klisly.bookbox.api.BookRetrofit;
import com.klisly.bookbox.domain.ApiResult;
import com.klisly.bookbox.domain.ArticleData;
import com.klisly.bookbox.logic.AccountLogic;
import com.klisly.bookbox.model.Article;
import com.klisly.bookbox.model.User2Article;
import com.klisly.bookbox.subscriber.AbsSubscriber;
import com.klisly.bookbox.subscriber.ApiException;
import com.klisly.bookbox.ui.base.BaseBackFragment;
import com.klisly.bookbox.utils.ShareUtil;
import com.klisly.bookbox.utils.ToastHelper;
import com.qq.e.ads.banner.ADSize;
import com.qq.e.ads.banner.AbstractBannerADListener;
import com.qq.e.ads.banner.BannerView;
import com.qq.e.comm.util.AdError;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static android.content.Context.NOTIFICATION_SERVICE;

public class DetailFragment extends BaseBackFragment implements Toolbar.OnMenuItemClickListener {
    private static final String ARG_CONTENT = "arg_article";
    NotificationManager manager;
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.webView)
    WebView tvContent;
    @Bind(R.id.bannerContainer)
    ViewGroup bannerContainer;

    BannerView bv;
    @Bind(R.id.ivcollect)
    ImageView ivcollect;
    @Bind(R.id.txtcollection)
    TextView txtcollection;
    @Bind(R.id.action_collect)
    RippleView actionCollect;
    @Bind(R.id.ivshare)
    ImageView ivshare;
    @Bind(R.id.txtshare)
    TextView txtshare;
    @Bind(R.id.action_share)
    RippleView actionShare;
    @Bind(R.id.behavior_demo_coordinatorLayout)
    CoordinatorLayout behaviorDemoCoordinatorLayout;
    private Article mData;
    private ArticleData mArticleData;
    private ArticleApi articleApi = BookRetrofit.getInstance().getArticleApi();
    private Menu menu;

    public static DetailFragment newInstance(Article article) {
        DetailFragment fragment = new DetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CONTENT, article);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mData = (Article) args.getSerializable(ARG_CONTENT);
        }
        manager = (NotificationManager) getContext().getSystemService(NOTIFICATION_SERVICE);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detail, container, false);
        ButterKnife.bind(this, view);
        initView(view);
        manager.cancel(mData.getId().hashCode());

        return view;
    }


    private void initBanner() {
        this.bv = new BannerView(getActivity(), ADSize.BANNER, Constants.QQ_APP_ID, Constants.BannerPosId);
        bv.setRefresh(30);
        bv.setADListener(new AbstractBannerADListener() {
            @Override
            public void onNoAD(AdError adError) {

            }

            @Override
            public void onADReceiv() {
                Log.i("AD_DEMO", "ONBannerReceive");
            }
        });
        bannerContainer.addView(bv);
    }


    private void updateData() {
        String info = mArticleData.getArticle().getSite();
//        if (StringUtils.isNotEmpty(mArticleData.getArticle().getAuthor())) {
//            info = info + "  " + mArticleData.getArticle().getAuthor();
//        }
//        tvSource.setText(info);
//
//        tvDate.setText(DateUtil.getFriendlyTimeSpanByNow(new Date(mArticleData.getArticle().getCreateAt())));
        String html = Constants.ARTICLE_PREFIX + mArticleData.getArticle().getContent() + Constants.ARTICLE_SUFFIX;
        tvContent.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    private void initView(View view) {
//        toolbar.setTitleTextAppearance(getContext(), R.style.TitleTextApperance);
        initToolbarNav(toolbar);
        toolbar.setOnMenuItemClickListener(this);
        tvContent.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return true;
            }
        });
        initBanner();
        bannerContainer.postDelayed(new Runnable() {
            @Override
            public void run() {
                bv.loadAD();
            }
        }, 1200);
        initListener();
    }


    private void initListener() {
        actionCollect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleCollect();
            }
        });
        actionShare.setOnClickListener(v -> {
            shareArticle();
        });
    }

    @Override
    protected void initToolbarMenu(Toolbar toolbar) {
        toolbar.inflateMenu(R.menu.menu_article_pop);
        menu = toolbar.getMenu();
    }

    private void updateMenu() {
        if (mArticleData != null && mArticleData.getUser2article() != null) {
            if (mArticleData.getUser2article().getToread()) {
                menu.getItem(0).setTitle(getString(R.string.notoread));
            } else {
                menu.getItem(0).setTitle(getString(R.string.toread));
            }
            if (mArticleData.getUser2article().getCollect()) {
                ivcollect.setImageResource(R.drawable.collected);
                menu.getItem(1).setTitle(getString(R.string.nocollect));
            } else {
                menu.getItem(1).setTitle(getString(R.string.collect));
                ivcollect.setImageResource(R.drawable.uncollected);
            }
        }
    }

    /**
     * 这里演示:
     * 比较复杂的Fragment页面会在第一次start时,导致动画卡顿
     * Fragmentation提供了onEnterAnimationEnd()方法,该方法会在 入栈动画 结束时回调
     * 所以在onCreateView进行一些简单的View初始化(比如 toolbar设置标题,返回按钮; 显示加载数据的进度条等),
     * 然后在onEnterAnimationEnd()方法里进行 复杂的耗时的初始化 (比如FragmentPagerAdapter的初始化 加载数据等)
     */
    @Override
    public void onEnterAnimationEnd(Bundle savedInstanceState) {
        articleApi.fetch(mData.getId(), AccountLogic.getInstance().getUserId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new AbsSubscriber<ApiResult<ArticleData>>(getActivity(), false) {
                    @Override
                    protected void onError(ApiException ex) {
                        ToastHelper.showShortTip(R.string.get_detial_fail);
                    }

                    @Override
                    protected void onPermissionError(ApiException ex) {
                        ToastHelper.showShortTip(R.string.get_detial_fail);
                    }

                    @Override
                    public void onNext(ApiResult<ArticleData> res) {
                        Timber.i("reache article:" + res);
                        if (res.getData() != null) {
                            mArticleData = res.getData();
                            updateData();
                        } else {
                            ToastHelper.showShortTip(R.string.get_detial_fail);
                        }
                    }
                });
        initLazyView();
    }

    private void initLazyView() {
        toolbar.setTitle(mData.getTitle());
//        tvTitle.setText();
    }

    @Override
    public void onFragmentResult(int requestCode, int resultCode, Bundle data) {
        super.onFragmentResult(requestCode, resultCode, data);
    }

    /**
     * 类似于 Activity的 onNewIntent()
     */
    @Override
    public void onNewBundle(Bundle args) {
        super.onNewBundle(args);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_to_read:
                toggleToRead();
                break;

            case R.id.action_collect:
                toggleCollect();
                break;

            case R.id.action_share:
                shareArticle();
                break;

            case R.id.action_original:
                if (mArticleData != null) {
                    start(OuterFragment.newInstance(mArticleData.getArticle()));
                }
                break;

            case R.id.action_notify_setting:
                ToastHelper.showShortTip(R.string.report);
                break;

            default:
                break;
        }
        return true;
    }


    private void shareArticle() {
        if (mArticleData == null) {
            return;
        }
        String shareUrl = "http://second.imdao.cn/articles/" + mArticleData.getArticle().getId();
        String img = mArticleData.getArticle().getImg();
        String title = mArticleData.getArticle().getTitle();
        String desc = "美文发现," + "\"" + mArticleData.getArticle().getTitle() + "\"" + "." + shareUrl;
        String from = mArticleData.getArticle().getAuthor();
        String comment = "我发现了这篇很走心的文章,分享给各位!";
        ShareUtil.shareArticle(shareUrl, img, title, desc, from, comment);
    }

    private void toggleCollect() {
        if(!AccountLogic.getInstance().isLogin()){
            ToastHelper.showShortTip("登录后才能收藏文章哦");
            return;
        }
        if (mArticleData.getUser2article() == null || !mArticleData.getUser2article().getCollect()) {
            articleApi.collect(mArticleData.getArticle().getId(), AccountLogic.getInstance().getToken())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new AbsSubscriber<ApiResult<User2Article>>(getActivity(), false) {
                        @Override
                        protected void onError(ApiException ex) {

                        }

                        @Override
                        protected void onPermissionError(ApiException ex) {

                        }

                        @Override
                        public void onNext(ApiResult<User2Article> res) {
                            Timber.i("reache article:" + res);
                            ToastHelper.showLongTip(R.string.collected_success);
                            mArticleData.setUser2article(res.getData());
                            updateMenu();
                        }
                    });
        } else {
            articleApi.uncollect(mArticleData.getArticle().getId(), AccountLogic.getInstance().getToken())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new AbsSubscriber<ApiResult<User2Article>>(getActivity(), false) {
                        @Override
                        protected void onError(ApiException ex) {

                        }

                        @Override
                        protected void onPermissionError(ApiException ex) {

                        }

                        @Override
                        public void onNext(ApiResult<User2Article> res) {
                            Timber.i("reache article:" + res);
                            ToastHelper.showLongTip(R.string.uncollected_success);
                            mArticleData.setUser2article(res.getData());
                            updateMenu();
                        }
                    });
        }
    }

    private void toggleToRead() {
        if (mArticleData.getUser2article() == null || !mArticleData.getUser2article().getToread()) {
            articleApi.toread(mArticleData.getArticle().getId(), AccountLogic.getInstance().getToken())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new AbsSubscriber<ApiResult<User2Article>>(getActivity(), false) {
                        @Override
                        protected void onError(ApiException ex) {

                        }

                        @Override
                        protected void onPermissionError(ApiException ex) {

                        }

                        @Override
                        public void onNext(ApiResult<User2Article> res) {
                            Timber.i("reache article:" + res);
                            mArticleData.setUser2article(res.getData());
                            updateMenu();
                        }
                    });
        } else {
            articleApi.untoread(mArticleData.getArticle().getId(), AccountLogic.getInstance().getToken())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new AbsSubscriber<ApiResult<User2Article>>(getActivity(), false) {
                        @Override
                        protected void onError(ApiException ex) {

                        }

                        @Override
                        protected void onPermissionError(ApiException ex) {

                        }

                        @Override
                        public void onNext(ApiResult<User2Article> res) {
                            Timber.i("reache article:" + res);
                            mArticleData.setUser2article(res.getData());
                            updateMenu();
                        }
                    });
        }
    }
}
