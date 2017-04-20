package com.klisly.bookbox.ui.fragment;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jude.easyrecyclerview.EasyRecyclerView;
import com.jude.easyrecyclerview.adapter.BaseViewHolder;
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter;
import com.klisly.bookbox.CommonHelper;
import com.klisly.bookbox.Constants;
import com.klisly.bookbox.R;
import com.klisly.bookbox.adapter.ArticleViewHolder;
import com.klisly.bookbox.adapter.WxArticleViewHolder;
import com.klisly.bookbox.adapter.WxUser2ArticleViewHolder;
import com.klisly.bookbox.api.ArticleApi;
import com.klisly.bookbox.api.BookRetrofit;
import com.klisly.bookbox.api.WxArticleApi;
import com.klisly.bookbox.domain.ApiResult;
import com.klisly.bookbox.logic.AccountLogic;
import com.klisly.bookbox.model.Article;
import com.klisly.bookbox.model.BaseModel;
import com.klisly.bookbox.model.ChannleEntity;
import com.klisly.bookbox.model.Site;
import com.klisly.bookbox.model.Topic;
import com.klisly.bookbox.model.User2WxArticle;
import com.klisly.bookbox.model.WxArticle;
import com.klisly.bookbox.subscriber.AbsSubscriber;
import com.klisly.bookbox.subscriber.ApiException;
import com.klisly.bookbox.ui.DetailFragment;
import com.klisly.bookbox.ui.OFragment;
import com.klisly.bookbox.ui.base.BaseFragment;
import com.klisly.bookbox.utils.ToastHelper;
import com.klisly.bookbox.utils.TopToastHelper;
import com.material.widget.CircularProgress;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class PagerChildFragment<T extends BaseModel> extends BaseFragment implements RecyclerArrayAdapter.OnLoadMoreListener, SwipeRefreshLayout.OnRefreshListener {
    public static final String ARG_FROM = "arg_from";
    public static final String ARG_CHANNEL = "arg_channel";
    public static final String ARG_NAME = "arg_name";

    @Bind(R.id.recyclerView)
    EasyRecyclerView mRecy;
    @Bind(R.id.tvTip)
    TextView mTvTip;
    @Bind(R.id.cprogress)
    CircularProgress mProgress;
    private int page = 0;
    private int pageSize = 15;
    private int mFrom;
    private T mData;
    //    private PagerContentAdapter mAdapter;
    private ArticleApi articleApi = BookRetrofit.getInstance().getArticleApi();
    private WxArticleApi wxArticleApi = BookRetrofit.getInstance().getWxArticleApi();

    private String name;
    private boolean needToast = false;
    private RecyclerArrayAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mFrom = args.getInt(ARG_FROM);
            mData = (T) args.getSerializable(ARG_CHANNEL);
            name = args.getString(ARG_NAME, this.getClass().getName());
        }
    }

    public T getmData() {
        return mData;
    }

    public void setmData(T mData) {
        this.mData = mData;
        this.page = 0;
        loadNew();
        ;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_pager, container, false);
        ButterKnife.bind(this, view);
        if (CommonHelper.getItemType(mData) == Constants.ITEM_TYPE_JOKE) {
            pageSize = 30;
        }
        initView(view);
        // todo 智能推荐服务打开后,显示该消息
