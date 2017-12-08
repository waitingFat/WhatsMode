package com.whatsmode.shopify.block;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.whatsmode.library.util.RegexUtils;
import com.whatsmode.shopify.AppNavigator;
import com.whatsmode.shopify.R;
import com.whatsmode.shopify.actionlog.ActionLog;
import com.whatsmode.shopify.base.BaseActivity;
import com.whatsmode.shopify.block.cart.AndroidJs;
import com.whatsmode.shopify.block.cart.BadgeActionProvider;
import com.whatsmode.shopify.block.cart.CartItem;
import com.whatsmode.shopify.block.cart.JumpMainTab;
import com.whatsmode.shopify.block.me.ShareUtil;
import com.whatsmode.shopify.common.Constant;
import com.whatsmode.shopify.ui.helper.ToolbarHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;


public class WebActivity extends BaseActivity{

    private static final String EXTRA_URL = "url";
    private static final String EXTRA_TITLE = "title";

    public static final String STATE_SEARCH = "SEARCH"; // 搜索
    public static final String STATE_PAY = "PAY";  // 支付頁
    public static final String STATE_PRODUCT = "PRODUCT";  // 商品詳情頁
    public static final String STATE_COLLECTIONS = "COLLECTIONS"; // 網紅

    private ProgressBar mProgressBar;
    private WebView mWebView;
    private String url;
    private String title;
    private MenuItem menuItemShare;
    private MenuItem menuItemCart;
    private BadgeActionProvider mActionProvider;


    public static Intent newIntent(Context context,String title, String url) {
        Intent intent = new Intent(context, WebActivity.class);
        intent.putExtra(EXTRA_URL, url);
        intent.putExtra(EXTRA_TITLE, title);
        return intent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_web, menu);
        menuItemShare = menu.findItem(R.id.action_share);
        menuItemCart = menu.findItem(R.id.action_cart);
        mActionProvider = (BadgeActionProvider) MenuItemCompat.getActionProvider(menuItemCart);
        mActionProvider.setOnClickListener(0, what -> {
            AppNavigator.jumpToMain(this);
            EventBus.getDefault().post(new JumpMainTab(2));
            ActionLog.onEvent(Constant.Event.MY_CART);
        });// 设置点击监听。
        initToolBar();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                ShareUtil.showShare(this,EXTRA_TITLE,"",EXTRA_URL,EXTRA_URL);
                shareActionLog();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareActionLog() {
        if (STATE_PRODUCT.equals(title)) {
            ActionLog.onEvent(Constant.Event.PRODUCT_SHARE);
        } else if (STATE_COLLECTIONS.equals(title)) {
            ActionLog.onEvent(Constant.Event.SHOP_SHARE);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page_web);
        title = getIntent().getStringExtra(EXTRA_TITLE);
        ToolbarHelper.ToolbarHolder toolbarHolder = ToolbarHelper.initToolbar(this, R.id.toolbar, true, title);
        toolbarHolder.titleView.setVisibility(View.VISIBLE);
        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.getSettings().setUserAgentString(Constant.USER_AGENT);
        mProgressBar = (ProgressBar) findViewById(R.id.indeterminateBar);
        url = getIntent().getStringExtra(EXTRA_URL);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.addJavascriptInterface(new AndroidJs(), "android");//AndroidtoJS类对象映射到js的test对象
        if (!TextUtils.isEmpty(url)){
            initWebTitle();
        }
        if (STATE_PRODUCT.equals(title)) {
            EventBus.getDefault().register(this);
        }
    }

    @Subscribe
    public void receive(CartItem list) {
        if (mActionProvider != null) {runOnUiThread(() -> mActionProvider.initIcon());}
    }

    private void initToolBar() {
        // modify toolbar display according title
        switch (title) {
            case WebActivity.STATE_COLLECTIONS:
                menuItemCart.setVisible(false);
                menuItemShare.setVisible(true);
                break;
            case WebActivity.STATE_PRODUCT:
                menuItemShare.setVisible(true);
                menuItemCart.setVisible(true);
                break;
            default:
                menuItemShare.setVisible(false);
                menuItemCart.setVisible(false);
                break;
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebTitle() {
        mWebView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mProgressBar.setVisibility(View.GONE);
                initToolBar();
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                mProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                mProgressBar.setVisibility(View.GONE);
            }
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                mProgressBar.setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (RegexUtils.isProduct(url)) {
                    startActivity(WebActivity.newIntent(WebActivity.this,WebActivity.STATE_PRODUCT,url));
                } else if (RegexUtils.isCollection(url)) {
                    startActivity(WebActivity.newIntent(WebActivity.this,WebActivity.STATE_COLLECTIONS,url));
                }else{
                    view.loadUrl(url);
                }
                return true;
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return shouldOverrideUrlLoading(view, request.getUrl().toString());
            }
        });
        mWebView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
            }
        });
        mWebView.loadUrl(url);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
