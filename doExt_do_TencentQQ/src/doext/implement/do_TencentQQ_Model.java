package doext.implement;

import java.util.ArrayList;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.tencent.connect.UserInfo;
import com.tencent.connect.common.Constants;
import com.tencent.connect.share.QQShare;
import com.tencent.connect.share.QzoneShare;
import com.tencent.open.utils.ThreadManager;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import core.DoServiceContainer;
import core.helper.DoIOHelper;
import core.helper.DoJsonHelper;
import core.interfaces.DoActivityResultListener;
import core.interfaces.DoIPageView;
import core.interfaces.DoIScriptEngine;
import core.object.DoInvokeResult;
import core.object.DoSingletonModule;
import doext.app.do_TencentQQ_App;
import doext.define.do_TencentQQ_IMethod;

/**
 * 自定义扩展SM组件Model实现，继承DoSingletonModule抽象类，并实现do_TencentQQ_IMethod接口方法；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.getUniqueKey());
 */
public class do_TencentQQ_Model extends DoSingletonModule implements do_TencentQQ_IMethod, DoActivityResultListener {
	private Tencent mTencent;
	private DoIPageView doActivity;
	private DoIScriptEngine doIScriptEngine;
	private String callbackFuncName;
	private DoActivityResultListener activityResultListener;
	private Activity  activity ;
	public do_TencentQQ_Model() throws Exception {
		super();
		do_TencentQQ_App.getInstance().setModuleTypeID(getTypeID());
		activityResultListener = this;
		activity = DoServiceContainer.getPageViewFactory().getAppContext();
	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V）
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		// ...do something
		return super.invokeSyncMethod(_methodName, _dictParas, _scriptEngine, _invokeResult);
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V）
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName,
	 *                    _invokeResult);
	 *                    参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
	 *                    获取DoInvokeResult对象方式new
	 *                    DoInvokeResult(this.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		if ("login".equals(_methodName)) {
			this.login(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		} else if ("getUserInfo".equals(_methodName)) {
			this.getUserInfo(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		} else if ("logout".equals(_methodName)) {
			this.logout(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		} else if ("shareToQQ".equals(_methodName)) {
			this.shareToQQ(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		} else if ("shareToQzone".equals(_methodName)) {
			this.shareToQzone(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		}
		return super.invokeAsyncMethod(_methodName, _dictParas, _scriptEngine, _callbackFuncName);
	}

	/**
	 * 获取用户信息；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_callbackFuncName 回调函数名
	 */
	@Override
	public void getUserInfo(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		String _token = DoJsonHelper.getString(_dictParas, "token", "");
		String _expires = DoJsonHelper.getString(_dictParas, "expires", "");
		String _openId = DoJsonHelper.getString(_dictParas, "openId", "");
		Activity _activity = DoServiceContainer.getPageViewFactory().getAppContext();
		if (mTencent != null && mTencent.isSessionValid() && !TextUtils.isEmpty(_token) && !TextUtils.isEmpty(_expires) && !TextUtils.isEmpty(_openId)) {
			mTencent.setAccessToken(_token, _expires);
			mTencent.setOpenId(_openId);
			UserInfo mInfo = new UserInfo(_activity, mTencent.getQQToken());
			mInfo.getUserInfo(new GetUserInfoListener(_activity, _scriptEngine, _callbackFuncName));
		}

	}

	private class GetUserInfoListener implements IUiListener {

		private DoIScriptEngine scriptEngine;
		private String callbackFuncName;
		private DoInvokeResult invokeResult;
		private Activity activity;

		public GetUserInfoListener(Activity _activity, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
			this.scriptEngine = _scriptEngine;
			this.callbackFuncName = _callbackFuncName;
			invokeResult = new DoInvokeResult(do_TencentQQ_Model.this.getUniqueKey());
			this.activity = _activity;
		}

		@Override
		public void onCancel() {
		}

		@Override
		public void onComplete(Object response) {
			if (null == response) {
				Toast.makeText(activity, "获取用户信息失败", Toast.LENGTH_SHORT).show();
				return;
			}
			JSONObject jsonResponse = (JSONObject) response;
			if (null != jsonResponse && jsonResponse.length() == 0) {
				Toast.makeText(activity, "获取用户信息失败", Toast.LENGTH_SHORT).show();
				return;
			}
			invokeResult.setResultText(jsonResponse.toString());
			scriptEngine.callback(callbackFuncName, invokeResult);
		}

		@Override
		public void onError(UiError e) {
			invokeResult.setError("code = " + e.errorCode + "\n\t message = " + e.errorMessage + "\n\t detail = " + e.errorDetail);
			scriptEngine.callback(callbackFuncName, invokeResult);
		}

	}

	/**
	 * 使用qq登录；
	 * 
	 * @throws Exception
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_callbackFuncName 回调函数名
	 */
	@Override
	public void login(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		String _appId = DoJsonHelper.getString(_dictParas, "appId", "");
		doActivity = _scriptEngine.getCurrentPage().getPageView();
		doActivity.registActivityResultListener(this);
		doIScriptEngine = _scriptEngine;
		callbackFuncName = _callbackFuncName;
		if (TextUtils.isEmpty(_appId))
			throw new Exception("appId 不能为空");
		Activity _activity = DoServiceContainer.getPageViewFactory().getAppContext();
		if (mTencent == null) {
			mTencent = Tencent.createInstance(_appId, _activity.getApplicationContext());
		}
		if (!mTencent.isSessionValid()) {
			mTencent.login(_activity, "all", new MyLoginListener(_activity, _scriptEngine, _callbackFuncName));
		}
	}

	private class MyLoginListener implements IUiListener {

		private DoIScriptEngine scriptEngine;
		private String callbackFuncName;
		private DoInvokeResult invokeResult;
		private Activity activity;

		public MyLoginListener(Activity _activity, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
			this.scriptEngine = _scriptEngine;
			this.callbackFuncName = _callbackFuncName;
			invokeResult = new DoInvokeResult(do_TencentQQ_Model.this.getUniqueKey());
			this.activity = _activity;
		}

		@Override
		public void onCancel() {
			Toast.makeText(activity, "取消授权", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onComplete(Object response) {

			if (null == response) {
				Toast.makeText(activity, "登录失败", Toast.LENGTH_SHORT).show();
				return;
			}
			JSONObject jsonResponse = (JSONObject) response;
			if (null != jsonResponse && jsonResponse.length() == 0) {
				Toast.makeText(activity, "登录失败", Toast.LENGTH_SHORT).show();
				return;
			}
			invokeResult.setResultText(jsonResponse.toString());
			scriptEngine.callback(callbackFuncName, invokeResult);
			doActivity.unregistActivityResultListener(activityResultListener);
		}

		@Override
		public void onError(UiError e) {
			invokeResult.setError("code = " + e.errorCode + "\n\t message = " + e.errorMessage + "\n\t detail = " + e.errorDetail);
			scriptEngine.callback(callbackFuncName, invokeResult);
		}

	}

	/**
	 * 注销；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void logout(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
		DoInvokeResult _invokeResult = new DoInvokeResult(do_TencentQQ_Model.this.getUniqueKey());
		try {
			Activity _activity = DoServiceContainer.getPageViewFactory().getAppContext();
			mTencent.logout(_activity.getApplicationContext());
			_invokeResult.setResultBoolean(true);
		} catch (Exception ex) {
			_invokeResult.setResultBoolean(false);
		} finally {
			_scriptEngine.callback(_callbackFuncName, _invokeResult);
		}
	}

	@Override
	public void shareToQQ(JSONObject _dictParas, final DoIScriptEngine _scriptEngine, final String _callbackFuncName) throws Exception {
		doActivity = _scriptEngine.getCurrentPage().getPageView();
		doActivity.registActivityResultListener(this);
		String _appId = DoJsonHelper.getString(_dictParas, "appId", "");
		if (TextUtils.isEmpty(_appId))
			throw new Exception("appId 不能为空");
		final Activity _activity = DoServiceContainer.getPageViewFactory().getAppContext();
		if (mTencent == null) {
			mTencent = Tencent.createInstance(_appId, _activity.getApplicationContext());
		}

		int _shareType = DoJsonHelper.getInt(_dictParas, "type", 0); // 分享的类型  0：默认，图文分享；1：纯图分享，只支持本地图；2：音乐分享；3：应用分享
		String _title = DoJsonHelper.getString(_dictParas, "title", ""); //标题  分享的标题, 最长30个字符
		String _targetUrl = DoJsonHelper.getString(_dictParas, "url", ""); //目标地址  分享后点击文本后打开的地址
		String _imageUrl = DoJsonHelper.getString(_dictParas, "image", ""); //图片地址  分享后显示的图片
		String _summary = DoJsonHelper.getString(_dictParas, "summary", ""); //摘要  分享的消息摘要，最长40个字
		String _audioUrl = DoJsonHelper.getString(_dictParas, "audio", ""); //音乐文件的远程链接   音乐文件的远程链接, 以URL的形式传入, 不支持本地音乐
		String _appName = DoJsonHelper.getString(_dictParas, "appName", ""); //应用名称

		if (TextUtils.isEmpty(_title)) {
			_title = "share title";
		}

		final Bundle params = new Bundle();
		switch (_shareType) {
		case 1:
			params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_IMAGE);
			if (null == DoIOHelper.getHttpUrlPath(_imageUrl)) {
				_imageUrl = DoIOHelper.getLocalFileFullPath(_scriptEngine.getCurrentApp(), _imageUrl);
			} else {
				throw new Exception("纯图分享，只支持选择本地图片");
			}
			if (TextUtils.isEmpty(_imageUrl)) {
				throw new Exception("纯图分享，图片不能为空");
			}
			params.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, _imageUrl);
			break;
		case 2:
			params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_AUDIO);
			params.putString(QQShare.SHARE_TO_QQ_TARGET_URL, checkUrl(_targetUrl)); // 必须是http://开头
			params.putString(QQShare.SHARE_TO_QQ_AUDIO_URL, checkUrl(_audioUrl));
			params.putString(QQShare.SHARE_TO_QQ_TITLE, _title);
			params.putString(QQShare.SHARE_TO_QQ_SUMMARY, _summary);
			if (null == DoIOHelper.getHttpUrlPath(_imageUrl)) {
				_imageUrl = DoIOHelper.getLocalFileFullPath(_scriptEngine.getCurrentApp(), _imageUrl);
			}
			params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, _imageUrl);
			break;
		case 3:
			params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_APP);
			params.putString(QQShare.SHARE_TO_QQ_TITLE, _title);
			params.putString(QQShare.SHARE_TO_QQ_SUMMARY, _summary);
			if (null == DoIOHelper.getHttpUrlPath(_imageUrl)) {
				_imageUrl = DoIOHelper.getLocalFileFullPath(_scriptEngine.getCurrentApp(), _imageUrl);
			}
			params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, _imageUrl);
			break;
		default:
			params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
			params.putString(QQShare.SHARE_TO_QQ_TITLE, _title);
			params.putString(QQShare.SHARE_TO_QQ_TARGET_URL, checkUrl(_targetUrl)); // 必须是http://开头
			params.putString(QQShare.SHARE_TO_QQ_SUMMARY, _summary);
			if (null == DoIOHelper.getHttpUrlPath(_imageUrl)) {
				_imageUrl = DoIOHelper.getLocalFileFullPath(_scriptEngine.getCurrentApp(), _imageUrl);
			}
			params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, _imageUrl);
			break;
		}
		params.putString(QQShare.SHARE_TO_QQ_APP_NAME, _appName);
		doIScriptEngine = _scriptEngine;
		callbackFuncName = _callbackFuncName;
		// QQ分享要在主线程做
		ThreadManager.getMainHandler().post(new Runnable() {
			@Override
			public void run() {
				if (null != mTencent) {
					params.putInt(QQShare.SHARE_TO_QQ_EXT_INT, QQShare.SHARE_TO_QQ_FLAG_QZONE_ITEM_HIDE);
					mTencent.shareToQQ(_activity, params, new QQShareListener(_scriptEngine, _callbackFuncName));
				}
			}
		});

	}

	private String checkUrl(String _targetUrl) throws Exception {
		if (TextUtils.isEmpty(_targetUrl)) {
			return "http://www.deviceone.net";
		}
		if (null == DoIOHelper.getHttpUrlPath(_targetUrl)) { //没有包含http:// 开头
			_targetUrl = "http://" + _targetUrl;
		}
		return _targetUrl;
	}

	private class QQShareListener implements IUiListener {

		private DoIScriptEngine scriptEngine;
		private String callbackFuncName;
		private DoInvokeResult invokeResult;

		public QQShareListener(DoIScriptEngine _scriptEngine, String _callbackFuncName) {
			this.scriptEngine = _scriptEngine;
			this.callbackFuncName = _callbackFuncName;
			invokeResult = new DoInvokeResult(do_TencentQQ_Model.this.getUniqueKey());
		}

		@Override
		public void onCancel() {

		}

		@Override
		public void onComplete(Object arg0) {

			invokeResult.setResultBoolean(true);
			scriptEngine.callback(callbackFuncName, invokeResult);
			doActivity.unregistActivityResultListener(activityResultListener);
		}

		@Override
		public void onError(UiError arg0) {
			invokeResult.setResultBoolean(false);
			scriptEngine.callback(callbackFuncName, invokeResult);
		}
	}

	@Override
	public void shareToQzone(JSONObject _dictParas, final DoIScriptEngine _scriptEngine, final String _callbackFuncName) throws Exception {
		doActivity = _scriptEngine.getCurrentPage().getPageView();
		doActivity.registActivityResultListener(this);
		doIScriptEngine = _scriptEngine;
		callbackFuncName = _callbackFuncName;
		String _appId = DoJsonHelper.getString(_dictParas, "appId", "");
		if (TextUtils.isEmpty(_appId))
			throw new Exception("appId 不能为空");
		final Activity _activity = DoServiceContainer.getPageViewFactory().getAppContext();
		if (mTencent == null) {
			mTencent = Tencent.createInstance(_appId, _activity.getApplicationContext());
		}

		int _shareType = DoJsonHelper.getInt(_dictParas, "type", 0); // 分享的类型  0：默认，图文分享；1：应用分享
		String _title = DoJsonHelper.getString(_dictParas, "title", ""); //标题  分享的标题, 最长200个字符
		String _targetUrl = DoJsonHelper.getString(_dictParas, "url", ""); //目标地址  分享后点击文本后打开的地址
		String _imageUrl = DoJsonHelper.getString(_dictParas, "image", ""); //图片地址  分享后显示的图片
		String _summary = DoJsonHelper.getString(_dictParas, "summary", ""); //摘要  分享的消息摘要，最长600个字

		if (TextUtils.isEmpty(_title)) {
			_title = "share title";
		}
		final Bundle params = new Bundle();
		switch (_shareType) {
		case 1:
			params.putInt(QzoneShare.SHARE_TO_QZONE_KEY_TYPE, QzoneShare.SHARE_TO_QZONE_TYPE_APP);
			break;
		default:
			params.putInt(QzoneShare.SHARE_TO_QZONE_KEY_TYPE, QzoneShare.SHARE_TO_QZONE_TYPE_IMAGE_TEXT);
			break;
		}

		params.putString(QzoneShare.SHARE_TO_QQ_TITLE, _title);
		params.putString(QzoneShare.SHARE_TO_QQ_TARGET_URL, checkUrl(_targetUrl));
		ArrayList<String> _images = new ArrayList<String>();
		if (null == DoIOHelper.getHttpUrlPath(_imageUrl)) {
			_imageUrl = DoIOHelper.getLocalFileFullPath(_scriptEngine.getCurrentApp(), _imageUrl);
		}
		if (TextUtils.isEmpty(_imageUrl)) {
			throw new Exception("分享的图片不能为空");
		}
		_images.add(_imageUrl);
		params.putStringArrayList(QzoneShare.SHARE_TO_QQ_IMAGE_URL, _images);
		params.putString(QzoneShare.SHARE_TO_QQ_SUMMARY, _summary);

		// QZone分享要在主线程做
		ThreadManager.getMainHandler().post(new Runnable() {
			@Override
			public void run() {
				if (null != mTencent) {
					mTencent.shareToQzone(_activity, params, new QQShareListener(_scriptEngine, _callbackFuncName));
				}
			}
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == Constants.REQUEST_QQ_SHARE) {
			Tencent.onActivityResultData(requestCode, resultCode, intent, new QQShareListener(doIScriptEngine, callbackFuncName));
		} else if (requestCode == Constants.REQUEST_QZONE_SHARE) {
			Tencent.onActivityResultData(requestCode, resultCode, intent, new QQShareListener(doIScriptEngine, callbackFuncName));
		} else if (requestCode == Constants.REQUEST_LOGIN || requestCode == Constants.REQUEST_APPBAR) {
			Tencent.onActivityResultData(requestCode, resultCode, intent, new MyLoginListener(activity, doIScriptEngine, callbackFuncName));
		}
	}
}