//        if(AccountLogic.getInstance().getNowUser() == null){
//            TopToastHelper.showTip(mTvTip, getString(R.string.recom_log), TopToastHelper.DURATION_LONG);
//        }
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    private void initView(View view) {

        mRecy.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecy.setAdapterWithProgress(adapter = new RecyclerArrayAdapter(getActivity()) {
            private final static int TYPE_ARTICLE = 1;
            private final static int TYPE_WX_ARTICLE = 2;
            private final static int TYPE_WX_COLLECTED = 3;

            @Override
            public int getViewType(int position) {

                if (getAllData().get(position) instanceof Article) {
                    return TYPE_ARTICLE;
                } else if (getAllData().get(position) instanceof WxArticle) {
                    return TYPE_WX_ARTICLE;
                } else if (getAllData().get(position) instanceof User2WxArticle) {
                    return TYPE_WX_COLLECTED;
                }
                return TYPE_ARTICLE;
            }

            @Override
            public BaseViewHolder OnCreateViewHolder(ViewGroup parent, int viewType) {
                if (viewType == TYPE_ARTICLE) {
                    return new ArticleViewHolder(parent);
                } else if (viewType == TYPE_WX_ARTICLE) {
                    return new WxArticleViewHolder(parent);
                } else if (viewType == TYPE_WX_COLLECTED) {
                    return new WxUser2ArticleViewHolder(parent);
                }
                return new ArticleViewHolder(parent);
            }
        });
        adapter.setMore(R.layout.view_more, this);
        adapter.setNoMore(R.layout.view_nomore, new RecyclerArrayAdapter.OnNoMoreListener() {
            @Override
            public void onNoMoreShow() {
                adapter.resumeMore();
            }

            @Override
            public void onNoMoreClick() {
                adapter.resumeMore();
            }
        });
        adapter.setOnItemLongClickListener(new RecyclerArrayAdapter.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(int position) {
                return true;
            }
        });
        adapter.setOnItemClickListener(new RecyclerArrayAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                queryData((BaseModel) adapter.getItem(position));
            }
        });
        adapter.setError(R.layout.view_error, new RecyclerArrayAdapter.OnErrorListener() {
            @Override
            public void onErrorShow() {
                adapter.resumeMore();
            }

            @Override
            public void onErrorClick() {
                adapter.resumeMore();
            }
        });
        mRecy.setRefreshListener(this);
        onRefresh();
    }

    private void queryData(BaseModel article) {
        try {
            if (mData != null) {
                if (article instanceof WxArticle) {
                    ((BaseFragment) getParentFragment()).start(OFragment.newInstance(article));
                } else if (article instanceof Article) {
                    ((BaseFragment) getParentFragment()).start(DetailFragment.newInstance((Article) article));
                }
                if (article instanceof User2WxArticle) {
                    wxArticleApi.fetch(((User2WxArticle) article).getArticleId())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new AbsSubscriber<ApiResult<WxArticle>>(getActivity(), false) {
                                @Override
                                protected void onError(ApiException ex) {
                                    ToastHelper.showShortTip("获取文章失败");
                                }

                                @Override
                                protected void onPermissionError(ApiException ex) {
                                    ToastHelper.showShortTip("获取文章失败");

                                }

                                @Override
                                public void onNext(ApiResult<WxArticle> res) {
                                    ((BaseFragment) getParentFragment()).start(OFragment.newInstance(res.getData()));
                                }
                            });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadNew() {
        int type = getAction();
        Map<String, String> params = new HashMap<>();
        if (type == ACTION_HOT) {
            params.put("type", "hot");
        } else if (type == ACTION_RECOMMEND) {
            params.put("type", "recommend");
        } else {
            if (mData instanceof Site) {
                params.put("siteId", ((Site) mData).getId());
            } else if (mData instanceof Topic) {
                params.put("topics", ((Topic) mData).getName());
            }
        }
        page++;
        params.put("page", String.valueOf(page));
        params.put("pageSize", String.valueOf(pageSize));
        Timber.i("start load page,params:" + params.toString());

        if (mData instanceof Site || mData instanceof Topic) {
            loadLiterature(params);
        } else if (mData instanceof ChannleEntity) {
            ChannleEntity channleEntity = (ChannleEntity) mData;
            if (channleEntity.getType() == 1) {
                params.put("topics", ((ChannleEntity) mData).getName());
                loadWxArticle(params);
            } else {
                if (channleEntity.getName().equals("微信精选")) {
                    loadWxCollect(params);
                } else if (channleEntity.getName().equals("小文学")) {
//                    loadWxCollect(params);
                }

            }

        }

    }


    private void loadLiteralCollect(Map<String, String> params) {
        params.put("uid", AccountLogic.getInstance().getUserId());
        wxArticleApi.listCollected(params)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new AbsSubscriber<ApiResult<List<User2WxArticle>>>(getActivity(), false) {
                    @Override
                    protected void onError(ApiException ex) {
                        page--;
                        mRecy.setRefreshing(false);
                        mProgress.setVisibility(View.GONE);
                    }

                    @Override
                    protected void onPermissionError(ApiException ex) {
                        page--;
                        mRecy.setRefreshing(false);
                        mProgress.setVisibility(View.GONE);
                    }

                    @Override
                    public void onNext(ApiResult<List<User2WxArticle>> res) {
                        if (needToast) {
                            if (getActivity() == null || getActivity().isFinishing()) {
                                return;
                            }
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (mTvTip == null) {
                                        return;
                                    }
                                    if (res.getData().size() > 0) {
                                        TopToastHelper.showTip(mTvTip, getString(R.string.load_success), TopToastHelper.DURATION_SHORT);
                                    } else {
                                        TopToastHelper.showTip(mTvTip, getString(R.string.load_empty), TopToastHelper.DURATION_SHORT);
                                    }
                                }
                            });
                        }
                        if (queryType == 1 && res.getData().size() > 0) {
                            adapter.addAll(res.getData());
                        } else {
                            adapter.addAll(res.getData());
                        }
                    }
                });
    }

    private void loadWxCollect(Map<String, String> params) {
        params.put("uid", AccountLogic.getInstance().getUserId());
        wxArticleApi.listCollected(params)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new AbsSubscriber<ApiResult<List<User2WxArticle>>>(getActivity(), false) {
                    @Override
                    protected void onError(ApiException ex) {
                        page--;
                        mRecy.setRefreshing(false);
                        mProgress.setVisibility(View.GONE);
                    }

                    @Override
                    protected void onPermissionError(ApiException ex) {
                        page--;
                        mRecy.setRefreshing(false);
                        mProgress.setVisibility(View.GONE);
                    }

                    @Override
                    public void onNext(ApiResult<List<User2WxArticle>> res) {
                        if (needToast) {
                            if (getActivity() == null || getActivity().isFinishing()) {
                                return;
                            }
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (mTvTip == null) {
                                        return;
                                    }
                                    if (res.getData().size() > 0) {
                                        TopToastHelper.showTip(mTvTip, getString(R.string.load_success), TopToastHelper.DURATION_SHORT);
                                    } else {
                                        TopToastHelper.showTip(mTvTip, getString(R.string.load_empty), TopToastHelper.DURATION_SHORT);
                                    }
                                }
                            });
                        }
                        if (queryType == 1 && res.getData().size() > 0) {
                            adapter.addAll(res.getData());
                        } else {
                            adapter.addAll(res.getData());
                        }
                    }
                });
    }

    private void loadWxArticle(Map<String, String> params) {
        Timber.i("wx params:" + params);
        wxArticleApi.list(params)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new AbsSubscriber<ApiResult<List<WxArticle>>>(getActivity(), false) {
                    @Override
                    protected void onError(ApiException ex) {
                        page--;
                        mRecy.setRefreshing(false);
                        mProgress.setVisibility(View.GONE);
                    }

                    @Override
                    protected void onPermissionError(ApiException ex) {
                        page--;
                        mRecy.setRefreshing(false);
                        mProgress.setVisibility(View.GONE);
                    }

                    @Override
                    public void onNext(ApiResult<List<WxArticle>> res) {
                        if (needToast) {
                            if (getActivity() == null || getActivity().isFinishing()) {
                                return;
                            }
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (mTvTip == null) {
                                        return;
                                    }
                                    if (res.getData().size() > 0) {
                                        TopToastHelper.showTip(mTvTip, getString(R.string.load_success), TopToastHelper.DURATION_SHORT);
                                    } else {
                                        TopToastHelper.showTip(mTvTip, getString(R.string.load_empty), TopToastHelper.DURATION_SHORT);
                                    }
                                }
                            });
                        }
                        if (queryType == 1 && res.getData().size() > 0) {
                            adapter.addAll(res.getData());
                        } else {
                            adapter.addAll(res.getData());
                        }
                    }
                });
    }

    private void loadLiterature(Map<String, String> params) {
        articleApi.list(params)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new AbsSubscriber<ApiResult<List<Article>>>(getActivity(), false) {
                    @Override
                    protected void onError(ApiException ex) {
                        page--;
                        mRecy.setRefreshing(false);
                        mProgress.setVisibility(View.GONE);
                    }

                    @Override
                    protected void onPermissionError(ApiException ex) {
                        page--;
                        mRecy.setRefreshing(false);
                        mProgress.setVisibility(View.GONE);
                    }

                    @Override
                    public void onNext(ApiResult<List<Article>> res) {
                        if (needToast) {
                            if (getActivity() == null || getActivity().isFinishing()) {
                                return;
                            }
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (mTvTip == null) {
                                        return;
                                    }
                                    if (res.getData().size() > 0) {
                                        TopToastHelper.showTip(mTvTip, getString(R.string.load_success), TopToastHelper.DURATION_SHORT);
                                    } else {
                                        TopToastHelper.showTip(mTvTip, getString(R.string.load_empty), TopToastHelper.DURATION_SHORT);
                                    }
                                }
                            });
                        }
                        if (queryType == 1 && res.getData().size() > 0) {
                            adapter.clear();
                            adapter.addAll(res.getData());
                        } else {
                            adapter.addAll(res.getData());
                        }
                    }
                });
    }

    private static int ACTION_HOT = 1;
    private static int ACTION_RECOMMEND = 2;
    private static int ACTION_TOPIC = 3;
    private static int ACTION_SITE = 4;

    private int getAction() {
        if (mData instanceof Topic) {
            Topic topic = (Topic) mData;
            if (Constants.RESERVE_TOPIC_HOT.equalsIgnoreCase(topic.getName())) {
                return ACTION_HOT;
            } else if (Constants.RESERVE_TOPIC_RECOMMEND.equalsIgnoreCase(topic.getName())) {
                return ACTION_RECOMMEND;
            } else {
                return ACTION_TOPIC;
            }
        } else if (mData instanceof Site) {
            return ACTION_SITE;
        }
        return -1;
    }

    int queryType = -1; // 1 refresh 2 loadmore

    @Override
    public void onRefresh() {
        queryType = 1;
        loadNew();
    }

    @Override
    public void onLoadMore() {
        queryType = 2;
        loadNew();
    }
}
